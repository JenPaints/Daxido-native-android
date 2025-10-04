package com.daxido.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daxido.core.crashlytics.CrashlyticsManager
import javax.inject.Inject

/**
 * Test component for Firebase Crashlytics
 * This component provides buttons to test crash reporting functionality
 * 
 * WARNING: This component is for testing purposes only!
 * Remove or disable in production builds.
 */
@Composable
fun CrashlyticsTestComponent(
    crashlyticsManager: CrashlyticsManager,
    modifier: Modifier = Modifier
) {
    var showWarning by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Crashlytics Test Panel",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "This will crash the app! Only use for testing Crashlytics integration.",
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
                    crashlyticsManager.log("User initiated test crash")
                    crashlyticsManager.forceTestCrash()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "🚨 FORCE TEST CRASH",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold
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
                    crashlyticsManager.logError(e, "Test non-fatal error: Division by zero")
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
                crashlyticsManager.log("Test custom log message")
                crashlyticsManager.setCustomKey("test_key", "test_value")
                crashlyticsManager.setCustomKey("test_number", 42)
                crashlyticsManager.setCustomKey("test_boolean", true)
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
                        "fare" to 25.50
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
        
        Text(
            text = "Check Firebase Console for crash reports",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
