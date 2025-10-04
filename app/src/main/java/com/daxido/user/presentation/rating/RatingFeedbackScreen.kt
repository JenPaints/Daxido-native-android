package com.daxido.user.presentation.rating

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.daxido.core.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RatingFeedbackScreen(
    rideId: String,
    driverName: String,
    driverPhoto: String,
    vehicleNumber: String,
    fare: Double,
    onRatingSubmit: (rating: Int, feedback: String, tags: List<String>) -> Unit,
    onSkip: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showTipOption by remember { mutableStateOf(false) }
    var tipAmount by remember { mutableStateOf(0) }

    val positiveTags = listOf(
        "Safe Driving", "Clean Vehicle", "Polite", "On Time",
        "Helpful", "Good Music", "Smooth Ride", "Professional"
    )

    val negativeTags = listOf(
        "Unsafe Driving", "Unclean Vehicle", "Rude", "Late",
        "Wrong Route", "AC Not Working", "Overcharging", "Unprofessional"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rate Your Ride",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Driver Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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
                    // Driver Photo
                    Box(
                        modifier = Modifier.size(80.dp)
                    ) {
                        AsyncImage(
                            model = driverPhoto,
                            contentDescription = "Driver",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(3.dp, DaxidoGold, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        if (selectedRating >= 4) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(DaxidoGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = DaxidoWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = driverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = vehicleNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = StatusOnline,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " Trip completed • ₹${fare.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusOnline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Rating Section
            Text(
                text = "How was your ride?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Star Rating
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 1..5) {
                    IconButton(
                        onClick = {
                            selectedRating = i
                            showTipOption = i >= 4
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i <= selectedRating) DaxidoGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Rating Text
            if (selectedRating > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when(selectedRating) {
                        1 -> "Very Poor"
                        2 -> "Poor"
                        3 -> "Average"
                        4 -> "Good"
                        5 -> "Excellent"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = when(selectedRating) {
                        1, 2 -> Color.Red
                        3 -> DaxidoWarning
                        4, 5 -> StatusOnline
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Medium
                )
            }

            // Tip Option for Good Ratings
            AnimatedVisibility(
                visible = showTipOption,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusOnline.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = StatusOnline,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " Add a tip for excellent service?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0, 10, 20, 30, 50).forEach { amount ->
                                FilterChip(
                                    selected = tipAmount == amount,
                                    onClick = { tipAmount = if (tipAmount == amount) 0 else amount },
                                    label = {
                                        Text(
                                            if (amount == 0) "No Tip" else "₹$amount",
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusOnline,
                                        selectedLabelColor = DaxidoWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Feedback Tags
            if (selectedRating > 0) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (selectedRating >= 4) "What went well?" else "What could be improved?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tags = if (selectedRating >= 4) positiveTags else negativeTags
                            tags.forEach { tag ->
                                FilterChip(
                                    selected = tag in selectedTags,
                                    onClick = {
                                        selectedTags = if (tag in selectedTags) {
                                            selectedTags - tag
                                        } else {
                                            selectedTags + tag
                                        }
                                    },
                                    label = { Text(tag, fontSize = 12.sp) },
                                    leadingIcon = if (tag in selectedTags) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (selectedRating >= 4)
                                            StatusOnline else DaxidoWarning,
                                        selectedLabelColor = DaxidoWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Additional Feedback
            if (selectedRating > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("Additional comments (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Compliments Section for 5-star ratings
            AnimatedVisibility(
                visible = selectedRating == 5,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DaxidoGold.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = DaxidoGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                text = "Driver will receive a compliment!",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Your feedback helps drivers improve",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    onRatingSubmit(
                        selectedRating,
                        feedbackText,
                        selectedTags.toList()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                enabled = selectedRating > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DaxidoGold,
                    disabledContainerColor = DaxidoLightGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (tipAmount > 0) "Submit & Tip ₹$tipAmount" else "Submit Rating",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}