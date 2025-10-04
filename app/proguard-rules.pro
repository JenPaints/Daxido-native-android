# Add project specific ProGuard rules here.

# Razorpay
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-keep class proguard.annotation.Keep
-keep class proguard.annotation.KeepClassMembers
-keep @proguard.annotation.Keep class * { *; }
-keepclassmembers class * {
    @proguard.annotation.Keep *;
}

# Stripe
-keep class com.stripe.** { *; }
-dontwarn com.stripe.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# AWS SDK
-keep class com.amazonaws.** { *; }
-keep class com.amazon.** { *; }
-dontwarn com.amazonaws.**
-dontwarn com.amazon.**

# Daxido Models
-keep class com.daxido.core.models.** { *; }
-keep class com.daxido.data.remote.dto.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Google Play Services Location
-keep class com.google.android.gms.location.** { *; }
-keep interface com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# Daxido Location Services
-keep class com.daxido.core.location.** { *; }
-keep class com.daxido.core.services.** { *; }
-keep class com.daxido.core.navigation.** { *; }

# Keep all enums
-keepclassmembers enum * { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Keep data classes
-keep class com.daxido.core.location.LocationData { *; }
-keep class com.daxido.core.location.LocationAccuracy { *; }

# Keep callback methods
-keepclassmembers class * extends com.google.android.gms.location.LocationCallback {
    public void onLocationResult(com.google.android.gms.location.LocationResult);
}

# Keep AndroidX and Lifecycle
-keep class androidx.lifecycle.** { *; }
-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.ViewModel { *; }

# Keep Hilt/Dagger generated classes
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltModules** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep all @Inject constructors
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
}

# Keep all @Inject fields
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}

# Keep all @Inject methods
-keepclasseswithmembers class * {
    @javax.inject.Inject <methods>;
}

# Keep @Singleton classes
-keep @javax.inject.Singleton class *

# Keep all ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# OkHttp Platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Location and Maps related interfaces
-keep interface com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.location.** { *; }

# Keep Notification classes
-keep class android.app.Notification { *; }
-keep class android.app.NotificationChannel { *; }
-keep class android.app.NotificationManager { *; }

# Keep Services
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# SECURITY FIX: Additional ProGuard rules for enhanced security and reliability

# Keep all Repositories
-keep class com.daxido.data.repository.** { *; }
-keep class com.daxido.core.data.repository.** { *; }

# Keep all ViewModels to prevent reflection issues
-keep class com.daxido.user.presentation.**.*ViewModel { *; }
-keep class com.daxido.driver.presentation.**.*ViewModel { *; }

# Keep all payment related classes (critical for transactions)
-keep class com.daxido.core.payment.** { *; }
-keep class com.daxido.user.presentation.payment.** { *; }

# Keep all tracking and location classes (critical for core functionality)
-keep class com.daxido.core.tracking.** { *; }
-keep class com.daxido.core.maps.** { *; }

# Keep all error handling classes
-keep class com.daxido.core.error.** { *; }

# Keep all safety and security classes
-keep class com.daxido.core.safety.** { *; }
-keep class com.daxido.core.verification.** { *; }

# Keep all algorithms (driver allocation, routing, etc.)
-keep class com.daxido.core.algorithms.** { *; }

# Keep all data classes with @Serializable annotation
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep encryption related classes
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Keep networking classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep all custom exceptions for better crash reporting
-keep class com.daxido.**.*Exception { *; }
-keep class com.daxido.**.*Error { *; }

# Optimize and shrink but preserve critical functionality
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile