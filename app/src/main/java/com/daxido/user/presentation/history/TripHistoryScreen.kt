package com.daxido.user.presentation.history

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.models.VehicleType
import com.daxido.core.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TripHistory(
    val id: String,
    val date: LocalDateTime,
    val pickupLocation: String,
    val dropLocation: String,
    val driverName: String,
    val vehicleType: VehicleType,
    val vehicleNumber: String,
    val fare: Double,
    val distance: Double,
    val duration: Int,
    val rating: Int?,
    val paymentMethod: String,
    val status: TripStatus
)

enum class TripStatus {
    COMPLETED, CANCELLED, DISPUTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    onTripClick: (String) -> Unit,
    onRebookTrip: (TripHistory) -> Unit,
    onDownloadInvoice: (String) -> Unit,
    onReportIssue: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") }
    var selectedDateRange by remember { mutableStateOf("all") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Sample trip history data
    val trips = remember {
        listOf(
            TripHistory(
                id = "TRIP001",
                date = LocalDateTime.now().minusDays(1),
                pickupLocation = "MG Road Metro Station",
                dropLocation = "Koramangala 5th Block",
                driverName = "Rajesh Kumar",
                vehicleType = VehicleType.CAR,
                vehicleNumber = "KA 01 AB 1234",
                fare = 185.0,
                distance = 8.5,
                duration = 35,
                rating = 5,
                paymentMethod = "UPI",
                status = TripStatus.COMPLETED
            ),
            TripHistory(
                id = "TRIP002",
                date = LocalDateTime.now().minusDays(3),
                pickupLocation = "Indiranagar 100 Feet Road",
                dropLocation = "Whitefield Tech Park",
                driverName = "Mohammed Ali",
                vehicleType = VehicleType.AUTO,
                vehicleNumber = "KA 05 CD 5678",
                fare = 250.0,
                distance = 12.3,
                duration = 45,
                rating = 4,
                paymentMethod = "Cash",
                status = TripStatus.COMPLETED
            ),
            TripHistory(
                id = "TRIP003",
                date = LocalDateTime.now().minusDays(5),
                pickupLocation = "HSR Layout Sector 1",
                dropLocation = "Electronic City Phase 1",
                driverName = "Suresh Babu",
                vehicleType = VehicleType.BIKE,
                vehicleNumber = "KA 03 EF 9012",
                fare = 95.0,
                distance = 6.2,
                duration = 25,
                rating = null,
                paymentMethod = "Wallet",
                status = TripStatus.CANCELLED
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trip History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Badge(
                            containerColor = if (selectedFilter != "all" || selectedDateRange != "all")
                                DaxidoGold else Color.Transparent
                        ) {
                            Icon(Icons.Default.FilterAlt, "Filter")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DaxidoPaleGold.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${trips.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoGold
                        )
                        Text(
                            text = "Total Trips",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "₹${trips.sumOf { it.fare }.toInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoMediumBrown
                        )
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${trips.sumOf { it.distance }.toInt()} km",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusOnline
                        )
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDateRange == "today",
                        onClick = { selectedDateRange = "today" },
                        label = { Text("Today") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateRange == "week",
                        onClick = { selectedDateRange = "week" },
                        label = { Text("This Week") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateRange == "month",
                        onClick = { selectedDateRange = "month" },
                        label = { Text("This Month") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "cancelled",
                        onClick = { selectedFilter = "cancelled" },
                        label = { Text("Cancelled") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trip List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips.size) { index ->
                    val trip = trips[index]
                    TripCard(
                        trip = trip,
                        onClick = { onTripClick(trip.id) },
                        onRebook = { onRebookTrip(trip) },
                        onDownloadInvoice = { onDownloadInvoice(trip.id) },
                        onReportIssue = { onReportIssue(trip.id) }
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Text("Filter Trips", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Vehicle Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Vehicle type filters would go here

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Date Range",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Date range options would go here

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Status filters would go here
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedFilter = "all"
                    selectedDateRange = "all"
                    showFilterDialog = false
                }) {
                    Text("Clear All")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCard(
    trip: TripHistory,
    onClick: () -> Unit,
    onRebook: () -> Unit,
    onDownloadInvoice: () -> Unit,
    onReportIssue: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when(trip.vehicleType) {
                                VehicleType.BIKE -> Icons.Default.TwoWheeler
                                VehicleType.AUTO -> Icons.Default.ElectricRickshaw
                                VehicleType.CAR -> Icons.Default.DirectionsCar
                                VehicleType.PREMIUM -> Icons.Default.DirectionsCar
                            },
                            contentDescription = null,
                            tint = DaxidoGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trip.vehicleType.name,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trip.vehicleNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = trip.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when(trip.status) {
                        TripStatus.COMPLETED -> StatusOnline.copy(alpha = 0.1f)
                        TripStatus.CANCELLED -> Color.Red.copy(alpha = 0.1f)
                        TripStatus.DISPUTED -> DaxidoWarning.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = trip.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(trip.status) {
                            TripStatus.COMPLETED -> StatusOnline
                            TripStatus.CANCELLED -> Color.Red
                            TripStatus.DISPUTED -> DaxidoWarning
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.width(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusOnline)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(40.dp)
                            .background(DaxidoLightGray)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = trip.pickupLocation,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = trip.dropLocation,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider()

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${trip.fare.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DaxidoGold
                    )
                    Text(
                        text = " • ${trip.distance} km • ${trip.duration} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    // Rating
                    if (trip.rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(trip.rating) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = DaxidoGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else if (trip.status == TripStatus.COMPLETED) {
                        TextButton(
                            onClick = { },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Rate", fontSize = 12.sp)
                        }
                    }

                    IconButton(
                        onClick = { showOptions = !showOptions },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Options Menu
            AnimatedVisibility(
                visible = showOptions,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Divider(modifier = Modifier.padding(bottom = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRebook,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rebook", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDownloadInvoice,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invoice", fontSize = 12.sp)
                        }

                        if (trip.status != TripStatus.CANCELLED) {
                            OutlinedButton(
                                onClick = onReportIssue,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Issue", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}