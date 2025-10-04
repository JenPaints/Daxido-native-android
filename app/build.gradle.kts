import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("kotlinx-serialization")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.daxido"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.daxido"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // SECURITY: Load API keys from local.properties (not committed to version control)
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        
        val mapsApiKey = localProperties.getProperty("GOOGLE_MAPS_API_KEY") ?: ""
        val razorpayKeyId = localProperties.getProperty("RAZORPAY_KEY_ID") ?: ""
        val googleOauthClientId = localProperties.getProperty("GOOGLE_OAUTH_CLIENT_ID") ?: ""
        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"$razorpayKeyId\"")
        buildConfigField("String", "GOOGLE_OAUTH_CLIENT_ID", "\"$googleOauthClientId\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    signingConfigs {
        create("release") {
            // SECURITY FIX: Load signing config from local.properties instead of hardcoding
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }
            
            val keystoreFile = localProperties.getProperty("KEYSTORE_FILE") ?: "daxido-debug-key.keystore"
            val keystorePassword = localProperties.getProperty("KEYSTORE_PASSWORD") ?: "daxido123"
            val keyAliasValue = localProperties.getProperty("KEY_ALIAS") ?: "daxido-debug"
            val keyPasswordValue = localProperties.getProperty("KEY_PASSWORD") ?: "daxido123"

            storeFile = file(keystoreFile)
            storePassword = keystorePassword
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            versionNameSuffix = "-debug"
            buildConfigField("String", "BASE_URL", "\"https://daxido-dev.firebaseapp.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            // Use default debug signing for Firebase compatibility
            signingConfig = signingConfigs.getByName("debug")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "BASE_URL", "\"https://daxido-prod.firebaseapp.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
        
        create("staging") {
            initWith(getByName("release"))
            versionNameSuffix = "-staging"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("String", "BASE_URL", "\"https://daxido-staging.firebaseapp.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val compose_version = "1.6.8"

    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:$compose_version")
    implementation("androidx.compose.animation:animation:$compose_version")
    implementation("androidx.compose.runtime:runtime-livedata:$compose_version")
    implementation("androidx.compose.foundation:foundation:$compose_version")
    implementation("androidx.compose.foundation:foundation-layout:$compose_version")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Material Design 3
    implementation("com.google.android.material:material:1.12.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    
    // Google AI SDK (Gemini API)
    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // Google Play Services Tasks
    implementation("com.google.android.gms:play-services-tasks:18.2.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

    // AWS SDK
    implementation("com.amazonaws:aws-android-sdk-core:2.75.2")
    implementation("com.amazonaws:aws-android-sdk-s3:2.75.2")
    implementation("com.amazonaws:aws-android-sdk-lambda:2.75.2")

    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Lottie Animations
    implementation("com.airbnb.android:lottie-compose:6.4.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Room Database - Offline Mode
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // Payment SDK - Razorpay only
    implementation("com.razorpay:checkout:1.6.38")

    // Phone Auth & Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.1.0")
    implementation("com.google.android.gms:play-services-identity:18.0.1")

    // ML Kit
    implementation("com.google.mlkit:face-detection:16.1.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
}