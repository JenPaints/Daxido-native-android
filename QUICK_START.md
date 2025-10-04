# Daxido Android - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### 1. Setup Configuration (2 minutes)
```bash
# Copy the template
cp local.properties.example local.properties

# Edit with your keys
nano local.properties
```

Add these **required** keys:
```properties
GOOGLE_MAPS_API_KEY=your_google_maps_api_key
```

Add these **optional** keys (for payments):
```properties
RAZORPAY_KEY_ID=your_razorpay_key
STRIPE_PUBLISHABLE_KEY=your_stripe_key
```

### 2. Build & Run (3 minutes)
```bash
# Build debug version
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Or run directly
./gradlew installDebug
```

## 🔑 Where to Get API Keys

### Google Maps (Required)
1. Go to: https://console.cloud.google.com/google/maps-apis
2. Create/Select project
3. Enable APIs: Maps SDK, Places, Directions, Geocoding
4. Create credentials → API Key
5. Copy key to local.properties

### Razorpay (Optional - for payments)
1. Go to: https://dashboard.razorpay.com/app/keys
2. Sign up/Login
3. Use **Test Keys** for development
4. Copy Key ID to local.properties

### Stripe (Optional - for payments)
1. Go to: https://dashboard.stripe.com/apikeys
2. Sign up/Login
3. Use **Test Keys** (starts with pk_test_)
4. Copy to local.properties

## 📱 System Requirements

- **Android Studio**: Arctic Fox or newer
- **JDK**: Version 17
- **Android Device/Emulator**: API 24+ (Android 7.0+)
- **Internet**: Required for API keys and dependencies

## ❓ Common Issues

### "GOOGLE_MAPS_API_KEY not found"
**Fix**: Create local.properties and add your Google Maps key

### "Permission denied" when installing
**Fix**: Enable USB debugging on your Android device

### Build fails with Gradle errors
**Fix**:
```bash
./gradlew clean
./gradlew assembleDebug --stacktrace
```

### App crashes on start
**Fix**: Make sure google-services.json is in app/ directory

## 📚 Full Documentation

- **Security Setup**: SECURITY_SETUP.md
- **All Fixes**: FIXES_SUMMARY.md
- **Config Template**: local.properties.example

## ✅ Verification

After build, verify:
- Build completes without errors
- APK installs on device
- App launches successfully
- Maps display correctly (if Google Maps key configured)

## 🎯 Next Steps

1. Build and run debug version
2. Test core functionality
3. Configure Firebase (see SECURITY_SETUP.md)
4. Set up payment gateways (if needed)
5. Start development!

---

**Need Help?** See SECURITY_SETUP.md for detailed instructions
