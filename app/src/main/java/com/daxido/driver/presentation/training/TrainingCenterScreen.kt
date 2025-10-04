package com.daxido.driver.presentation.training

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.theme.*

data class TrainingModule(
    val id: String,
    val title: String,
    val description: String,
    val duration: Int, // in minutes
    val category: TrainingCategory,
    val isCompleted: Boolean,
    val isMandatory: Boolean,
    val progress: Float, // 0 to 1
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val points: Int
)

enum class TrainingCategory {
    SAFETY,
    CUSTOMER_SERVICE,
    NAVIGATION,
    VEHICLE_MAINTENANCE,
    EARNINGS,
    APP_USAGE,
    COMPLIANCE
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isUnlocked: Boolean,
    val progress: Float,
    val target: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrainingCenterScreen(
    onBack: () -> Unit,
    onStartModule: (TrainingModule) -> Unit,
    onViewCertificate: () -> Unit,
    onContactSupport: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<TrainingCategory?>(null) }
    var showAchievements by remember { mutableStateOf(false) }

    val completedModules = 15
    val totalModules = 20
    val totalPoints = 850
    val certificatesEarned = 3

    val trainingModules = remember {
        listOf(
            TrainingModule(
                "MOD001",
                "Safe Driving Practices",
                "Learn essential safety guidelines and defensive driving techniques",
                30,
                TrainingCategory.SAFETY,
                isCompleted = true,
                isMandatory = true,
                progress = 1f,
                videoUrl = "https://example.com/video1",
                thumbnailUrl = null,
                points = 50
            ),
            TrainingModule(
                "MOD002",
                "Customer Excellence",
                "Master the art of providing 5-star customer service",
                25,
                TrainingCategory.CUSTOMER_SERVICE,
                isCompleted = true,
                isMandatory = true,
                progress = 1f,
                videoUrl = "https://example.com/video2",
                thumbnailUrl = null,
                points = 40
            ),
            TrainingModule(
                "MOD003",
                "App Navigation Mastery",
                "Advanced features and tips for using the Daxido app",
                20,
                TrainingCategory.APP_USAGE,
                isCompleted = false,
                isMandatory = false,
                progress = 0.6f,
                videoUrl = "https://example.com/video3",
                thumbnailUrl = null,
                points = 30
            ),
            TrainingModule(
                "MOD004",
                "Vehicle Care & Maintenance",
                "Keep your vehicle in top condition for better earnings",
                35,
                TrainingCategory.VEHICLE_MAINTENANCE,
                isCompleted = false,
                isMandatory = false,
                progress = 0.2f,
                videoUrl = "https://example.com/video4",
                thumbnailUrl = null,
                points = 45
            ),
            TrainingModule(
                "MOD005",
                "Maximizing Earnings",
                "Strategies to increase your daily income",
                15,
                TrainingCategory.EARNINGS,
                isCompleted = false,
                isMandatory = false,
                progress = 0f,
                videoUrl = "https://example.com/video5",
                thumbnailUrl = null,
                points = 35
            )
        )
    }

    val achievements = remember {
        listOf(
            Achievement(
                "ACH001",
                "Safety Champion",
                "Complete all safety modules",
                Icons.Default.Shield,
                isUnlocked = true,
                progress = 1f,
                target = 5
            ),
            Achievement(
                "ACH002",
                "5-Star Driver",
                "Maintain 4.8+ rating for 30 days",
                Icons.Default.Star,
                isUnlocked = false,
                progress = 0.8f,
                target = 30
            ),
            Achievement(
                "ACH003",
                "Learning Expert",
                "Complete 20 training modules",
                Icons.Default.School,
                isUnlocked = false,
                progress = 0.75f,
                target = 20
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Training Center",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAchievements = !showAchievements }) {
                        Badge(
                            containerColor = DaxidoGold
                        ) {
                            Icon(Icons.Default.EmojiEvents, "Achievements")
                        }
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
            // Progress Card
            item {
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
                                brush = Brush.horizontalGradient(
                                    colors = listOf(DaxidoGold, DaxidoLightGold)
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        "Your Progress",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DaxidoWhite.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        "$completedModules of $totalModules completed",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DaxidoWhite
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(DaxidoWhite.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${(completedModules * 100 / totalModules)}%",
                                        fontWeight = FontWeight.Bold,
                                        color = DaxidoWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LinearProgressIndicator(
                                progress = completedModules.toFloat() / totalModules,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = DaxidoWhite,
                                trackColor = DaxidoWhite.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = null,
                                            tint = DaxidoWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            " $totalPoints",
                                            fontWeight = FontWeight.Bold,
                                            color = DaxidoWhite
                                        )
                                    }
                                    Text(
                                        "Points",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DaxidoWhite.copy(alpha = 0.8f)
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = DaxidoWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            " $certificatesEarned",
                                            fontWeight = FontWeight.Bold,
                                            color = DaxidoWhite
                                        )
                                    }
                                    Text(
                                        "Certificates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DaxidoWhite.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = onViewCertificate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusOnline.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardMembership,
                                contentDescription = null,
                                tint = StatusOnline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "My Certificates",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Card(
                        onClick = onContactSupport,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Get Help",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Category Filter
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Training Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    items(TrainingCategory.values().size) { index ->
                        val category = TrainingCategory.values()[index]
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(getCategoryName(category)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Mandatory Modules Section
            val mandatoryModules = trainingModules.filter { it.isMandatory && !it.isCompleted }
            if (mandatoryModules.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
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
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                " ${mandatoryModules.size} mandatory modules pending",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Training Modules List
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (selectedCategory != null) getCategoryName(selectedCategory!!) else "All Modules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            val filteredModules = if (selectedCategory != null) {
                trainingModules.filter { it.category == selectedCategory }
            } else {
                trainingModules
            }

            items(filteredModules.size) { index ->
                val module = filteredModules[index]
                TrainingModuleCard(
                    module = module,
                    onStart = { onStartModule(module) }
                )
            }

            // Achievements Section (when expanded)
            if (showAchievements) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(achievements.size) { index ->
                    val achievement = achievements[index]
                    AchievementCard(achievement = achievement)
                }
            }

            // Learning Tips
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
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
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Pro Tip",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Complete training modules during off-peak hours to maximize your learning without affecting earnings!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingModuleCard(
    module: TrainingModule,
    onStart: () -> Unit
) {
    Card(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (module.isMandatory && !module.isCompleted)
            BorderStroke(1.dp, Color.Red) else null
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
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                getCategoryColor(module.category).copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (module.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusOnline,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = getCategoryIcon(module.category),
                                contentDescription = null,
                                tint = getCategoryColor(module.category),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = module.title,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (module.isMandatory) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Red.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "MANDATORY",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    " ${module.duration} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = DaxidoGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    " ${module.points} pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DaxidoGold,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (!module.isCompleted && module.progress > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = module.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = DaxidoGold,
                                trackColor = DaxidoLightGray
                            )
                            Text(
                                "${(module.progress * 100).toInt()}% complete",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                DaxidoGold.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) DaxidoGold
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) DaxidoWhite
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = achievement.title,
                    fontWeight = FontWeight.Medium,
                    color = if (achievement.isUnlocked)
                        MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!achievement.isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = achievement.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = DaxidoGold,
                        trackColor = DaxidoLightGray
                    )
                    Text(
                        "${(achievement.progress * achievement.target).toInt()}/${achievement.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (achievement.isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = DaxidoGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

fun getCategoryName(category: TrainingCategory): String {
    return when(category) {
        TrainingCategory.SAFETY -> "Safety"
        TrainingCategory.CUSTOMER_SERVICE -> "Customer Service"
        TrainingCategory.NAVIGATION -> "Navigation"
        TrainingCategory.VEHICLE_MAINTENANCE -> "Vehicle Care"
        TrainingCategory.EARNINGS -> "Earnings"
        TrainingCategory.APP_USAGE -> "App Usage"
        TrainingCategory.COMPLIANCE -> "Compliance"
    }
}

fun getCategoryIcon(category: TrainingCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when(category) {
        TrainingCategory.SAFETY -> Icons.Default.Security
        TrainingCategory.CUSTOMER_SERVICE -> Icons.Default.SupportAgent
        TrainingCategory.NAVIGATION -> Icons.Default.Navigation
        TrainingCategory.VEHICLE_MAINTENANCE -> Icons.Default.Build
        TrainingCategory.EARNINGS -> Icons.Default.AttachMoney
        TrainingCategory.APP_USAGE -> Icons.Default.PhoneAndroid
        TrainingCategory.COMPLIANCE -> Icons.Default.Policy
    }
}

fun getCategoryColor(category: TrainingCategory): Color {
    return when(category) {
        TrainingCategory.SAFETY -> Color.Red
        TrainingCategory.CUSTOMER_SERVICE -> StatusOnline
        TrainingCategory.NAVIGATION -> Color(0xFF2196F3)
        TrainingCategory.VEHICLE_MAINTENANCE -> DaxidoMediumBrown
        TrainingCategory.EARNINGS -> DaxidoGold
        TrainingCategory.APP_USAGE -> Color(0xFF9C27B0)
        TrainingCategory.COMPLIANCE -> Color(0xFF607D8B)
    }
}