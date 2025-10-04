package com.daxido.user.presentation.emergency

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.EmergencyStream
import com.daxido.core.models.StreamFrame
import com.daxido.core.models.StreamStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveDashcamUiState(
    val isLoading: Boolean = false,
    val currentFrame: Bitmap? = null,
    val streamStatus: String = "INACTIVE",
    val currentFps: Int = 0,
    val error: String? = null,
    val streamInfo: EmergencyStream? = null,
    val isRecording: Boolean = false,
    val totalFramesReceived: Int = 0,
    val droppedFrames: Int = 0,
    val averageLatency: Long = 0L
)

@HiltViewModel
class LiveDashcamViewModel @Inject constructor(
    private val realtimeDb: FirebaseDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveDashcamUiState())
    val uiState: StateFlow<LiveDashcamUiState> = _uiState.asStateFlow()

    private var streamListener: ValueEventListener? = null
    private var frameListener: ValueEventListener? = null
    private var currentStreamId: String? = null

    private val frameTimestamps = mutableListOf<Long>()
    private var lastFrameSequence = -1

    companion object {
        private const val TAG = "LiveDashcamViewModel"
        private const val FPS_WINDOW_SIZE = 30 // Calculate FPS over last 30 frames
    }

    fun startWatchingStream(rideId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // First, get the stream ID for this ride
                val streamRef = realtimeDb.reference
                    .child("emergencyStreams")
                    .orderByChild("rideId")
                    .equalTo(rideId)

                streamListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val streamData = snapshot.children.firstOrNull()
                            streamData?.let {
                                val stream = it.getValue(EmergencyStream::class.java)
                                stream?.let { s ->
                                    currentStreamId = s.streamId
                                    handleStreamUpdate(s)
                                    startWatchingFrames(s.streamId)
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "No active emergency stream for this ride"
                                )
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Stream listener cancelled", error.toException())
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to connect to stream: ${error.message}"
                            )
                        }
                    }
                }

                streamRef.addValueEventListener(streamListener!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting stream watch", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to start watching: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startWatchingFrames(streamId: String) {
        try {
            val framesRef = realtimeDb.reference
                .child("emergencyStreams")
                .child(streamId)
                .child("latestFrame")

            frameListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val frame = snapshot.getValue(StreamFrame::class.java)
                        frame?.let { handleNewFrame(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Frame listener cancelled", error.toException())
                }
            }

            framesRef.addValueEventListener(frameListener!!)

            _uiState.update { it.copy(isLoading = false) }
        } catch (e: Exception) {
            Log.e(TAG, "Error watching frames", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to watch frames: ${e.message}"
                )
            }
        }
    }

    private fun handleStreamUpdate(stream: EmergencyStream) {
        _uiState.update {
            it.copy(
                streamInfo = stream,
                streamStatus = stream.status.name,
                isRecording = stream.autoRecording
            )
        }
    }

    private fun handleNewFrame(frame: StreamFrame) {
        viewModelScope.launch {
            try {
                // Check for dropped frames
                if (lastFrameSequence >= 0 && frame.sequenceNumber > lastFrameSequence + 1) {
                    val dropped = frame.sequenceNumber - lastFrameSequence - 1
                    _uiState.update {
                        it.copy(droppedFrames = it.droppedFrames + dropped)
                    }
                }
                lastFrameSequence = frame.sequenceNumber

                // Decode Base64 image
                val bitmap = decodeBase64ToBitmap(frame.imageData)

                // Calculate FPS
                val currentTime = System.currentTimeMillis()
                frameTimestamps.add(currentTime)
                if (frameTimestamps.size > FPS_WINDOW_SIZE) {
                    frameTimestamps.removeAt(0)
                }
                val fps = calculateFps()

                // Calculate latency
                val latency = currentTime - frame.timestamp

                _uiState.update {
                    it.copy(
                        currentFrame = bitmap,
                        currentFps = fps,
                        totalFramesReceived = it.totalFramesReceived + 1,
                        averageLatency = calculateAverageLatency(latency)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling frame", e)
            }
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun calculateFps(): Int {
        if (frameTimestamps.size < 2) return 0

        val timeSpan = frameTimestamps.last() - frameTimestamps.first()
        if (timeSpan == 0L) return 0

        return ((frameTimestamps.size - 1) * 1000 / timeSpan).toInt()
    }

    private fun calculateAverageLatency(newLatency: Long): Long {
        val currentAvg = _uiState.value.averageLatency
        val totalFrames = _uiState.value.totalFramesReceived + 1
        return ((currentAvg * (totalFrames - 1)) + newLatency) / totalFrames
    }

    fun stopWatchingStream() {
        currentStreamId?.let { streamId ->
            streamListener?.let {
                realtimeDb.reference
                    .child("emergencyStreams")
                    .orderByChild("rideId")
                    .removeEventListener(it)
            }

            frameListener?.let {
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(streamId)
                    .child("latestFrame")
                    .removeEventListener(it)
            }
        }

        streamListener = null
        frameListener = null
        currentStreamId = null

        _uiState.update {
            LiveDashcamUiState() // Reset to initial state
        }
    }

    fun takeScreenshot() {
        viewModelScope.launch {
            try {
                val bitmap = _uiState.value.currentFrame ?: return@launch

                // TODO: Save bitmap to gallery or share
                Log.d(TAG, "Screenshot taken")
            } catch (e: Exception) {
                Log.e(TAG, "Error taking screenshot", e)
            }
        }
    }

    fun toggleAudio() {
        // TODO: Implement audio toggle
        Log.d(TAG, "Audio toggle requested")
    }

    fun shareWithAuthorities() {
        viewModelScope.launch {
            try {
                val streamInfo = _uiState.value.streamInfo ?: return@launch

                // TODO: Create share intent with stream details and location
                Log.d(TAG, "Sharing stream with authorities: ${streamInfo.streamId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing stream", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopWatchingStream()
    }
}
