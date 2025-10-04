package com.daxido.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daxido.core.location.RealTimeLocationService
import com.daxido.core.maps.PreciseRoutingService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreciseNavigationScreen(
    rideId: String,
    origin: LatLng,
    destination: LatLng,
    onNavigateBack: () -> Unit,
    onNavigationComplete: () -> Unit,
    viewModel: PreciseNavigationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize services
    val routingService = remember { PreciseRoutingService(context) }
    val locationService = remember { RealTimeLocationService(context) }
    
    // Load route and start navigation
    LaunchedEffect(rideId, origin, destination) {
        viewModel.loadRoute(rideId)
        
        // Get precise route
        val route = routingService.getPreciseDirections(
            origin = origin,
            destination = destination,
            mode = "driving",
            alternatives = false,
            avoidTolls = false,
            avoidHighways = false
        )
        
        route?.let { routeResult ->
            viewModel.setRoute(routeResult.steps)
            viewModel.startNavigation()
        }
    }
    
    // Start location tracking with error handling
    LaunchedEffect(Unit) {
        try {
            locationService.startLocationTracking().collect { location ->
                viewModel.updateCurrentLocation(
                    com.google.android.gms.maps.model.LatLng(
                        location.latitude,
                        location.longitude
                    )
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.e("PreciseNavigationScreen", "Location permission error", e)
            // Handle permission error gracefully - navigation will continue with manual updates
        } catch (e: Exception) {
            android.util.Log.e("PreciseNavigationScreen", "Location tracking error", e)
            // Handle other errors gracefully
        }
    }
    
    // Simulate navigation progress
    LaunchedEffect(uiState.isNavigating) {
        if (uiState.isNavigating) {
            while (uiState.isNavigating && uiState.currentStepIndex < uiState.route.size) {
                delay(3000) // Update every 3 seconds
                viewModel.updateNavigationProgress(uiState.currentStepIndex + 1)
            }
            
            if (uiState.currentStepIndex >= uiState.route.size) {
                viewModel.completeNavigation()
                onNavigationComplete()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Navigation Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Navigation Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusChip(
                            status = if (uiState.isNavigating) "Navigating" else "Stopped"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Route Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${uiState.distance.div(1000)} km",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${uiState.duration.div(60)} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${uiState.progressPercentage.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Current Step Instructions
            if (uiState.currentStep != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Next Turn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Turn Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getTurnIcon(uiState.currentStep?.instruction ?: ""),
                                    contentDescription = "Turn",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Instruction
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.currentStep?.instruction ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "In ${uiState.currentStep?.distance ?: 0}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Route Steps List
            if (uiState.route.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Route Steps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        uiState.route.forEachIndexed { index, step ->
                            RouteStepCard(
                                step = step,
                                isCurrent = index == uiState.currentStepIndex,
                                isCompleted = index < uiState.currentStepIndex
                            )
                            
                            if (index < uiState.route.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            
            // Navigation Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Navigation Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.toggleNavigation() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isNavigating) Color.Red else Color.Green
                            )
                        ) {
                            Icon(
                                if (uiState.isNavigating) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isNavigating) "Pause" else "Start",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isNavigating) "Pause" else "Start")
                        }
                        
                        Button(
                            onClick = { viewModel.skipStep() },
                            enabled = uiState.isNavigating
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Skip",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Skip")
                        }
                        
                        Button(
                            onClick = { viewModel.recalculateRoute() },
                            enabled = uiState.isNavigating
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Recalculate",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recalculate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "Navigating" -> Pair(Color.Green, Color.White)
        "Stopped" -> Pair(Color.Red, Color.White)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RouteStepCard(
    step: com.daxido.core.maps.RouteStep,
    isCurrent: Boolean,
    isCompleted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                isCompleted -> Color.Green.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isCurrent) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> Color.Green
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                } else {
                    Icon(
                        getTurnIcon(step.instruction),
                        contentDescription = "Step",
                        modifier = Modifier.size(16.dp),
                        tint = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Step Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${step.distance}m • ${step.duration}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getTurnIcon(instruction: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        instruction.contains("left", ignoreCase = true) -> Icons.Default.TurnLeft
        instruction.contains("right", ignoreCase = true) -> Icons.Default.TurnRight
        instruction.contains("straight", ignoreCase = true) -> Icons.Default.Straight
        instruction.contains("u-turn", ignoreCase = true) -> Icons.Default.UTurnLeft
        instruction.contains("merge", ignoreCase = true) -> Icons.Default.Merge
        instruction.contains("exit", ignoreCase = true) -> Icons.Default.ExitToApp
        else -> Icons.Default.Navigation
    }
}
