package com.daxido.core.models

import java.util.Date

data class ScheduledRide(
    val scheduleId: String = "",
    val userId: String = "",
    val pickupLocation: Location = Location(),
    val dropLocation: Location = Location(),
    val vehicleType: VehicleType = VehicleType.CAR,
    val scheduledTime: Date = Date(),
    val recurringSchedule: RecurringSchedule? = null,
    val paymentMethod: PaymentMethod = PaymentMethod(),
    val estimatedFare: Fare = Fare(),
    val status: ScheduleStatus = ScheduleStatus.SCHEDULED,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val flexibleTiming: FlexibleTiming? = null,
    val specialInstructions: String? = null,
    val createdAt: Date = Date(),
    val assignedDriverId: String? = null,
    val confirmedAt: Date? = null,
    val cancelledAt: Date? = null,
    val cancellationReason: String? = null
)

data class RecurringSchedule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val daysOfWeek: List<DayOfWeek> = emptyList(),
    val endDate: Date? = null,
    val totalOccurrences: Int? = null,
    val excludedDates: List<Date> = emptyList(),
    val autoRenew: Boolean = false
)

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    WEEKDAYS_ONLY,
    WEEKENDS_ONLY,
    CUSTOM
}

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

data class ReminderSettings(
    val enableReminders: Boolean = true,
    val firstReminderMinutes: Int = 60, // 1 hour before
    val secondReminderMinutes: Int = 15, // 15 minutes before
    val smsReminder: Boolean = true,
    val pushReminder: Boolean = true,
    val emailReminder: Boolean = false
)

data class FlexibleTiming(
    val earliestTime: Date = Date(),
    val latestTime: Date = Date(),
    val preferredTime: Date = Date(),
    val flexibilityMinutes: Int = 15
)

enum class ScheduleStatus {
    SCHEDULED,
    DRIVER_ASSIGNED,
    REMINDER_SENT,
    CONFIRMED,
    EN_ROUTE,
    COMPLETED,
    CANCELLED,
    MISSED,
    RESCHEDULED
}

data class ScheduleConflict(
    val conflictType: ConflictType = ConflictType.TIME_OVERLAP,
    val existingSchedule: ScheduledRide? = null,
    val suggestedAlternatives: List<Date> = emptyList(),
    val message: String = ""
)

enum class ConflictType {
    TIME_OVERLAP,
    LOCATION_TOO_FAR,
    NO_DRIVERS_AVAILABLE,
    SURGE_PRICING,
    MAINTENANCE_WINDOW
}

data class ScheduledRideReminder(
    val scheduleId: String,
    val userId: String,
    val reminderType: ReminderType,
    val scheduledTime: Date,
    val message: String,
    val actionUrl: String? = null,
    val sent: Boolean = false,
    val sentAt: Date? = null
)

enum class ReminderType {
    FIRST_REMINDER,
    FINAL_REMINDER,
    DRIVER_ASSIGNED,
    SCHEDULE_CHANGED,
    CANCELLATION_WARNING
}