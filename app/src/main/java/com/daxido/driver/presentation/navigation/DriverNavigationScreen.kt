package com.daxido.driver.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.daxido.core.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

data class NavigationInstruction(
    val direction: String,
    val distance: String,
    val street: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverNavigationScreen(
    tripId: String,
    currentLocation: LatLng,
    destination: LatLng,
    customerName: String,
    isPickupPhase: Boolean,
    onNavigationAction: (String) -> Unit,
    onEndNavigation: () -> Unit
) {
    var currentSpeed by remember { mutableStateOf(45) }
    var distanceRemaining by remember { mutableStateOf(2.3) }
    var timeRemaining by remember { mutableStateOf(8) }
    var showRouteOptions by remember { mutableStateOf(false) }
    var selectedRoute by remember { mutableStateOf(0) }
    var isNavigationMuted by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation,
            17f
        )
    }

    val currentInstruction = NavigationInstruction(
        direction = "Turn Right",
        distance = "200m",
        street = "MG Road",
        icon = Icons.Default.TurnRight
    )

    val upcomingInstructions = listOf(
        NavigationInstruction("Continue", "1.2km", "Brigade Road", Icons.Default.ArrowUpward),
        NavigationInstruction("Turn Left", "500m", "Church Street", Icons.Default.TurnLeft)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Navigation Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL,
                isTrafficEnabled = true,
                isBuildingEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = true,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = true
            )
        ) {
            // Current position marker
            Marker(
                state = MarkerState(position = currentLocation),
                title = "You",
                icon = null
            )

            // Destination marker
            Marker(
                state = MarkerState(position = destination),
                title = if (isPickupPhase) "Pickup" else "Drop",
                icon = null
            )

            // Navigation route
            Polyline(
                points = listOf(
                    currentLocation,
                    LatLng(currentLocation.latitude + 0.002, currentLocation.longitude + 0.001),
                    LatLng(currentLocation.latitude + 0.003, currentLocation.longitude + 0.003),
                    destination
                ),
                color = MapRouteColor,
                width = 14f
            )

            // Alternative routes (if showing)
            if (showRouteOptions) {
                Polyline(
                    points = listOf(
                        currentLocation,
                        LatLng(currentLocation.latitude + 0.001, currentLocation.longitude + 0.002),
                        destination
                    ),
                    color = Color.Gray.copy(alpha = 0.5f),
                    width = 10f
                )
            }
        }

        // Top Navigation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = DaxidoDarkBrown
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Current Instruction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(DaxidoGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentInstruction.icon,
                            contentDescription = null,
                            tint = DaxidoWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = currentInstruction.distance,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoWhite
                        )
                        Text(
                            text = "${currentInstruction.direction} onto ${currentInstruction.street}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = DaxidoWhite.copy(alpha = 0.9f)
                        )
                    }

                    // Mute/Unmute button
                    IconButton(
                        onClick = { isNavigationMuted = !isNavigationMuted }
                    ) {
                        Icon(
                            imageVector = if (isNavigationMuted)
                                Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Toggle voice",
                            tint = DaxidoWhite
                        )
                    }
                }

                // Lane Guidance (if applicable)
                AnimatedVisibility(
                    visible = currentInstruction.direction.contains("Turn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(3) { lane ->
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(30.dp)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (lane == 1) DaxidoGold.copy(alpha = 0.3f)
                                        else DaxidoWhite.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(lane) {
                                        0 -> Icons.Default.ArrowUpward
                                        1 -> Icons.Default.TurnRight
                                        else -> Icons.Default.ArrowUpward
                                    },
                                    contentDescription = null,
                                    tint = if (lane == 1) DaxidoGold else DaxidoWhite.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
                // Trip Info Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DaxidoPaleGold.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DaxidoGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " $customerName",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPickupPhase) StatusOnline.copy(alpha = 0.1f)
                                   else Color.Red.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (isPickupPhase) "To Pickup" else "To Drop",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isPickupPhase) StatusOnline else Color.Red
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$currentSpeed",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSpeed > 60) DaxidoWarning else StatusOnline
                        )
                        Text(
                            text = "km/h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$distanceRemaining km",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$timeRemaining min",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ETA",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Route Options
                    OutlinedButton(
                        onClick = { showRouteOptions = !showRouteOptions },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Routes")
                    }

                    // Report Issue
                    OutlinedButton(
                        onClick = { onNavigationAction("report_issue") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report")
                    }

                    // End Navigation
                    Button(
                        onClick = onEndNavigation,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPickupPhase) StatusOnline else DaxidoGold
                        )
                    ) {
                        Icon(
                            imageVector = if (isPickupPhase)
                                Icons.Default.Person else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPickupPhase) "Arrived" else "Complete")
                    }
                }

                // Upcoming turns preview
                AnimatedVisibility(visible = upcomingInstructions.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            upcomingInstructions.take(2).forEach { instruction ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = instruction.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = " ${instruction.distance}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Speed Limit Warning
        AnimatedVisibility(
            visible = currentSpeed > 80,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 120.dp, end = 16.dp),
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red
                ),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "80",
                            fontWeight = FontWeight.Bold,
                            color = DaxidoWhite,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "LIMIT",
                            fontSize = 10.sp,
                            color = DaxidoWhite
                        )
                    }
                }
            }
        }
    }
}