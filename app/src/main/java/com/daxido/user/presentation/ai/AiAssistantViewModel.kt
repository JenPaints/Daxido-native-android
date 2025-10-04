package com.daxido.user.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.ai.FirebaseAiLogicService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI-powered features in the Daxido app
 */
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val firebaseAiLogicService: FirebaseAiLogicService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()
    
    /**
     * Generate route suggestions using AI
     */
    fun generateRouteSuggestions(
        origin: String,
        destination: String,
        preferences: String = "",
        trafficConditions: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            firebaseAiLogicService.generateRouteSuggestions(
                origin = origin,
                destination = destination,
                preferences = preferences,
                trafficConditions = trafficConditions
            ).fold(
                onSuccess = { suggestions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        routeSuggestions = suggestions,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate route suggestions: ${error.message}"
                    )
                }
            )
        }
    }
    
    /**
     * Generate pricing recommendations using AI
     */
    fun generatePricingRecommendations(
        baseFare: Double,
        demandLevel: String,
        weatherConditions: String = "",
        timeOfDay: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            firebaseAiLogicService.generatePricingRecommendations(
                baseFare = baseFare,
                demandLevel = demandLevel,
                weatherConditions = weatherConditions,
                timeOfDay = timeOfDay
            ).fold(
                onSuccess = { recommendations ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pricingRecommendations = recommendations,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate pricing recommendations: ${error.message}"
                    )
                }
            )
        }
    }
    
    /**
     * Generate customer support response using AI
     */
    fun generateCustomerSupportResponse(
        userQuery: String,
        userContext: String = "",
        previousMessages: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            firebaseAiLogicService.generateCustomerSupportResponse(
                userQuery = userQuery,
                userContext = userContext,
                previousMessages = previousMessages
            ).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        customerSupportResponse = response,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate support response: ${error.message}"
                    )
                }
            )
        }
    }
    
    /**
     * Generate demand forecast using AI
     */
    fun generateDemandForecast(
        location: String,
        timeRange: String,
        historicalData: String = "",
        events: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            firebaseAiLogicService.generateDemandForecast(
                location = location,
                timeRange = timeRange,
                historicalData = historicalData,
                events = events
            ).fold(
                onSuccess = { forecast ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        demandForecast = forecast,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate demand forecast: ${error.message}"
                    )
                }
            )
        }
    }
    
    /**
     * Generate general AI response
     */
    fun generateGeneralResponse(prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            firebaseAiLogicService.generateResponse(prompt).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalResponse = response,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate response: ${error.message}"
                    )
                }
            )
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Clear all AI responses
     */
    fun clearResponses() {
        _uiState.value = _uiState.value.copy(
            routeSuggestions = null,
            pricingRecommendations = null,
            customerSupportResponse = null,
            demandForecast = null,
            generalResponse = null,
            errorMessage = null
        )
    }
}

/**
 * UI State for AI Assistant
 */
data class AiAssistantUiState(
    val isLoading: Boolean = false,
    val routeSuggestions: String? = null,
    val pricingRecommendations: String? = null,
    val customerSupportResponse: String? = null,
    val demandForecast: String? = null,
    val generalResponse: String? = null,
    val errorMessage: String? = null
)
