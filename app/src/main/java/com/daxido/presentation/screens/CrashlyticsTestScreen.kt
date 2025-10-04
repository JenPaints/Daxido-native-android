package com.daxido.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.crashlytics.CrashlyticsManager
import javax.inject.Inject

/**
 * Simple test screen for Firebase Crashlytics
 * This screen provides buttons to test crash reporting functionality
 * 
 * WARNING: This screen is for testing purposes only!
 * Remove or disable in production builds.
 */
@Composable
fun CrashlyticsTestScreen(
    crashlyticsManager: CrashlyticsManager,
    modifier: Modifier = Modifier
) {
    var showWarning by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Firebase Crashlytics Test",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "This screen is for testing Firebase Crashlytics integration. Use these buttons to test crash reporting functionality.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (showWarning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ WARNING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "The 'Force Test Crash' button will actually crash the app! This is intentional for testing Crashlytics integration. Make sure you have saved your work before proceeding.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Button(
            onClick = { showWarning = !showWarning },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (showWarning) "Hide Warning" else "Show Crash Warning",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        if (showWarning) {
            Button(
                onClick = { 
                    crashlyticsManager.log("User initiated test crash from test screen")
                    crashlyticsManager.setCustomKey("test_crash_source", "test_screen")
                    crashlyticsManager.forceTestCrash()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "🚨 FORCE TEST CRASH",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
        
        // Test non-fatal error logging
        Button(
            onClick = {
                try {
                    // Simulate an error condition
                    val result = 10 / 0
                } catch (e: ArithmeticException) {
                    crashlyticsManager.logError(e, "Test non-fatal error: Division by zero from test screen")
                    crashlyticsManager.setCustomKey("error_type", "arithmetic_exception")
                    crashlyticsManager.setCustomKey("error_source", "test_screen")
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "Test Non-Fatal Error",
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
        
        // Test custom logging
        Button(
            onClick = {
                crashlyticsManager.log("Test custom log message from test screen")
                crashlyticsManager.setCustomKey("test_key", "test_value")
                crashlyticsManager.setCustomKey("test_number", 42)
                crashlyticsManager.setCustomKey("test_boolean", true)
                crashlyticsManager.setCustomKey("test_source", "test_screen")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(
                text = "Test Custom Logging",
                color = MaterialTheme.colorScheme.onTertiary
            )
        }
        
        // Test ride event logging
        Button(
            onClick = {
                crashlyticsManager.logRideEvent(
                    event = "Test Ride Started",
                    rideId = "test_ride_123",
                    additionalData = mapOf(
                        "pickup_location" to "Test Location",
                        "destination" to "Test Destination",
                        "fare" to 25.50,
                        "test_source" to "test_screen"
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Test Ride Event Logging",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        // Test driver event logging
        Button(
            onClick = {
                crashlyticsManager.logDriverEvent(
                    event = "Test Driver Online",
                    driverId = "test_driver_456",
                    additionalData = mapOf(
                        "vehicle_type" to "car",
                        "rating" to 4.8,
                        "test_source" to "test_screen"
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "Test Driver Event Logging",
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
        
        // Test payment event logging
        Button(
            onClick = {
                crashlyticsManager.logPaymentEvent(
                    event = "Test Payment Processed",
                    paymentId = "test_payment_789",
                    amount = 25.50,
                    additionalData = mapOf(
                        "payment_method" to "credit_card",
                        "currency" to "USD",
                        "test_source" to "test_screen"
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(
                text = "Test Payment Event Logging",
                color = MaterialTheme.colorScheme.onTertiary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "After testing, check the Firebase Console > Crashlytics dashboard to see your crash reports and logs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Text(
            text = "Note: Crash reports may take a few minutes to appear in the Firebase Console.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
