package com.daxido.driver.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.daxido.core.services.RealTimeDriverMatchingService
import com.daxido.core.models.RecentRide
import com.daxido.core.algorithms.RealRideAllocationEngine
import com.daxido.core.maps.DirectionsApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToEarnings: () -> Unit,
    onNavigateToRideRequest: (String) -> Unit,
    onNavigateToNavigation: (String) -> Unit,
    viewModel: DriverHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize services
    val directionsService = remember { DirectionsApiService(context) }
    val allocationEngine = remember { RealRideAllocationEngine(directionsService) }
    val driverMatchingService = remember { RealTimeDriverMatchingService(context, allocationEngine, directionsService) }
    val locationService = remember { RealTimeLocationService(context) }
    
    // Start location tracking with error handling
    LaunchedEffect(Unit) {
        try {
            locationService.startLocationTracking().collect { location ->
                viewModel.updateLocation(location.latitude, location.longitude)
                // Update driver location in Firebase
                driverMatchingService.updateDriverLocation(
                    driverId = "driver123", // This should be from auth
                    location = location,
                    isAvailable = uiState.isOnline
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.e("DriverHomeScreen", "Location permission error", e)
        } catch (e: Exception) {
            android.util.Log.e("DriverHomeScreen", "Location tracking error", e)
        }
    }
    
    // Start monitoring for ride requests
    LaunchedEffect(uiState.isOnline) {
        if (uiState.isOnline) {
            driverMatchingService.startDriverMonitoring()
            // Simulate ride request monitoring
            while (uiState.isOnline) {
                delay(10000) // Check every 10 seconds
                viewModel.checkForRideRequests()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
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
            // Online/Offline Toggle
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
                        Column {
                            Text(
                                text = if (uiState.isOnline) "You're Online" else "You're Offline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isOnline) "Ready to accept rides" else "Not accepting rides",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = uiState.isOnline,
                            onCheckedChange = { viewModel.toggleOnlineStatus() }
                        )
                    }
                    
                    if (uiState.isOnline) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Earnings",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "₹${String.format("%.2f", uiState.todaysEarnings)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Quick Stats
            if (uiState.isOnline) {
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
                            text = "Today's Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCard(
                                icon = Icons.Default.DirectionsCar,
                                title = "Rides",
                                value = uiState.todaysRides.toString(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatCard(
                                icon = Icons.Default.Star,
                                title = "Rating",
                                value = String.format("%.1f", uiState.currentRating),
                                color = Color(0xFFFFD700)
                            )
                            StatCard(
                                icon = Icons.Default.Timer,
                                title = "Online",
                                value = "${uiState.onlineHours}h",
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
            
            // Current Ride Request
            if (uiState.currentRideRequest != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "New Ride Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Ride Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RadioButtonChecked,
                                contentDescription = "Pickup",
                                tint = Color.Green
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.currentRideRequest?.pickupLocation ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Dropoff",
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.currentRideRequest?.dropoffLocation ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fare: ₹${String.format("%.2f", uiState.currentRideRequest?.fare ?: 0.0)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Distance: ${uiState.currentRideRequest?.distance ?: 0}m",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        driverMatchingService.handleDriverResponse(
                                            rideId = uiState.currentRideRequest?.rideId ?: "",
                                            driverId = "driver123",
                                            response = com.daxido.core.services.DriverResponse.REJECT
                                        )
                                        viewModel.rejectRideRequest(uiState.currentRideRequest?.rideId ?: "")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("Reject")
                            }
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        driverMatchingService.handleDriverResponse(
                                            rideId = uiState.currentRideRequest?.rideId ?: "",
                                            driverId = "driver123",
                                            response = com.daxido.core.services.DriverResponse.ACCEPT
                                        )
                                        viewModel.acceptRideRequest(uiState.currentRideRequest?.rideId ?: "")
                                        onNavigateToRideRequest(uiState.currentRideRequest?.rideId ?: "")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Green
                                )
                            ) {
                                Text("Accept")
                            }
                        }
                    }
                }
            }
            
            // Recent Rides
            if (uiState.recentRides.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                text = "Recent Rides",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { /* View all rides */ }) {
                                Text("View All")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.recentRides.take(3)) { ride ->
                                RecentRideCard(
                                    ride = ride,
                                    onClick = { onNavigateToNavigation(ride.id) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Quick Actions
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
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.AccountBalanceWallet,
                            text = "Earnings",
                            onClick = onNavigateToEarnings
                        )
                        QuickActionButton(
                            icon = Icons.Default.Help,
                            text = "Help",
                            onClick = { /* Help */ }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Settings,
                            text = "Settings",
                            onClick = { /* Settings */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecentRideCard(
    ride: RecentRide,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = "Ride",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ride.pickupLocation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ride.dropoffLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${String.format("%.2f", ride.fare)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

// Data classes

