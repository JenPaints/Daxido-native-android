package com.daxido.core.maps

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class MapMatchingService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://roads.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val roadsApi = retrofit.create(RoadsApi::class.java)

    interface RoadsApi {
        @GET("v1/snapToRoads")
        suspend fun snapToRoads(
            @Query("path") path: String,
            @Query("interpolate") interpolate: Boolean = true,
            @Query("key") apiKey: String
        ): SnapToRoadsResponse

        @GET("v1/nearestRoads")
        suspend fun nearestRoads(
            @Query("points") points: String,
            @Query("key") apiKey: String
        ): NearestRoadsResponse

        @GET("v1/speedLimits")
        suspend fun getSpeedLimits(
            @Query("path") path: String,
            @Query("placeId") placeIds: String? = null,
            @Query("units") units: String = "KPH",
            @Query("key") apiKey: String
        ): SpeedLimitsResponse
    }

    data class SnapToRoadsResponse(
        val snappedPoints: List<SnappedPoint>,
        val warningMessage: String?
    )

    data class SnappedPoint(
        val location: LatLngLiteral,
        val originalIndex: Int?,
        val placeId: String
    )

    data class LatLngLiteral(
        val latitude: Double,
        val longitude: Double
    )

    data class NearestRoadsResponse(
        val snappedPoints: List<SnappedPoint>
    )

    data class SpeedLimitsResponse(
        val speedLimits: List<SpeedLimit>,
        val snappedPoints: List<SnappedPoint>
    )

    data class SpeedLimit(
        val placeId: String,
        val speedLimit: Float,
        val units: String
    )

    data class MatchedLocation(
        val originalLocation: LatLng,
        val snappedLocation: LatLng,
        val roadPlaceId: String,
        val confidence: Float,
        val bearing: Float,
        val speedLimit: Float?,
        val roadType: RoadType,
        val lanePosition: LanePosition?
    )

    enum class RoadType {
        HIGHWAY, ARTERIAL, LOCAL, SERVICE, UNKNOWN
    }

    data class LanePosition(
        val laneNumber: Int,
        val totalLanes: Int,
        val laneWidth: Float,
        val offsetFromCenter: Float
    )

    private val locationCache = mutableMapOf<String, MatchedLocation>()
    private val speedLimitCache = mutableMapOf<String, Float>()

    suspend fun matchToRoad(
        locations: List<LatLng>,
        includeSpeedLimits: Boolean = true
    ): Result<List<MatchedLocation>> = withContext(Dispatchers.IO) {
        try {
            // Prepare path string
            val pathString = locations.joinToString("|") { "${it.latitude},${it.longitude}" }

            // Snap to roads
            val snapResponse = roadsApi.snapToRoads(
                path = pathString,
                interpolate = true,
                apiKey = getApiKey()
            )

            val matchedLocations = mutableListOf<MatchedLocation>()

            // Get speed limits if requested
            val speedLimits = if (includeSpeedLimits) {
                val placeIds = snapResponse.snappedPoints.map { it.placeId }.distinct()
                getSpeedLimitsForPlaces(placeIds)
            } else {
                emptyMap()
            }

            // Process snapped points
            snapResponse.snappedPoints.forEach { snappedPoint ->
                val originalIndex = snappedPoint.originalIndex ?: 0
                val originalLocation = if (originalIndex < locations.size) {
                    locations[originalIndex]
                } else {
                    locations.last()
                }

                val snappedLatLng = LatLng(
                    snappedPoint.location.latitude,
                    snappedPoint.location.longitude
                )

                val matchedLocation = MatchedLocation(
                    originalLocation = originalLocation,
                    snappedLocation = snappedLatLng,
                    roadPlaceId = snappedPoint.placeId,
                    confidence = calculateSnapConfidence(originalLocation, snappedLatLng),
                    bearing = calculateBearing(originalLocation, snappedLatLng),
                    speedLimit = speedLimits[snappedPoint.placeId],
                    roadType = inferRoadType(snappedPoint.placeId, speedLimits[snappedPoint.placeId]),
                    lanePosition = estimateLanePosition(originalLocation, snappedLatLng)
                )

                matchedLocations.add(matchedLocation)
                cacheLocation(matchedLocation)
            }

            Result.success(matchedLocations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun snapSinglePoint(location: LatLng): Result<MatchedLocation> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cacheKey = "${location.latitude},${location.longitude}"
            locationCache[cacheKey]?.let {
                return@withContext Result.success(it)
            }

            // Find nearest road
            val response = roadsApi.nearestRoads(
                points = "${location.latitude},${location.longitude}",
                apiKey = getApiKey()
            )

            if (response.snappedPoints.isNotEmpty()) {
                val snappedPoint = response.snappedPoints.first()
                val snappedLatLng = LatLng(
                    snappedPoint.location.latitude,
                    snappedPoint.location.longitude
                )

                val matchedLocation = MatchedLocation(
                    originalLocation = location,
                    snappedLocation = snappedLatLng,
                    roadPlaceId = snappedPoint.placeId,
                    confidence = calculateSnapConfidence(location, snappedLatLng),
                    bearing = calculateBearing(location, snappedLatLng),
                    speedLimit = getSpeedLimitForPlace(snappedPoint.placeId),
                    roadType = inferRoadType(snappedPoint.placeId, null),
                    lanePosition = estimateLanePosition(location, snappedLatLng)
                )

                cacheLocation(matchedLocation)
                Result.success(matchedLocation)
            } else {
                Result.failure(Exception("No roads found nearby"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getSpeedLimitsForPlaces(placeIds: List<String>): Map<String, Float> {
        return try {
            val placeIdsString = placeIds.joinToString(",")
            val response = roadsApi.getSpeedLimits(
                path = "",
                placeIds = placeIdsString,
                units = "KPH",
                apiKey = getApiKey()
            )

            response.speedLimits.associate { it.placeId to it.speedLimit }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun getSpeedLimitForPlace(placeId: String): Float? {
        // Check cache
        speedLimitCache[placeId]?.let { return it }

        return try {
            val response = roadsApi.getSpeedLimits(
                path = "",
                placeIds = placeId,
                units = "KPH",
                apiKey = getApiKey()
            )

            response.speedLimits.firstOrNull()?.speedLimit?.also {
                speedLimitCache[placeId] = it
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateSnapConfidence(original: LatLng, snapped: LatLng): Float {
        val distance = SphericalUtil.computeDistanceBetween(original, snapped)

        return when {
            distance <= 5 -> 1.0f
            distance <= 10 -> 0.9f
            distance <= 20 -> 0.8f
            distance <= 30 -> 0.7f
            distance <= 50 -> 0.5f
            else -> 0.3f
        }
    }

    private fun calculateBearing(from: LatLng, to: LatLng): Float {
        return SphericalUtil.computeHeading(from, to).toFloat()
    }

    private fun inferRoadType(placeId: String, speedLimit: Float?): RoadType {
        // Infer road type based on place ID pattern and speed limit
        return when {
            speedLimit != null && speedLimit >= 100 -> RoadType.HIGHWAY
            speedLimit != null && speedLimit >= 60 -> RoadType.ARTERIAL
            speedLimit != null && speedLimit >= 30 -> RoadType.LOCAL
            placeId.contains("highway") -> RoadType.HIGHWAY
            placeId.contains("service") -> RoadType.SERVICE
            else -> RoadType.UNKNOWN
        }
    }

    private fun estimateLanePosition(original: LatLng, snapped: LatLng): LanePosition? {
        val distance = SphericalUtil.computeDistanceBetween(original, snapped)

        if (distance > 20) return null // Too far to estimate

        // Estimate based on offset
        val bearing = SphericalUtil.computeHeading(snapped, original)
        val perpendicular = (bearing + 90) % 360

        // Standard lane width is approximately 3.5 meters
        val laneWidth = 3.5f
        val offset = distance.toFloat()
        val estimatedLane = (offset / laneWidth).roundToInt()

        return LanePosition(
            laneNumber = estimatedLane.coerceIn(1, 4),
            totalLanes = 3, // Default assumption
            laneWidth = laneWidth,
            offsetFromCenter = offset
        )
    }

    private fun cacheLocation(location: MatchedLocation) {
        val key = "${location.originalLocation.latitude},${location.originalLocation.longitude}"
        locationCache[key] = location

        // Limit cache size
        if (locationCache.size > 1000) {
            locationCache.entries.take(500).forEach { locationCache.remove(it.key) }
        }
    }

    fun interpolateRoute(
        snapppedPoints: List<MatchedLocation>,
        targetFrequency: Int = 10 // Hz
    ): List<MatchedLocation> {
        val interpolated = mutableListOf<MatchedLocation>()

        for (i in 0 until snapppedPoints.size - 1) {
            val start = snapppedPoints[i]
            val end = snapppedPoints[i + 1]

            interpolated.add(start)

            // Calculate number of intermediate points needed
            val distance = SphericalUtil.computeDistanceBetween(
                start.snappedLocation,
                end.snappedLocation
            )

            val timeSeconds = distance / 15.0 // Assume 15 m/s average speed
            val numPoints = (timeSeconds * targetFrequency).toInt()

            if (numPoints > 1) {
                for (j in 1 until numPoints) {
                    val fraction = j.toDouble() / numPoints
                    val interpolatedPoint = SphericalUtil.interpolate(
                        start.snappedLocation,
                        end.snappedLocation,
                        fraction
                    )

                    interpolated.add(
                        MatchedLocation(
                            originalLocation = interpolatedPoint,
                            snappedLocation = interpolatedPoint,
                            roadPlaceId = start.roadPlaceId,
                            confidence = (start.confidence + end.confidence) / 2,
                            bearing = SphericalUtil.computeHeading(
                                start.snappedLocation,
                                end.snappedLocation
                            ).toFloat(),
                            speedLimit = start.speedLimit,
                            roadType = start.roadType,
                            lanePosition = start.lanePosition
                        )
                    )
                }
            }
        }

        interpolated.add(snapppedPoints.last())
        return interpolated
    }

    suspend fun detectOffRoute(
        currentLocation: LatLng,
        expectedRoute: List<LatLng>,
        threshold: Double = 50.0 // meters
    ): Boolean {
        val nearestPointOnRoute = findNearestPointOnRoute(currentLocation, expectedRoute)
        val distance = SphericalUtil.computeDistanceBetween(currentLocation, nearestPointOnRoute)
        return distance > threshold
    }

    private fun findNearestPointOnRoute(point: LatLng, route: List<LatLng>): LatLng {
        var nearestPoint = route.first()
        var minDistance = Double.MAX_VALUE

        for (i in 0 until route.size - 1) {
            val segmentStart = route[i]
            val segmentEnd = route[i + 1]

            val projectedPoint = projectPointOnSegment(point, segmentStart, segmentEnd)
            val distance = SphericalUtil.computeDistanceBetween(point, projectedPoint)

            if (distance < minDistance) {
                minDistance = distance
                nearestPoint = projectedPoint
            }
        }

        return nearestPoint
    }

    private fun projectPointOnSegment(point: LatLng, segmentStart: LatLng, segmentEnd: LatLng): LatLng {
        val heading = SphericalUtil.computeHeading(segmentStart, segmentEnd)
        val segmentLength = SphericalUtil.computeDistanceBetween(segmentStart, segmentEnd)

        val pointHeading = SphericalUtil.computeHeading(segmentStart, point)
        val pointDistance = SphericalUtil.computeDistanceBetween(segmentStart, point)

        val angle = Math.toRadians(pointHeading - heading)
        val projection = pointDistance * cos(angle)

        return when {
            projection <= 0 -> segmentStart
            projection >= segmentLength -> segmentEnd
            else -> SphericalUtil.computeOffset(segmentStart, projection, heading)
        }
    }

    private fun getApiKey(): String = "YOUR_GOOGLE_MAPS_API_KEY"
}