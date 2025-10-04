package com.daxido.user.presentation.ride

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.core.location.RealTimeLocationService
import com.daxido.core.models.RideStatus
import com.daxido.core.services.RealTimeDriverMatchingService
import com.daxido.core.algorithms.RealRideAllocationEngine
import com.daxido.core.maps.DirectionsApiService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideTrackingScreen(
    rideId: String,
    onNavigateBack: () -> Unit,
    onEndRide: () -> Unit,
    viewModel: RideTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize services
    val directionsService = remember { DirectionsApiService(context) }
    val allocationEngine = remember { RealRideAllocationEngine(directionsService) }
    val driverMatchingService = remember { RealTimeDriverMatchingService(context, allocationEngine, directionsService) }
    val locationService = remember { RealTimeLocationService(context) }
    
    // Start real-time tracking
    LaunchedEffect(rideId) {
        viewModel.startRideTracking(rideId)
        
        // Simulate real-time updates
        while (true) {
            delay(5000) // Update every 5 seconds
            viewModel.updateRideStatus(RideStatus.TRIP_STARTED)
        }
    }
    
    // Track location updates with error handling
    LaunchedEffect(Unit) {
        try {
            locationService.startLocationTracking().collect { location ->
                viewModel.updateUserLocation(location.latitude, location.longitude)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("RideTrackingScreen", "Location permission error", e)
        } catch (e: Exception) {
            android.util.Log.e("RideTrackingScreen", "Location tracking error", e)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride Tracking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ride Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ride Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusChip(status = uiState.rideStatus.name)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress Indicator
                    RideProgressIndicator(
                        currentStatus = uiState.rideStatus.name,
                        onStatusChange = { /* TODO: Handle status change */ }
                    )
                }
            }
            
            // Driver Information
            if (uiState.driverInfo != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Your Driver",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Driver Avatar
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Driver",
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Driver Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.driverInfo?.name ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.driverInfo?.vehicleNumber ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Rating",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFFD700)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", uiState.driverInfo?.rating ?: 0.0),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            // Action Buttons
                            Column {
                                IconButton(
                                    onClick = { /* Call driver */ }
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { /* Message driver */ }
                                ) {
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = "Message",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Ride Details
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ride Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Pickup Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RadioButtonChecked,
                            contentDescription = "Pickup",
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pickup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.pickupLocation,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Dropoff Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Dropoff",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dropoff",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.dropoffLocation,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Fare Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimated Fare",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${String.format("%.2f", uiState.estimatedFare)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Emergency Button
            if (uiState.rideStatus == RideStatus.TRIP_STARTED) {
                Button(
                    onClick = { viewModel.triggerEmergency() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Emergency",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // End Ride Button
            if (uiState.rideStatus == RideStatus.COMPLETED) {
                Button(
                    onClick = onEndRide,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "End Ride",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "searching" -> Pair(Color(0xFFFFA726), Color.White)
        "driver_found" -> Pair(Color(0xFF4CAF50), Color.White)
        "driver_arrived" -> Pair(Color(0xFF2196F3), Color.White)
        "in_progress" -> Pair(Color(0xFF9C27B0), Color.White)
        "completed" -> Pair(Color(0xFF4CAF50), Color.White)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RideProgressIndicator(
    currentStatus: String,
    onStatusChange: () -> Unit
) {
    val statuses = listOf("searching", "driver_found", "driver_arrived", "in_progress", "completed")
    val currentIndex = statuses.indexOf(currentStatus)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEachIndexed { index, status ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentIndex) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    } else if (index == currentIndex) {
                        Icon(
                            when (status) {
                                "searching" -> Icons.Default.Search
                                "driver_found" -> Icons.Default.Person
                                "driver_arrived" -> Icons.Default.LocationOn
                                "in_progress" -> Icons.Default.DirectionsCar
                                "completed" -> Icons.Default.Check
                                else -> Icons.Default.Circle
                            },
                            contentDescription = status,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status Text
                Text(
                    text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (index <= currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Connector Line
            if (index < statuses.size - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            if (index < currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

// Data classes for ride tracking
