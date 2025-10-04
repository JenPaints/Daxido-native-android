package com.daxido.admin.presentation.emergency

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.daxido.core.models.*

/**
 * Admin Emergency Monitor Screen
 *
 * Allows administrators to:
 * - View all active emergency streams in real-time
 * - Monitor multiple streams simultaneously
 * - Access full-screen view of any stream
 * - Dispatch emergency services
 * - Communicate with rider/driver
 * - Mark incidents as resolved
 */
@Composable
fun AdminEmergencyMonitorScreen(
    viewModel: AdminEmergencyMonitorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    Scaffold(
        topBar = {
            AdminEmergencyTopBar(
                activeStreams = uiState.activeStreams.size,
                criticalCount = uiState.criticalCount
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView()
                }
                uiState.selectedStream != null -> {
                    // Full-screen stream view
                    FullScreenStreamView(
                        stream = uiState.selectedStream!!,
                        currentFrame = uiState.selectedStreamFrame,
                        onClose = { viewModel.clearSelectedStream() },
                        onDispatchEmergency = { viewModel.dispatchEmergencyServices(it) },
                        onContactRider = { viewModel.contactRider(it) },
                        onMarkResolved = { viewModel.markAsResolved(it) },
                        onSwitchCamera = { stream, cameraType ->
                            viewModel.switchCamera(stream, cameraType)
                        }
                    )
                }
                uiState.activeStreams.isEmpty() -> {
                    NoActiveStreamsView()
                }
                else -> {
                    ActiveStreamsGrid(
                        streams = uiState.activeStreams,
                        streamFrames = uiState.streamFrames,
                        onStreamSelected = { viewModel.selectStream(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminEmergencyTopBar(
    activeStreams: Int,
    criticalCount: Int
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.Red
                )
                Text(
                    text = "EMERGENCY MONITOR",
                    fontWeight = FontWeight.Bold
                )
                if (activeStreams > 0) {
                    Badge(containerColor = Color.Red) {
                        Text(text = activeStreams.toString())
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White
        )
    )
}

@Composable
private fun ActiveStreamsGrid(
    streams: List<EmergencyStream>,
    streamFrames: Map<String, android.graphics.Bitmap>,
    onStreamSelected: (EmergencyStream) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(streams) { stream ->
            StreamPreviewCard(
                stream = stream,
                currentFrame = streamFrames[stream.streamId],
                onClick = { onStreamSelected(stream) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamPreviewCard(
    stream: EmergencyStream,
    currentFrame: android.graphics.Bitmap?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {
        Column {
            // Video preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black)
            ) {
                if (currentFrame != null) {
                    Image(
                        bitmap = currentFrame.asImageBitmap(),
                        contentDescription = "Stream preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                // Emergency type badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    color = getEmergencyTypeColor(stream.emergencyType),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stream.emergencyType.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Live indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Stream info
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Ride #${stream.rideId.takeLast(8)}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Started ${getTimeAgo(stream.startedAt)}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stream.adminViewers.size} viewers",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stream.status.name,
                            color = getStatusColor(stream.status.name),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (stream.location != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stream.location.address.ifEmpty { "${stream.location.latitude}, ${stream.location.longitude}" },
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Stream")
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenStreamView(
    stream: EmergencyStream,
    currentFrame: android.graphics.Bitmap?,
    onClose: () -> Unit,
    onDispatchEmergency: (EmergencyStream) -> Unit,
    onContactRider: (EmergencyStream) -> Unit,
    onMarkResolved: (EmergencyStream) -> Unit,
    onSwitchCamera: ((EmergencyStream, CameraType) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Stream video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (currentFrame != null) {
                Image(
                    bitmap = currentFrame.asImageBitmap(),
                    contentDescription = "Live stream",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Stream info overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stream.emergencyType.name.replace("_", " "),
                        color = Color.Red,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ride: ${stream.rideId.takeLast(8)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Duration: ${getTimeAgo(stream.startedAt)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Admin controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🎥 CAMERA CONTROLS - New Feature!
                Text(
                    text = "Camera Controls",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Front Camera Button
                    Button(
                        onClick = { onSwitchCamera?.invoke(stream, CameraType.FRONT) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (stream.cameraType == CameraType.FRONT)
                                Color(0xFF4CAF50) else Color(0xFF424242)
                        )
                    ) {
                        Icon(Icons.Default.CameraFront, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Front")
                    }

                    // Rear Camera Button
                    Button(
                        onClick = { onSwitchCamera?.invoke(stream, CameraType.REAR) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (stream.cameraType == CameraType.REAR)
                                Color(0xFF4CAF50) else Color(0xFF424242)
                        )
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rear")
                    }
                }

                // Current Camera Indicator
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2A2A2A),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Currently viewing: ${stream.cameraType.name} camera",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Divider
                androidx.compose.material3.Divider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                // Emergency Actions
                Text(
                    text = "Emergency Actions",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onDispatchEmergency(stream) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Icon(Icons.Default.LocalPolice, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispatch")
                    }

                    OutlinedButton(
                        onClick = { onContactRider(stream) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contact")
                    }
                }

                Button(
                    onClick = { onMarkResolved(stream) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Resolved")
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
        CircularProgressIndicator()
    }
}

@Composable
private fun NoActiveStreamsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No Active Emergencies",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "All riders are safe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun getEmergencyTypeColor(type: EmergencyType): Color {
    return when (type) {
        EmergencyType.ACCIDENT -> Color(0xFFFF5252)
        EmergencyType.HARASSMENT -> Color(0xFFFF6B6B)
        EmergencyType.SUSPICIOUS_ROUTE -> Color(0xFFFFA726)
        EmergencyType.MEDICAL -> Color(0xFFEF5350)
        EmergencyType.VEHICLE_ISSUE -> Color(0xFFFFCA28)
        else -> Color(0xFFFF7043)
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        "ACTIVE" -> Color.Green
        "STARTING" -> Color.Yellow
        "ERROR" -> Color.Red
        else -> Color.Gray
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "${seconds}s ago"
    }
}
