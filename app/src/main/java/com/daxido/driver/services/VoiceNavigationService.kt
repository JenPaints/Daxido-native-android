package com.daxido.driver.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice navigation service for turn-by-turn voice instructions
 */
@Singleton
class VoiceNavigationService @Inject constructor(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage = Locale.US

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(currentLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceNavigation", "Language not supported")
                isInitialized = false
            } else {
                isInitialized = true
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setPitch(1.0f)
                Log.d("VoiceNavigation", "TTS initialized successfully")
            }
        } else {
            Log.e("VoiceNavigation", "TTS initialization failed")
            isInitialized = false
        }
    }

    /**
     * Speak navigation instruction
     */
    fun speak(text: String, priority: SpeechPriority = SpeechPriority.NORMAL) {
        if (!isInitialized) {
            Log.w("VoiceNavigation", "TTS not initialized")
            return
        }

        val queueMode = when (priority) {
            SpeechPriority.URGENT -> TextToSpeech.QUEUE_FLUSH
            SpeechPriority.NORMAL -> TextToSpeech.QUEUE_ADD
        }

        textToSpeech?.speak(text, queueMode, null, null)
        Log.d("VoiceNavigation", "Speaking: $text")
    }

    /**
     * Announce turn instruction
     */
    fun announceTurn(direction: TurnDirection, distance: Int, streetName: String?) {
        val instruction = buildTurnInstruction(direction, distance, streetName)
        speak(instruction)

        _navigationState.value = _navigationState.value.copy(
            currentInstruction = instruction,
            lastAnnouncementTime = System.currentTimeMillis()
        )
    }

    /**
     * Announce distance to destination
     */
    fun announceDistanceToDestination(distanceMeters: Int, estimatedTimeMinutes: Int) {
        val distance = formatDistance(distanceMeters)
        val time = formatTime(estimatedTimeMinutes)

        val instruction = "In $distance, you will arrive at your destination. Estimated time: $time"
        speak(instruction)
    }

    /**
     * Announce arrival
     */
    fun announceArrival() {
        speak("You have arrived at your destination", SpeechPriority.URGENT)
    }

    /**
     * Announce rerouting
     */
    fun announceRerouting() {
        speak("Rerouting", SpeechPriority.URGENT)
    }

    /**
     * Announce traffic alert
     */
    fun announceTrafficAlert(severity: TrafficSeverity, delayMinutes: Int) {
        val severityText = when (severity) {
            TrafficSeverity.LIGHT -> "light traffic"
            TrafficSeverity.MODERATE -> "moderate traffic"
            TrafficSeverity.HEAVY -> "heavy traffic"
        }

        val instruction = "Heads up, there is $severityText ahead. Expected delay: $delayMinutes minutes"
        speak(instruction, SpeechPriority.URGENT)
    }

    /**
     * Announce speed limit
     */
    fun announceSpeedLimit(speedLimit: Int) {
        speak("Speed limit is $speedLimit kilometers per hour")
    }

    /**
     * Announce lane guidance
     */
    fun announceLaneGuidance(lanes: List<Lane>) {
        val laneInstruction = when {
            lanes.any { it.isRecommended } -> {
                val recommendedLane = lanes.indexOfFirst { it.isRecommended } + 1
                "Keep in lane $recommendedLane"
            }
            else -> "Continue in current lane"
        }
        speak(laneInstruction)
    }

    /**
     * Set language for voice instructions
     */
    fun setLanguage(locale: Locale) {
        currentLanguage = locale
        textToSpeech?.language = locale
        Log.d("VoiceNavigation", "Language set to: ${locale.displayName}")
    }

    /**
     * Enable/disable voice instructions
     */
    fun setVoiceEnabled(enabled: Boolean) {
        _navigationState.value = _navigationState.value.copy(isVoiceEnabled = enabled)
    }

    /**
     * Set speech rate (0.5 to 2.0)
     */
    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(clampedRate)
        _navigationState.value = _navigationState.value.copy(speechRate = clampedRate)
    }

    /**
     * Stop speaking
     */
    fun stop() {
        textToSpeech?.stop()
    }

    /**
     * Release resources
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }

    private fun buildTurnInstruction(
        direction: TurnDirection,
        distance: Int,
        streetName: String?
    ): String {
        val distanceText = formatDistance(distance)
        val directionText = when (direction) {
            TurnDirection.LEFT -> "turn left"
            TurnDirection.RIGHT -> "turn right"
            TurnDirection.SLIGHT_LEFT -> "keep left"
            TurnDirection.SLIGHT_RIGHT -> "keep right"
            TurnDirection.SHARP_LEFT -> "make a sharp left turn"
            TurnDirection.SHARP_RIGHT -> "make a sharp right turn"
            TurnDirection.STRAIGHT -> "continue straight"
            TurnDirection.U_TURN -> "make a U-turn"
            TurnDirection.ROUNDABOUT_LEFT -> "take the roundabout and exit left"
            TurnDirection.ROUNDABOUT_RIGHT -> "take the roundabout and exit right"
        }

        return if (streetName != null) {
            "In $distanceText, $directionText onto $streetName"
        } else {
            "In $distanceText, $directionText"
        }
    }

    private fun formatDistance(meters: Int): String {
        return when {
            meters < 100 -> "$meters meters"
            meters < 1000 -> "${(meters / 100) * 100} meters"
            else -> String.format("%.1f kilometers", meters / 1000.0)
        }
    }

    private fun formatTime(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes minutes"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "$hours hours $mins minutes" else "$hours hours"
            }
        }
    }
}

data class NavigationState(
    val isVoiceEnabled: Boolean = true,
    val currentInstruction: String? = null,
    val lastAnnouncementTime: Long = 0,
    val speechRate: Float = 1.0f
)

data class Lane(
    val index: Int,
    val isRecommended: Boolean,
    val directions: List<TurnDirection>
)

enum class TurnDirection {
    LEFT, RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT,
    SHARP_LEFT, SHARP_RIGHT, STRAIGHT, U_TURN,
    ROUNDABOUT_LEFT, ROUNDABOUT_RIGHT
}

enum class SpeechPriority {
    NORMAL, URGENT
}

enum class TrafficSeverity {
    LIGHT, MODERATE, HEAVY
}