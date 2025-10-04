package com.daxido.user.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.daxido.core.models.VehicleType
import com.daxido.core.location.LocationData
import com.daxido.core.maps.PlacesApiService
import com.daxido.core.maps.DirectionsApiService
import com.daxido.core.services.RealTimeDriverMatchingService
import com.daxido.core.algorithms.RealRideAllocationEngine
import com.daxido.core.location.RealTimeLocationService
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.CameraUpdateFactory
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapScreen(
    onNavigateToLocationSearch: () -> Unit,
    onNavigateToRideBooking: (String, String, String, String, Double, Double, Double, Double) -> Unit,
    onNavigateToRideTracking: (String) -> Unit,
    onNavigateToAiAssistant: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // State for location tracking
    var shouldStartLocationTracking by remember { mutableStateOf(false) }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, start location tracking
            viewModel.startLocationTracking()
            shouldStartLocationTracking = true
        }
    }
    
    // Check and request location permissions on first composition
    LaunchedEffect(Unit) {
        val hasLocationPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
            
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.startLocationTracking()
            shouldStartLocationTracking = true
        }
    }
    
    // Real-time location tracking
    val locationService = remember { RealTimeLocationService(context) }
    var currentLocation by remember { mutableStateOf<LocationData?>(null) }
    
    // Start location tracking only when permissions are granted
    LaunchedEffect(shouldStartLocationTracking) {
        if (shouldStartLocationTracking) {
            try {
                locationService.startLocationTracking().collect { location ->
                    currentLocation = location
                    viewModel.updateCurrentLocation(location)
                }
            } catch (e: SecurityException) {
                android.util.Log.e("HomeMapScreen", "Location permission error", e)
            } catch (e: Exception) {
                android.util.Log.e("HomeMapScreen", "Location tracking error", e)
            }
        }
    }
    
    // Places API service
    val placesService = remember { PlacesApiService(context) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<com.daxido.core.maps.PlaceSearchResult>>(emptyList()) }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            placesService.searchPlaces(searchQuery).collect { results ->
                searchResults = results
            }
        }
    }
    
    // Directions API service
    val directionsService = remember { DirectionsApiService(context) }
    var estimatedFare by remember { mutableStateOf(0.0) }
    var estimatedTime by remember { mutableStateOf(0) }
    
    // Driver matching service
    val driverMatchingService = remember { RealTimeDriverMatchingService(context, RealRideAllocationEngine(directionsService), directionsService) }
    
    // Camera position state for Google Maps
    val defaultLocation = LatLng(12.9716, 77.5946) // Bangalore coordinates
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation, 
            15f
        )
    }
    
    // Update camera position when location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f)
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daxido") },
                actions = {
                    IconButton(onClick = { /* Profile */ }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Maps
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
                    myLocationButtonEnabled = true,
                    mapToolbarEnabled = false,
                    compassEnabled = true,
                    rotationGesturesEnabled = true,
                    tiltGesturesEnabled = true
                )
            ) {
                // Current location marker
                currentLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                        title = "Your Location"
                    )
                }
                
                // Nearby drivers markers
                uiState.nearbyDrivers.forEach { driver ->
                    Marker(
                        state = MarkerState(position = LatLng(driver.location.latitude, driver.location.longitude)),
                        title = "Driver - ${driver.vehicleType.name}",
                        snippet = "ETA: ${driver.estimatedArrival} mins"
                    )
                }
            }
            
            // Search Section Overlay
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Pickup Location
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToLocationSearch() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RadioButtonChecked,
                            contentDescription = "Pickup",
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.pickupLocation?.address ?: "Pickup location",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Dropoff Location
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToLocationSearch() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Dropoff",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.dropLocation?.address ?: "Where to?",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search")
                    }
                }
            }
            
            // Vehicle Type Selection Overlay
            if (uiState.pickupLocation != null && uiState.dropLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Choose your ride",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(VehicleType.values()) { vehicleType ->
                                VehicleTypeCard(
                                    vehicleType = vehicleType,
                                    isSelected = uiState.selectedVehicleType == vehicleType,
                                    estimatedFare = estimatedFare,
                                    estimatedTime = estimatedTime,
                                    onSelect = { viewModel.selectVehicleType(vehicleType) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Quick Actions Overlay (when no vehicle selection is shown)
            if (uiState.pickupLocation == null || uiState.dropLocation == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
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
                            icon = Icons.Default.Schedule,
                            text = "Schedule",
                            onClick = { /* Schedule ride */ }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Share,
                            text = "Share",
                            onClick = { /* Share location */ }
                        )
                        QuickActionButton(
                            icon = Icons.Default.SmartToy,
                            text = "AI Assistant",
                            onClick = { onNavigateToAiAssistant() }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Help,
                            text = "Help",
                            onClick = { /* Help */ }
                        )
                    }
                }
                }
            }
            
            // Book Ride Button Overlay
            if (uiState.pickupLocation != null && uiState.dropLocation != null && uiState.selectedVehicleType != null) {
                Button(
                    onClick = {
                        val pickupLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)
                        val dropoffLatLng = LatLng(0.0, 0.0) // This should be from selected location
                        
                        onNavigateToRideBooking(
                            uiState.pickupLocation?.address ?: "",
                            uiState.dropLocation?.address ?: "",
                            uiState.selectedVehicleType!!.name,
                            "ride_${System.currentTimeMillis()}",
                            pickupLatLng.latitude,
                            pickupLatLng.longitude,
                            dropoffLatLng.latitude,
                            dropoffLatLng.longitude
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Book ${uiState.selectedVehicleType!!.name} Ride",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleTypeCard(
    vehicleType: VehicleType,
    isSelected: Boolean,
    estimatedFare: Double,
    estimatedTime: Int,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (vehicleType) {
                        VehicleType.AUTO -> Icons.Default.DirectionsCar
                        VehicleType.CAR -> Icons.Default.DirectionsCar
                        VehicleType.BIKE -> Icons.Default.TwoWheeler
                        VehicleType.PREMIUM -> Icons.Default.CarRental
                    },
                    contentDescription = vehicleType.name,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Vehicle Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = vehicleType.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Est. ${estimatedTime}min • ₹${String.format("%.2f", estimatedFare)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Selection Indicator
            if (isSelected) {
                Icon(
                    Icons.Default.RadioButtonChecked,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Not selected",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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