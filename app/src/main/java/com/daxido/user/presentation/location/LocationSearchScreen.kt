package com.daxido.user.presentation.location

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daxido.core.models.Location
import com.daxido.core.theme.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    searchType: LocationSearchType,
    onLocationSelected: (Location) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadRecentLocations()
        viewModel.getCurrentLocation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DaxidoWhite)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = DaxidoDarkBrown
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = when (searchType) {
                    LocationSearchType.PICKUP -> "Where are you?"
                    LocationSearchType.DROP -> "Where to?"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DaxidoDarkBrown
            )
        }

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    text = when (searchType) {
                        LocationSearchType.PICKUP -> "Enter pickup location"
                        LocationSearchType.DROP -> "Enter destination"
                    },
                    color = DaxidoMediumBrown
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = DaxidoMediumBrown
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = DaxidoMediumBrown
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DaxidoGold,
                unfocusedBorderColor = DaxidoLightGray,
                focusedTextColor = DaxidoDarkBrown,
                unfocusedTextColor = DaxidoDarkGray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DaxidoGold)
                }
            }
            
            uiState.searchQuery.isNotEmpty() -> {
                // Search Results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(uiState.searchResults) { place ->
                        LocationSearchItem(
                            place = place,
                            onClick = { onLocationSelected(place.location) }
                        )
                    }
                }
            }
            
            else -> {
                // Default Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    // Current Location
                    if (uiState.currentLocation != null) {
                        item {
                            LocationSection(
                                title = "Current Location",
                                icon = Icons.Default.MyLocation,
                                iconColor = DaxidoGold
                            ) {
                                LocationSearchItem(
                                    place = Place(
                                        name = "Current Location",
                                        address = "Tap to use current location",
                                        location = uiState.currentLocation!!,
                                        type = PlaceType.CURRENT
                                    ),
                                    onClick = { onLocationSelected(uiState.currentLocation!!) }
                                )
                            }
                        }
                    }

                    // Recent Locations
                    if (uiState.recentLocations.isNotEmpty()) {
                        item {
                            LocationSection(
                                title = "Recent",
                                icon = Icons.Default.History,
                                iconColor = DaxidoMediumBrown
                            ) {
                                uiState.recentLocations.forEach { place ->
                                    LocationSearchItem(
                                        place = place,
                                        onClick = { onLocationSelected(place.location) }
                                    )
                                }
                            }
                        }
                    }

                    // Saved Places
                    if (uiState.savedPlaces.isNotEmpty()) {
                        item {
                            LocationSection(
                                title = "Saved Places",
                                icon = Icons.Default.Star,
                                iconColor = DaxidoGold
                            ) {
                                uiState.savedPlaces.forEach { place ->
                                    LocationSearchItem(
                                        place = place,
                                        onClick = { onLocationSelected(place.location) }
                                    )
                                }
                            }
                        }
                    }

                    // Popular Places
                    item {
                        LocationSection(
                            title = "Popular Places",
                            icon = Icons.Default.Place,
                            iconColor = DaxidoMediumBrown
                        ) {
                            uiState.popularPlaces.forEach { place ->
                                LocationSearchItem(
                                    place = place,
                                    onClick = { onLocationSelected(place.location) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DaxidoDarkBrown
            )
        }
        
        content()
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LocationSearchItem(
    place: Place,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DaxidoWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (place.type) {
                            PlaceType.CURRENT -> DaxidoPaleGold
                            PlaceType.RECENT -> DaxidoLightGray
                            PlaceType.SAVED -> DaxidoPaleGold
                            PlaceType.POPULAR -> DaxidoLightGray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (place.type) {
                        PlaceType.CURRENT -> Icons.Default.MyLocation
                        PlaceType.RECENT -> Icons.Default.History
                        PlaceType.SAVED -> Icons.Default.Star
                        PlaceType.POPULAR -> Icons.Default.Place
                    },
                    contentDescription = null,
                    tint = when (place.type) {
                        PlaceType.CURRENT -> DaxidoGold
                        PlaceType.RECENT -> DaxidoMediumBrown
                        PlaceType.SAVED -> DaxidoGold
                        PlaceType.POPULAR -> DaxidoMediumBrown
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = place.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DaxidoDarkBrown,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (place.address.isNotEmpty()) {
                    Text(
                        text = place.address,
                        fontSize = 14.sp,
                        color = DaxidoMediumBrown,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action Button
            if (place.type == PlaceType.SAVED) {
                IconButton(onClick = { /* Remove from saved */ }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Remove from saved",
                        tint = DaxidoGold
                    )
                }
            }
        }
    }
}

enum class LocationSearchType {
    PICKUP, DROP
}

data class Place(
    val name: String,
    val address: String,
    val location: Location,
    val type: PlaceType
)

enum class PlaceType {
    CURRENT, RECENT, SAVED, POPULAR
}
