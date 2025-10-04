package com.daxido.driver.presentation.incentives

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Incentive(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int,
    val reward: Double,
    val deadline: LocalDate?,
    val type: IncentiveType,
    val isActive: Boolean,
    val isCompleted: Boolean
)

enum class IncentiveType {
    DAILY,
    WEEKLY,
    MONTHLY,
    PEAK_HOUR,
    ZONE_BASED,
    SPECIAL_EVENT
}

data class Bonus(
    val id: String,
    val title: String,
    val amount: Double,
    val earnedDate: LocalDate,
    val type: BonusType
)

enum class BonusType {
    COMPLETION,
    PERFORMANCE,
    REFERRAL,
    MILESTONE,
    FESTIVAL
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IncentivesScreen(
    onBack: () -> Unit,
    onViewDetails: (Incentive) -> Unit,
    onClaimReward: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedFilter by remember { mutableStateOf<IncentiveType?>(null) }

    val totalEarnedThisMonth = 5850.0
    val potentialEarnings = 3200.0
    val completedIncentives = 12
    val activeIncentives = 8

    val currentIncentives = remember {
        listOf(
            Incentive(
                "INC001",
                "Morning Rush Bonus",
                "Complete 5 rides between 6 AM - 9 AM",
                5,
                3,
                200.0,
                LocalDate.now(),
                IncentiveType.PEAK_HOUR,
                isActive = true,
                isCompleted = false
            ),
            Incentive(
                "INC002",
                "Weekly Target",
                "Complete 50 rides this week",
                50,
                35,
                1000.0,
                LocalDate.now().plusDays(3),
                IncentiveType.WEEKLY,
                isActive = true,
                isCompleted = false
            ),
            Incentive(
                "INC003",
                "Zone Champion",
                "10 rides in Koramangala area",
                10,
                10,
                300.0,
                LocalDate.now(),
                IncentiveType.ZONE_BASED,
                isActive = true,
                isCompleted = true
            ),
            Incentive(
                "INC004",
                "Weekend Warrior",
                "20 rides on Saturday & Sunday",
                20,
                8,
                500.0,
                LocalDate.now().plusDays(2),
                IncentiveType.SPECIAL_EVENT,
                isActive = true,
                isCompleted = false
            ),
            Incentive(
                "INC005",
                "Monthly Milestone",
                "200 rides in March",
                200,
                156,
                3000.0,
                LocalDate.now().plusDays(10),
                IncentiveType.MONTHLY,
                isActive = true,
                isCompleted = false
            )
        )
    }

    val bonusHistory = remember {
        listOf(
            Bonus(
                "BON001",
                "Weekly Target Achieved",
                1000.0,
                LocalDate.now().minusDays(2),
                BonusType.COMPLETION
            ),
            Bonus(
                "BON002",
                "5-Star Performance",
                500.0,
                LocalDate.now().minusDays(5),
                BonusType.PERFORMANCE
            ),
            Bonus(
                "BON003",
                "Friend Referral",
                300.0,
                LocalDate.now().minusDays(8),
                BonusType.REFERRAL
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Incentives & Bonuses",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Info, "Info")
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
            // Earnings Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DaxidoGold
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(DaxidoGold, DaxidoMediumBrown)
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            "Bonus Earnings",
                            style = MaterialTheme.typography.titleMedium,
                            color = DaxidoWhite.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "₹${totalEarnedThisMonth.toInt()}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DaxidoWhite
                                )
                                Text(
                                    "Earned this month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DaxidoWhite.copy(alpha = 0.8f)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "₹${potentialEarnings.toInt()}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DaxidoPaleGold
                                )
                                Text(
                                    "Potential",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DaxidoWhite.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusOnline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    " $completedIncentives completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DaxidoWhite
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = DaxidoPaleGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    " $activeIncentives active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DaxidoWhite
                                )
                            }
                        }
                    }
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
                    text = { Text("Active") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Upcoming") }
                )
            }

            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Active Incentives
                        item {
                            // Filter Chips
                            LazyRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedFilter == null,
                                        onClick = { selectedFilter = null },
                                        label = { Text("All") }
                                    )
                                }
                                items(IncentiveType.values().size) { index ->
                                    val type = IncentiveType.values()[index]
                                    FilterChip(
                                        selected = selectedFilter == type,
                                        onClick = { selectedFilter = type },
                                        label = { Text(getIncentiveTypeName(type)) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Peak Hours Alert
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 12.dp)
                                    ) {
                                        Text(
                                            "Peak Hours Active!",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )
                                        Text(
                                            "2x bonus on all rides until 9 PM",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Active Incentive Cards
                        val activeIncentivesList = currentIncentives.filter {
                            it.isActive && !it.isCompleted &&
                            (selectedFilter == null || it.type == selectedFilter)
                        }

                        items(activeIncentivesList.size) { index ->
                            val incentive = activeIncentivesList[index]
                            IncentiveCard(
                                incentive = incentive,
                                onViewDetails = { onViewDetails(incentive) },
                                onClaim = if (incentive.isCompleted) {
                                    { onClaimReward(incentive.id) }
                                } else null
                            )
                        }

                        // Completed but unclaimed
                        val completedIncentivesList = currentIncentives.filter {
                            it.isCompleted && it.isActive
                        }

                        if (completedIncentivesList.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Ready to Claim",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            items(completedIncentivesList.size) { index ->
                                val incentive = completedIncentivesList[index]
                                IncentiveCard(
                                    incentive = incentive,
                                    onViewDetails = { onViewDetails(incentive) },
                                    onClaim = { onClaimReward(incentive.id) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Bonus History
                        item {
                            Text(
                                "Bonus History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(bonusHistory.size) { index ->
                            val bonus = bonusHistory[index]
                            BonusHistoryCard(bonus = bonus)
                        }
                    }

                    2 -> {
                        // Upcoming Incentives
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
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
                                            imageVector = Icons.Default.Upcoming,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Coming Soon",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    UpcomingIncentiveItem(
                                        title = "Holi Festival Bonus",
                                        description = "Extra ₹100 per ride",
                                        startDate = "March 25",
                                        icon = Icons.Default.Celebration
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    UpcomingIncentiveItem(
                                        title = "Night Owl Special",
                                        description = "1.5x earnings after midnight",
                                        startDate = "Every Friday",
                                        icon = Icons.Default.Nightlight
                                    )
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
fun IncentiveCard(
    incentive: Incentive,
    onViewDetails: () -> Unit,
    onClaim: (() -> Unit)?
) {
    val progress = incentive.currentValue.toFloat() / incentive.targetValue
    val daysRemaining = if (incentive.deadline != null) {
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), incentive.deadline)
    } else null

    Card(
        onClick = onViewDetails,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (incentive.isCompleted)
                StatusOnline.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getIncentiveIcon(incentive.type),
                            contentDescription = null,
                            tint = getIncentiveColor(incentive.type),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = incentive.title,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = incentive.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "₹${incentive.reward.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (incentive.isCompleted) StatusOnline else DaxidoGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${incentive.currentValue}/${incentive.targetValue}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (incentive.isCompleted) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (incentive.isCompleted) StatusOnline else DaxidoGold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (daysRemaining != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (daysRemaining <= 1) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when {
                                daysRemaining == 0L -> " Ends today"
                                daysRemaining == 1L -> " 1 day left"
                                else -> " $daysRemaining days left"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (daysRemaining <= 1) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (onClaim != null) {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusOnline
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("Claim", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BonusHistoryCard(bonus: Bonus) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
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
                        .background(getBonusColor(bonus.type).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBonusIcon(bonus.type),
                        contentDescription = null,
                        tint = getBonusColor(bonus.type),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = bonus.title,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = bonus.earnedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "+₹${bonus.amount.toInt()}",
                fontWeight = FontWeight.Bold,
                color = StatusOnline
            )
        }
    }
}

@Composable
fun UpcomingIncentiveItem(
    title: String,
    description: String,
    startDate: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Starts: $startDate",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2196F3)
            )
        }
    }
}

fun getIncentiveTypeName(type: IncentiveType): String {
    return when(type) {
        IncentiveType.DAILY -> "Daily"
        IncentiveType.WEEKLY -> "Weekly"
        IncentiveType.MONTHLY -> "Monthly"
        IncentiveType.PEAK_HOUR -> "Peak Hour"
        IncentiveType.ZONE_BASED -> "Zone"
        IncentiveType.SPECIAL_EVENT -> "Special"
    }
}

fun getIncentiveIcon(type: IncentiveType): androidx.compose.ui.graphics.vector.ImageVector {
    return when(type) {
        IncentiveType.DAILY -> Icons.Default.Today
        IncentiveType.WEEKLY -> Icons.Default.DateRange
        IncentiveType.MONTHLY -> Icons.Default.CalendarMonth
        IncentiveType.PEAK_HOUR -> Icons.Default.Schedule
        IncentiveType.ZONE_BASED -> Icons.Default.LocationOn
        IncentiveType.SPECIAL_EVENT -> Icons.Default.Celebration
    }
}

fun getIncentiveColor(type: IncentiveType): Color {
    return when(type) {
        IncentiveType.DAILY -> Color(0xFF4CAF50)
        IncentiveType.WEEKLY -> Color(0xFF2196F3)
        IncentiveType.MONTHLY -> DaxidoGold
        IncentiveType.PEAK_HOUR -> Color.Red
        IncentiveType.ZONE_BASED -> Color(0xFF9C27B0)
        IncentiveType.SPECIAL_EVENT -> Color(0xFFFF9800)
    }
}

fun getBonusIcon(type: BonusType): androidx.compose.ui.graphics.vector.ImageVector {
    return when(type) {
        BonusType.COMPLETION -> Icons.Default.CheckCircle
        BonusType.PERFORMANCE -> Icons.Default.Star
        BonusType.REFERRAL -> Icons.Default.GroupAdd
        BonusType.MILESTONE -> Icons.Default.EmojiEvents
        BonusType.FESTIVAL -> Icons.Default.Celebration
    }
}

fun getBonusColor(type: BonusType): Color {
    return when(type) {
        BonusType.COMPLETION -> StatusOnline
        BonusType.PERFORMANCE -> DaxidoGold
        BonusType.REFERRAL -> Color(0xFF2196F3)
        BonusType.MILESTONE -> Color(0xFF9C27B0)
        BonusType.FESTIVAL -> Color(0xFFFF9800)
    }
}