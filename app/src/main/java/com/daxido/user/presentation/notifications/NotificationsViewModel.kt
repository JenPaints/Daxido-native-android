package com.daxido.user.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.data.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        observeNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val notifications = notificationsRepository.getNotifications()
                _uiState.update {
                    it.copy(
                        notifications = notifications,
                        filteredNotifications = filterNotifications(notifications, it.selectedFilter),
                        unreadCount = notifications.count { !it.isRead },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to load notifications",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationsRepository.observeNotifications()
                .collect { notifications ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            notifications = notifications,
                            filteredNotifications = filterNotifications(notifications, currentState.selectedFilter),
                            unreadCount = notifications.count { !it.isRead }
                        )
                    }
                }
        }
    }

    fun setFilter(filter: NotificationFilter) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedFilter = filter,
                filteredNotifications = filterNotifications(currentState.notifications, filter)
            )
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val success = notificationsRepository.markAsRead(notificationId)
                if (success) {
                    _uiState.update { currentState ->
                        val updatedNotifications = currentState.notifications.map { notification ->
                            if (notification.id == notificationId) {
                                notification.copy(isRead = true)
                            } else {
                                notification
                            }
                        }
                        currentState.copy(
                            notifications = updatedNotifications,
                            filteredNotifications = filterNotifications(updatedNotifications, currentState.selectedFilter),
                            unreadCount = updatedNotifications.count { !it.isRead }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to mark notification as read")
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val success = notificationsRepository.markAllAsRead()
                if (success) {
                    _uiState.update { currentState ->
                        val updatedNotifications = currentState.notifications.map { it.copy(isRead = true) }
                        currentState.copy(
                            notifications = updatedNotifications,
                            filteredNotifications = filterNotifications(updatedNotifications, currentState.selectedFilter),
                            unreadCount = 0
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to mark all notifications as read")
                }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                val success = notificationsRepository.deleteNotification(notificationId)
                if (success) {
                    _uiState.update { currentState ->
                        val updatedNotifications = currentState.notifications.filterNot { it.id == notificationId }
                        currentState.copy(
                            notifications = updatedNotifications,
                            filteredNotifications = filterNotifications(updatedNotifications, currentState.selectedFilter),
                            unreadCount = updatedNotifications.count { !it.isRead }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to delete notification")
                }
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                val success = notificationsRepository.clearAllNotifications()
                if (success) {
                    _uiState.update {
                        it.copy(
                            notifications = emptyList(),
                            filteredNotifications = emptyList(),
                            unreadCount = 0
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to clear notifications")
                }
            }
        }
    }

    fun refreshNotifications() {
        loadNotifications()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun filterNotifications(
        notifications: List<Notification>,
        filter: NotificationFilter
    ): List<Notification> {
        return when (filter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.RIDE -> notifications.filter {
                it.type in listOf(
                    NotificationType.RIDE_REQUEST,
                    NotificationType.RIDE_ACCEPTED,
                    NotificationType.DRIVER_ARRIVED,
                    NotificationType.RIDE_STARTED,
                    NotificationType.RIDE_COMPLETED
                )
            }
            NotificationFilter.PAYMENT -> notifications.filter {
                it.type == NotificationType.PAYMENT_RECEIVED
            }
            NotificationFilter.OFFERS -> notifications.filter {
                it.type in listOf(NotificationType.PROMO_OFFER, NotificationType.REFERRAL_BONUS)
            }
            NotificationFilter.SAFETY -> notifications.filter {
                it.type == NotificationType.SAFETY_ALERT
            }
            NotificationFilter.ACCOUNT -> notifications.filter {
                it.type in listOf(NotificationType.ACCOUNT_UPDATE, NotificationType.SYSTEM_UPDATE)
            }
        }
    }
}

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val filteredNotifications: List<Notification> = emptyList(),
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class NotificationFilter {
    ALL,
    RIDE,
    PAYMENT,
    OFFERS,
    SAFETY,
    ACCOUNT
}

data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val actionUrl: String? = null,
    val imageUrl: String? = null,
    val data: Map<String, String> = emptyMap()
)

enum class NotificationType {
    RIDE_REQUEST,
    RIDE_ACCEPTED,
    DRIVER_ARRIVED,
    RIDE_STARTED,
    RIDE_COMPLETED,
    PAYMENT_RECEIVED,
    PROMO_OFFER,
    SYSTEM_UPDATE,
    SAFETY_ALERT,
    RATING_REMINDER,
    REFERRAL_BONUS,
    ACCOUNT_UPDATE
}
