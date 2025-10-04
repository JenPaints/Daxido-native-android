package com.daxido.admin.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daxido.core.theme.*

/**
 * ADMIN DASHBOARD - Main Control Panel
 * Like Ola/Uber/Rapido admin dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToLiveRides: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToDrivers: () -> Unit,
    onNavigateToFinancial: () -> Unit,
    onNavigateToPromos: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Daxido Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Complete Control Panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DaxidoGold,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToEmergency) {
                        Badge(
                            containerColor = Color.Red
                        ) {
                            Text("!")
                        }
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Emergency",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Stats
                item {
                    Text(
                        "Quick Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(400.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            listOf(
                                StatCard("Active Rides", uiState.analytics?.activeRides?.toString() ?: "0", Icons.Default.DirectionsCar, DaxidoGold),
                                StatCard("Online Drivers", uiState.analytics?.onlineDrivers?.toString() ?: "0", Icons.Default.Person, Color(0xFF4CAF50)),
                                StatCard("Today's Revenue", "₹${uiState.analytics?.revenueToday?.toInt() ?: 0}", Icons.Default.AttachMoney, Color(0xFF2196F3)),
                                StatCard("Completed Today", uiState.analytics?.completedRidesToday?.toString() ?: "0", Icons.Default.CheckCircle, Color(0xFF9C27B0)),
                                StatCard("Total Users", uiState.analytics?.totalUsers?.toString() ?: "0", Icons.Default.People, Color(0xFFFF9800)),
                                StatCard("Total Drivers", uiState.analytics?.totalDrivers?.toString() ?: "0", Icons.Default.LocalTaxi, Color(0xFF00BCD4))
                            )
                        ) { stat ->
                            StatsCardItem(stat)
                        }
                    }
                }

                // Quick Actions
                item {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Live Rides",
                                icon = Icons.Default.Map,
                                color = DaxidoGold,
                                onClick = onNavigateToLiveRides,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                title = "Emergency",
                                icon = Icons.Default.Warning,
                                color = Color.Red,
                                onClick = onNavigateToEmergency,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Users",
                                icon = Icons.Default.People,
                                color = Color(0xFF4CAF50),
                                onClick = onNavigateToUsers,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                title = "Drivers",
                                icon = Icons.Default.LocalTaxi,
                                color = Color(0xFF2196F3),
                                onClick = onNavigateToDrivers,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Financial",
                                icon = Icons.Default.AccountBalance,
                                color = Color(0xFF9C27B0),
                                onClick = onNavigateToFinancial,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                title = "Promos",
                                icon = Icons.Default.LocalOffer,
                                color = Color(0xFFFF9800),
                                onClick = onNavigateToPromos,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Support",
                                icon = Icons.Default.Support,
                                color = Color(0xFF00BCD4),
                                onClick = onNavigateToSupport,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                title = "Analytics",
                                icon = Icons.Default.Analytics,
                                color = Color(0xFF673AB7),
                                onClick = onNavigateToAnalytics,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Settings",
                                icon = Icons.Default.Settings,
                                color = Color.Gray,
                                onClick = onNavigateToSettings,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Recent Activity
                item {
                    Text(
                        "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(uiState.recentActivity) { activity ->
                    ActivityItem(activity)
                }
            }
        }
    }
}

data class StatCard(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun StatsCardItem(stat: StatCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = stat.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = stat.color,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    stat.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = stat.color
                )
                Text(
                    stat.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun ActivityItem(activity: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = DaxidoGold
                )
                Column {
                    Text(
                        activity,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Just now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
