package com.daxido.user.presentation.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * AI Assistant Screen for Daxido ride-sharing app
 * Provides AI-powered features like route suggestions, pricing recommendations, and customer support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    
    val tabs = listOf(
        "Route Suggestions" to Icons.Default.Route,
        "Pricing" to Icons.Default.AttachMoney,
        "Support" to Icons.Default.Support,
        "Forecast" to Icons.Default.TrendingUp,
        "General" to Icons.Default.Chat
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "AI",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = title,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> RouteSuggestionsContent(
                uiState = uiState,
                userInput = userInput,
                onUserInputChange = { userInput = it },
                onGenerateSuggestions = { origin, destination, preferences ->
                    viewModel.generateRouteSuggestions(origin, destination, preferences)
                }
            )
            1 -> PricingRecommendationsContent(
                uiState = uiState,
                userInput = userInput,
                onUserInputChange = { userInput = it },
                onGenerateRecommendations = { baseFare, demandLevel ->
                    viewModel.generatePricingRecommendations(baseFare, demandLevel)
                }
            )
            2 -> CustomerSupportContent(
                uiState = uiState,
                userInput = userInput,
                onUserInputChange = { userInput = it },
                onGenerateResponse = { query ->
                    viewModel.generateCustomerSupportResponse(query)
                }
            )
            3 -> DemandForecastContent(
                uiState = uiState,
                userInput = userInput,
                onUserInputChange = { userInput = it },
                onGenerateForecast = { location, timeRange ->
                    viewModel.generateDemandForecast(location, timeRange)
                }
            )
            4 -> GeneralAiContent(
                uiState = uiState,
                userInput = userInput,
                onUserInputChange = { userInput = it },
                onGenerateResponse = { prompt ->
                    viewModel.generateGeneralResponse(prompt)
                }
            )
        }
        
        // Error message
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteSuggestionsContent(
    uiState: AiAssistantUiState,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onGenerateSuggestions: (String, String, String) -> Unit
) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var preferences by remember { mutableStateOf("") }
    
    Column {
        Text(
            text = "Get AI-powered route suggestions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = origin,
            onValueChange = { origin = it },
            label = { Text("Origin") },
            placeholder = { Text("e.g., Central Park, New York") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Origin") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination") },
            placeholder = { Text("e.g., Times Square, New York") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Destination") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = preferences,
            onValueChange = { preferences = it },
            label = { Text("Preferences (Optional)") },
            placeholder = { Text("e.g., Avoid highways, scenic route") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Preferences") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onGenerateSuggestions(origin, destination, preferences) },
            enabled = origin.isNotEmpty() && destination.isNotEmpty() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Generate Route Suggestions")
        }
        
        uiState.routeSuggestions?.let { suggestions ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Route Suggestions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = suggestions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PricingRecommendationsContent(
    uiState: AiAssistantUiState,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onGenerateRecommendations: (Double, String) -> Unit
) {
    var baseFare by remember { mutableStateOf("") }
    var demandLevel by remember { mutableStateOf("") }
    
    Column {
        Text(
            text = "Get AI-powered pricing recommendations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = baseFare,
            onValueChange = { baseFare = it },
            label = { Text("Base Fare (₹)") },
            placeholder = { Text("e.g., 150") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Fare") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = demandLevel,
            onValueChange = { demandLevel = it },
            label = { Text("Demand Level") },
            placeholder = { Text("e.g., High, Medium, Low") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = "Demand") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                baseFare.toDoubleOrNull()?.let { fare ->
                    onGenerateRecommendations(fare, demandLevel)
                }
            },
            enabled = baseFare.isNotEmpty() && demandLevel.isNotEmpty() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Generate Pricing Recommendations")
        }
        
        uiState.pricingRecommendations?.let { recommendations ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Pricing Recommendations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recommendations,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerSupportContent(
    uiState: AiAssistantUiState,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onGenerateResponse: (String) -> Unit
) {
    Column {
        Text(
            text = "AI Customer Support",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = userInput,
            onValueChange = onUserInputChange,
            label = { Text("Your Question") },
            placeholder = { Text("e.g., How do I cancel my ride?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            leadingIcon = { Icon(Icons.Default.Help, contentDescription = "Question") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onGenerateResponse(userInput) },
            enabled = userInput.isNotEmpty() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Get AI Support")
        }
        
        uiState.customerSupportResponse?.let { response ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Support Response",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DemandForecastContent(
    uiState: AiAssistantUiState,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onGenerateForecast: (String, String) -> Unit
) {
    var location by remember { mutableStateOf("") }
    var timeRange by remember { mutableStateOf("") }
    
    Column {
        Text(
            text = "AI Demand Forecasting",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            placeholder = { Text("e.g., Manhattan, New York") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = timeRange,
            onValueChange = { timeRange = it },
            label = { Text("Time Range") },
            placeholder = { Text("e.g., Next 2 hours, Tomorrow morning") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Time") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onGenerateForecast(location, timeRange) },
            enabled = location.isNotEmpty() && timeRange.isNotEmpty() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Generate Demand Forecast")
        }
        
        uiState.demandForecast?.let { forecast ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Demand Forecast",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = forecast,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralAiContent(
    uiState: AiAssistantUiState,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onGenerateResponse: (String) -> Unit
) {
    Column {
        Text(
            text = "General AI Assistant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = userInput,
            onValueChange = onUserInputChange,
            label = { Text("Ask me anything") },
            placeholder = { Text("e.g., What's the best time to book a ride?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = "Question") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onGenerateResponse(userInput) },
            enabled = userInput.isNotEmpty() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Ask AI")
        }
        
        uiState.generalResponse?.let { response ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Response",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
