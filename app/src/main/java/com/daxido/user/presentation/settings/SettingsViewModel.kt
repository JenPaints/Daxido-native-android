package com.daxido.user.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.data.repository.UserRepository
import com.daxido.core.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Load actual user preferences from DataStore/SharedPreferences
            val accountSettings = listOf(
                SettingItem(
                    id = "edit_profile",
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    icon = Icons.Default.Person,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "payment_methods",
                    title = "Payment Methods",
                    subtitle = "Manage your payment options",
                    icon = Icons.Default.CreditCard,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "emergency_contacts",
                    title = "Emergency Contacts",
                    subtitle = "Add trusted contacts",
                    icon = Icons.Default.ContactPhone,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "saved_places",
                    title = "Saved Places",
                    subtitle = "Home, Work, and favorites",
                    icon = Icons.Default.BookmarkBorder,
                    type = SettingType.NAVIGATION
                )
            )

            val privacySettings = listOf(
                SettingItem(
                    id = "location_sharing",
                    title = "Share Live Location",
                    subtitle = "During active rides",
                    icon = Icons.Default.LocationOn,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isLocationSharingEnabled()
                ),
                SettingItem(
                    id = "biometric_auth",
                    title = "Biometric Authentication",
                    subtitle = "Use fingerprint or face ID",
                    icon = Icons.Default.Fingerprint,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isBiometricEnabled()
                ),
                SettingItem(
                    id = "two_factor",
                    title = "Two-Factor Authentication",
                    subtitle = "Extra security for your account",
                    icon = Icons.Default.Security,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isTwoFactorEnabled()
                ),
                SettingItem(
                    id = "data_sharing",
                    title = "Data & Analytics",
                    subtitle = "Help improve our services",
                    icon = Icons.Default.Analytics,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isDataSharingEnabled()
                )
            )

            val notificationSettings = listOf(
                SettingItem(
                    id = "ride_updates",
                    title = "Ride Updates",
                    subtitle = "Driver arrival, trip start/end",
                    icon = Icons.Default.DirectionsCar,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isRideNotificationsEnabled()
                ),
                SettingItem(
                    id = "promotions",
                    title = "Promotions & Offers",
                    subtitle = "Discounts and special deals",
                    icon = Icons.Default.LocalOffer,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isPromotionsEnabled()
                ),
                SettingItem(
                    id = "payment_updates",
                    title = "Payment Updates",
                    subtitle = "Transaction confirmations",
                    icon = Icons.Default.Payment,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isPaymentNotificationsEnabled()
                ),
                SettingItem(
                    id = "safety_alerts",
                    title = "Safety Alerts",
                    subtitle = "Emergency and security updates",
                    icon = Icons.Default.Shield,
                    type = SettingType.TOGGLE,
                    isEnabled = true // Always enabled
                )
            )

            val appSettings = listOf(
                SettingItem(
                    id = "dark_mode",
                    title = "Dark Mode",
                    subtitle = "Reduce eye strain",
                    icon = Icons.Default.DarkMode,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isDarkModeEnabled()
                ),
                SettingItem(
                    id = "language",
                    title = "Language",
                    subtitle = userPreferences.getSelectedLanguage(),
                    icon = Icons.Default.Language,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "auto_pay",
                    title = "Auto-Pay",
                    subtitle = "Automatically pay after rides",
                    icon = Icons.Default.CreditScore,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isAutoPayEnabled()
                ),
                SettingItem(
                    id = "data_saver",
                    title = "Data Saver Mode",
                    subtitle = "Reduce mobile data usage",
                    icon = Icons.Default.DataSaverOn,
                    type = SettingType.TOGGLE,
                    isEnabled = userPreferences.isDataSaverEnabled()
                ),
                SettingItem(
                    id = "clear_cache",
                    title = "Clear Cache",
                    subtitle = "Free up storage space",
                    icon = Icons.Default.CleaningServices,
                    type = SettingType.ACTION,
                    actionText = "Clear"
                )
            )

            val aboutSettings = listOf(
                SettingItem(
                    id = "help_support",
                    title = "Help & Support",
                    subtitle = "FAQs and contact support",
                    icon = Icons.Default.HelpOutline,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "terms_service",
                    title = "Terms of Service",
                    icon = Icons.Default.Article,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "privacy_policy",
                    title = "Privacy Policy",
                    icon = Icons.Default.PrivacyTip,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "licenses",
                    title = "Open Source Licenses",
                    icon = Icons.Default.Code,
                    type = SettingType.NAVIGATION
                ),
                SettingItem(
                    id = "app_version",
                    title = "App Version",
                    subtitle = "1.0.0 (Build 100)",
                    icon = Icons.Default.Info,
                    type = SettingType.ACTION,
                    actionText = "Check Updates"
                ),
                SettingItem(
                    id = "rate_app",
                    title = "Rate Daxido",
                    subtitle = "Help us improve",
                    icon = Icons.Default.StarRate,
                    type = SettingType.ACTION,
                    actionText = "Rate"
                ),
                SettingItem(
                    id = "logout",
                    title = "Logout",
                    icon = Icons.Default.Logout,
                    type = SettingType.ACTION,
                    actionText = "Sign Out"
                )
            )

            _uiState.update { currentState ->
                currentState.copy(
                    accountSettings = accountSettings,
                    privacySettings = privacySettings,
                    notificationSettings = notificationSettings,
                    appSettings = appSettings,
                    aboutSettings = aboutSettings,
                    isLoading = false
                )
            }
        }
    }

    fun toggleSetting(settingId: String) {
        viewModelScope.launch {
            when (settingId) {
                "location_sharing" -> {
                    val newValue = !userPreferences.isLocationSharingEnabled()
                    userPreferences.setLocationSharing(newValue)
                    updateSettingState(settingId, newValue)
                }
                "biometric_auth" -> {
                    val newValue = !userPreferences.isBiometricEnabled()
                    userPreferences.setBiometric(newValue)
                    updateSettingState(settingId, newValue)
                }
                "two_factor" -> {
                    val newValue = !userPreferences.isTwoFactorEnabled()
                    userPreferences.setTwoFactor(newValue)
                    updateSettingState(settingId, newValue)
                }
                "data_sharing" -> {
                    val newValue = !userPreferences.isDataSharingEnabled()
                    userPreferences.setDataSharing(newValue)
                    updateSettingState(settingId, newValue)
                }
                "ride_updates" -> {
                    val newValue = !userPreferences.isRideNotificationsEnabled()
                    userPreferences.setRideNotifications(newValue)
                    updateSettingState(settingId, newValue)
                }
                "promotions" -> {
                    val newValue = !userPreferences.isPromotionsEnabled()
                    userPreferences.setPromotions(newValue)
                    updateSettingState(settingId, newValue)
                }
                "payment_updates" -> {
                    val newValue = !userPreferences.isPaymentNotificationsEnabled()
                    userPreferences.setPaymentNotifications(newValue)
                    updateSettingState(settingId, newValue)
                }
                "dark_mode" -> {
                    val newValue = !userPreferences.isDarkModeEnabled()
                    userPreferences.setDarkMode(newValue)
                    updateSettingState(settingId, newValue)
                }
                "auto_pay" -> {
                    val newValue = !userPreferences.isAutoPayEnabled()
                    userPreferences.setAutoPay(newValue)
                    updateSettingState(settingId, newValue)
                }
                "data_saver" -> {
                    val newValue = !userPreferences.isDataSaverEnabled()
                    userPreferences.setDataSaver(newValue)
                    updateSettingState(settingId, newValue)
                }
            }
        }
    }

    private fun updateSettingState(settingId: String, newValue: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                accountSettings = updateSettingsList(currentState.accountSettings, settingId, newValue),
                privacySettings = updateSettingsList(currentState.privacySettings, settingId, newValue),
                notificationSettings = updateSettingsList(currentState.notificationSettings, settingId, newValue),
                appSettings = updateSettingsList(currentState.appSettings, settingId, newValue),
                aboutSettings = updateSettingsList(currentState.aboutSettings, settingId, newValue)
            )
        }
    }

    private fun updateSettingsList(
        settings: List<SettingItem>,
        settingId: String,
        newValue: Boolean
    ): List<SettingItem> {
        return settings.map { setting ->
            if (setting.id == settingId) {
                setting.copy(isEnabled = newValue)
            } else {
                setting
            }
        }
    }

    fun onSettingClick(settingId: String) {
        viewModelScope.launch {
            when (settingId) {
                "clear_cache" -> clearCache()
                "logout" -> logout()
                "rate_app" -> rateApp()
                "app_version" -> checkForUpdates()
                else -> {
                    // Navigation will be handled by the UI layer
                }
            }
        }
    }

    private suspend fun clearCache() {
        try {
            userRepository.clearCache()
            _uiState.update { it.copy(message = "Cache cleared successfully") }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "Failed to clear cache") }
        }
    }

    private suspend fun logout() {
        try {
            userRepository.logout()
            userPreferences.clearAll()
            _uiState.update { it.copy(message = "Logged out successfully") }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "Failed to logout") }
        }
    }

    private fun rateApp() {
        // Open Play Store for rating
        _uiState.update { it.copy(message = "Opening Play Store...") }
    }

    private suspend fun checkForUpdates() {
        try {
            val result = userRepository.checkForAppUpdate()
            result.fold(
                onSuccess = { hasUpdate ->
                    if (hasUpdate) {
                        _uiState.update { it.copy(message = "New update available!") }
                    } else {
                        _uiState.update { it.copy(message = "App is up to date") }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(message = "Failed to check for updates") }
                }
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "Failed to check for updates") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class SettingsUiState(
    val accountSettings: List<SettingItem> = emptyList(),
    val privacySettings: List<SettingItem> = emptyList(),
    val notificationSettings: List<SettingItem> = emptyList(),
    val appSettings: List<SettingItem> = emptyList(),
    val aboutSettings: List<SettingItem> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)