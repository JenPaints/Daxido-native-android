package com.daxido.core.maps

import com.daxido.core.models.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectionsService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val url = request.url.newBuilder()
                .addQueryParameter("key", getApiKey())
                .build()
            chain.proceed(request.newBuilder().url(url).build())
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(DirectionsApi::class.java)

    interface DirectionsApi {
        @GET("maps/api/directions/json")
        suspend fun getDirections(
            @Query("origin") origin: String,
            @Query("destination") destination: String,
            @Query("mode") mode: String = "driving",
            @Query("alternatives") alternatives: Boolean = true,
            @Query("departure_time") departureTime: String = "now",
            @Query("traffic_model") trafficModel: String = "best_guess"
        ): DirectionsResponse
    }

    data class DirectionsResponse(
        val routes: List<Route>,
        val status: String
    )

    data class Route(
        val overview_polyline: OverviewPolyline,
        val legs: List<Leg>,
        val bounds: Bounds,
        val summary: String,
        val warnings: List<String>,
        val waypoint_order: List<Int>
    )

    data class OverviewPolyline(
        val points: String
    )

    data class Leg(
        val distance: Distance,
        val duration: Duration,
        val duration_in_traffic: DurationInTraffic?,
        val end_address: String,
        val end_location: LatLngLiteral,
        val start_address: String,
        val start_location: LatLngLiteral,
        val steps: List<Step>,
        val via_waypoint: List<ViaWaypoint>
    )

    data class Step(
        val distance: Distance,
        val duration: Duration,
        val end_location: LatLngLiteral,
        val html_instructions: String,
        val polyline: StepPolyline,
        val start_location: LatLngLiteral,
        val travel_mode: String,
        val maneuver: String?
    )

    data class StepPolyline(
        val points: String
    )

    data class Distance(
        val text: String,
        val value: Int
    )

    data class Duration(
        val text: String,
        val value: Int
    )

    data class DurationInTraffic(
        val text: String,
        val value: Int
    )

    data class LatLngLiteral(
        val lat: Double,
        val lng: Double
    )

    data class Bounds(
        val northeast: LatLngLiteral,
        val southwest: LatLngLiteral
    )

    data class ViaWaypoint(
        val location: LatLngLiteral,
        val step_index: Int,
        val step_interpolation: Double
    )

    data class RouteInfo(
        val polylinePoints: List<LatLng>,
        val encodedPolyline: String,
        val distance: Double, // in km
        val duration: Int, // in minutes
        val durationInTraffic: Int?, // in minutes with traffic
        val steps: List<NavigationStep>,
        val bounds: RouteBounds
    )

    data class NavigationStep(
        val instruction: String,
        val distance: String,
        val duration: String,
        val maneuver: String?,
        val polylinePoints: List<LatLng>
    )

    data class RouteBounds(
        val northeast: LatLng,
        val southwest: LatLng
    )

    suspend fun getRoute(
        origin: Location,
        destination: Location,
        vehicleType: String = "car"
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            val mode = when (vehicleType) {
                "BIKE" -> "two_wheeler"
                "PREMIUM", "CAR", "AUTO" -> "driving"
                else -> "driving"
            }

            val originStr = "${origin.latitude},${origin.longitude}"
            val destinationStr = "${destination.latitude},${destination.longitude}"

            val response = api.getDirections(
                origin = originStr,
                destination = destinationStr,
                mode = if (mode == "two_wheeler") "driving" else mode,
                alternatives = true,
                departureTime = "now",
                trafficModel = "best_guess"
            )

            if (response.status == "OK" && response.routes.isNotEmpty()) {
                // Select best route based on traffic
                val bestRoute = selectBestRoute(response.routes)
                val routeInfo = parseRoute(bestRoute)
                Result.success(routeInfo)
            } else {
                Result.failure(Exception("No routes found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun selectBestRoute(routes: List<Route>): Route {
        // Select route with shortest duration in traffic
        return routes.minByOrNull { route ->
            route.legs.sumOf { leg ->
                leg.duration_in_traffic?.value ?: leg.duration.value
            }
        } ?: routes.first()
    }

    private fun parseRoute(route: Route): RouteInfo {
        val polylinePoints = PolyUtil.decode(route.overview_polyline.points)

        val totalDistance = route.legs.sumOf { it.distance.value } / 1000.0 // Convert to km
        val totalDuration = route.legs.sumOf { it.duration.value } / 60 // Convert to minutes
        val totalDurationInTraffic = route.legs.sumOf {
            it.duration_in_traffic?.value ?: it.duration.value
        } / 60

        val navigationSteps = route.legs.flatMap { leg ->
            leg.steps.map { step ->
                NavigationStep(
                    instruction = step.html_instructions.replace(Regex("<[^>]*>"), ""),
                    distance = step.distance.text,
                    duration = step.duration.text,
                    maneuver = step.maneuver,
                    polylinePoints = PolyUtil.decode(step.polyline.points)
                )
            }
        }

        val bounds = RouteBounds(
            northeast = LatLng(route.bounds.northeast.lat, route.bounds.northeast.lng),
            southwest = LatLng(route.bounds.southwest.lat, route.bounds.southwest.lng)
        )

        return RouteInfo(
            polylinePoints = polylinePoints,
            encodedPolyline = route.overview_polyline.points,
            distance = totalDistance,
            duration = totalDuration,
            durationInTraffic = totalDurationInTraffic,
            steps = navigationSteps,
            bounds = bounds
        )
    }

    suspend fun getMultipleRoutes(
        waypoints: List<Location>
    ): Result<List<RouteInfo>> = withContext(Dispatchers.IO) {
        try {
            val routes = mutableListOf<RouteInfo>()

            for (i in 0 until waypoints.size - 1) {
                val origin = waypoints[i]
                val destination = waypoints[i + 1]

                val routeResult = getRoute(origin, destination)
                if (routeResult.isSuccess) {
                    routeResult.getOrNull()?.let { routes.add(it) }
                }
            }

            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOptimizedRoute(
        origin: Location,
        destination: Location,
        waypoints: List<Location>
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            val waypointsStr = waypoints.joinToString("|") {
                "${it.latitude},${it.longitude}"
            }

            val originStr = "${origin.latitude},${origin.longitude}"
            val destinationStr = "${destination.latitude},${destination.longitude}"

            // Call API with optimize waypoints
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$originStr&destination=$destinationStr&" +
                    "waypoints=optimize:true|$waypointsStr&key=${getApiKey()}"

            // Parse and return optimized route
            // Implementation details...

            getRoute(origin, destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculatePolylineDistance(polylinePoints: List<LatLng>): Double {
        var distance = 0.0
        for (i in 0 until polylinePoints.size - 1) {
            val p1 = polylinePoints[i]
            val p2 = polylinePoints[i + 1]
            distance += calculateHaversineDistance(
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude
            )
        }
        return distance
    }

    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun getApiKey(): String {
        // This should be retrieved from BuildConfig or secure storage
        return "YOUR_GOOGLE_MAPS_API_KEY"
    }

    // Real-time traffic updates
    suspend fun getTrafficUpdate(
        currentLocation: Location,
        destination: Location
    ): Result<TrafficInfo> = withContext(Dispatchers.IO) {
        try {
            val route = getRoute(currentLocation, destination)
            route.fold(
                onSuccess = { routeInfo ->
                    val trafficInfo = TrafficInfo(
                        currentSpeed = calculateCurrentSpeed(currentLocation),
                        averageSpeed = calculateAverageSpeed(routeInfo),
                        congestionLevel = calculateCongestionLevel(routeInfo),
                        incidents = getTrafficIncidents(routeInfo),
                        alternativeRoutes = getAlternativeRoutes(currentLocation, destination)
                    )
                    Result.success(trafficInfo)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class TrafficInfo(
        val currentSpeed: Float,
        val averageSpeed: Float,
        val congestionLevel: CongestionLevel,
        val incidents: List<TrafficIncident>,
        val alternativeRoutes: List<RouteInfo>
    )

    enum class CongestionLevel {
        NONE, LIGHT, MODERATE, HEAVY, SEVERE
    }

    data class TrafficIncident(
        val type: String,
        val severity: String,
        val description: String,
        val location: LatLng
    )

    private fun calculateCurrentSpeed(location: Location): Float {
        // Calculate based on recent location updates
        return 30.0f // km/h placeholder
    }

    private fun calculateAverageSpeed(routeInfo: RouteInfo): Float {
        return if (routeInfo.durationInTraffic != null && routeInfo.durationInTraffic > 0) {
            (routeInfo.distance / routeInfo.durationInTraffic * 60).toFloat()
        } else {
            (routeInfo.distance / routeInfo.duration * 60).toFloat()
        }
    }

    private fun calculateCongestionLevel(routeInfo: RouteInfo): CongestionLevel {
        val normalDuration = routeInfo.duration
        val trafficDuration = routeInfo.durationInTraffic ?: normalDuration

        val delayRatio = (trafficDuration - normalDuration).toFloat() / normalDuration

        return when {
            delayRatio < 0.1 -> CongestionLevel.NONE
            delayRatio < 0.25 -> CongestionLevel.LIGHT
            delayRatio < 0.5 -> CongestionLevel.MODERATE
            delayRatio < 0.75 -> CongestionLevel.HEAVY
            else -> CongestionLevel.SEVERE
        }
    }

    private suspend fun getTrafficIncidents(routeInfo: RouteInfo): List<TrafficIncident> {
        // This would integrate with traffic incident APIs
        return emptyList()
    }

    private suspend fun getAlternativeRoutes(
        origin: Location,
        destination: Location
    ): List<RouteInfo> {
        // Fetch alternative routes
        return emptyList()
    }
}