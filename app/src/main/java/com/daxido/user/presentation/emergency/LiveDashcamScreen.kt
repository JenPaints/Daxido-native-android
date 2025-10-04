package com.daxido.user.presentation.emergency

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LiveDashcamScreen(
    rideId: String,
    onNavigateBack: () -> Unit = {},
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
            LiveDashcamTopBar(
                isRecording = uiState.isRecording,
                fps = uiState.currentFps,
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            if (uiState.currentFrame != null) {
                LiveDashcamControls(
                    onScreenshot = { viewModel.takeScreenshot() },
                    onToggleAudio = { viewModel.toggleAudio() },
                    onShareWithAuthorities = { viewModel.shareWithAuthorities() }
                )
            }
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
                    ErrorView(
                        error = uiState.error!!,
                        onRetry = { viewModel.startWatchingStream(rideId) }
                    )
                }
                uiState.currentFrame != null -> {
                    LiveStreamView(
                        frame = uiState.currentFrame!!,
                        streamStatus = uiState.streamStatus,
                        totalFrames = uiState.totalFramesReceived,
                        droppedFrames = uiState.droppedFrames,
                        latency = uiState.averageLatency
                    )
                }
                else -> {
                    NoStreamView()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveDashcamTopBar(
    isRecording: Boolean,
    fps: Int,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing red dot
                if (isRecording) {
                    PulsingRecordingIndicator()
                }

                Text(
                    text = "EMERGENCY DASHCAM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (fps > 0) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$fps FPS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
private fun PulsingRecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .alpha(alpha)
            .background(Color.Red, CircleShape)
    )
}

@Composable
private fun LiveStreamView(
    frame: android.graphics.Bitmap,
    streamStatus: String,
    totalFrames: Int,
    droppedFrames: Int,
    latency: Long
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

            // Stream stats overlay (top-right)
            StreamStatsOverlay(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                status = streamStatus,
                totalFrames = totalFrames,
                droppedFrames = droppedFrames,
                latency = latency
            )

            // Warning message (bottom)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Red.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color.White
                    )
                    Text(
                        text = "Emergency Mode Active • Admin Monitoring • Recording in Progress",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamStatsOverlay(
    modifier: Modifier = Modifier,
    status: String,
    totalFrames: Int,
    droppedFrames: Int,
    latency: Long
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.small)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatItem("Status", status)
        StatItem("Frames", totalFrames.toString())
        if (droppedFrames > 0) {
            StatItem("Dropped", droppedFrames.toString(), Color.Yellow)
        }
        StatItem("Latency", "${latency}ms")
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color = Color.White) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LiveDashcamControls(
    onScreenshot: () -> Unit,
    onToggleAudio: () -> Unit,
    onShareWithAuthorities: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.DarkGray,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlButton(
                icon = Icons.Default.CameraAlt,
                label = "Screenshot",
                onClick = onScreenshot
            )

            ControlButton(
                icon = Icons.Default.VolumeUp,
                label = "Audio",
                onClick = onToggleAudio
            )

            ControlButton(
                icon = Icons.Default.LocalPolice,
                label = "Alert Police",
                onClick = onShareWithAuthorities,
                tint = Color.Red
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(
                text = "Activating emergency dashcam...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Connecting to driver's camera",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Stream Error",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = error,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Connection")
            }
        }
    }
}

@Composable
private fun NoStreamView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = "No stream",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No active stream",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
