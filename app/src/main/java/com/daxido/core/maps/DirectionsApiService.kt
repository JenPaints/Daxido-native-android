package com.daxido.core.maps

import android.content.Context
import android.util.Log
import com.daxido.core.config.AppConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Directions API service for route calculation and navigation
 */
@Singleton
class DirectionsApiService @Inject constructor(
    private val context: Context
) {
    
    private val apiKey = AppConfig.GOOGLE_DIRECTIONS_API_KEY
    private val baseUrl = "https://maps.googleapis.com/maps/api/directions/json"
    
    /**
     * Get directions between two points
     */
    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList(),
        mode: TravelMode = TravelMode.DRIVING,
        avoid: List<AvoidType> = emptyList(),
        trafficModel: TrafficModel = TrafficModel.BEST_GUESS,
        departureTime: Long? = null
    ): DirectionsResult? = withContext(Dispatchers.IO) {
        try {
            val url = buildDirectionsUrl(origin, destination, waypoints, mode, avoid, trafficModel, departureTime)
            val response = makeHttpRequest(url)
            parseDirectionsResponse(response)
        } catch (e: Exception) {
            Log.e("DirectionsApiService", "Error getting directions", e)
            null
        }
    }
    
    /**
     * Get ETA (Estimated Time of Arrival)
     */
    suspend fun getETA(
        origin: LatLng,
        destination: LatLng,
        mode: TravelMode = TravelMode.DRIVING,
        departureTime: Long? = null
    ): ETAResult? = withContext(Dispatchers.IO) {
        try {
            val directions = getDirections(origin, destination, mode = mode, departureTime = departureTime)
            directions?.let {
                ETAResult(
                    duration = it.routes.firstOrNull()?.legs?.firstOrNull()?.duration?.value ?: 0,
                    durationInTraffic = it.routes.firstOrNull()?.legs?.firstOrNull()?.durationInTraffic?.value ?: 0,
                    distance = it.routes.firstOrNull()?.legs?.firstOrNull()?.distance?.value ?: 0,
                    polyline = it.routes.firstOrNull()?.polyline ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("DirectionsApiService", "Error getting ETA", e)
            null
        }
    }
    
    /**
     * Get distance between two points
     */
    suspend fun getDistance(
        origin: LatLng,
        destination: LatLng,
        mode: TravelMode = TravelMode.DRIVING
    ): DistanceResult? = withContext(Dispatchers.IO) {
        try {
            val eta = getETA(origin, destination, mode)
            eta?.let {
                DistanceResult(
                    distance = it.distance,
                    duration = it.duration,
                    status = "OK"
                )
            }
        } catch (e: Exception) {
            Log.e("DirectionsApiService", "Error getting distance", e)
            null
        }
    }
    
    /**
     * Get alternative routes
     */
    suspend fun getAlternativeRoutes(
        origin: LatLng,
        destination: LatLng,
        alternatives: Boolean = true
    ): List<Route>? = withContext(Dispatchers.IO) {
        try {
            val directions = getDirections(origin, destination)
            directions?.routes ?: emptyList()
        } catch (e: Exception) {
            Log.e("DirectionsApiService", "Error getting alternative routes", e)
            null
        }
    }
    
    private fun buildDirectionsUrl(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng>,
        mode: TravelMode,
        avoid: List<AvoidType>,
        trafficModel: TrafficModel,
        departureTime: Long?
    ): String {
        val url = StringBuilder(baseUrl)
        url.append("?origin=${origin.latitude},${origin.longitude}")
        url.append("&destination=${destination.latitude},${destination.longitude}")
        
        if (waypoints.isNotEmpty()) {
            url.append("&waypoints=")
            waypoints.forEachIndexed { index, waypoint ->
                if (index > 0) url.append("|")
                url.append("${waypoint.latitude},${waypoint.longitude}")
            }
        }
        
        url.append("&mode=${mode.value}")
        url.append("&key=$apiKey")
        
        if (avoid.isNotEmpty()) {
            url.append("&avoid=")
            avoid.forEachIndexed { index, avoidType ->
                if (index > 0) url.append("|")
                url.append(avoidType.value)
            }
        }
        
        if (departureTime != null) {
            url.append("&departure_time=$departureTime")
            url.append("&traffic_model=${trafficModel.value}")
        }
        
        return url.toString()
    }
    
    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            
            reader.close()
            inputStream.close()
            connection.disconnect()
            
            return response.toString()
        } else {
            connection.disconnect()
            throw Exception("HTTP error: $responseCode")
        }
    }
    
    private fun parseDirectionsResponse(response: String): DirectionsResult? {
        return try {
            val json = JSONObject(response)
            val status = json.getString("status")
            
            if (status == "OK") {
                val routesArray = json.getJSONArray("routes")
                val routes = mutableListOf<Route>()
                
                for (i in 0 until routesArray.length()) {
                    val routeJson = routesArray.getJSONObject(i)
                    val route = parseRoute(routeJson)
                    routes.add(route)
                }
                
                DirectionsResult(
                    routes = routes,
                    status = status
                )
            } else {
                Log.e("DirectionsApiService", "Directions API error: $status")
                null
            }
        } catch (e: Exception) {
            Log.e("DirectionsApiService", "Error parsing directions response", e)
            null
        }
    }
    
    private fun parseRoute(routeJson: JSONObject): Route {
        val legsArray = routeJson.getJSONArray("legs")
        val legs = mutableListOf<Leg>()
        
        for (i in 0 until legsArray.length()) {
            val legJson = legsArray.getJSONObject(i)
            val leg = parseLeg(legJson)
            legs.add(leg)
        }
        
        val overviewPolyline = routeJson.getJSONObject("overview_polyline")
        val polyline = overviewPolyline.getString("points")
        
        return Route(
            legs = legs,
            polyline = polyline,
            summary = routeJson.optString("summary", ""),
            warnings = emptyList(),
            waypointOrder = emptyList()
        )
    }
    
    private fun parseLeg(legJson: JSONObject): Leg {
        val distance = legJson.getJSONObject("distance")
        val duration = legJson.getJSONObject("duration")
        val durationInTraffic = legJson.optJSONObject("duration_in_traffic")
        
        val startLocation = legJson.getJSONObject("start_location")
        val endLocation = legJson.getJSONObject("end_location")
        
        val stepsArray = legJson.getJSONArray("steps")
        val steps = mutableListOf<Step>()
        
        for (i in 0 until stepsArray.length()) {
            val stepJson = stepsArray.getJSONObject(i)
            val step = parseStep(stepJson)
            steps.add(step)
        }
        
        return Leg(
            distance = Distance(
                text = distance.getString("text"),
                value = distance.getInt("value")
            ),
            duration = Duration(
                text = duration.getString("text"),
                value = duration.getInt("value")
            ),
            durationInTraffic = durationInTraffic?.let {
                Duration(
                    text = it.getString("text"),
                    value = it.getInt("value")
                )
            },
            startLocation = LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng")),
            endLocation = LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng")),
            steps = steps
        )
    }
    
    private fun parseStep(stepJson: JSONObject): Step {
        val distance = stepJson.getJSONObject("distance")
        val duration = stepJson.getJSONObject("duration")
        val startLocation = stepJson.getJSONObject("start_location")
        val endLocation = stepJson.getJSONObject("end_location")
        
        return Step(
            distance = Distance(
                text = distance.getString("text"),
                value = distance.getInt("value")
            ),
            duration = Duration(
                text = duration.getString("text"),
                value = duration.getInt("value")
            ),
            startLocation = LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng")),
            endLocation = LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng")),
            htmlInstructions = stepJson.getString("html_instructions"),
            polyline = stepJson.getJSONObject("polyline").getString("points"),
            travelMode = stepJson.getString("travel_mode")
        )
    }
}

/**
 * Travel modes for directions
 */
enum class TravelMode(val value: String) {
    DRIVING("driving"),
    WALKING("walking"),
    BICYCLING("bicycling"),
    TRANSIT("transit")
}

/**
 * Avoid types for directions
 */
enum class AvoidType(val value: String) {
    TOLLS("tolls"),
    HIGHWAYS("highways"),
    FERRIES("ferries"),
    INDOOR("indoor")
}

/**
 * Traffic models for ETA calculation
 */
enum class TrafficModel(val value: String) {
    BEST_GUESS("best_guess"),
    PESSIMISTIC("pessimistic"),
    OPTIMISTIC("optimistic")
}

/**
 * Data classes for directions API responses
 */
data class DirectionsResult(
    val routes: List<Route>,
    val status: String
)

data class Route(
    val legs: List<Leg>,
    val polyline: String,
    val summary: String,
    val warnings: List<String>,
    val waypointOrder: List<Int>
)

data class Leg(
    val distance: Distance,
    val duration: Duration,
    val durationInTraffic: Duration?,
    val startLocation: LatLng,
    val endLocation: LatLng,
    val steps: List<Step>
)

data class Step(
    val distance: Distance,
    val duration: Duration,
    val startLocation: LatLng,
    val endLocation: LatLng,
    val htmlInstructions: String,
    val polyline: String,
    val travelMode: String
)

data class Distance(
    val text: String,
    val value: Int
)

data class Duration(
    val text: String,
    val value: Int
)

data class DistanceResult(
    val distance: Int,
    val duration: Int,
    val status: String
)
