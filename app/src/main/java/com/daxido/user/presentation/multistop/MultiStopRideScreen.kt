package com.daxido.user.presentation.multistop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.daxido.core.multistop.StopType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiStopRideScreen(
    viewModel: MultiStopRideViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onConfirmRide: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddStopDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Stop Ride") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
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
                        Column {
                            Text(
                                "Total Fare",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                "₹${uiState.totalFare}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = onConfirmRide,
                            enabled = uiState.stops.size >= 2,
                            modifier = Modifier.width(150.dp)
                        ) {
                            Text("Confirm Ride")
                        }
                    }

                    if (uiState.stops.size >= 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Save ₹${uiState.savings} with multi-stop",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AddLocation,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Add Multiple Stops",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Save time and money with one continuous ride",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Stops list
            if (uiState.stops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Add at least 2 stops to create a multi-stop ride",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(uiState.stops) { index, stop ->
                        StopCard(
                            stop = stop,
                            index = index,
                            totalStops = uiState.stops.size,
                            onRemove = { viewModel.removeStop(stop.id) }
                        )
                        if (index < uiState.stops.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Add stop button
            OutlinedButton(
                onClick = { showAddStopDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Stop")
            }
        }
    }

    if (showAddStopDialog) {
        AddStopDialog(
            onDismiss = { showAddStopDialog = false },
            onAddStop = { address, stopType, notes ->
                viewModel.addStop(address, stopType, notes)
                showAddStopDialog = false
            }
        )
    }
}

@Composable
fun StopCard(
    stop: StopInfo,
    index: Int,
    totalStops: Int,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = when (stop.stopType) {
                                StopType.PICKUP -> Color(0xFF4CAF50)
                                StopType.DROPOFF -> Color(0xFFF44336)
                                StopType.WAYPOINT -> Color(0xFF2196F3)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (index < totalStops - 1) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(Color.Gray)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Stop details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (stop.stopType) {
                            StopType.PICKUP -> "Pickup"
                            StopType.DROPOFF -> "Drop-off"
                            StopType.WAYPOINT -> "Waypoint"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (stop.stopType) {
                            StopType.PICKUP -> Color(0xFF4CAF50)
                            StopType.DROPOFF -> Color(0xFFF44336)
                            StopType.WAYPOINT -> Color(0xFF2196F3)
                        },
                        modifier = Modifier
                            .background(
                                color = when (stop.stopType) {
                                    StopType.PICKUP -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    StopType.DROPOFF -> Color(0xFFF44336).copy(alpha = 0.1f)
                                    StopType.WAYPOINT -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    stop.address,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (stop.notes != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stop.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ETA: ${stop.estimatedTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove stop",
                    tint = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStopDialog(
    onDismiss: () -> Unit,
    onAddStop: (String, StopType, String?) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var selectedStopType by remember { mutableStateOf(StopType.WAYPOINT) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stop") },
        text = {
            Column {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Stop Type", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStopType == StopType.PICKUP,
                        onClick = { selectedStopType = StopType.PICKUP },
                        label = { Text("Pickup") }
                    )
                    FilterChip(
                        selected = selectedStopType == StopType.WAYPOINT,
                        onClick = { selectedStopType = StopType.WAYPOINT },
                        label = { Text("Waypoint") }
                    )
                    FilterChip(
                        selected = selectedStopType == StopType.DROPOFF,
                        onClick = { selectedStopType = StopType.DROPOFF },
                        label = { Text("Drop-off") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (address.isNotBlank()) {
                        onAddStop(address, selectedStopType, notes.ifBlank { null })
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class StopInfo(
    val id: String,
    val address: String,
    val stopType: StopType,
    val notes: String?,
    val estimatedTime: String
)

data class MultiStopRideUiState(
    val stops: List<StopInfo> = emptyList(),
    val totalFare: Double = 0.0,
    val savings: Double = 0.0,
    val isLoading: Boolean = false
)