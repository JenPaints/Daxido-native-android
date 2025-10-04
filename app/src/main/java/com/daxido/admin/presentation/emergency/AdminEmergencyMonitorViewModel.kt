package com.daxido.admin.presentation.emergency

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.*
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class AdminEmergencyMonitorUiState(
    val isLoading: Boolean = false,
    val activeStreams: List<EmergencyStream> = emptyList(),
    val streamFrames: Map<String, Bitmap> = emptyMap(), // streamId -> latest frame
    val selectedStream: EmergencyStream? = null,
    val selectedStreamFrame: Bitmap? = null,
    val criticalCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class AdminEmergencyMonitorViewModel @Inject constructor(
    private val realtimeDb: FirebaseDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminEmergencyMonitorUiState())
    val uiState: StateFlow<AdminEmergencyMonitorUiState> = _uiState.asStateFlow()

    private val streamListeners = mutableMapOf<String, ChildEventListener>()
    private val frameListeners = mutableMapOf<String, ValueEventListener>()
    private var streamsListener: ChildEventListener? = null

    companion object {
        private const val TAG = "AdminEmergencyMonitorVM"
    }

    fun startMonitoring() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val streamsRef = realtimeDb.reference.child("emergencyStreams")

                streamsListener = object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val stream = snapshot.getValue(EmergencyStream::class.java)
                        stream?.let {
                            if (it.status == StreamStatus.ACTIVE || it.status == StreamStatus.STARTING) {
                                addActiveStream(it)
                                startWatchingStreamFrames(it.streamId)
                            }
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        val stream = snapshot.getValue(EmergencyStream::class.java)
                        stream?.let { updateStream(it) }
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        val stream = snapshot.getValue(EmergencyStream::class.java)
                        stream?.let { removeStream(it.streamId) }
                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Streams listener cancelled", error.toException())
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to monitor streams: ${error.message}"
                            )
                        }
                    }
                }

                streamsRef.addChildEventListener(streamsListener!!)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting monitoring", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to start monitoring: ${e.message}"
                    )
                }
            }
        }
    }

    private fun addActiveStream(stream: EmergencyStream) {
        _uiState.update { state ->
            val updatedStreams = state.activeStreams + stream
            val criticalCount = updatedStreams.count {
                it.emergencyType in listOf(
                    EmergencyType.ACCIDENT,
                    EmergencyType.MEDICAL,
                    EmergencyType.HARASSMENT
                )
            }
            state.copy(
                activeStreams = updatedStreams,
                criticalCount = criticalCount
            )
        }
    }

    private fun updateStream(stream: EmergencyStream) {
        _uiState.update { state ->
            val updatedStreams = state.activeStreams.map {
                if (it.streamId == stream.streamId) stream else it
            }
            state.copy(activeStreams = updatedStreams)
        }

        // Update selected stream if it's being viewed
        if (_uiState.value.selectedStream?.streamId == stream.streamId) {
            _uiState.update { it.copy(selectedStream = stream) }
        }
    }

    private fun removeStream(streamId: String) {
        stopWatchingStreamFrames(streamId)

        _uiState.update { state ->
            val updatedStreams = state.activeStreams.filter { it.streamId != streamId }
            val updatedFrames = state.streamFrames - streamId
            val criticalCount = updatedStreams.count {
                it.emergencyType in listOf(
                    EmergencyType.ACCIDENT,
                    EmergencyType.MEDICAL,
                    EmergencyType.HARASSMENT
                )
            }
            state.copy(
                activeStreams = updatedStreams,
                streamFrames = updatedFrames,
                criticalCount = criticalCount,
                selectedStream = if (state.selectedStream?.streamId == streamId) null else state.selectedStream,
                selectedStreamFrame = if (state.selectedStream?.streamId == streamId) null else state.selectedStreamFrame
            )
        }
    }

    private fun startWatchingStreamFrames(streamId: String) {
        try {
            val framesRef = realtimeDb.reference
                .child("emergencyStreams")
                .child(streamId)
                .child("latestFrame")

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val frame = snapshot.getValue(StreamFrame::class.java)
                        frame?.let { handleNewFrame(streamId, it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Frame listener cancelled for $streamId", error.toException())
                }
            }

            framesRef.addValueEventListener(listener)
            frameListeners[streamId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Error watching frames for $streamId", e)
        }
    }

    private fun stopWatchingStreamFrames(streamId: String) {
        frameListeners[streamId]?.let { listener ->
            realtimeDb.reference
                .child("emergencyStreams")
                .child(streamId)
                .child("latestFrame")
                .removeEventListener(listener)
            frameListeners.remove(streamId)
        }
    }

    private fun handleNewFrame(streamId: String, frame: StreamFrame) {
        viewModelScope.launch {
            try {
                val bitmap = decodeBase64ToBitmap(frame.imageData)

                _uiState.update { state ->
                    val updatedFrames = state.streamFrames + (streamId to bitmap)
                    val updatedSelectedFrame = if (state.selectedStream?.streamId == streamId) {
                        bitmap
                    } else {
                        state.selectedStreamFrame
                    }

                    state.copy(
                        streamFrames = updatedFrames,
                        selectedStreamFrame = updatedSelectedFrame
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling frame for $streamId", e)
            }
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    fun selectStream(stream: EmergencyStream) {
        _uiState.update {
            it.copy(
                selectedStream = stream,
                selectedStreamFrame = it.streamFrames[stream.streamId]
            )
        }

        // Mark admin as viewer
        markAdminAsViewer(stream.streamId)
    }

    fun clearSelectedStream() {
        val currentStreamId = _uiState.value.selectedStream?.streamId
        currentStreamId?.let { removeAdminAsViewer(it) }

        _uiState.update {
            it.copy(
                selectedStream = null,
                selectedStreamFrame = null
            )
        }
    }

    private fun markAdminAsViewer(streamId: String) {
        viewModelScope.launch {
            try {
                // Add current admin to viewers list
                val adminId = "admin_001" // TODO: Get from auth service
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(streamId)
                    .child("adminViewers")
                    .child(adminId)
                    .setValue(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Error marking admin as viewer", e)
            }
        }
    }

    private fun removeAdminAsViewer(streamId: String) {
        viewModelScope.launch {
            try {
                val adminId = "admin_001" // TODO: Get from auth service
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(streamId)
                    .child("adminViewers")
                    .child(adminId)
                    .removeValue()
            } catch (e: Exception) {
                Log.e(TAG, "Error removing admin as viewer", e)
            }
        }
    }

    fun dispatchEmergencyServices(stream: EmergencyStream) {
        viewModelScope.launch {
            try {
                // TODO: Implement emergency services dispatch
                // - Call 911 API
                // - Send location to emergency services
                // - Create emergency ticket
                // - Notify rider/driver

                Log.d(TAG, "Dispatching emergency services for stream: ${stream.streamId}")

                // Update stream status
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(stream.streamId)
                    .child("emergencyServicesDispatched")
                    .setValue(true)

                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(stream.streamId)
                    .child("emergencyServicesDispatchedAt")
                    .setValue(System.currentTimeMillis())

            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching emergency services", e)
            }
        }
    }

    fun contactRider(stream: EmergencyStream) {
        viewModelScope.launch {
            try {
                // TODO: Implement rider contact
                // - Initiate call
                // - Send SMS
                // - Open chat

                Log.d(TAG, "Contacting rider for stream: ${stream.streamId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error contacting rider", e)
            }
        }
    }

    fun markAsResolved(stream: EmergencyStream) {
        viewModelScope.launch {
            try {
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(stream.streamId)
                    .child("status")
                    .setValue(StreamStatus.ENDED.name)

                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(stream.streamId)
                    .child("resolvedByAdmin")
                    .setValue(true)

                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(stream.streamId)
                    .child("resolvedAt")
                    .setValue(System.currentTimeMillis())

                Log.d(TAG, "Marked stream as resolved: ${stream.streamId}")

                // Close selected stream if it's the one being resolved
                if (_uiState.value.selectedStream?.streamId == stream.streamId) {
                    clearSelectedStream()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking stream as resolved", e)
            }
        }
    }

    /**
     * 🎥 CAMERA SWITCHING - Send command to driver's phone
     */
    fun switchCamera(stream: EmergencyStream, targetCamera: CameraType) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Admin requesting camera switch to: $targetCamera")

                val adminId = "admin_001" // TODO: Get from auth service
                val commandId = UUID.randomUUID().toString()

                val command = CameraCommand(
                    commandId = commandId,
                    streamId = stream.streamId,
                    commandType = CameraCommandType.SWITCH_CAMERA,
                    cameraType = targetCamera,
                    issuedBy = adminId,
                    issuedAt = System.currentTimeMillis(),
                    executed = false,
                    result = CommandResult.PENDING
                )

                // Send command to Firebase
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(stream.streamId)
                    .child("commands")
                    .child(commandId)
                    .setValue(command)
                    .await()

                Log.d(TAG, "✅ Camera switch command sent successfully")

                // Optional: Show toast/snackbar to admin
                // "Camera switching to ${targetCamera.name.lowercase()}..."

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send camera switch command", e)
                // Optional: Show error to admin
            }
        }
    }

    /**
     * 🎯 Get opposite camera type for quick toggle
     */
    fun getOppositeCamera(current: CameraType): CameraType {
        return when (current) {
            CameraType.FRONT -> CameraType.REAR
            CameraType.REAR -> CameraType.FRONT
            CameraType.BOTH -> CameraType.REAR
        }
    }

    fun stopMonitoring() {
        // Remove all listeners
        streamsListener?.let {
            realtimeDb.reference.child("emergencyStreams").removeEventListener(it)
        }
        streamsListener = null

        // Remove all frame listeners
        frameListeners.forEach { (streamId, listener) ->
            realtimeDb.reference
                .child("emergencyStreams")
                .child(streamId)
                .child("latestFrame")
                .removeEventListener(listener)
        }
        frameListeners.clear()

        // Clear state
        _uiState.update { AdminEmergencyMonitorUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
