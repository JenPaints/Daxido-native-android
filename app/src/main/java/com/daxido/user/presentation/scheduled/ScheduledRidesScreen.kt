package com.daxido.user.presentation.scheduled

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daxido.core.models.RepeatOff
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledRidesScreen(
    viewModel: ScheduledRidesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onScheduleNewRide: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Rides") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScheduleNewRide) {
                        Icon(Icons.Default.Add, "Schedule New Ride")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScheduleNewRide,
                icon = { Icon(Icons.Default.Schedule, "Schedule") },
                text = { Text("Schedule Ride") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var selectedTab by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Upcoming") },
                    icon = { Icon(Icons.Default.Schedule, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recurring") },
                    icon = { Icon(Icons.Default.Repeat, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Past") },
                    icon = { Icon(Icons.Default.History, null) }
                )
            }

            when (selectedTab) {
                0 -> UpcomingRidesTab(uiState.upcomingRides, viewModel)
                1 -> RecurringRidesTab(uiState.recurringRides, viewModel)
                2 -> PastRidesTab(uiState.pastRides)
            }
        }
    }
}

@Composable
fun UpcomingRidesTab(rides: List<ScheduledRideInfo>, viewModel: ScheduledRidesViewModel) {
    if (rides.isEmpty()) {
        EmptyState(
            icon = Icons.Default.EventBusy,
            message = "No upcoming scheduled rides"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rides) { ride ->
                ScheduledRideCard(ride = ride, onCancelClick = {
                    viewModel.cancelScheduledRide(ride.id)
                })
            }
        }
    }
}

@Composable
fun RecurringRidesTab(rides: List<RecurringRideInfo>, viewModel: ScheduledRidesViewModel) {
    if (rides.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Repeat,
            message = "No recurring rides set up"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rides) { ride ->
                RecurringRideCard(ride = ride, onCancelClick = {
                    viewModel.cancelRecurringRide(ride.id)
                })
            }
        }
    }
}

@Composable
fun PastRidesTab(rides: List<ScheduledRideInfo>) {
    if (rides.isEmpty()) {
        EmptyState(
            icon = Icons.Default.HistoryToggleOff,
            message = "No past scheduled rides"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rides) { ride ->
                PastRideCard(ride = ride)
            }
        }
    }
}

@Composable
fun ScheduledRideCard(ride: ScheduledRideInfo, onCancelClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ride.pickupAddress,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            ride.dropoffAddress,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            ride.scheduledDate,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            ride.scheduledTime,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        ride.vehicleType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        "₹${ride.estimatedFare}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Ride")
            }
        }
    }
}

@Composable
fun RecurringRideCard(ride: RecurringRideInfo, onCancelClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recurring Ride",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = ride.isActive,
                    onCheckedChange = { /* Toggle recurring ride */ }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                ride.pickupAddress,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    ride.dropoffAddress,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Every: ${ride.days.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Time: ${ride.time}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    "₹${ride.estimatedFare}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Recurring Ride")
            }
        }
    }
}

@Composable
fun PastRideCard(ride: ScheduledRideInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ride.pickupAddress,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "→ ${ride.dropoffAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        ride.scheduledDate,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "₹${ride.estimatedFare}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}

data class ScheduledRideInfo(
    val id: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val vehicleType: String,
    val estimatedFare: Double
)

data class RecurringRideInfo(
    val id: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val time: String,
    val days: List<String>,
    val vehicleType: String,
    val estimatedFare: Double,
    val isActive: Boolean
)

data class ScheduledRidesUiState(
    val upcomingRides: List<ScheduledRideInfo> = emptyList(),
    val recurringRides: List<RecurringRideInfo> = emptyList(),
    val pastRides: List<ScheduledRideInfo> = emptyList(),
    val isLoading: Boolean = false
)