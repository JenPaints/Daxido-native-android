package com.daxido.user.presentation.ride

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.daxido.R
import com.daxido.core.theme.*
import kotlinx.coroutines.delay

@Composable
fun DriverSearchScreen(
    onDriverFound: (String) -> Unit,
    onCancelSearch: () -> Unit
) {
    var searchRadius by remember { mutableStateOf(2f) }
    var driversFound by remember { mutableStateOf(0) }
    var searchAttempts by remember { mutableStateOf(0) }
    var showCancelWarning by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(Unit) {
        // Simulate progressive search
        while (searchRadius <= 10f) {
            delay(3000)
            searchRadius += 1f
            searchAttempts++

            // Simulate finding drivers
            if (searchRadius >= 4f) {
                driversFound = (1..5).random()
                if (driversFound > 0 && (0..100).random() > 30) {
                    // Driver accepted
                    onDriverFound("driver_${(1000..9999).random()}")
                    break
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        DaxidoPaleGold
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Finding Your Driver",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Searching within ${searchRadius.toInt()} km radius",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Animated search visualization
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Expanding ripple effect
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .alpha(1f - pulseAnimation / 1.5f)
                ) {
                    drawCircle(
                        color = DaxidoGold,
                        radius = size.minDimension / 2 * pulseAnimation,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Secondary ripple
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .alpha(0.5f - pulseAnimation / 3f)
                ) {
                    drawCircle(
                        color = DaxidoLightGold,
                        radius = size.minDimension / 3 * pulseAnimation,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Central car icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(DaxidoGold)
                        .scale(1f + pulseAnimation * 0.1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalTaxi,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = DaxidoWhite
                    )
                }

                // Orbiting driver indicators
                if (driversFound > 0) {
                    for (i in 0 until driversFound.coerceAtMost(5)) {
                        val angle = rotationAnimation + (i * 72f)
                        val radius = 100.dp

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (radius.value * kotlin.math.cos(Math.toRadians(angle.toDouble()))).dp,
                                    y = (radius.value * kotlin.math.sin(Math.toRadians(angle.toDouble()))).dp
                                )
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(StatusOnline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                tint = DaxidoWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = driversFound.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoGold
                        )
                        Text(
                            text = "Drivers Found",
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
                            text = searchAttempts.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DaxidoMediumBrown
                        )
                        Text(
                            text = "Requests Sent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tips while waiting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DaxidoPaleGold.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Tip",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DaxidoGold
                    )
                    Text(
                        text = "Peak hours may have longer wait times. Consider booking in advance next time!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cancel button
            OutlinedButton(
                onClick = { showCancelWarning = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 2.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancel Search",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showCancelWarning) {
        AlertDialog(
            onDismissRequest = { showCancelWarning = false },
            title = {
                Text(
                    text = "Cancel Search?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Cancelling now may result in a ₹50 cancellation fee if a driver has already accepted your request.",
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelWarning = false
                        onCancelSearch()
                    }
                ) {
                    Text(
                        text = "Yes, Cancel",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelWarning = false }
                ) {
                    Text("Continue Searching")
                }
            }
        )
    }
}