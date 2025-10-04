package com.daxido.user.presentation.ride

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.models.RideStatus
import com.daxido.core.models.VehicleType
import com.daxido.core.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InRideScreen(
    rideId: String,
    onAddStop: () -> Unit,
    onEndRide: () -> Unit,
    onEmergency: () -> Unit
) {
    var currentSpeed by remember { mutableStateOf(45f) }
    var distanceTraveled by remember { mutableStateOf(3.2f) }
    var estimatedTimeRemaining by remember { mutableStateOf(12) }
    var fareEstimate by remember { mutableStateOf(170.0) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var isPremiumVehicle by remember { mutableStateOf(false) }
    var acEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(12.9716, 77.5946),
            16f
        )
    }

    LaunchedEffect(Unit) {
        // Simulate speed and distance updates
        while (true) {
            delay(1000)
            currentSpeed = (30..60).random().toFloat()
            distanceTraveled += currentSpeed / 3600
            if (estimatedTimeRemaining > 0) {
                estimatedTimeRemaining--
            }
            fareEstimate = 60.0 + (distanceTraveled * 15.0) + (15 - estimatedTimeRemaining) * 2.0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL,
                isTrafficEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = true
            )
        ) {
            // Route polyline would be displayed here
            Polyline(
                points = listOf(
                    LatLng(12.9716, 77.5946),
                    LatLng(12.9756, 77.6068),
                    LatLng(12.9356, 77.6068)
                ),
                color = MapRouteColor,
                width = 12f
            )
        }

        // Top Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        currentSpeed > 80 -> Color.Red.copy(alpha = 0.1f)
                                        currentSpeed > 60 -> DaxidoWarning.copy(alpha = 0.1f)
                                        else -> StatusOnline.copy(alpha = 0.1f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentSpeed.toInt().toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        currentSpeed > 80 -> Color.Red
                                        currentSpeed > 60 -> DaxidoWarning
                                        else -> StatusOnline
                                    }
                                )
                                Text(
                                    text = "km/h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Trip Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Trip in Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusOnline
                        )
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = String.format("%.1f km", distanceTraveled),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Distance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "$estimatedTimeRemaining min",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "ETA",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Fare Estimate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "₹${fareEstimate.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoGold
                        )
                        Text(
                            text = "Estimate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Route Deviation Alert
                AnimatedVisibility(
                    visible = currentSpeed < 10 && estimatedTimeRemaining > 5
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DaxidoWarning.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DaxidoWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Heavy traffic ahead. ETA may increase.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Control Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Premium Controls (AC, Music)
                if (isPremiumVehicle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // AC Control
                        Card(
                            onClick = { acEnabled = !acEnabled },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (acEnabled) DaxidoPaleGold else DaxidoLightGray
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = "AC",
                                    tint = if (acEnabled) DaxidoGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (acEnabled) "AC On" else "AC Off",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        // Music Control
                        Card(
                            onClick = { musicEnabled = !musicEnabled },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (musicEnabled) DaxidoPaleGold else DaxidoLightGray
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Music",
                                    tint = if (musicEnabled) DaxidoGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (musicEnabled) "Music On" else "Music Off",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Stop
                    OutlinedButton(
                        onClick = { showStopDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Stop")
                    }

                    // Share Trip
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Emergency Button
                Button(
                    onClick = { showEmergencyDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SOS Emergency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Add Stop Dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Text("Add Stop", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Enter the address for your stop:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        placeholder = { Text("Enter address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Additional charges may apply",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    onAddStop()
                }) {
                    Text("Add Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Emergency Dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Emergency Alert",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    "This will immediately:\n" +
                    "• Alert emergency contacts\n" +
                    "• Share your live location\n" +
                    "• Contact local authorities\n" +
                    "• Record audio/video",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onEmergency()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirm Emergency")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}