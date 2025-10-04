package com.daxido.driver.presentation.ride

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
import com.daxido.core.models.CustomerInfo
import com.daxido.core.services.RealTimeDriverMatchingService
import com.daxido.core.algorithms.RealRideAllocationEngine
import com.daxido.core.maps.DirectionsApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideRequestScreen(
    requestId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNavigation: (String) -> Unit,
    viewModel: RideRequestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize services
    val directionsService = remember { DirectionsApiService(context) }
    val allocationEngine = remember { RealRideAllocationEngine(directionsService) }
    val driverMatchingService = remember { RealTimeDriverMatchingService(context, allocationEngine, directionsService) }
    val locationService = remember { RealTimeLocationService(context) }
    
    // Load ride request details
    LaunchedEffect(requestId) {
        viewModel.loadRideRequest(requestId)
    }
    
    // Start location tracking with error handling
    LaunchedEffect(Unit) {
        try {
            locationService.startLocationTracking().collect { location ->
                viewModel.updateDriverLocation(location.latitude, location.longitude)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("RideRequestScreen", "Location permission error", e)
        } catch (e: Exception) {
            android.util.Log.e("RideRequestScreen", "Location tracking error", e)
        }
    }
    
    // Simulate ride status updates
    LaunchedEffect(uiState.rideStatus) {
        when (uiState.rideStatus) {
            RideStatus.DRIVER_ASSIGNED -> {
                delay(2000)
                viewModel.updateRideStatus(RideStatus.DRIVER_ARRIVED)
            }
            RideStatus.DRIVER_ARRIVED -> {
                delay(3000)
                viewModel.updateRideStatus(RideStatus.TRIP_STARTED)
            }
            RideStatus.TRIP_STARTED -> {
                delay(5000)
                viewModel.updateRideStatus(RideStatus.COMPLETED)
            }
            RideStatus.COMPLETED -> {
                // Ride completed
            }
            RideStatus.SEARCHING -> {
                // Handle searching state
            }
            RideStatus.TRIP_ENDED -> {
                // Handle trip ended state
            }
            RideStatus.CANCELLED -> {
                // Handle cancelled state
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride Request") },
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
            
            // Customer Information
            if (uiState.customerInfo != null) {
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
                            text = "Customer Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Customer Avatar
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Customer",
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Customer Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.customerInfo?.name ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.customerInfo?.phone ?: "",
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
                                        text = String.format("%.1f", uiState.customerInfo?.rating ?: 0.0),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            // Action Buttons
                            Column {
                                IconButton(
                                    onClick = { /* Call customer */ }
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { /* Message customer */ }
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
                        if (uiState.rideStatus == RideStatus.DRIVER_ARRIVED || uiState.rideStatus == RideStatus.TRIP_STARTED) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color.Green
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
                        if (uiState.rideStatus == RideStatus.COMPLETED) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color.Green
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
                            text = "Fare",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${String.format("%.2f", uiState.fare)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${uiState.distance}m",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimated Time",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${uiState.estimatedTime}min",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Action Buttons
            when (uiState.rideStatus) {
                RideStatus.DRIVER_ASSIGNED -> {
                    Button(
                        onClick = { viewModel.updateRideStatus(RideStatus.DRIVER_ARRIVED) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        )
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Arrived",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I've Arrived",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                RideStatus.DRIVER_ARRIVED -> {
                    Button(
                        onClick = { 
                            viewModel.updateRideStatus(RideStatus.TRIP_STARTED)
                            onNavigateToNavigation(requestId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Ride",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                RideStatus.TRIP_STARTED -> {
                    Button(
                        onClick = { viewModel.updateRideStatus(RideStatus.COMPLETED) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Complete",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Complete Ride",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                RideStatus.COMPLETED -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Green.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Green
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ride Completed!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                            Text(
                                text = "Earnings: ₹${String.format("%.2f", uiState.fare)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                RideStatus.SEARCHING -> {
                    // Handle searching state
                }
                RideStatus.TRIP_ENDED -> {
                    // Handle trip ended state
                }
                RideStatus.CANCELLED -> {
                    // Handle cancelled state
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "accepted" -> Pair(Color(0xFF4CAF50), Color.White)
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

// Data classes
