package com.daxido.user.presentation.ride

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
import com.daxido.core.models.VehicleType
import com.daxido.core.models.PaymentType
import com.daxido.core.models.PaymentMethod
import com.daxido.core.services.RealTimeDriverMatchingService
import com.daxido.core.algorithms.RealRideAllocationEngine
import com.daxido.core.maps.DirectionsApiService
import com.daxido.core.maps.PlacesApiService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideBookingScreen(
    pickupLocation: String,
    dropoffLocation: String,
    vehicleType: String,
    rideId: String,
    pickupLat: Double,
    pickupLng: Double,
    dropoffLat: Double,
    dropoffLng: Double,
    onNavigateBack: () -> Unit,
    onNavigateToRideTracking: (String) -> Unit,
    viewModel: RideBookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize services
    val directionsService = remember { DirectionsApiService(context) }
    val placesService = remember { PlacesApiService(context) }
    val allocationEngine = remember { RealRideAllocationEngine(directionsService) }
    val driverMatchingService = remember { RealTimeDriverMatchingService(context, allocationEngine, directionsService) }
    
    // Calculate fare and ETA
    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods()
        viewModel.loadWalletBalance()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Ride") },
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
            // Ride DuiState.estimatedTimeils Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
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
                                text = pickupLocation,
                                style = MaterialTheme.typography.bodyLarge
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
                                text = dropoffLocation,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            // Vehicle Type and Fare DuiState.estimatedTimeils
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                        Column {
                            Text(
                                text = vehicleType.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Est. ${uiState.estimatedTime}min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "₹${String.format("%.2f", uiState.estimatedFare)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Fare Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Base fare",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${String.format("%.2f", getBaseFare(vehicleType))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance (${uiState.estimatedDistance?.toInt() ?: 0}m)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${String.format("%.2f", getDistanceFare(uiState.estimatedDistance?.toInt() ?: 0, vehicleType))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time (${uiState.estimatedTime ?: 0}min)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${String.format("%.2f", getTimeFare((uiState.estimatedTime ?: 0) / 60, vehicleType))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Payment Method Selection
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
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PaymentMethodCard(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Wallet",
                        subtitle = "₹${String.format("%.2f", uiState.walletBalance)}",
                        isSelected = uiState.selectedPaymentMethod?.type == PaymentType.WALLET,
                        onSelect = { viewModel.selectPaymentMethod(PaymentMethod(type = PaymentType.WALLET)) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PaymentMethodCard(
                        icon = Icons.Default.CreditCard,
                        title = "Credit Card",
                        subtitle = "**** 1234",
                        isSelected = uiState.selectedPaymentMethod?.type == PaymentType.CARD,
                        onSelect = { viewModel.selectPaymentMethod(PaymentMethod(type = PaymentType.CARD)) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PaymentMethodCard(
                        icon = Icons.Default.Payment,
                        title = "Cash",
                        subtitle = "Pay after ride",
                        isSelected = uiState.selectedPaymentMethod?.type == PaymentType.CASH,
                        onSelect = { viewModel.selectPaymentMethod(PaymentMethod(type = PaymentType.CASH)) }
                    )
                }
            }
            
            // Promo Code Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.promoCode,
                            onValueChange = { viewModel.updatePromoCode(it) },
                            label = { Text("Promo Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = uiState.promoCodeError != null,
                            supportingText = if (uiState.promoCodeError != null) {
                                { Text(uiState.promoCodeError!!) }
                            } else null,
                            enabled = uiState.appliedPromo == null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (uiState.appliedPromo == null) {
                            Button(
                                onClick = { viewModel.applyPromoCode() },
                                enabled = uiState.promoCode.isNotEmpty() && !uiState.isApplyingPromo
                            ) {
                                if (uiState.isApplyingPromo) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("Apply")
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.removePromoCode() }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }

                    if (uiState.appliedPromo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Promo discount",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = uiState.appliedPromo!!.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "-₹${String.format("%.2f", uiState.estimatedFare?.discount ?: 0.0)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }
                    }
                }
            }
            
            // Book Ride Button
            Button(
                onClick = {
                    scope.launch {
                        val result = driverMatchingService.requestRide(
                            userId = "user123", // This should be from auth
                            pickupLocation = LatLng(pickupLat, pickupLng),
                            dropoffLocation = LatLng(dropoffLat, dropoffLng),
                            vehicleType = vehicleType,
                            fareEstimate = calculateFare(uiState.estimatedDistance?.toInt() ?: 0, uiState.estimatedTime ?: 0, vehicleType)
                        )
                        
                        when (result) {
                            is com.daxido.core.services.RideRequestResult.Success -> {
                                onNavigateToRideTracking(result.rideId)
                            }
                            is com.daxido.core.services.RideRequestResult.Failure -> {
                                // Handle error
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Book Ride - ₹${String.format("%.2f", calculateFare(uiState.estimatedDistance?.toInt() ?: 0, uiState.estimatedTime ?: 0, vehicleType))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
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
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

// Helper functions for fare calculation
private fun calculateFare(distance: Int, duration: Int, vehicleType: String): Double {
    val baseFare = getBaseFare(vehicleType)
    val distanceFare = getDistanceFare(distance, vehicleType)
    val timeFare = getTimeFare(duration / 60, vehicleType)
    return baseFare + distanceFare + timeFare
}

private fun getBaseFare(vehicleType: String): Double {
    return when (vehicleType.lowercase()) {
        "economy" -> 2.0
        "comfort" -> 3.0
        "premium" -> 5.0
        "luxury" -> 8.0
        "suv" -> 4.0
        "bike" -> 1.0
        else -> 2.5
    }
}

private fun getDistanceFare(distance: Int, vehicleType: String): Double {
    val distanceKm = distance / 1000.0
    val ratePerKm = when (vehicleType.lowercase()) {
        "economy" -> 1.5
        "comfort" -> 2.0
        "premium" -> 3.0
        "luxury" -> 5.0
        "suv" -> 2.5
        "bike" -> 0.8
        else -> 1.8
    }
    return distanceKm * ratePerKm
}

private fun getTimeFare(timeMinutes: Int, vehicleType: String): Double {
    val ratePerMinute = when (vehicleType.lowercase()) {
        "economy" -> 0.3
        "comfort" -> 0.4
        "premium" -> 0.6
        "luxury" -> 1.0
        "suv" -> 0.5
        "bike" -> 0.2
        else -> 0.35
    }
    return timeMinutes * ratePerMinute
}