package com.daxido.user.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.core.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = DaxidoDarkBrown) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DaxidoDarkBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DaxidoWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DaxidoWhite)
        ) {
            TabRow(
                selectedTabIndex = uiState.selectedFilter.ordinal,
                containerColor = DaxidoWhite,
                contentColor = DaxidoGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedFilter.ordinal]),
                        color = DaxidoGold
                    )
                }
            ) {
                NotificationFilter.values().forEach { filter ->
                    Tab(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        text = { 
                            Text(
                                filter.name.lowercase().replaceFirstChar { it.uppercase() }, 
                                color = if (uiState.selectedFilter == filter) DaxidoGold else DaxidoDarkGray
                            ) 
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    color = DaxidoGold
                )
            } else if (uiState.filteredNotifications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "No Notifications",
                        tint = DaxidoMediumBrown,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications yet!",
                        style = MaterialTheme.typography.titleMedium,
                        color = DaxidoDarkGray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredNotifications) { notification ->
                        NotificationItem(notification = notification) {
                            viewModel.markAsRead(notification.id)
                        }
                    }
                }
            }

            uiState.error?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = DaxidoGold)
                        }
                    }
                ) {
                    Text(message, color = DaxidoWhite)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: (Notification) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(notification) },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) DaxidoLightGray else DaxidoPaleGold
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (notification.type) {
                    NotificationType.RIDE_REQUEST,
                    NotificationType.RIDE_ACCEPTED,
                    NotificationType.DRIVER_ARRIVED,
                    NotificationType.RIDE_STARTED,
                    NotificationType.RIDE_COMPLETED -> Icons.Default.DirectionsCar
                    NotificationType.PAYMENT_RECEIVED -> Icons.Default.Payment
                    NotificationType.PROMO_OFFER,
                    NotificationType.REFERRAL_BONUS -> Icons.Default.LocalOffer
                    NotificationType.SAFETY_ALERT -> Icons.Default.Warning
                    NotificationType.SYSTEM_UPDATE,
                    NotificationType.ACCOUNT_UPDATE -> Icons.Default.Info
                    NotificationType.RATING_REMINDER -> Icons.Default.Star
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = DaxidoGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DaxidoDarkBrown
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DaxidoDarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = DaxidoMediumBrown
                )
            }
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(DaxidoGold)
                )
            }
        }
    }
}
