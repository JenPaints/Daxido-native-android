package com.daxido

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.daxido.core.crashlytics.CrashlyticsManager
import com.daxido.core.navigation.DaxidoNavHost
import com.daxido.core.theme.DaxidoTheme
import com.daxido.core.auth.GoogleSignInService
import com.daxido.core.auth.GoogleSignInResult
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var crashlyticsManager: CrashlyticsManager
    
    @Inject
    lateinit var googleSignInService: GoogleSignInService
    
    // Google Sign-In result launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val googleSignInResult = googleSignInService.handleSignInResult(task)
        
        // Handle the result - this will be passed to the AuthViewModel
        handleGoogleSignInResult(googleSignInResult)
    }
    
    private fun handleGoogleSignInResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> {
                android.util.Log.d("MainActivity", "Google Sign-In successful: ${result.account.email}")
                // The AuthViewModel will handle the Firebase authentication
            }
            is GoogleSignInResult.Failure -> {
                android.util.Log.e("MainActivity", "Google Sign-In failed: ${result.error}")
                // The AuthViewModel will handle the error display
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            installSplashScreen()
            enableEdgeToEdge()

            // Initialize Crashlytics logging with error handling
            try {
                crashlyticsManager.log("MainActivity onCreate - App started")
                crashlyticsManager.setCustomKey("app_version", "1.0.0")
                crashlyticsManager.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Crashlytics initialization failed", e)
            }

            setContent {
                DaxidoTheme {
                    val systemUiController = rememberSystemUiController()
                    val primaryColor = MaterialTheme.colorScheme.primary

                    LaunchedEffect(Unit) {
                        try {
                            systemUiController.setSystemBarsColor(
                                color = primaryColor,
                                darkIcons = false
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "System bar color setting failed", e)
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        DaxidoApp(
                            onGoogleSignIn = {
                                googleSignInLauncher.launch(googleSignInService.getSignInIntent())
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Fatal error in onCreate", e)
            // Attempt to log to Crashlytics before crashing
            try {
                crashlyticsManager.logError(e, "Fatal error in MainActivity onCreate")
            } catch (crashlyticError: Exception) {
                android.util.Log.e("MainActivity", "Failed to log crash", crashlyticError)
            }
            throw e
        }
    }
}

@Composable
fun DaxidoApp(onGoogleSignIn: () -> Unit) {
    val navController = rememberNavController()
    
    DaxidoNavHost(
        navController = navController,
        onGoogleSignIn = onGoogleSignIn
    )
}