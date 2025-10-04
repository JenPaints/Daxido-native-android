package com.daxido.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.daxido.data.repository.AuthRepository
import com.daxido.data.repository.RideRepository
import com.daxido.data.repository.LocationRepository
import com.daxido.data.repository.PaymentRepository
import com.daxido.data.repository.NotificationRepository
import com.daxido.core.data.repository.UserRepository
import com.daxido.core.data.preferences.UserPreferences
import com.daxido.core.location.LocationService
import com.daxido.core.auth.GoogleSignInService
import com.daxido.core.multistop.MultiStopRidesService
import com.daxido.core.corporate.CorporateAccountsService
import com.daxido.core.ridepooling.RidePoolingService
import com.daxido.core.scheduling.ScheduledRidesService
import com.daxido.core.ai.FirebaseAiLogicService
import com.daxido.core.optimization.ImageOptimizer
import com.daxido.core.optimization.CacheManager
import com.daxido.core.optimization.FirestoreOptimizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions
import com.daxido.core.data.repository.SupportRepository
import com.daxido.core.error.ErrorHandler
import com.daxido.core.crashlytics.CrashlyticsManager
import com.daxido.core.payment.RazorpayPaymentService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daxido_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        imageOptimizer: ImageOptimizer
    ): AuthRepository = AuthRepository(auth, firestore, imageOptimizer)

    @Provides
    @Singleton
    fun provideRideRepository(
        firestore: FirebaseFirestore,
        realtimeDb: FirebaseDatabase,
        auth: FirebaseAuth,
        firestoreOptimizer: com.daxido.core.optimization.FirestoreOptimizer,
        memoryCache: com.daxido.core.optimization.MemoryCache
    ): RideRepository = RideRepository(firestore, realtimeDb, auth, firestoreOptimizer, memoryCache)

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): LocationService = LocationService(context, firestore, auth)

    @Provides
    @Singleton
    fun provideLocationRepository(locationService: LocationService): LocationRepository = 
        LocationRepository(locationService)

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()

    @Provides
    @Singleton
    fun provideRazorpayPaymentService(@ApplicationContext context: Context): RazorpayPaymentService = RazorpayPaymentService(context)

    @Provides
    @Singleton
    fun providePaymentRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        functions: FirebaseFunctions,
        razorpayService: RazorpayPaymentService
    ): PaymentRepository = PaymentRepository(firestore, auth, functions, razorpayService)

    @Provides
    @Singleton
    fun provideSupportRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): SupportRepository = SupportRepository(firestore, auth)

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        cacheManager: CacheManager,
        firestoreOptimizer: FirestoreOptimizer
    ): UserRepository = UserRepository(firestore, auth, cacheManager, firestoreOptimizer)

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences = 
        UserPreferences(context)

    @Provides
    @Singleton
    fun provideErrorHandler(): ErrorHandler = ErrorHandler()

    @Provides
    @Singleton
    fun provideCrashlyticsManager(): CrashlyticsManager = CrashlyticsManager()

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): NotificationRepository = NotificationRepository(firestore, auth)

    @Provides
    @Singleton
    fun provideMultiStopRidesService(@ApplicationContext context: Context): MultiStopRidesService = 
        MultiStopRidesService(context)

    @Provides
    @Singleton
    fun provideCorporateAccountsService(@ApplicationContext context: Context): CorporateAccountsService = 
        CorporateAccountsService(context)

    @Provides
    @Singleton
    fun provideRidePoolingService(@ApplicationContext context: Context): RidePoolingService = 
        RidePoolingService(context)

    @Provides
    @Singleton
    fun provideScheduledRidesService(@ApplicationContext context: Context): ScheduledRidesService =
        ScheduledRidesService(context)

    @Provides
    @Singleton
    fun provideGoogleSignInService(@ApplicationContext context: Context): GoogleSignInService =
        GoogleSignInService(context)

    /**
     * NETWORK FIX: Provides OkHttpClient with proper timeout configuration
     * SECURITY FIX: Added certificate pinning to prevent MITM attacks
     * Prevents app from hanging on slow/unreliable networks
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.daxido.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // CERTIFICATE PINNING: Pin SSL certificates for production API
        // To get certificate pins for your domain, run:
        // openssl s_client -connect yourdomain.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
        val certificatePinner = CertificatePinner.Builder()
            // Firebase domains - Using Google's official certificate pins
            // These are the actual pins for Firebase services
            .add("*.firebaseio.com", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
            .add("*.firebaseio.com", "sha256/f0KW/FtqTjs108NpYj42SrGvOB2PpxIVM8nWxjPqJGE=")
            .add("*.googleapis.com", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
            .add("*.googleapis.com", "sha256/f0KW/FtqTjs108NpYj42SrGvOB2PpxIVM8nWxjPqJGE=")
            .add("*.google.com", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
            .add("*.google.com", "sha256/f0KW/FtqTjs108NpYj42SrGvOB2PpxIVM8nWxjPqJGE=")
            // Add your custom API domain pins here when deploying to production
            // .add("api.daxido.com", "sha256/YOUR_PRIMARY_PIN_HERE=")
            // .add("api.daxido.com", "sha256/YOUR_BACKUP_PIN_HERE=")
            .build()

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Time to establish connection
            .readTimeout(30, TimeUnit.SECONDS) // Time to read data from server
            .writeTimeout(30, TimeUnit.SECONDS) // Time to write data to server
            .callTimeout(60, TimeUnit.SECONDS) // Total time for entire call
            .retryOnConnectionFailure(true) // Retry on connection failures
            .addInterceptor(loggingInterceptor)
            // Only enable certificate pinning in release builds
            .apply {
                if (!com.daxido.BuildConfig.DEBUG) {
                    certificatePinner(certificatePinner)
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideFirebaseAiLogicService(): FirebaseAiLogicService =
        FirebaseAiLogicService()

    /**
     * Provides Retrofit instance for REST API calls
     * Uses the configured OkHttpClient with timeouts
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.daxido.com/") // Replace with actual base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Industry-standard services (Ola/Uber/Rapido features)

    @Provides
    @Singleton
    fun provideFareCalculationService(
        firestore: FirebaseFirestore
    ): com.daxido.core.services.FareCalculationService =
        com.daxido.core.services.FareCalculationService(firestore)

    @Provides
    @Singleton
    fun provideSafetyService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        functions: FirebaseFunctions,
        @ApplicationContext context: Context
    ): com.daxido.core.services.SafetyService =
        com.daxido.core.services.SafetyService(firestore, auth, functions, context)

    @Provides
    @Singleton
    fun provideReferralService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        functions: FirebaseFunctions
    ): com.daxido.core.services.ReferralService =
        com.daxido.core.services.ReferralService(firestore, auth, functions)

    @Provides
    @Singleton
    fun provideLocationHistoryService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): com.daxido.core.services.LocationHistoryService =
        com.daxido.core.services.LocationHistoryService(firestore, auth)

    @Provides
    @Singleton
    fun provideRatingService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        functions: FirebaseFunctions
    ): com.daxido.core.services.RatingService =
        com.daxido.core.services.RatingService(firestore, auth, functions)

    // Cost Optimization Services

    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context
    ): com.daxido.core.optimization.CacheManager =
        com.daxido.core.optimization.CacheManager(context, com.google.gson.Gson())

    @Provides
    @Singleton
    fun provideMemoryCache(): com.daxido.core.optimization.MemoryCache =
        com.daxido.core.optimization.MemoryCache()

    @Provides
    @Singleton
    fun provideFirestoreOptimizer(
        firestore: FirebaseFirestore
    ): com.daxido.core.optimization.FirestoreOptimizer =
        com.daxido.core.optimization.FirestoreOptimizer(firestore)

    @Provides
    @Singleton
    fun provideQueryCache(): com.daxido.core.optimization.QueryCache =
        com.daxido.core.optimization.QueryCache()

    @Provides
    @Singleton
    fun provideListenerManager(): com.daxido.core.optimization.ListenerManager =
        com.daxido.core.optimization.ListenerManager()

    @Provides
    @Singleton
    fun provideImageOptimizer(
        @ApplicationContext context: Context,
        storage: FirebaseStorage
    ): com.daxido.core.optimization.ImageOptimizer =
        com.daxido.core.optimization.ImageOptimizer(context, storage)

    @Provides
    @Singleton
    fun provideImageCacheManager(
        @ApplicationContext context: Context
    ): com.daxido.core.optimization.ImageCacheManager =
        com.daxido.core.optimization.ImageCacheManager(context)

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson = com.google.gson.Gson()

    // Admin Dashboard

    @Provides
    @Singleton
    fun provideAdminRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        functions: FirebaseFunctions,
        firestoreOptimizer: com.daxido.core.optimization.FirestoreOptimizer
    ): com.daxido.admin.data.AdminRepository =
        com.daxido.admin.data.AdminRepository(firestore, auth, functions, firestoreOptimizer)
}