package com.daxido.user.presentation.ride

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.core.location.RealTimeLocationService
import com.daxido.core.models.RideStatus
import com.daxido.core.models.DriverInfo
import com.daxido.core.maps.PreciseRoutingService
import com.daxido.core.verification.OTPVerificationService
import com.daxido.core.rating.RatingService
import com.daxido.core.rating.RatingTag
import com.daxido.core.rating.UserType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteRiderRideFlow(
    rideId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (String, Double) -> Unit,
    onRideCompleted: () -> Unit,
    viewModel: CompleteRiderRideViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize services
    val routingService = remember { PreciseRoutingService(context) }
    val otpService = remember { OTPVerificationService(context) }
    val ratingService = remember { RatingService(context) }
    val locationService = remember { RealTimeLocationService(context) }
    
    // Load ride details
    LaunchedEffect(rideId) {
        viewModel.loadRideDetails(rideId)
    }
    
    // Start location tracking with error handling
    LaunchedEffect(Unit) {
        try {
            locationService.startLocationTracking().collect { location ->
                viewModel.updateRiderLocation(location.latitude, location.longitude)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("CompleteRiderRideFlow", "Location permission error", e)
        } catch (e: Exception) {
            android.util.Log.e("CompleteRiderRideFlow", "Location tracking error", e)
        }
    }
    
    // Handle ride status changes
    LaunchedEffect(uiState.rideStatus) {
        when (uiState.rideStatus) {
            RideStatus.DRIVER_ASSIGNED -> {
                delay(2000)
                viewModel.updateRideStatus(RideStatus.DRIVER_ARRIVED)
            }
            RideStatus.DRIVER_ARRIVED -> {
                // Wait for OTP verification
            }
            RideStatus.TRIP_STARTED -> {
                delay(1000)
                viewModel.updateRideStatus(RideStatus.TRIP_STARTED)
            }
            RideStatus.TRIP_ENDED -> {
                // Ride is ongoing
            }
            RideStatus.COMPLETED -> {
                // Show payment and rating screen
            }
            RideStatus.SEARCHING -> {
                // Handle searching state
            }
            RideStatus.CANCELLED -> {
                // Handle cancelled state
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride Status") },
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
            // Ride Status Progress
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ride Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    RiderProgressIndicator(
                        currentStatus = uiState.rideStatus.name,
                        onStatusChange = { /* TODO: Handle status change */ }
                    )
                }
            }
            
            // Current Stage Content
            when (uiState.rideStatus) {
                RideStatus.DRIVER_ASSIGNED -> {
                    DriverFoundContent(
                        driverInfo = uiState.driverInfo,
                        estimatedArrival = uiState.estimatedArrival.toIntOrNull() ?: 5,
                        onDriverArrived = { viewModel.updateRideStatus(RideStatus.DRIVER_ARRIVED) }
                    )
                }
                RideStatus.DRIVER_ARRIVED -> {
                    RiderOTPVerificationContent(
                        rideId = rideId,
                        userType = com.daxido.core.verification.UserType.RIDER,
                        onOTPVerified = { viewModel.updateRideStatus(RideStatus.TRIP_STARTED) },
                        onResendOTP = { scope.launch { otpService.resendOTP(rideId) } }
                    )
                }
                RideStatus.TRIP_STARTED -> {
                    RideStartingContent(
                        driverInfo = uiState.driverInfo,
                        pickupLocation = uiState.pickupLocation,
                        dropoffLocation = uiState.dropoffLocation,
                        onStartRide = { viewModel.updateRideStatus(RideStatus.TRIP_ENDED) }
                    )
                }
                RideStatus.TRIP_ENDED -> {
                    RideInProgressContent(
                        driverInfo = uiState.driverInfo,
                        dropoffLocation = uiState.dropoffLocation,
                        fare = uiState.fare,
                        onCompleteRide = { viewModel.updateRideStatus(RideStatus.COMPLETED) }
                    )
                }
                RideStatus.COMPLETED -> {
                    RideCompletionContent(
                        driverInfo = uiState.driverInfo,
                        fare = uiState.fare,
                        onPayNow = { onNavigateToPayment(rideId, uiState.fare) },
                        onRateDriver = { rating ->
                            scope.launch {
                                ratingService.submitRating(
                                    rideId = rideId,
                                    fromUserId = "rider123", // This should be from auth
                                    toUserId = uiState.driverInfo?.id ?: "",
                                    userType = UserType.RIDER,
                                    rating = rating,
                                    feedback = null,
                                    tags = emptyList()
                                )
                            }
                        },
                        onFinish = onRideCompleted
                    )
                }
                RideStatus.COMPLETED -> {
                    // Ride completed, show summary
                }
                RideStatus.SEARCHING -> {
                    // Handle searching state
                }
                RideStatus.CANCELLED -> {
                    // Handle cancelled state
                }
            }
        }
    }
}

@Composable
fun DriverFoundContent(
    driverInfo: DriverInfo?,
    estimatedArrival: Int,
    onDriverArrived: () -> Unit
) {
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
                text = "Driver Found!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            driverInfo?.let { driver ->
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
                            text = driver.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = driver.vehicleNumber,
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
                                text = String.format("%.1f", driver.rating),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Action Buttons
                    Column {
                        IconButton(onClick = { /* Call driver */ }) {
                            Icon(Icons.Default.Phone, contentDescription = "Call")
                        }
                        IconButton(onClick = { /* Message driver */ }) {
                            Icon(Icons.Default.Message, contentDescription = "Message")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated Arrival",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${estimatedArrival} minutes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onDriverArrived,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Arrived", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Driver Arrived")
            }
        }
    }
}

@Composable
fun RiderOTPVerificationContent(
    rideId: String,
    userType: com.daxido.core.verification.UserType,
    onOTPVerified: () -> Unit,
    onResendOTP: () -> Unit
) {
    var otpInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = "OTP",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Verify OTP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Enter the 4-digit OTP sent to your phone",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = otpInput,
                onValueChange = { 
                    if (it.length <= 4) {
                        otpInput = it
                        errorMessage = null
                    }
                },
                label = { Text("Enter OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (otpInput.length == 4) {
                        isVerifying = true
                        // In a real app, this would verify with the OTP service
                        onOTPVerified()
                    } else {
                        errorMessage = "Please enter a valid 4-digit OTP"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isVerifying && otpInput.length == 4
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify OTP")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(onClick = onResendOTP) {
                Text("Resend OTP")
            }
        }
    }
}

@Composable
fun RideStartingContent(
    driverInfo: DriverInfo?,
    pickupLocation: String,
    dropoffLocation: String,
    onStartRide: () -> Unit
) {
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
                text = "Ready to Start",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Ride Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.RadioButtonChecked, contentDescription = "Pickup", tint = Color.Green)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = pickupLocation, style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Dropoff", tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = dropoffLocation, style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onStartRide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Ride")
            }
        }
    }
}

@Composable
fun RideInProgressContent(
    driverInfo: DriverInfo?,
    dropoffLocation: String,
    fare: Double,
    onCompleteRide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = "Ride in Progress",
                modifier = Modifier.size(64.dp),
                tint = Color.Green
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Ride in Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Dropoff: $dropoffLocation",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Fare: ₹${String.format("%.2f", fare)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onCompleteRide,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Complete", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete Ride")
            }
        }
    }
}

@Composable
fun RideCompletionContent(
    driverInfo: DriverInfo?,
    fare: Double,
    onPayNow: () -> Unit,
    onRateDriver: (Float) -> Unit,
    onFinish: () -> Unit
) {
    var rating by remember { mutableStateOf(0f) }
    var isRatingSubmitted by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Completed",
                modifier = Modifier.size(64.dp),
                tint = Color.Green
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Ride Completed!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total Fare: ₹${String.format("%.2f", fare)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isRatingSubmitted) {
                Text(
                    text = "Rate your driver",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { rating = (index + 1).toFloat() }
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Star ${index + 1}",
                                tint = if (rating > index) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        onRateDriver(rating)
                        isRatingSubmitted = true
                    },
                    enabled = rating > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Rating")
                }
            } else {
                Text(
                    text = "Thank you for rating!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Green
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onPayNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pay Now")
                }
            }
        }
    }
}

@Composable
fun RiderProgressIndicator(
    currentStatus: String,
    onStatusChange: () -> Unit
) {
    val statuses = listOf("driver_found", "driver_arrived", "otp_verified", "in_progress", "completed")
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
                                "driver_found" -> Icons.Default.Person
                                "driver_arrived" -> Icons.Default.LocationOn
                                "otp_verified" -> Icons.Default.Security
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

// Data classes
