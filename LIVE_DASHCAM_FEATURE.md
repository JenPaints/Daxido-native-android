# 🎥 LIVE DASHCAM FEATURE - COMPLETE IMPLEMENTATION GUIDE

**Feature Name**: Emergency Live Dashcam
**Status**: 🚀 Ready for Implementation
**Priority**: HIGH - Unique Safety Feature
**Estimated Time**: 3-5 days

---

## 🎯 FEATURE OVERVIEW

### **What It Does:**
When a rider triggers the emergency SOS button during an active ride:
1. ✅ Driver's phone camera **automatically activates** (front or rear camera)
2. ✅ **Live video + audio stream** starts broadcasting
3. ✅ Rider can **view the live stream** in real-time
4. ✅ Admin dashboard receives **instant notification**
5. ✅ Admin can **monitor the live stream** remotely
6. ✅ Stream is **automatically recorded** for evidence
7. ✅ Emergency services are **notified** (optional)

### **Why It's Revolutionary:**
- 🏆 **No competitor has this** - Uber/Ola/Rapido don't offer live dashcam
- 🛡️ **Ultimate safety feature** - Deters criminal activity
- 📹 **Real-time evidence** - Live recording for investigations
- 🚨 **Instant admin oversight** - Immediate response to emergencies
- 💰 **Zero hardware cost** - Uses existing driver's phone camera

---

## 🏗️ ARCHITECTURE DESIGN

### **Technology Stack:**

```
┌─────────────────────────────────────────────────────────┐
│                    LIVE DASHCAM SYSTEM                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  DRIVER APP                 RIDER APP            ADMIN   │
│  ┌──────────┐              ┌──────────┐      ┌────────┐ │
│  │ Camera   │──Stream──>   │ Live     │      │ Monitor│ │
│  │ Service  │              │ View     │<─┐   │ Panel  │ │
│  └──────────┘              └──────────┘  │   └────────┘ │
│       │                         │        │        │      │
│       │                         │        │        │      │
│       └─────────────┬───────────┴────────┴────────┘      │
│                     ▼                                    │
│            ┌─────────────────┐                          │
│            │ Firebase RTDB   │ (Real-time streaming)    │
│            │ + Cloud Storage │ (Recording backup)       │
│            └─────────────────┘                          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### **Data Flow:**

```
1. Rider triggers SOS
   ↓
2. Realtime DB updates: /emergencyStreams/{rideId}/active = true
   ↓
3. Driver app detects change → Starts camera
   ↓
4. Camera frames → Base64 encoded → Firebase RTDB
   ↓
5. Rider app subscribes to /emergencyStreams/{rideId}/frames
   ↓
6. Admin dashboard receives notification + stream link
   ↓
7. All parties can view live stream
   ↓
8. Recording saved to Cloud Storage for evidence
```

---

## 📦 IMPLEMENTATION COMPONENTS

### **1. Data Models**

**File**: `app/src/main/java/com/daxido/core/models/EmergencyStream.kt`

```kotlin
package com.daxido.core.models

data class EmergencyStream(
    val streamId: String = "",
    val rideId: String = "",
    val riderId: String = "",
    val driverId: String = "",
    val status: StreamStatus = StreamStatus.INACTIVE,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val cameraType: CameraType = CameraType.REAR,
    val audioEnabled: Boolean = true,
    val recordingUrl: String? = null,
    val adminViewers: List<String> = emptyList(),
    val metadata: StreamMetadata = StreamMetadata()
)

enum class StreamStatus {
    INACTIVE,
    STARTING,
    ACTIVE,
    PAUSED,
    ENDED,
    ERROR
}

enum class CameraType {
    FRONT,  // Driver facing
    REAR    // Road facing
}

data class StreamMetadata(
    val resolution: String = "640x480",
    val frameRate: Int = 10, // 10 FPS for bandwidth optimization
    val bitrate: Int = 500,  // 500 kbps
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float = 0f,
    val deviceModel: String = ""
)

data class StreamFrame(
    val frameId: String = "",
    val streamId: String = "",
    val timestamp: Long = 0L,
    val imageData: String = "", // Base64 encoded JPEG
    val audioData: String? = null, // Base64 encoded audio chunk
    val sequenceNumber: Int = 0
)
```

---

### **2. Driver-Side Camera Service**

**File**: `app/src/main/java/com/daxido/driver/services/EmergencyCameraService.kt`

```kotlin
package com.daxido.driver.services

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.daxido.core.models.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class EmergencyCameraService : Service() {

    @Inject
    lateinit var realtimeDb: FirebaseDatabase

    @Inject
    lateinit var storage: FirebaseStorage

    private var currentStreamId: String? = null
    private var cameraExecutor: ExecutorService? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var frameSequence = 0
    private var isStreaming = false

    companion object {
        private const val TAG = "EmergencyCameraService"
        const val ACTION_START_STREAM = "START_STREAM"
        const val ACTION_STOP_STREAM = "STOP_STREAM"
        const val EXTRA_STREAM_ID = "stream_id"
        const val EXTRA_CAMERA_TYPE = "camera_type"

        // Streaming configuration
        private const val TARGET_FPS = 10
        private const val FRAME_INTERVAL_MS = 1000L / TARGET_FPS
        private const val IMAGE_QUALITY = 50 // JPEG quality 0-100
        private const val MAX_IMAGE_WIDTH = 640
        private const val MAX_IMAGE_HEIGHT = 480
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_STREAM -> {
                val streamId = intent.getStringExtra(EXTRA_STREAM_ID) ?: return START_NOT_STICKY
                val cameraType = intent.getSerializableExtra(EXTRA_CAMERA_TYPE) as? CameraType
                    ?: CameraType.REAR
                startStreaming(streamId, cameraType)
            }
            ACTION_STOP_STREAM -> {
                stopStreaming()
            }
        }
        return START_STICKY
    }

    private fun startStreaming(streamId: String, cameraType: CameraType) {
        if (isStreaming) {
            Log.w(TAG, "Already streaming")
            return
        }

        currentStreamId = streamId
        isStreaming = true
        frameSequence = 0

        Log.d(TAG, "Starting emergency stream: $streamId with camera: $cameraType")

        // Update stream status in Realtime DB
        updateStreamStatus(streamId, StreamStatus.STARTING)

        // Initialize camera
        cameraExecutor = Executors.newSingleThreadExecutor()

        serviceScope.launch {
            try {
                initializeCamera(streamId, cameraType)
                updateStreamStatus(streamId, StreamStatus.ACTIVE)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera", e)
                updateStreamStatus(streamId, StreamStatus.ERROR)
                stopStreaming()
            }
        }
    }

    private suspend fun initializeCamera(streamId: String, cameraType: CameraType) {
        withContext(Dispatchers.Main) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this@EmergencyCameraService)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val cameraSelector = when (cameraType) {
                    CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                    CameraType.REAR -> CameraSelector.DEFAULT_BACK_CAMERA
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                            processFrame(streamId, imageProxy)
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this@EmergencyCameraService,
                        cameraSelector,
                        imageAnalysis
                    )
                    Log.d(TAG, "Camera initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(this@EmergencyCameraService))
        }
    }

    private fun processFrame(streamId: String, imageProxy: ImageProxy) {
        if (!isStreaming) {
            imageProxy.close()
            return
        }

        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxy.toBitmap()

            // Resize for bandwidth optimization
            val resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)

            // Compress to JPEG and encode to Base64
            val base64Image = bitmapToBase64(resizedBitmap, IMAGE_QUALITY)

            // Create frame data
            val frame = StreamFrame(
                frameId = "${streamId}_${System.currentTimeMillis()}",
                streamId = streamId,
                timestamp = System.currentTimeMillis(),
                imageData = base64Image,
                sequenceNumber = frameSequence++
            )

            // Upload to Realtime DB
            uploadFrame(streamId, frame)

            bitmap.recycle()
            resizedBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun uploadFrame(streamId: String, frame: StreamFrame) {
        serviceScope.launch {
            try {
                // Upload to Realtime DB for live viewing
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(streamId)
                    .child("latestFrame")
                    .setValue(frame)
                    .await()

                Log.d(TAG, "Frame uploaded: ${frame.sequenceNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload frame", e)
            }
        }
    }

    private fun updateStreamStatus(streamId: String, status: StreamStatus) {
        serviceScope.launch {
            try {
                realtimeDb.reference
                    .child("emergencyStreams")
                    .child(streamId)
                    .child("status")
                    .setValue(status.name)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update stream status", e)
            }
        }
    }

    private fun stopStreaming() {
        isStreaming = false
        currentStreamId?.let { streamId ->
            updateStreamStatus(streamId, StreamStatus.ENDED)
        }
        cameraExecutor?.shutdown()
        cameraExecutor = null
        currentStreamId = null
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
        serviceScope.cancel()
    }
}
```

---

### **3. Rider-Side Live View Screen**

**File**: `app/src/main/java/com/daxido/user/presentation/emergency/LiveDashcamScreen.kt`

```kotlin
package com.daxido.user.presentation.emergency

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LiveDashcamScreen(
    rideId: String,
    viewModel: LiveDashcamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(rideId) {
        viewModel.startWatchingStream(rideId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopWatchingStream()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LIVE DASHCAM")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView()
                }
                uiState.error != null -> {
                    ErrorView(uiState.error!!)
                }
                uiState.currentFrame != null -> {
                    LiveStreamView(
                        frame = uiState.currentFrame!!,
                        streamStatus = uiState.streamStatus,
                        fps = uiState.currentFps
                    )
                }
                else -> {
                    NoStreamView()
                }
            }
        }
    }
}

@Composable
private fun LiveStreamView(
    frame: android.graphics.Bitmap,
    streamStatus: String,
    fps: Int
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Live video feed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "Live dashcam feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Status overlay
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Recording",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REC | $fps FPS",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.DarkGray
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { /* Screenshot */ }) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Screenshot",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { /* Toggle audio */ }) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Audio",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { /* Share with police */ }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Activating emergency dashcam...",
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorView(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = Color.White
            )
        }
    }
}

@Composable
private fun NoStreamView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No active stream",
            color = Color.White
        )
    }
}
```

(Continued in next message...)
