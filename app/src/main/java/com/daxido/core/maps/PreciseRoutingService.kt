package com.daxido.core.maps

import android.content.Context
import android.util.Log
import com.daxido.core.config.AppConfig
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Directions API service with precise polyline routing
 */
@Singleton
class PreciseRoutingService @Inject constructor(
    private val context: Context
) {
    
    private val apiKey = AppConfig.GOOGLE_DIRECTIONS_API_KEY
    private val baseUrl = "https://maps.googleapis.com/maps/api/directions/json"
    
    /**
     * Get precise directions with detailed polyline routing
     */
    suspend fun getPreciseDirections(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList(),
        mode: String = "driving",
        alternatives: Boolean = true,
        avoidTolls: Boolean = false,
        avoidHighways: Boolean = false
    ): PreciseRouteResult? = withContext(Dispatchers.IO) {
        try {
            val urlString = buildDirectionsUrl(
                origin, destination, waypoints, mode, alternatives, avoidTolls, avoidHighways
            )
            
            val response = makeHttpRequest(urlString)
            return@withContext parsePreciseDirectionsResponse(response)
        } catch (e: Exception) {
            Log.e("PreciseRoutingService", "Error fetching precise directions: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Get real-time traffic-aware directions
     */
    suspend fun getTrafficAwareDirections(
        origin: LatLng,
        destination: LatLng,
        departureTime: Long = System.currentTimeMillis()
    ): PreciseRouteResult? = withContext(Dispatchers.IO) {
        try {
            val urlString = "$baseUrl?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&departure_time=$departureTime" +
                    "&traffic_model=best_guess" +
                    "&key=$apiKey"
            
            val response = makeHttpRequest(urlString)
            return@withContext parsePreciseDirectionsResponse(response)
        } catch (e: Exception) {
            Log.e("PreciseRoutingService", "Error fetching traffic-aware directions: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Decode polyline to list of LatLng points
     */
    fun decodePolyline(encodedPolyline: String): List<LatLng> {
        return try {
            PolyUtil.decode(encodedPolyline)
        } catch (e: Exception) {
            Log.e("PreciseRoutingService", "Error decoding polyline: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Calculate distance between two points along a route
     */
    fun calculateRouteDistance(polyline: String): Double {
        val points = decodePolyline(polyline)
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until points.size) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                points[i-1].latitude, points[i-1].longitude,
                points[i].latitude, points[i].longitude,
                results
            )
            totalDistance += results[0]
        }
        return totalDistance
    }
    
    /**
     * Get estimated time of arrival considering traffic
     */
    suspend fun getETA(
        origin: LatLng,
        destination: LatLng,
        departureTime: Long = System.currentTimeMillis()
    ): ETAResult? = withContext(Dispatchers.IO) {
        try {
            val route = getTrafficAwareDirections(origin, destination, departureTime)
            return@withContext route?.let {
                ETAResult(
                    duration = it.duration,
                    durationInTraffic = it.durationInTraffic,
                    distance = it.distance,
                    polyline = it.polyline
                )
            }
        } catch (e: Exception) {
            Log.e("PreciseRoutingService", "Error calculating ETA: ${e.message}", e)
            return@withContext null
        }
    }
    
    private fun buildDirectionsUrl(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng>,
        mode: String,
        alternatives: Boolean,
        avoidTolls: Boolean,
        avoidHighways: Boolean
    ): String {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destStr = "${destination.latitude},${destination.longitude}"
        
        val waypointsStr = if (waypoints.isNotEmpty()) {
            "&waypoints=" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        } else ""
        
        val avoidStr = buildString {
            if (avoidTolls) append("tolls|")
            if (avoidHighways) append("highways|")
            if (isNotEmpty()) {
                setLength(length - 1) // Remove last |
                insert(0, "&avoid=")
            }
        }
        
        return "$baseUrl?origin=$originStr&destination=$destStr&mode=$mode" +
                "&alternatives=$alternatives$waypointsStr$avoidStr&key=$apiKey"
    }
    
    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
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
    
    private fun parsePreciseDirectionsResponse(jsonResponse: String): PreciseRouteResult? {
        try {
            val json = JSONObject(jsonResponse)
            if (json.getString("status") != "OK") {
                Log.e("PreciseRoutingService", "Directions API status not OK: ${json.getString("status")}")
                return null
            }
            
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return null
            }
            
            val firstRoute = routes.getJSONObject(0)
            val legs = firstRoute.getJSONArray("legs")
            if (legs.length() == 0) {
                return null
            }
            
            val firstLeg = legs.getJSONObject(0)
            val distance = firstLeg.getJSONObject("distance").getInt("value")
            val duration = firstLeg.getJSONObject("duration").getInt("value")
            val durationInTraffic = firstLeg.optJSONObject("duration_in_traffic")?.getInt("value") ?: duration
            val polyline = firstRoute.getJSONObject("overview_polyline").getString("points")
            
            // Parse detailed steps for turn-by-turn navigation
            val steps = mutableListOf<RouteStep>()
            val stepsArray = firstLeg.getJSONArray("steps")
            for (i in 0 until stepsArray.length()) {
                val step = stepsArray.getJSONObject(i)
                val stepPolyline = step.getJSONObject("polyline").getString("points")
                val stepDistance = step.getJSONObject("distance").getInt("value")
                val stepDuration = step.getJSONObject("duration").getInt("value")
                val instruction = step.getString("html_instructions")
                
                steps.add(
                    RouteStep(
                        instruction = instruction,
                        distance = stepDistance,
                        duration = stepDuration,
                        polyline = stepPolyline,
                        startLocation = LatLng(
                            step.getJSONObject("start_location").getDouble("lat"),
                            step.getJSONObject("start_location").getDouble("lng")
                        ),
                        endLocation = LatLng(
                            step.getJSONObject("end_location").getDouble("lat"),
                            step.getJSONObject("end_location").getDouble("lng")
                        )
                    )
                )
            }
            
            return PreciseRouteResult(
                distance = distance,
                duration = duration,
                durationInTraffic = durationInTraffic,
                polyline = polyline,
                steps = steps,
                summary = firstRoute.getString("summary")
            )
        } catch (e: Exception) {
            Log.e("PreciseRoutingService", "Error parsing directions response: ${e.message}", e)
            return null
        }
    }
}

data class PreciseRouteResult(
    val distance: Int, // in meters
    val duration: Int, // in seconds
    val durationInTraffic: Int, // in seconds
    val polyline: String,
    val steps: List<RouteStep>,
    val summary: String
)

data class RouteStep(
    val instruction: String,
    val distance: Int, // in meters
    val duration: Int, // in seconds
    val polyline: String,
    val startLocation: LatLng,
    val endLocation: LatLng
)

data class ETAResult(
    val duration: Int, // in seconds
    val durationInTraffic: Int, // in seconds
    val distance: Int, // in meters
    val polyline: String
)
