package com.daxido.driver.presentation.earnings

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
import com.daxido.core.models.TransactionType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsDashboard(
    onNavigateBack: () -> Unit,
    onNavigateToWithdrawal: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    viewModel: EarningsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Load earnings data
    LaunchedEffect(Unit) {
        viewModel.loadEarningsData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings") },
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
            // Total Earnings Card
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
                        text = "Total Earnings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${String.format("%.2f", uiState.totalEarnings)}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onNavigateToWithdrawal,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Withdraw",
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
            
            // Earnings Summary
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
                        text = "Today's Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EarningsStatCard(
                            icon = Icons.Default.DirectionsCar,
                            title = "Rides",
                            value = uiState.todaysRides.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        EarningsStatCard(
                            icon = Icons.Default.Timer,
                            title = "Hours",
                            value = "${uiState.todaysHours}h",
                            color = Color(0xFF4CAF50)
                        )
                        EarningsStatCard(
                            icon = Icons.Default.Star,
                            title = "Rating",
                            value = String.format("%.1f", uiState.todaysRating),
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
            
            // Weekly Earnings Chart
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
                        text = "Weekly Earnings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple bar chart representation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val weeklyData = listOf(450.0, 380.0, 520.0, 410.0, 480.0, 350.0, 420.0)
                        weeklyData.forEachIndexed { index, earnings ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height((earnings / weeklyData.maxOrNull()!! * 100).dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (index) {
                                        0 -> "Mon"
                                        1 -> "Tue"
                                        2 -> "Wed"
                                        3 -> "Thu"
                                        4 -> "Fri"
                                        5 -> "Sat"
                                        6 -> "Sun"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            
            // Recent Transactions
            if (uiState.recentTransactions.isNotEmpty()) {
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
            
            // Incentives and Bonuses
            if (uiState.availableIncentives.isNotEmpty()) {
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
                            text = "Available Incentives",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.availableIncentives) { incentive ->
                                IncentiveCard(
                                    incentive = incentive,
                                    onClick = { viewModel.claimIncentive(incentive.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsStatCard(
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
fun TransactionCard(
    transaction: EarningsTransaction,
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
                            "RIDE_FARE" -> Color.Green.copy(alpha = 0.1f)
                            "BONUS" -> Color.Blue.copy(alpha = 0.1f)
                            "WITHDRAWAL" -> Color.Red.copy(alpha = 0.1f)
                            else -> Color.Gray.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (transaction.type) {
                        "RIDE_FARE" -> Icons.Default.DirectionsCar
                        "BONUS" -> Icons.Default.Star
                        "WITHDRAWAL" -> Icons.Default.AccountBalanceWallet
                        else -> Icons.Default.Info
                    },
                    contentDescription = transaction.type,
                    modifier = Modifier.size(20.dp),
                    tint = when (transaction.type) {
                        "RIDE_FARE" -> Color.Green
                        "BONUS" -> Color.Blue
                        "WITHDRAWAL" -> Color.Red
                        else -> Color.Gray
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
                    text = transaction.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Amount
            Text(
                text = "${if (transaction.type == "WITHDRAWAL") "-" else "+"}₹${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    "RIDE_FARE" -> Color.Green
                    "BONUS" -> Color.Blue
                    "WITHDRAWAL" -> Color.Red
                    else -> Color.Gray
                }
            )
        }
    }
}

@Composable
fun IncentiveCard(
    incentive: Incentive,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Blue.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Incentive",
                tint = Color.Blue
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incentive.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = incentive.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${String.format("%.2f", incentive.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
        }
    }
}
