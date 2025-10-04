package com.daxido.user.presentation.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.data.repository.SupportRepository
import com.daxido.core.models.SupportCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    fun loadFaqs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val faqs = supportRepository.getFaqs()
                _uiState.update {
                    it.copy(
                        faqs = faqs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to load FAQs: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleFaq(faqId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                expandedFaqId = if (currentState.expandedFaqId == faqId) null else faqId
            )
        }
    }

    fun startLiveChat() {
        viewModelScope.launch {
            try {
                val chatSession = supportRepository.initiateLiveChat()
                _uiState.update {
                    it.copy(
                        liveChatSessionId = chatSession.id,
                        message = "Connecting to live chat..."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to start live chat: ${e.message}")
                }
            }
        }
    }

    fun callSupport() {
        viewModelScope.launch {
            try {
                val supportNumber = supportRepository.getSupportPhoneNumber()
                _uiState.update {
                    it.copy(
                        supportPhoneNumber = supportNumber,
                        message = "Dialing support..."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to get support number: ${e.message}")
                }
            }
        }
    }

    fun emailSupport() {
        viewModelScope.launch {
            try {
                val emailAddress = supportRepository.getSupportEmailAddress()
                _uiState.update {
                    it.copy(
                        supportEmailAddress = emailAddress,
                        message = "Opening email client..."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to get support email: ${e.message}")
                }
            }
        }
    }

    fun reportIssue(
        issueType: IssueType,
        description: String,
        rideId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingIssue = true) }
            try {
                val ticketId = supportRepository.reportIssue(
                    issueType = issueType,
                    description = description,
                    rideId = rideId
                )
                _uiState.update {
                    it.copy(
                        isSubmittingIssue = false,
                        message = "Issue reported. Ticket #$ticketId created.",
                        lastTicketId = ticketId
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmittingIssue = false,
                        error = "Failed to report issue: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }
}

data class SupportUiState(
    val faqs: List<FAQ> = emptyList(),
    val expandedFaqId: String? = null,
    val isLoading: Boolean = false,
    val isSubmittingIssue: Boolean = false,
    val liveChatSessionId: String? = null,
    val supportPhoneNumber: String? = null,
    val supportEmailAddress: String? = null,
    val lastTicketId: String? = null,
    val message: String? = null,
    val error: String? = null,
    val supportCategories: List<SupportCategory> = emptyList(),
    val selectedCategoryId: String? = null
)

enum class IssueType {
    PAYMENT_ISSUE,
    DRIVER_COMPLAINT,
    LOST_ITEM,
    SAFETY_CONCERN,
    APP_BUG,
    TRIP_ISSUE,
    REFUND_REQUEST,
    OTHER
}

data class ChatSession(
    val id: String,
    val agentName: String,
    val startTime: Long
)
