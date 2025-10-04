package com.daxido.user.presentation.surge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurgePricingScreen(
    viewModel: SurgePricingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val bangalore = LatLng(12.9716, 77.5946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bangalore, 12f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Surge Pricing Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map with surge heat map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Add markers for surge zones
                uiState.surgeZones.forEach { zone ->
                    Marker(
                        state = MarkerState(position = zone.location),
                        title = "${zone.multiplier}x Surge",
                        snippet = zone.areaName
                    )

                    // Draw circle for surge zone
                    Circle(
                        center = zone.location,
                        radius = zone.radiusMeters.toDouble(),
                        fillColor = when {
                            zone.multiplier >= 2.5 -> Color.Red.copy(alpha = 0.3f)
                            zone.multiplier >= 2.0 -> Color(0xFFFF9800).copy(alpha = 0.3f)
                            zone.multiplier >= 1.5 -> Color(0xFFFFEB3B).copy(alpha = 0.3f)
                            else -> Color.Green.copy(alpha = 0.2f)
                        },
                        strokeColor = when {
                            zone.multiplier >= 2.5 -> Color.Red
                            zone.multiplier >= 2.0 -> Color(0xFFFF9800)
                            zone.multiplier >= 1.5 -> Color(0xFFFFEB3B)
                            else -> Color.Green
                        },
                        strokeWidth = 2f
                    )
                }
            }

            // Surge legend
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Surge Levels",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    SurgeLegendItem(Color.Red, "2.5x+ High")
                    SurgeLegendItem(Color(0xFFFF9800), "2.0x Medium")
                    SurgeLegendItem(Color(0xFFFFEB3B), "1.5x Low")
                    SurgeLegendItem(Color.Green, "Normal")
                }
            }

            // Current area surge info
            if (uiState.currentAreaSurge != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                                "Current Area",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                uiState.currentAreaSurge!!.areaName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = when {
                                uiState.currentAreaSurge!!.multiplier >= 2.5 -> Color.Red
                                uiState.currentAreaSurge!!.multiplier >= 2.0 -> Color(0xFFFF9800)
                                uiState.currentAreaSurge!!.multiplier >= 1.5 -> Color(0xFFFFEB3B)
                                else -> Color.Green
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${uiState.currentAreaSurge!!.multiplier}x",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (uiState.currentAreaSurge!!.multiplier > 1.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "High demand in this area. Fares are temporarily higher.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SurgeLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

data class SurgeZone(
    val id: String,
    val location: LatLng,
    val areaName: String,
    val multiplier: Double,
    val radiusMeters: Int,
    val demandLevel: String
)

data class SurgePricingUiState(
    val surgeZones: List<SurgeZone> = emptyList(),
    val currentAreaSurge: SurgeZone? = null,
    val isLoading: Boolean = false
)