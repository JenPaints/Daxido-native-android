package com.daxido.driver.presentation.profile

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.daxido.core.theme.*

data class DriverProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val photoUrl: String?,
    val rating: Float,
    val totalTrips: Int,
    val joinedDate: String,
    val vehicleNumber: String,
    val vehicleModel: String,
    val driverLevel: DriverLevel,
    val isVerified: Boolean,
    val languages: List<String>,
    val city: String
)

enum class DriverLevel {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onManageDocuments: () -> Unit,
    onManageVehicle: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSwitchToUserMode: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val driverProfile = remember {
        DriverProfile(
            id = "DRV123456",
            name = "Rajesh Kumar",
            email = "rajesh.kumar@email.com",
            phone = "+91 98765 43210",
            photoUrl = null,
            rating = 4.8f,
            totalTrips = 2847,
            joinedDate = "January 2023",
            vehicleNumber = "KA 01 AB 1234",
            vehicleModel = "Maruti Suzuki Swift",
            driverLevel = DriverLevel.GOLD,
            isVerified = true,
            languages = listOf("English", "Hindi", "Kannada"),
            city = "Bangalore"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
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
            // Profile Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Profile Photo
                            Box(
                                modifier = Modifier.size(100.dp)
                            ) {
                                if (driverProfile.photoUrl != null) {
                                    AsyncImage(
                                        model = driverProfile.photoUrl,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(4.dp, DaxidoWhite, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(DaxidoWhite)
                                            .border(4.dp, DaxidoWhite.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = driverProfile.name.first().toString(),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = DaxidoMediumBrown
                                        )
                                    }
                                }

                                // Verified Badge
                                if (driverProfile.isVerified) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(StatusOnline)
                                            .border(2.dp, DaxidoWhite, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = DaxidoWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Name and ID
                            Text(
                                text = driverProfile.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = DaxidoWhite
                            )

                            Text(
                                text = "ID: ${driverProfile.id}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DaxidoWhite.copy(alpha = 0.9f)
                            )

                            // Driver Level Badge
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = when(driverProfile.driverLevel) {
                                    DriverLevel.DIAMOND -> Color(0xFF00BCD4)
                                    DriverLevel.PLATINUM -> Color(0xFFE5E4E2)
                                    DriverLevel.GOLD -> DaxidoPaleGold
                                    DriverLevel.SILVER -> Color(0xFFC0C0C0)
                                    DriverLevel.BRONZE -> Color(0xFFCD7F32)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = when(driverProfile.driverLevel) {
                                            DriverLevel.DIAMOND, DriverLevel.GOLD -> DaxidoDarkBrown
                                            else -> Color.White
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${driverProfile.driverLevel.name} DRIVER",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when(driverProfile.driverLevel) {
                                            DriverLevel.DIAMOND, DriverLevel.GOLD -> DaxidoDarkBrown
                                            else -> Color.White
                                        }
                                    )
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = DaxidoWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            " ${driverProfile.rating}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = DaxidoWhite
                                        )
                                    }
                                    Text(
                                        "Rating",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DaxidoWhite.copy(alpha = 0.8f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(DaxidoWhite.copy(alpha = 0.3f))
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "${driverProfile.totalTrips}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = DaxidoWhite
                                    )
                                    Text(
                                        "Total Trips",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DaxidoWhite.copy(alpha = 0.8f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(DaxidoWhite.copy(alpha = 0.3f))
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        driverProfile.joinedDate,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = DaxidoWhite
                                    )
                                    Text(
                                        "Member Since",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DaxidoWhite.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Personal Information
            item {
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
                                "Personal Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showEditDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = DaxidoGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileInfoItem(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = driverProfile.email
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.Phone,
                            label = "Phone",
                            value = driverProfile.phone
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.LocationOn,
                            label = "City",
                            value = driverProfile.city
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.Language,
                            label = "Languages",
                            value = driverProfile.languages.joinToString(", ")
                        )
                    }
                }
            }

            // Vehicle Information
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    onClick = onManageVehicle,
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
                                "Vehicle Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DaxidoPaleGold.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = DaxidoGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Text(
                                    text = driverProfile.vehicleModel,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = driverProfile.vehicleNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusOnline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        " Documents verified",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusOnline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Cards
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Description,
                        title = "Manage Documents",
                        subtitle = "Update licenses and certificates",
                        onClick = onManageDocuments
                    )

                    QuickActionCard(
                        icon = Icons.Default.DirectionsCar,
                        title = "Vehicle Details",
                        subtitle = "Update vehicle information",
                        onClick = onManageVehicle
                    )

                    QuickActionCard(
                        icon = Icons.Default.Lock,
                        title = "Change Password",
                        subtitle = "Update your account password",
                        onClick = onChangePassword
                    )

                    QuickActionCard(
                        icon = Icons.Default.HelpOutline,
                        title = "Help & Support",
                        subtitle = "Get help with your account",
                        onClick = { }
                    )
                }
            }

            // Account Actions
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Switch to User Mode Button
            item {
                Card(
                    onClick = onSwitchToUserMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DaxidoPaleGold
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = DaxidoGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Switch to User Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = DaxidoDarkBrown
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Logout Button
            item {
                Card(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Logout",
                            fontWeight = FontWeight.Medium,
                            color = Color.Red
                        )
                    }
                }
            }

            // Delete Account
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Delete Account",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to logout? You'll need to sign in again to access your account.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Edit Profile",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Profile editing functionality will be available soon.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditDialog = false
                        onEditProfile()
                    }
                ) {
                    Text("Edit Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DaxidoGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}