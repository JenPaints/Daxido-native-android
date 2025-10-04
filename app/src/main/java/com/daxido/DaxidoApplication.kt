package com.daxido

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DaxidoApplication : Application() {

    companion object {
        private const val TAG = "DaxidoApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // Set up global exception handler
        setupGlobalExceptionHandler()

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")

            // Enable Crashlytics collection in release builds
            if (BuildConfig.DEBUG) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
                Log.d(TAG, "Crashlytics disabled in debug mode")
            } else {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                Log.d(TAG, "Crashlytics enabled in release mode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
            // Don't crash the app, log and continue
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread: ${thread.name}", throwable)

            try {
                // Log to Crashlytics if available
                FirebaseCrashlytics.getInstance().apply {
                    recordException(throwable)
                    setCustomKey("thread_name", thread.name)
                    setCustomKey("app_version", BuildConfig.VERSION_NAME)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log to Crashlytics", e)
            }

            // Call the default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}