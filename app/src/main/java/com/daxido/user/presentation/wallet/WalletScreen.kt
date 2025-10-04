package com.daxido.user.presentation.wallet

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
import com.daxido.core.models.Transaction
import com.daxido.core.models.TransactionType
import com.daxido.core.models.TransactionStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddMoney: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Load wallet data
    LaunchedEffect(Unit) {
        viewModel.loadWalletData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet") },
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
            // Wallet Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Wallet Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${String.format("%.2f", uiState.walletBalance)}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onNavigateToAddMoney,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Add Money",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        OutlinedButton(
                            onClick = onNavigateToTransactionHistory,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                            )
                        ) {
                            Text(
                                text = "History",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Quick Actions
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
                            icon = Icons.Default.Add,
                            text = "Add Money",
                            onClick = onNavigateToAddMoney
                        )
                        QuickActionButton(
                            icon = Icons.Default.History,
                            text = "History",
                            onClick = onNavigateToTransactionHistory
                        )
                        QuickActionButton(
                            icon = Icons.Default.Share,
                            text = "Share",
                            onClick = { /* Share wallet */ }
                        )
                    }
                }
            }
            
            // Recent Transactions
            if (uiState.recentTransactions.isNotEmpty()) {
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
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToTransactionHistory) {
                                Text("View All")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.recentTransactions.take(5)) { transaction ->
                                TransactionCard(
                                    transaction = transaction,
                                    onClick = { /* View transaction details */ }
                                )
                            }
                        }
                    }
                }
            }
            
            // Wallet Features
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
                        text = "Wallet Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    WalletFeatureCard(
                        icon = Icons.Default.Security,
                        title = "Secure Payments",
                        description = "Your money is safe with bank-level security"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    WalletFeatureCard(
                        icon = Icons.Default.Speed,
                        title = "Instant Payments",
                        description = "Pay for rides instantly without any delays"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    WalletFeatureCard(
                        icon = Icons.Default.Star,
                        title = "Cashback Rewards",
                        description = "Earn cashback on every ride payment"
                    )
                }
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

@Composable
fun TransactionCard(
    transaction: Transaction,
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
            // Transaction Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (transaction.type) {
                            TransactionType.CREDIT -> Color.Green.copy(alpha = 0.1f)
                            TransactionType.DEBIT -> Color.Red.copy(alpha = 0.1f)
                            TransactionType.REFUND -> Color.Blue.copy(alpha = 0.1f)
                            TransactionType.DEPOSIT -> Color.Green.copy(alpha = 0.1f)
                            TransactionType.WITHDRAWAL -> Color.Red.copy(alpha = 0.1f)
                            TransactionType.RIDE_FARE -> Color(0xFFFF9800) // Orange
                            TransactionType.PROMO_BONUS -> Color(0xFF9C27B0) // Purple
                            TransactionType.REFERRAL_BONUS -> Color(0xFF9C27B0) // Purple
                            TransactionType.RIDE_PAYMENT -> Color(0xFFFF9800).copy(alpha = 0.1f)
                            TransactionType.WALLET_TOP_UP -> Color.Green.copy(alpha = 0.1f)
                            TransactionType.CASHBACK -> Color.Blue.copy(alpha = 0.1f)
                            TransactionType.PENALTY -> Color.Red.copy(alpha = 0.1f)
                            TransactionType.COMMISSION -> Color(0xFFFFEB3B).copy(alpha = 0.1f)
                            TransactionType.BONUS -> Color(0xFF9C27B0).copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (transaction.type) {
                        TransactionType.CREDIT -> Icons.Default.Add
                        TransactionType.DEBIT -> Icons.Default.Remove
                        TransactionType.REFUND -> Icons.Default.Undo
                        TransactionType.DEPOSIT -> Icons.Default.Add
                        TransactionType.WITHDRAWAL -> Icons.Default.Remove
                        TransactionType.RIDE_FARE -> Icons.Default.LocalTaxi
                        TransactionType.PROMO_BONUS -> Icons.Default.CardGiftcard
                        TransactionType.REFERRAL_BONUS -> Icons.Default.CardGiftcard
                        TransactionType.RIDE_PAYMENT -> Icons.Default.LocalTaxi
                        TransactionType.WALLET_TOP_UP -> Icons.Default.Add
                        TransactionType.CASHBACK -> Icons.Default.Undo
                        TransactionType.PENALTY -> Icons.Default.Warning
                        TransactionType.COMMISSION -> Icons.Default.AttachMoney
                        TransactionType.BONUS -> Icons.Default.CardGiftcard
                    },
                    contentDescription = transaction.type.name,
                    modifier = Modifier.size(20.dp),
                    tint = when (transaction.type) {
                        TransactionType.CREDIT -> Color.Green
                        TransactionType.DEBIT -> Color.Red
                        TransactionType.REFUND -> Color.Blue
                        TransactionType.DEPOSIT -> Color.Green
                        TransactionType.WITHDRAWAL -> Color.Red
                        TransactionType.RIDE_FARE -> Color(0xFFFF9800)
                        TransactionType.PROMO_BONUS -> Color(0xFF9C27B0)
                        TransactionType.REFERRAL_BONUS -> Color(0xFF9C27B0)
                        TransactionType.RIDE_PAYMENT -> Color(0xFFFF9800)
                        TransactionType.WALLET_TOP_UP -> Color.Green
                        TransactionType.CASHBACK -> Color.Blue
                        TransactionType.PENALTY -> Color.Red
                        TransactionType.COMMISSION -> Color(0xFFFFEB3B)
                        TransactionType.BONUS -> Color(0xFF9C27B0)
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Amount
            Text(
                text = "${if (transaction.type == TransactionType.CREDIT) "+" else "-"}₹${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    TransactionType.CREDIT -> Color.Green
                    TransactionType.DEBIT -> Color.Red
                    TransactionType.REFUND -> Color.Blue
                    TransactionType.DEPOSIT -> Color.Green
                    TransactionType.WITHDRAWAL -> Color.Red
                    TransactionType.RIDE_FARE -> Color(0xFFFF9800)
                    TransactionType.PROMO_BONUS -> Color(0xFF9C27B0)
                    TransactionType.REFERRAL_BONUS -> Color(0xFF9C27B0)
                    TransactionType.RIDE_PAYMENT -> Color(0xFFFF9800)
                    TransactionType.WALLET_TOP_UP -> Color.Green
                    TransactionType.CASHBACK -> Color.Blue
                    TransactionType.PENALTY -> Color.Red
                    TransactionType.COMMISSION -> Color(0xFFFFEB3B)
                    TransactionType.BONUS -> Color(0xFF9C27B0)
                }
            )
        }
    }
}

@Composable
fun WalletFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data classes
