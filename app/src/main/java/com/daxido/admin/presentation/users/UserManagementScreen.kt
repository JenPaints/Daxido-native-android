package com.daxido.admin.presentation.users

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
import com.daxido.admin.models.UserManagement
import com.daxido.core.theme.DaxidoGold
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showBanDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserManagement?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("User Management", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.users.size} total users",
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
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add export functionality */ },
                containerColor = DaxidoGold
            ) {
                Icon(Icons.Default.Download, "Export", tint = Color.White)
            }
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
                placeholder = { Text("Search by name, phone or email") },
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
                    selected = uiState.filter == UserFilter.ALL,
                    onClick = { viewModel.setFilter(UserFilter.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.filter == UserFilter.ACTIVE,
                    onClick = { viewModel.setFilter(UserFilter.ACTIVE) },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = uiState.filter == UserFilter.BANNED,
                    onClick = { viewModel.setFilter(UserFilter.BANNED) },
                    label = { Text("Banned") }
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
                val filteredUsers = uiState.users.filter { user ->
                    val matchesSearch = searchQuery.isEmpty() ||
                            user.name.contains(searchQuery, ignoreCase = true) ||
                            user.phoneNumber.contains(searchQuery) ||
                            (user.email?.contains(searchQuery, ignoreCase = true) == true)

                    val matchesFilter = when (uiState.filter) {
                        UserFilter.ALL -> true
                        UserFilter.ACTIVE -> user.isActive && !user.isBanned
                        UserFilter.BANNED -> user.isBanned
                    }

                    matchesSearch && matchesFilter
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserCard(
                            user = user,
                            onClick = { onUserClick(user.userId) },
                            onBanClick = {
                                selectedUser = user
                                showBanDialog = true
                            },
                            onUnbanClick = { viewModel.unbanUser(user.userId) }
                        )
                    }
                }
            }
        }
    }

    if (showBanDialog && selectedUser != null) {
        BanUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showBanDialog = false
                selectedUser = null
            },
            onConfirm = { reason ->
                viewModel.banUser(selectedUser!!.userId, reason)
                showBanDialog = false
                selectedUser = null
            }
        )
    }
}

@Composable
fun UserCard(
    user: UserManagement,
    onClick: () -> Unit,
    onBanClick: () -> Unit,
    onUnbanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isBanned) Color.Red.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
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
                            .background(DaxidoGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = DaxidoGold,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column {
                        Text(
                            user.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            user.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        if (user.email != null) {
                            Text(
                                user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (user.isBanned) {
                        Badge(containerColor = Color.Red) {
                            Text("BANNED", color = Color.White)
                        }
                    } else if (!user.isActive) {
                        Badge(containerColor = Color.Gray) {
                            Text("INACTIVE", color = Color.White)
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
                StatItem("Rides", user.totalRides.toString(), Icons.Default.DirectionsCar)
                StatItem("Rating", "%.1f".format(user.rating), Icons.Default.Star)
                StatItem("Spent", "₹${user.totalSpent.toInt()}", Icons.Default.AttachMoney)
                StatItem("Wallet", "₹${user.walletBalance.toInt()}", Icons.Default.AccountBalance)
            }

            Divider()

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Joined: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(user.joinedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (user.complaints > 0) {
                        Text(
                            "${user.complaints} complaints",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.isBanned) {
                        Button(
                            onClick = onUnbanClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unban")
                        }
                    } else {
                        Button(
                            onClick = onBanClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ban")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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

@Composable
fun BanUserDialog(
    user: UserManagement,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ban User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to ban ${user.name}?")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Ban User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class UserFilter {
    ALL, ACTIVE, BANNED
}
