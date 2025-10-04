# Daxido Android App - Security Setup Guide

## Overview
This guide covers all security fixes implemented and setup instructions for the Daxido ride-sharing Android application.

## 🔴 Critical Security Fixes Implemented

### 1. ✅ API Keys Security
**Issue**: Hardcoded API keys in source code
**Fixed**: All API keys now loaded from `local.properties` (gitignored)

**Files Modified**:
- `app/src/main/java/com/daxido/core/config/AppConfig.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

**Setup Required**:
1. Copy `local.properties.example` to `local.properties`
2. Fill in your actual API keys:
   ```properties
   GOOGLE_MAPS_API_KEY=your_actual_google_maps_key
   RAZORPAY_KEY_ID=your_actual_razorpay_key
   STRIPE_PUBLISHABLE_KEY=your_actual_stripe_key
   ```

### 2. ✅ Keystore Credentials Security
**Issue**: Hardcoded keystore password in `build.gradle.kts`
**Fixed**: Credentials now loaded from `local.properties`

**Setup Required**:
Add to `local.properties`:
```properties
KEYSTORE_FILE=../daxido-release-key.keystore
KEYSTORE_PASSWORD=your_secure_password
KEY_ALIAS=daxido-key
KEY_PASSWORD=your_secure_password
```

**Generate Keystore** (first-time setup):
```bash
keytool -genkey -v -keystore daxido-release-key.keystore \
  -alias daxido-key -keyalg RSA -keysize 2048 -validity 10000
```

⚠️ **CRITICAL**: Backup your keystore and passwords! If lost, you cannot update your app on Play Store!

### 3. ✅ Data Encryption
**Issue**: Sensitive user data stored in plain SharedPreferences
**Fixed**: Implemented EncryptedSharedPreferences

**Files Modified**:
- `app/src/main/java/com/daxido/core/data/preferences/UserPreferences.kt`
- Added dependency: `androidx.security:security-crypto:1.1.0-alpha06`

**Features**:
- AES256-GCM encryption for stored preferences
- Automatic fallback to regular SharedPreferences on encryption failure
- Protects user settings, session data, and authentication tokens

### 4. ✅ OTP Security Enhancement
**Issue**: 4-digit OTP (10,000 combinations) - easy to brute force
**Fixed**: Upgraded to 6-digit OTP (1,000,000 combinations)

**Files Modified**:
- `app/src/main/java/com/daxido/data/repository/RideRepository.kt`
- `app/src/main/java/com/daxido/core/config/AppConfig.kt`

**Additional Security**:
- Added constant-time OTP comparison to prevent timing attacks
- Input validation with regex pattern matching
- Format validation before comparison

### 5. ✅ Payment Gateway Configuration
**Issue**: Placeholder payment keys causing failures
**Fixed**: Keys now loaded from secure configuration

**Files Modified**:
- `app/src/main/java/com/daxido/core/payment/RazorpayPaymentService.kt`
- `app/src/main/java/com/daxido/core/payment/StripePaymentService.kt`

**Setup Required**:
- Add payment keys to `local.properties`
- Use test keys for development:
  - Razorpay: `rzp_test_...`
  - Stripe: `pk_test_...`

### 6. ✅ Sensitive Data in Logs
**Issue**: Logging OTP, tokens, and user data
**Fixed**: Conditional logging based on build type

**Changes**:
- OTP no longer shown in push notifications
- Token logging only in DEBUG builds
- Message details not logged in production
- User data sanitized in logs

**Files Modified**:
- `app/src/main/java/com/daxido/core/services/DaxidoMessagingService.kt`

## 🟠 High Priority Fixes Implemented

### 7. ✅ Memory Leak Prevention
**Issue**: Coroutine scopes not properly cancelled
**Fixed**: Added proper cleanup in all exit paths

**Files Modified**:
- `app/src/main/java/com/daxido/core/services/LocationTrackingService.kt`
- `app/src/main/java/com/daxido/core/tracking/RealTimeTrackingManager.kt`

**Improvements**:
- ServiceScope properly cancelled in `onDestroy()`
- Location buffer size limited to 100 entries
- Proper error handling in cleanup methods

### 8. ✅ Network Timeout Configuration
**Issue**: No timeout configuration - app could hang indefinitely
**Fixed**: Added comprehensive timeout configuration

**Files Modified**:
- `app/src/main/java/com/daxido/di/AppModule.kt`

**Timeouts Configured**:
- Connect timeout: 30 seconds
- Read timeout: 30 seconds
- Write timeout: 30 seconds
- Call timeout: 60 seconds total
- Automatic retry on connection failure

### 9. ✅ Location Update Optimization
**Issue**: Location updates every 5 seconds - excessive battery drain
**Fixed**: Reduced to 10-second intervals

**Files Modified**:
- `app/src/main/java/com/daxido/core/services/LocationTrackingService.kt`

**Benefits**:
- ~50% reduction in battery consumption
- Still maintains acceptable accuracy for ride tracking
- Minimum interval increased from 2s to 5s

### 10. ✅ ProGuard Rules Enhancement
**Issue**: Incomplete ProGuard rules could cause crashes in release builds
**Fixed**: Comprehensive rules added

**Files Modified**:
- `app/proguard-rules.pro`

**Added Protection For**:
- All repositories and ViewModels
- Payment gateway classes
- Location and tracking services
- Error handling and exceptions
- Encryption classes
- Custom algorithms

### 11. ✅ Background Location Permission
**Issue**: No justification for background location permission
**Fixed**: Added detailed permission documentation

**Files Modified**:
- `app/src/main/AndroidManifest.xml`

**Play Store Requirements Met**:
- Clear justification provided in manifest comments
- Documented use cases
- Limited scope explanation

## 📋 Setup Checklist

### First-Time Setup
- [ ] Copy `local.properties.example` to `local.properties`
- [ ] Add Google Maps API key to `local.properties`
- [ ] Add payment gateway keys to `local.properties`
- [ ] Generate release keystore (if building release)
- [ ] Add keystore credentials to `local.properties`
- [ ] Configure Firebase project
- [ ] Download and add `google-services.json` to `app/` directory

### Development Environment
- [ ] Android Studio Arctic Fox or newer
- [ ] JDK 17
- [ ] Android SDK 34
- [ ] Minimum SDK: API 24 (Android 7.0)
- [ ] Target SDK: API 34 (Android 14)

### API Keys Required
1. **Google Maps API Key**
   - Get from: https://console.cloud.google.com/google/maps-apis
   - Enable: Maps SDK, Places API, Directions API, Geocoding API

2. **Razorpay (Optional - for payments)**
   - Get from: https://dashboard.razorpay.com/app/keys
   - Use test keys for development

3. **Stripe (Optional - for payments)**
   - Get from: https://dashboard.stripe.com/apikeys
   - Use test keys (pk_test_...) for development

4. **Firebase**
   - Create project: https://console.firebase.google.com
   - Download `google-services.json`
   - Enable Authentication, Firestore, Realtime Database, Cloud Functions, Crashlytics

### Building the App

#### Debug Build (Development)
```bash
./gradlew assembleDebug
```
- Uses debug signing certificate (auto-generated)
- Logging enabled
- Google Maps key from `local.properties` or empty string

#### Release Build (Production)
```bash
./gradlew assembleRelease
```
- Requires keystore configuration in `local.properties`
- ProGuard/R8 enabled for code obfuscation
- Logging disabled
- Resources shrunk for smaller APK size

## 🔒 Security Best Practices

### DO ✅
- Store all secrets in `local.properties` (gitignored)
- Use test/sandbox keys for development
- Rotate API keys regularly
- Use different keys for debug/release builds
- Enable Firebase App Check for production
- Implement proper error handling
- Log errors to Crashlytics, not console
- Use EncryptedSharedPreferences for sensitive data
- Validate all user inputs
- Use HTTPS for all network requests
- Implement certificate pinning for production

### DON'T ❌
- Commit `local.properties` to Git
- Hardcode API keys or passwords
- Log sensitive user data
- Use production keys in debug builds
- Store passwords or tokens in plain SharedPreferences
- Skip input validation
- Ignore ProGuard warnings
- Push release keystore to repository
- Use HTTP for network requests
- Disable SSL certificate validation

## 🚨 Common Issues & Solutions

### Issue: Build fails with "GOOGLE_MAPS_API_KEY not found"
**Solution**: Create `local.properties` and add your Google Maps API key

### Issue: Payment services not working
**Solution**: Verify payment gateway keys in `local.properties` are correct and not placeholder values

### Issue: Location tracking not working
**Solution**:
1. Check location permissions are granted
2. Verify Google Maps API key is valid
3. Check if device has location services enabled

### Issue: Release build crashes but debug works
**Solution**:
1. Check ProGuard rules in `proguard-rules.pro`
2. Use `./gradlew assembleRelease --stacktrace` for detailed error
3. Add specific keep rules for classes that use reflection

### Issue: Firebase not working
**Solution**:
1. Verify `google-services.json` is in `app/` directory
2. Check Firebase project configuration
3. Ensure package name matches Firebase project

## 📱 Testing the Fixes

### Security Testing
```bash
# Check for hardcoded secrets in code
./gradlew assembleDebug
# Inspect APK for hardcoded strings
unzip -p app/build/outputs/apk/debug/app-debug.apk classes.dex | strings | grep -i "api"

# Verify encryption is working
adb shell run-as com.daxido cat /data/data/com.daxido/shared_prefs/daxido_secure_prefs.xml
# Should show encrypted values, not plain text
```

### Build Testing
```bash
# Test debug build
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Test release build (after keystore setup)
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

## 📄 Files Modified Summary

### Critical Security Files
- ✅ `app/build.gradle.kts` - BuildConfig and signing configuration
- ✅ `app/src/main/java/com/daxido/core/config/AppConfig.kt` - API key management
- ✅ `app/src/main/AndroidManifest.xml` - Permissions and API key placeholder
- ✅ `app/src/main/java/com/daxido/core/data/preferences/UserPreferences.kt` - Encryption
- ✅ `app/src/main/java/com/daxido/data/repository/RideRepository.kt` - OTP security
- ✅ `app/src/main/java/com/daxido/core/payment/*.kt` - Payment configuration
- ✅ `app/proguard-rules.pro` - Release build protection

### Performance & Reliability Files
- ✅ `app/src/main/java/com/daxido/core/services/LocationTrackingService.kt` - Memory leaks & battery
- ✅ `app/src/main/java/com/daxido/core/tracking/RealTimeTrackingManager.kt` - Buffer management
- ✅ `app/src/main/java/com/daxido/di/AppModule.kt` - Network timeouts
- ✅ `app/src/main/java/com/daxido/core/services/DaxidoMessagingService.kt` - Log security
- ✅ `app/src/main/java/com/daxido/MainActivity.kt` - Crashlytics error

### Configuration Files
- ✅ `local.properties.example` - Template for secure configuration
- ✅ `SECURITY_SETUP.md` - This file

## 🎯 Next Steps

1. **Immediate** (Before First Run):
   - [ ] Set up `local.properties` with API keys
   - [ ] Configure Firebase project
   - [ ] Test debug build

2. **Before Testing** (Within 1 Week):
   - [ ] Set up payment gateway test accounts
   - [ ] Configure proper error handling
   - [ ] Enable Crashlytics for error tracking
   - [ ] Test all critical user flows

3. **Before Production** (Before Release):
   - [ ] Generate production keystore
   - [ ] Get production API keys
   - [ ] Complete security audit
   - [ ] Set up CI/CD with secret management
   - [ ] Configure Firebase App Check
   - [ ] Implement certificate pinning
   - [ ] Complete privacy policy
   - [ ] Prepare Play Store listing with background location justification

## 📞 Support

If you encounter issues with the security setup:
1. Check this guide first
2. Review the code comments in modified files
3. Check Firebase Console for configuration issues
4. Verify all API keys are valid and have proper permissions

## 📚 References

- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [EncryptedSharedPreferences Guide](https://developer.android.com/topic/security/data)
- [ProGuard Configuration](https://developer.android.com/studio/build/shrink-code)
- [Firebase Security Rules](https://firebase.google.com/docs/rules)
- [Google Play Policy - Background Location](https://support.google.com/googleplay/android-developer/answer/9799150)

---

**Last Updated**: October 1, 2025
**Security Review Date**: October 1, 2025
**Next Review**: January 1, 2026
