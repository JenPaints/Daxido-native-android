package com.daxido.admin.presentation.drivers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daxido.admin.models.DriverManagement
import com.daxido.core.theme.DaxidoGold
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverManagementScreen(
    onNavigateBack: () -> Unit,
    onDriverClick: (String) -> Unit,
    viewModel: DriverManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadDrivers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Driver Management", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.drivers.size} total drivers",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DaxidoGold,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadDrivers() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name, phone or vehicle") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == DriverFilter.ALL,
                    onClick = { viewModel.setFilter(DriverFilter.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.filter == DriverFilter.ONLINE,
                    onClick = { viewModel.setFilter(DriverFilter.ONLINE) },
                    label = { Text("Online") }
                )
                FilterChip(
                    selected = uiState.filter == DriverFilter.VERIFIED,
                    onClick = { viewModel.setFilter(DriverFilter.VERIFIED) },
                    label = { Text("Verified") }
                )
                FilterChip(
                    selected = uiState.filter == DriverFilter.PENDING,
                    onClick = { viewModel.setFilter(DriverFilter.PENDING) },
                    label = { Text("Pending") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredDrivers = uiState.drivers.filter { driver ->
                    val matchesSearch = searchQuery.isEmpty() ||
                            driver.name.contains(searchQuery, ignoreCase = true) ||
                            driver.phoneNumber.contains(searchQuery) ||
                            driver.vehicleNumber.contains(searchQuery, ignoreCase = true)

                    val matchesFilter = when (uiState.filter) {
                        DriverFilter.ALL -> true
                        DriverFilter.ONLINE -> driver.isOnline
                        DriverFilter.VERIFIED -> driver.isVerified
                        DriverFilter.PENDING -> !driver.isVerified
                    }

                    matchesSearch && matchesFilter
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDrivers) { driver ->
                        DriverCard(
                            driver = driver,
                            onClick = { onDriverClick(driver.driverId) },
                            onVerifyClick = { viewModel.verifyDriver(driver.driverId) },
                            onRejectClick = { viewModel.rejectDriver(driver.driverId, "Documents incomplete") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverCard(
    driver: DriverManagement,
    onClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    driver.isOnline -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    driver.isVerified -> DaxidoGold.copy(alpha = 0.2f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalTaxi,
                            contentDescription = null,
                            tint = when {
                                driver.isOnline -> Color(0xFF4CAF50)
                                driver.isVerified -> DaxidoGold
                                else -> Color.Gray
                            },
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column {
                        Text(
                            driver.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            driver.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${driver.vehicleType.name} • ${driver.vehicleNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (driver.isOnline) {
                        Badge(containerColor = Color(0xFF4CAF50)) {
                            Text("ONLINE", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (driver.isVerified) {
                        Badge(containerColor = Color(0xFF2196F3)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                                Text("VERIFIED", color = Color.White)
                            }
                        }
                    } else {
                        Badge(containerColor = Color(0xFFFF9800)) {
                            Text("PENDING", color = Color.White)
                        }
                    }
                }
            }

            Divider()

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DriverStatItem("Rides", driver.totalRides.toString(), Icons.Default.DirectionsCar)
                DriverStatItem("Rating", "%.1f".format(driver.rating), Icons.Default.Star)
                DriverStatItem("Earnings", "₹${driver.earnings.toInt()}", Icons.Default.AttachMoney)
                DriverStatItem("Accept", "${(driver.acceptanceRate * 100).toInt()}%", Icons.Default.CheckCircle)
            }

            Divider()

            // Additional Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Joined: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(driver.joinedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "Last active: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(driver.lastActive)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (!driver.isVerified) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRejectClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = onVerifyClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = DaxidoGold,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

enum class DriverFilter {
    ALL, ONLINE, VERIFIED, PENDING
}
