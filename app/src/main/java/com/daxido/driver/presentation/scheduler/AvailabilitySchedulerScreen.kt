package com.daxido.driver.presentation.scheduler

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.theme.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ScheduleSlot(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isEnabled: Boolean,
    val preferredZones: List<String> = emptyList()
)

data class PreferredZone(
    val id: String,
    val name: String,
    val surgeMultiplier: Float,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvailabilitySchedulerScreen(
    onBack: () -> Unit,
    onSaveSchedule: (List<ScheduleSlot>) -> Unit,
    onQuickToggle: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var autoAcceptRides by remember { mutableStateOf(true) }
    var nightModeEnabled by remember { mutableStateOf(false) }
    var weekendBonus by remember { mutableStateOf(true) }

    // Schedule slots for each day
    val scheduleSlots = remember {
        mutableStateListOf(
            ScheduleSlot(DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(22, 0), true),
            ScheduleSlot(DayOfWeek.TUESDAY, LocalTime.of(6, 0), LocalTime.of(22, 0), true),
            ScheduleSlot(DayOfWeek.WEDNESDAY, LocalTime.of(6, 0), LocalTime.of(22, 0), true),
            ScheduleSlot(DayOfWeek.THURSDAY, LocalTime.of(6, 0), LocalTime.of(22, 0), true),
            ScheduleSlot(DayOfWeek.FRIDAY, LocalTime.of(6, 0), LocalTime.of(23, 0), true),
            ScheduleSlot(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(23, 0), true),
            ScheduleSlot(DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(20, 0), false)
        )
    }

    val zones = remember {
        listOf(
            PreferredZone("Z001", "Koramangala", 1.5f, true),
            PreferredZone("Z002", "Indiranagar", 1.3f, true),
            PreferredZone("Z003", "Whitefield", 1.8f, false),
            PreferredZone("Z004", "Electronic City", 2.0f, false),
            PreferredZone("Z005", "HSR Layout", 1.4f, true),
            PreferredZone("Z006", "MG Road", 1.6f, false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Availability Schedule",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSaveSchedule(scheduleSlots) }
                    ) {
                        Text("Save", color = DaxidoGold)
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
            // Quick Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StatusOnline.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Currently Online",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Accepting rides in all zones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = true,
                        onCheckedChange = onQuickToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusOnline,
                            checkedTrackColor = StatusOnline.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = DaxidoGold
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Schedule") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Zones") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Settings") }
                )
            }

            // Tab Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Schedule Tab
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = DaxidoPaleGold.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = DaxidoGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        " Set your weekly working hours",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DaxidoMediumBrown
                                    )
                                }
                            }
                        }

                        items(scheduleSlots.size) { index ->
                            val slot = scheduleSlots[index]
                            DayScheduleCard(
                                slot = slot,
                                onToggle = {
                                    scheduleSlots[index] = slot.copy(isEnabled = !slot.isEnabled)
                                },
                                onEditTime = {
                                    selectedDay = slot.dayOfWeek
                                    showTimePickerDialog = true
                                }
                            )
                        }

                        // Weekly Summary
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Text(
                                        "Weekly Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    val totalHours = scheduleSlots
                                        .filter { it.isEnabled }
                                        .sumOf {
                                            java.time.Duration.between(it.startTime, it.endTime).toHours()
                                        }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "${scheduleSlots.count { it.isEnabled }}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DaxidoGold
                                            )
                                            Text(
                                                "Days",
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
                                                "$totalHours",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusOnline
                                            )
                                            Text(
                                                "Hours",
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
                                                "₹${totalHours * 250}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DaxidoMediumBrown
                                            )
                                            Text(
                                                "Est. Earnings",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Zones Tab
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            " Preferred Zones",
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        "Get more ride requests in your preferred areas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        items(zones.size) { index ->
                            val zone = zones[index]
                            ZoneCard(
                                zone = zone,
                                onToggle = { /* Toggle zone selection */ }
                            )
                        }

                        // Zone Benefits
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = DaxidoPaleGold.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = DaxidoGold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Zone Benefits",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    listOf(
                                        "Priority ride allocation in selected zones",
                                        "Higher surge pricing during peak hours",
                                        "Reduced waiting time between rides",
                                        "Zone completion bonuses"
                                    ).forEach { benefit ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Text("• ", color = DaxidoGold)
                                            Text(
                                                benefit,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Settings Tab
                        item {
                            Text(
                                "Auto-Accept Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    // Auto Accept Rides
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { autoAcceptRides = !autoAcceptRides }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Auto-accept rides",
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "Automatically accept rides in your preferred zones",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = autoAcceptRides,
                                            onCheckedChange = { autoAcceptRides = it }
                                        )
                                    }

                                    Divider()

                                    // Night Mode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { nightModeEnabled = !nightModeEnabled }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Night mode priority",
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "Get more rides after 10 PM",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = nightModeEnabled,
                                            onCheckedChange = { nightModeEnabled = it }
                                        )
                                    }

                                    Divider()

                                    // Weekend Bonus
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { weekendBonus = !weekendBonus }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Weekend bonus eligible",
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "Work Fri-Sun for extra incentives",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = weekendBonus,
                                            onCheckedChange = { weekendBonus = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = DaxidoGold,
                                                checkedTrackColor = DaxidoGold.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Break Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Text(
                                        "Scheduled Breaks",
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    listOf(
                                        "Lunch" to "12:00 PM - 1:00 PM",
                                        "Tea Break" to "4:00 PM - 4:15 PM",
                                        "Dinner" to "8:00 PM - 8:30 PM"
                                    ).forEach { (breakName, time) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when(breakName) {
                                                        "Lunch" -> Icons.Default.Restaurant
                                                        "Tea Break" -> Icons.Default.Coffee
                                                        else -> Icons.Default.DinnerDining
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column(
                                                    modifier = Modifier.padding(start = 12.dp)
                                                ) {
                                                    Text(breakName, fontWeight = FontWeight.Medium)
                                                    Text(
                                                        time,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedButton(
                                        onClick = { },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Break")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScheduleCard(
    slot: ScheduleSlot,
    onToggle: () -> Unit,
    onEditTime: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (slot.isEnabled)
                MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                modifier = Modifier.weight(1f)
            ) {
                Switch(
                    checked = slot.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StatusOnline,
                        checkedTrackColor = StatusOnline.copy(alpha = 0.5f)
                    )
                )

                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = slot.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Medium,
                        color = if (slot.isEnabled)
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (slot.isEnabled) {
                        Text(
                            text = "${slot.startTime.format(DateTimeFormatter.ofPattern("hh:mm a"))} - " +
                                   slot.endTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Day off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (slot.isEnabled) {
                IconButton(onClick = onEditTime) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit time",
                        tint = DaxidoGold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneCard(
    zone: PreferredZone,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = if (zone.isSelected)
            BorderStroke(2.dp, DaxidoGold) else null
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
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (zone.isSelected) DaxidoGold.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (zone.isSelected) DaxidoGold
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = zone.name,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (zone.surgeMultiplier >= 1.5f) Color.Red else DaxidoWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            " ${zone.surgeMultiplier}x surge",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (zone.surgeMultiplier >= 1.5f) Color.Red else DaxidoWarning
                        )
                    }
                }
            }

            Checkbox(
                checked = zone.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = DaxidoGold
                )
            )
        }
    }
}