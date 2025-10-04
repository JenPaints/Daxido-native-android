package com.daxido.driver.presentation.performance

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.theme.*
import kotlin.math.cos
import kotlin.math.sin

data class PerformanceMetric(
    val name: String,
    val value: Float,
    val target: Float,
    val unit: String = "%",
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

data class CustomerFeedback(
    val rating: Int,
    val comment: String,
    val date: String,
    val tripId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceMetricsScreen(
    onBack: () -> Unit,
    onViewAchievements: () -> Unit,
    onViewTraining: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("week") }
    var showImprovementTips by remember { mutableStateOf(false) }

    val overallRating = 4.8f
    val totalTrips = 1234
    val memberSince = "Jan 2023"

    val metrics = remember {
        listOf(
            PerformanceMetric(
                "Acceptance Rate",
                92f,
                85f,
                "%",
                Icons.Default.CheckCircle,
                StatusOnline
            ),
            PerformanceMetric(
                "Completion Rate",
                98f,
                95f,
                "%",
                Icons.Default.Flag,
                DaxidoGold
            ),
            PerformanceMetric(
                "Cancellation Rate",
                2f,
                5f,
                "%",
                Icons.Default.Cancel,
                Color.Red
            ),
            PerformanceMetric(
                "On-Time Arrival",
                88f,
                90f,
                "%",
                Icons.Default.Schedule,
                Color(0xFF2196F3)
            )
        )
    }

    val recentFeedback = remember {
        listOf(
            CustomerFeedback(
                5,
                "Excellent driver! Very professional and courteous.",
                "2 days ago",
                "TRIP1234"
            ),
            CustomerFeedback(
                4,
                "Good ride, smooth driving.",
                "3 days ago",
                "TRIP1233"
            ),
            CustomerFeedback(
                5,
                "Clean vehicle, great music selection!",
                "5 days ago",
                "TRIP1232"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Performance",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onViewAchievements) {
                        Icon(Icons.Default.EmojiEvents, "Achievements")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Driver Score Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DaxidoPaleGold.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular Rating Display
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .drawBehind {
                                    drawCircularProgress(
                                        progress = overallRating / 5f,
                                        color = DaxidoGold,
                                        strokeWidth = 12.dp
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = overallRating.toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DaxidoGold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { index ->
                                        Icon(
                                            imageVector = if (index < overallRating.toInt())
                                                Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = DaxidoGold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = totalTrips.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
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
                                    text = memberSince,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Member Since",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Driver Level Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DaxidoGold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = DaxidoWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GOLD DRIVER",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DaxidoWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Period Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "today" to "Today",
                        "week" to "This Week",
                        "month" to "This Month",
                        "all" to "All Time"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = selectedPeriod == value,
                            onClick = { selectedPeriod = value },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DaxidoGold,
                                selectedLabelColor = DaxidoWhite
                            )
                        )
                    }
                }
            }

            // Performance Metrics
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Key Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Metrics Grid
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    metrics.chunked(2).forEach { rowMetrics ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowMetrics.forEach { metric ->
                                MetricCard(
                                    metric = metric,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowMetrics.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Performance Trend
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Performance Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = StatusOnline,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trend Chart Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Performance improving by 12% this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusOnline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Recent Feedback
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { }) {
                        Text("View All")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(recentFeedback.size) { index ->
                val feedback = recentFeedback[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                repeat(feedback.rating) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = DaxidoGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                feedback.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            feedback.comment,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Improvement Tips
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    onClick = { showImprovementTips = !showImprovementTips },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Improvement Tips",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (showImprovementTips)
                                    Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(
                            visible = showImprovementTips,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                listOf(
                                    "Accept more rides during peak hours",
                                    "Maintain vehicle cleanliness for better ratings",
                                    "Use navigation for optimal routes",
                                    "Communicate politely with customers",
                                    "Complete weekly targets for bonus earnings"
                                ).forEach { tip ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text("• ", color = Color(0xFF2196F3))
                                        Text(
                                            tip,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Training Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onViewTraining,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DaxidoGold
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "View Training Materials",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun MetricCard(
    metric: PerformanceMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = metric.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${metric.value.toInt()}${metric.unit}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (metric.name == "Cancellation Rate" && metric.value <= metric.target ||
                    metric.name != "Cancellation Rate" && metric.value >= metric.target)
                    StatusOnline else DaxidoWarning
            )
            Text(
                text = metric.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Target: ${metric.target.toInt()}${metric.unit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun DrawScope.drawCircularProgress(
    progress: Float,
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp
) {
    val startAngle = -90f
    val sweepAngle = progress * 360f
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        cap = StrokeCap.Round
    )

    // Background circle
    drawArc(
        color = color.copy(alpha = 0.1f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = stroke
    )

    // Progress arc
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = stroke
    )
}