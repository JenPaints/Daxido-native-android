package com.daxido.core.navigation

import android.location.Location
import android.speech.tts.TextToSpeech
import com.daxido.core.location.PrecisionLocationManager
import com.daxido.core.maps.DirectionsService
import com.daxido.core.maps.MapMatchingService
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class PrecisionNavigationEngine @Inject constructor(
    private val precisionLocationManager: PrecisionLocationManager,
    private val mapMatchingService: MapMatchingService,
    private val directionsService: DirectionsService,
    private val textToSpeech: TextToSpeech
) {

    private val navigationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentNavigation: NavigationSession? = null

    companion object {
        const val WAYPOINT_ARRIVAL_RADIUS = 20.0 // meters
        const val REROUTE_THRESHOLD = 50.0 // meters
        const val INSTRUCTION_LEAD_TIME = 150.0 // meters
        const val TURN_ANGLE_THRESHOLD = 30.0 // degrees
        const val U_TURN_THRESHOLD = 160.0 // degrees
        const val SPEED_LIMIT_WARNING_THRESHOLD = 10.0 // km/h over limit
    }

    data class NavigationSession(
        val sessionId: String,
        val origin: LatLng,
        val destination: LatLng,
        val waypoints: List<Waypoint>,
        var currentRoute: Route,
        var currentWaypointIndex: Int = 0,
        var currentStepIndex: Int = 0,
        var distanceTraveled: Double = 0.0,
        var deviationCount: Int = 0,
        var isActive: Boolean = true
    )

    data class Waypoint(
        val location: LatLng,
        val name: String,
        val type: WaypointType,
        val estimatedDuration: Int? = null, // seconds to spend at waypoint
        val instructions: String? = null
    )

    enum class WaypointType {
        PICKUP, DROP, STOP, VIA_POINT, FUEL_STATION, CHARGING_POINT
    }

    data class Route(
        val polyline: List<LatLng>,
        val encodedPolyline: String,
        val steps: List<NavigationStep>,
        val distance: Double, // meters
        val duration: Int, // seconds
        val waypoints: List<Waypoint>
    )

    data class NavigationStep(
        val instruction: String,
        val distance: Double, // meters
        val duration: Int, // seconds
        val startLocation: LatLng,
        val endLocation: LatLng,
        val maneuver: Maneuver?,
        val polyline: List<LatLng>,
        val speedLimit: Float?, // km/h
        val laneGuidance: LaneGuidance?
    )

    data class Maneuver(
        val type: ManeuverType,
        val modifier: ManeuverModifier?,
        val bearing: Float,
        val exitNumber: Int?
    )

    enum class ManeuverType {
        TURN, MERGE, FORK, RAMP, ROUNDABOUT, ARRIVAL, DEPARTURE, U_TURN, CONTINUE
    }

    enum class ManeuverModifier {
        LEFT, RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, SHARP_LEFT, SHARP_RIGHT, STRAIGHT, U_TURN
    }

    data class LaneGuidance(
        val totalLanes: Int,
        val recommendedLanes: List<Int>,
        val laneInstructions: List<String>
    )

    data class NavigationUpdate(
        val currentLocation: LatLng,
        val snappedLocation: LatLng,
        val speed: Float, // km/h
        val bearing: Float,
        val currentStep: NavigationStep?,
        val nextStep: NavigationStep?,
        val distanceToNextManeuver: Double,
        val timeToNextManeuver: Int,
        val distanceRemaining: Double,
        val timeRemaining: Int,
        val currentWaypoint: Waypoint?,
        val isOffRoute: Boolean,
        val speedLimitWarning: SpeedWarning?,
        val voiceInstruction: String?
    )

    data class SpeedWarning(
        val currentSpeed: Float,
        val speedLimit: Float,
        val severity: WarningSeverity
    )

    enum class WarningSeverity {
        INFO, WARNING, CRITICAL
    }

    suspend fun startNavigation(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<Waypoint> = emptyList(),
        vehicleType: String = "CAR"
    ): Flow<NavigationUpdate> = flow {
        // Get initial route
        val routeResult = getOptimalRoute(origin, destination, waypoints)

        routeResult.fold(
            onSuccess = { route ->
                val session = NavigationSession(
                    sessionId = UUID.randomUUID().toString(),
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    currentRoute = route
                )

                currentNavigation = session

                // Start precision tracking
                precisionLocationManager.startPrecisionTracking(
                    PrecisionLocationManager.TrackingMode.HIGH_ACCURACY
                ).collect { precisionLocation ->
                    if (!session.isActive) return@collect

                    val currentLatLng = LatLng(
                        precisionLocation.latitude,
                        precisionLocation.longitude
                    )

                    // Snap to road for maximum precision
                    val snappedResult = mapMatchingService.snapSinglePoint(currentLatLng)

                    val snappedLocation = snappedResult.getOrNull()?.snappedLocation ?: currentLatLng

                    // Process navigation update
                    val update = processNavigationUpdate(
                        session,
                        currentLatLng,
                        snappedLocation,
                        precisionLocation
                    )

                    emit(update)

                    // Check if rerouting is needed
                    if (update.isOffRoute) {
                        handleRerouting(session, currentLatLng)
                    }

                    // Update session progress
                    updateSessionProgress(session, update)

                    // Generate voice instructions
                    generateVoiceInstructions(update)
                }
            },
            onFailure = {
                throw it
            }
        )
    }

    private suspend fun getOptimalRoute(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<Waypoint>
    ): Result<Route> {
        val waypointLocations = waypoints.map {
            com.daxido.core.models.Location(it.location.latitude, it.location.longitude)
        }

        val routeResult = if (waypointLocations.isNotEmpty()) {
            directionsService.getOptimizedRoute(
                com.daxido.core.models.Location(origin.latitude, origin.longitude),
                com.daxido.core.models.Location(destination.latitude, destination.longitude),
                waypointLocations
            )
        } else {
            directionsService.getRoute(
                com.daxido.core.models.Location(origin.latitude, origin.longitude),
                com.daxido.core.models.Location(destination.latitude, destination.longitude)
            )
        }

        return routeResult.map { routeInfo ->
            Route(
                polyline = routeInfo.polylinePoints,
                encodedPolyline = routeInfo.encodedPolyline,
                steps = convertToNavigationSteps(routeInfo.steps),
                distance = routeInfo.distance * 1000, // Convert km to meters
                duration = routeInfo.duration * 60, // Convert minutes to seconds
                waypoints = waypoints
            )
        }
    }

    private fun convertToNavigationSteps(steps: List<DirectionsService.NavigationStep>): List<NavigationStep> {
        return steps.map { step ->
            NavigationStep(
                instruction = step.instruction,
                distance = parseDistance(step.distance),
                duration = parseDuration(step.duration),
                startLocation = step.polylinePoints.firstOrNull() ?: LatLng(0.0, 0.0),
                endLocation = step.polylinePoints.lastOrNull() ?: LatLng(0.0, 0.0),
                maneuver = parseManeuver(step.maneuver),
                polyline = step.polylinePoints,
                speedLimit = null, // Would be fetched from map data
                laneGuidance = null // Would be parsed from instructions
            )
        }
    }

    private fun processNavigationUpdate(
        session: NavigationSession,
        currentLocation: LatLng,
        snappedLocation: LatLng,
        precisionLocation: PrecisionLocationManager.PrecisionLocation
    ): NavigationUpdate {
        val currentStep = getCurrentStep(session, snappedLocation)
        val nextStep = getNextStep(session)

        val distanceToNextManeuver = currentStep?.let {
            SphericalUtil.computeDistanceBetween(snappedLocation, it.endLocation)
        } ?: 0.0

        val speedKmh = precisionLocation.speed * 3.6f
        val timeToNextManeuver = if (speedKmh > 0) {
            (distanceToNextManeuver / (speedKmh / 3.6)).toInt()
        } else {
            Int.MAX_VALUE
        }

        val (distanceRemaining, timeRemaining) = calculateRemaining(session, snappedLocation)

        val isOffRoute = detectOffRoute(snappedLocation, session.currentRoute)

        val speedWarning = checkSpeedLimit(speedKmh, currentStep?.speedLimit)

        val voiceInstruction = generateInstructionText(
            currentStep,
            nextStep,
            distanceToNextManeuver
        )

        return NavigationUpdate(
            currentLocation = currentLocation,
            snappedLocation = snappedLocation,
            speed = speedKmh,
            bearing = precisionLocation.bearing,
            currentStep = currentStep,
            nextStep = nextStep,
            distanceToNextManeuver = distanceToNextManeuver,
            timeToNextManeuver = timeToNextManeuver,
            distanceRemaining = distanceRemaining,
            timeRemaining = timeRemaining,
            currentWaypoint = getCurrentWaypoint(session),
            isOffRoute = isOffRoute,
            speedLimitWarning = speedWarning,
            voiceInstruction = voiceInstruction
        )
    }

    private fun getCurrentStep(session: NavigationSession, location: LatLng): NavigationStep? {
        val steps = session.currentRoute.steps
        if (session.currentStepIndex >= steps.size) return null

        val currentStep = steps[session.currentStepIndex]

        // Check if we've passed this step
        val distanceToStepEnd = SphericalUtil.computeDistanceBetween(
            location,
            currentStep.endLocation
        )

        if (distanceToStepEnd < WAYPOINT_ARRIVAL_RADIUS && session.currentStepIndex < steps.size - 1) {
            session.currentStepIndex++
            return steps[session.currentStepIndex]
        }

        return currentStep
    }

    private fun getNextStep(session: NavigationSession): NavigationStep? {
        val steps = session.currentRoute.steps
        val nextIndex = session.currentStepIndex + 1

        return if (nextIndex < steps.size) {
            steps[nextIndex]
        } else {
            null
        }
    }

    private fun calculateRemaining(
        session: NavigationSession,
        currentLocation: LatLng
    ): Pair<Double, Int> {
        var distanceRemaining = 0.0
        var timeRemaining = 0

        val steps = session.currentRoute.steps

        // Add distance/time for current step
        val currentStep = steps.getOrNull(session.currentStepIndex)
        currentStep?.let {
            val distanceInCurrentStep = SphericalUtil.computeDistanceBetween(
                currentLocation,
                it.endLocation
            )
            distanceRemaining += distanceInCurrentStep

            val speedMs = 15.0 // Assume 15 m/s average
            timeRemaining += (distanceInCurrentStep / speedMs).toInt()
        }

        // Add remaining steps
        for (i in (session.currentStepIndex + 1) until steps.size) {
            distanceRemaining += steps[i].distance
            timeRemaining += steps[i].duration
        }

        return Pair(distanceRemaining, timeRemaining)
    }

    private fun detectOffRoute(location: LatLng, route: Route): Boolean {
        val nearestPoint = findNearestPointOnRoute(location, route.polyline)
        val distance = SphericalUtil.computeDistanceBetween(location, nearestPoint)
        return distance > REROUTE_THRESHOLD
    }

    private fun findNearestPointOnRoute(location: LatLng, route: List<LatLng>): LatLng {
        var nearestPoint = route.firstOrNull() ?: location
        var minDistance = Double.MAX_VALUE

        for (i in 0 until route.size - 1) {
            val segmentStart = route[i]
            val segmentEnd = route[i + 1]

            val projectedPoint = projectPointOnSegment(location, segmentStart, segmentEnd)
            val distance = SphericalUtil.computeDistanceBetween(location, projectedPoint)

            if (distance < minDistance) {
                minDistance = distance
                nearestPoint = projectedPoint
            }
        }

        return nearestPoint
    }

    private fun projectPointOnSegment(
        point: LatLng,
        segmentStart: LatLng,
        segmentEnd: LatLng
    ): LatLng {
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

    private fun checkSpeedLimit(currentSpeed: Float, speedLimit: Float?): SpeedWarning? {
        speedLimit?.let {
            val difference = currentSpeed - speedLimit

            return when {
                difference > SPEED_LIMIT_WARNING_THRESHOLD * 2 -> {
                    SpeedWarning(currentSpeed, speedLimit, WarningSeverity.CRITICAL)
                }
                difference > SPEED_LIMIT_WARNING_THRESHOLD -> {
                    SpeedWarning(currentSpeed, speedLimit, WarningSeverity.WARNING)
                }
                difference > 0 -> {
                    SpeedWarning(currentSpeed, speedLimit, WarningSeverity.INFO)
                }
                else -> null
            }
        }

        return null
    }

    private fun generateInstructionText(
        currentStep: NavigationStep?,
        nextStep: NavigationStep?,
        distanceToManeuver: Double
    ): String? {
        currentStep?.let { step ->
            return when {
                distanceToManeuver > 1000 -> {
                    "Continue for ${(distanceToManeuver / 1000).roundToInt()} kilometers"
                }
                distanceToManeuver > INSTRUCTION_LEAD_TIME -> {
                    "In ${distanceToManeuver.roundToInt()} meters, ${step.instruction}"
                }
                distanceToManeuver > 50 -> {
                    step.instruction
                }
                else -> {
                    nextStep?.let {
                        "Then ${it.instruction}"
                    }
                }
            }
        }

        return null
    }

    private suspend fun handleRerouting(session: NavigationSession, currentLocation: LatLng) {
        session.deviationCount++

        if (session.deviationCount >= 3) {
            // Get new route
            val newRoute = getOptimalRoute(
                currentLocation,
                session.destination,
                session.waypoints.drop(session.currentWaypointIndex)
            )

            newRoute.getOrNull()?.let {
                session.currentRoute = it
                session.currentStepIndex = 0
                session.deviationCount = 0
            }
        }
    }

    private fun updateSessionProgress(session: NavigationSession, update: NavigationUpdate) {
        // Check waypoint arrival
        session.waypoints.getOrNull(session.currentWaypointIndex)?.let { waypoint ->
            val distanceToWaypoint = SphericalUtil.computeDistanceBetween(
                update.snappedLocation,
                waypoint.location
            )

            if (distanceToWaypoint < WAYPOINT_ARRIVAL_RADIUS) {
                session.currentWaypointIndex++
                announceWaypointArrival(waypoint)
            }
        }

        // Update distance traveled
        session.distanceTraveled += update.speed / 3.6 // Convert km/h to m/s
    }

    private fun getCurrentWaypoint(session: NavigationSession): Waypoint? {
        return session.waypoints.getOrNull(session.currentWaypointIndex)
    }

    private fun generateVoiceInstructions(update: NavigationUpdate) {
        update.voiceInstruction?.let { instruction ->
            if (shouldAnnounceInstruction(update.distanceToNextManeuver)) {
                textToSpeech.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        update.speedLimitWarning?.let { warning ->
            if (warning.severity == WarningSeverity.CRITICAL) {
                textToSpeech.speak(
                    "Speed limit exceeded. Please slow down.",
                    TextToSpeech.QUEUE_ADD,
                    null,
                    null
                )
            }
        }
    }

    private fun shouldAnnounceInstruction(distanceToManeuver: Double): Boolean {
        return listOf(1000.0, 500.0, 200.0, 50.0)
            .map { it..it + 20 }
            .any { range -> distanceToManeuver in range }
    }

    private fun announceWaypointArrival(waypoint: Waypoint) {
        val message = when (waypoint.type) {
            WaypointType.PICKUP -> "Arriving at pickup location"
            WaypointType.DROP -> "Arriving at destination"
            WaypointType.STOP -> "Arriving at stop: ${waypoint.name}"
            else -> "Waypoint reached"
        }

        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun parseDistance(distanceStr: String): Double {
        val number = distanceStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        return when {
            distanceStr.contains("km") -> number * 1000
            distanceStr.contains("m") -> number
            else -> number
        }
    }

    private fun parseDuration(durationStr: String): Int {
        val number = durationStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        return when {
            durationStr.contains("hour") -> number * 3600
            durationStr.contains("min") -> number * 60
            else -> number
        }
    }

    private fun parseManeuver(maneuverStr: String?): Maneuver? {
        maneuverStr?.let {
            val type = when {
                it.contains("turn") -> ManeuverType.TURN
                it.contains("merge") -> ManeuverType.MERGE
                it.contains("fork") -> ManeuverType.FORK
                it.contains("ramp") -> ManeuverType.RAMP
                it.contains("roundabout") -> ManeuverType.ROUNDABOUT
                it.contains("arrival") -> ManeuverType.ARRIVAL
                it.contains("u-turn") -> ManeuverType.U_TURN
                else -> ManeuverType.CONTINUE
            }

            val modifier = when {
                it.contains("sharp-left") -> ManeuverModifier.SHARP_LEFT
                it.contains("sharp-right") -> ManeuverModifier.SHARP_RIGHT
                it.contains("slight-left") -> ManeuverModifier.SLIGHT_LEFT
                it.contains("slight-right") -> ManeuverModifier.SLIGHT_RIGHT
                it.contains("left") -> ManeuverModifier.LEFT
                it.contains("right") -> ManeuverModifier.RIGHT
                it.contains("u-turn") -> ManeuverModifier.U_TURN
                else -> ManeuverModifier.STRAIGHT
            }

            return Maneuver(type, modifier, 0f, null)
        }

        return null
    }

    fun stopNavigation() {
        currentNavigation?.isActive = false
        currentNavigation = null
        navigationScope.cancel()
    }
}