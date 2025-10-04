package com.daxido.user.presentation.payment

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
import com.daxido.core.models.PaymentType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.core.config.AppConfig
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    rideId: String,
    amount: Double,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize payment amount
    LaunchedEffect(rideId, amount) {
        viewModel.setPaymentAmount(amount)
    }

    // Handle payment success
    LaunchedEffect(uiState.isPaymentSuccessful) {
        if (uiState.isPaymentSuccessful) {
            onPaymentSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
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
            // Payment Amount Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${String.format("%.2f", uiState.totalAmount)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (uiState.discount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Discount: -₹${String.format("%.2f", uiState.discount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Green
                        )
                    }
                }
            }
            
            // Payment Methods
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
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Wallet Payment
                    PaymentMethodCard(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Daxido Wallet",
                        subtitle = "Balance: ₹${String.format("%.2f", uiState.walletBalance)}",
                        isSelected = uiState.selectedPaymentMethod == PaymentType.WALLET,
                        onSelect = { viewModel.selectPaymentMethod(PaymentType.WALLET) },
                        isEnabled = uiState.walletBalance >= uiState.totalAmount
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Credit Card Payment
                    PaymentMethodCard(
                        icon = Icons.Default.CreditCard,
                        title = "Credit Card",
                        subtitle = "**** 1234",
                        isSelected = uiState.selectedPaymentMethod == PaymentType.CARD,
                        onSelect = { viewModel.selectPaymentMethod(PaymentType.CARD) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // UPI Payment
                    PaymentMethodCard(
                        icon = Icons.Default.Payment,
                        title = "UPI",
                        subtitle = "Pay with UPI",
                        isSelected = uiState.selectedPaymentMethod == PaymentType.UPI,
                        onSelect = { viewModel.selectPaymentMethod(PaymentType.UPI) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cash Payment
                    PaymentMethodCard(
                        icon = Icons.Default.Money,
                        title = "Cash",
                        subtitle = "Pay after ride",
                        isSelected = uiState.selectedPaymentMethod == PaymentType.CASH,
                        onSelect = { viewModel.selectPaymentMethod(PaymentType.CASH) }
                    )
                }
            }
            
            // Promo Code Section
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
                        text = "Promo Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.promoCode,
                            onValueChange = { /* TODO: Update promo code */ },
                            label = { Text("Enter promo code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.applyPromoCode(uiState.promoCode) },
                            enabled = uiState.promoCode.isNotEmpty() && !uiState.isApplyingPromo
                        ) {
                            if (uiState.isApplyingPromo) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Apply")
                            }
                        }
                    }
                    
                    if (uiState.appliedPromoCode != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Promo discount applied",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Green
                            )
                            Text(
                                text = "-₹${String.format("%.2f", uiState.discount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (uiState.promoError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.promoError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
            
            // Payment Summary
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
                        text = "Payment Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ride fare",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${String.format("%.2f", uiState.rideFare)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (uiState.tip > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tip",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "₹${String.format("%.2f", uiState.tip)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (uiState.discount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Discount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Green
                            )
                            Text(
                                text = "-₹${String.format("%.2f", uiState.discount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Green
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HorizontalDivider()
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${String.format("%.2f", uiState.totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Pay Button
            Button(
                onClick = {
                    scope.launch {
                        // TODO: Pass actual ride ID from navigation or state
                        viewModel.processPayment("RIDE_${System.currentTimeMillis()}", uiState.totalAmount, uiState.selectedPaymentMethod)
                        // Payment success/failure is handled via uiState observation
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = uiState.selectedPaymentMethod != null && !uiState.isProcessingPayment
            ) {
                if (uiState.isProcessingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text(
                        text = "Pay ₹${String.format("%.2f", uiState.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Payment Error
            if (uiState.paymentError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.paymentError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red
                        )
                    }
                }
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
    onSelect: () -> Unit,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { onSelect() },
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
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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

// Data classes
data class PromoCode(
    val code: String,
    val discount: Double,
    val description: String,
    val validUntil: Long
)