package com.daxido.core.models

import java.util.Date

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val actionText: String? = null,
    val actionData: Map<String, String> = emptyMap()
)

enum class NotificationType {
    RIDE_UPDATE,
    PAYMENT,
    PROMOTION,
    SAFETY,
    SYSTEM
}
