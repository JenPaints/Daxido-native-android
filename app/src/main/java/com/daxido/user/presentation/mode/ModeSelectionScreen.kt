package com.daxido.user.presentation.mode

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    onSelectUserMode: () -> Unit,
    onSelectDriverMode: () -> Unit,
    onSelectAdminMode: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DaxidoWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Daxido",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = DaxidoGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose your mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = DaxidoDarkGray,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Mode Selection Cards (3 cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Mode Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .clickable { onSelectUserMode() },
                    colors = CardDefaults.cardColors(containerColor = DaxidoPaleGold),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Mode",
                            tint = DaxidoGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Rider",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoDarkBrown
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Book rides and travel comfortably",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DaxidoDarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Driver Mode Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .clickable { onSelectDriverMode() },
                    colors = CardDefaults.cardColors(containerColor = DaxidoLightGray),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Driver Mode",
                            tint = DaxidoGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Driver",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoDarkBrown
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Drive and earn money",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DaxidoDarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Admin Mode Card (Full width)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onSelectAdminMode() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Mode",
                        tint = DaxidoGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Column {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manage platform, users, and drivers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Additional Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DaxidoLightGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Switch modes anytime",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = DaxidoDarkBrown
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You can switch between rider and driver modes from your profile settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = DaxidoDarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
