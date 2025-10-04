package com.daxido.user.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = DaxidoDarkBrown) },
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Account Settings
                item {
                    SettingsSectionHeader(title = "Account")
                }
                
                items(uiState.accountSettings) { setting ->
                    SettingsItem(
                        setting = setting,
                        onToggle = { viewModel.toggleSetting(setting.id) },
                        onClick = { viewModel.onSettingClick(setting.id) }
                    )
                }

                // Privacy & Security
                item {
                    SettingsSectionHeader(title = "Privacy & Security")
                }
                
                items(uiState.privacySettings) { setting ->
                    SettingsItem(
                        setting = setting,
                        onToggle = { viewModel.toggleSetting(setting.id) },
                        onClick = { viewModel.onSettingClick(setting.id) }
                    )
                }

                // Notifications
                item {
                    SettingsSectionHeader(title = "Notifications")
                }
                
                items(uiState.notificationSettings) { setting ->
                    SettingsItem(
                        setting = setting,
                        onToggle = { viewModel.toggleSetting(setting.id) },
                        onClick = { viewModel.onSettingClick(setting.id) }
                    )
                }

                // App Preferences
                item {
                    SettingsSectionHeader(title = "App Preferences")
                }
                
                items(uiState.appSettings) { setting ->
                    SettingsItem(
                        setting = setting,
                        onToggle = { viewModel.toggleSetting(setting.id) },
                        onClick = { viewModel.onSettingClick(setting.id) }
                    )
                }

                // About
                item {
                    SettingsSectionHeader(title = "About")
                }
                
                items(uiState.aboutSettings) { setting ->
                    SettingsItem(
                        setting = setting,
                        onToggle = { viewModel.toggleSetting(setting.id) },
                        onClick = { viewModel.onSettingClick(setting.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            uiState.message?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessage() }) {
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
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = DaxidoDarkBrown,
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingsItem(
    setting: SettingItem,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DaxidoLightGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = setting.icon,
                    contentDescription = null,
                    tint = DaxidoGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = setting.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = DaxidoDarkBrown
                    )
                    if (setting.subtitle.isNotEmpty()) {
                        Text(
                            text = setting.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = DaxidoDarkGray
                        )
                    }
                }
            }
            
            when (setting.type) {
                SettingType.TOGGLE -> {
                    Switch(
                        checked = setting.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DaxidoWhite,
                            checkedTrackColor = DaxidoGold,
                            uncheckedThumbColor = DaxidoWhite,
                            uncheckedTrackColor = DaxidoDarkGray
                        )
                    )
                }
                SettingType.NAVIGATION -> {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate",
                        tint = DaxidoMediumBrown
                    )
                }
                SettingType.ACTION -> {
                    Text(
                        text = setting.actionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DaxidoGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val type: SettingType,
    val isEnabled: Boolean = false,
    val actionText: String = ""
)

enum class SettingType {
    TOGGLE, NAVIGATION, ACTION
}
