# Daxido - Production Setup Guide

## 🚀 Complete Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Node.js 14+ and npm
- Firebase CLI (`npm install -g firebase-tools`)
- Google Cloud Platform account
- Firebase project created

### 1. Firebase Configuration

#### A. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project named "daxido-production"
3. Enable Google Analytics

#### B. Download Configuration Files
1. Add Android app with package name: `com.daxido`
2. Download `google-services.json`
3. Place it in `app/` directory

#### C. Enable Firebase Services
Enable the following in Firebase Console:
- ✅ Authentication (Email/Password, Phone, Google)
- ✅ Cloud Firestore
- ✅ Realtime Database
- ✅ Cloud Functions
- ✅ Cloud Messaging
- ✅ Cloud Storage

### 2. Google Maps Setup

#### A. Enable APIs
In [Google Cloud Console](https://console.cloud.google.com):
1. Enable Maps SDK for Android
2. Enable Directions API
3. Enable Places API
4. Enable Roads API
5. Enable Distance Matrix API
6. Enable Geocoding API

#### B. Get API Key
1. Create API key in Credentials section
2. Add Android app restriction with SHA-1 fingerprint
3. Restrict APIs to the ones enabled above

#### C. Update Configuration
Replace placeholders in `app/src/main/java/com/daxido/core/config/AppConfig.kt`:
```kotlin
const val GOOGLE_MAPS_API_KEY = "YOUR_ACTUAL_API_KEY_HERE"
```

### 3. Payment Gateway Setup

#### Razorpay (India)
1. Create account at [Razorpay Dashboard](https://dashboard.razorpay.com)
2. Get API keys from Settings → API Keys
3. Update in `AppConfig.kt`:
```kotlin
const val RAZORPAY_KEY_ID = "YOUR_RAZORPAY_KEY_ID"
```

#### Stripe (International)
1. Create account at [Stripe Dashboard](https://dashboard.stripe.com)
2. Get API keys from Developers → API Keys
3. Update in `AppConfig.kt`:
```kotlin
const val STRIPE_PUBLISHABLE_KEY = "YOUR_STRIPE_PUBLISHABLE_KEY"
```

### 4. Cloud Functions Deployment

#### A. Configure Environment Variables
```bash
firebase functions:config:set \
  google.maps_key="YOUR_GOOGLE_MAPS_API_KEY" \
  stripe.secret_key="YOUR_STRIPE_SECRET_KEY" \
  razorpay.secret_key="YOUR_RAZORPAY_SECRET_KEY"
```

#### B. Deploy Functions
```bash
cd functions
npm install
firebase deploy --only functions
```

### 5. Database Security Rules

#### Firestore Rules
Update `firestore.rules`:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // Rides access
    match /rides/{rideId} {
      allow read: if request.auth != null &&
        (request.auth.uid == resource.data.userId ||
         request.auth.uid == resource.data.driverId);
      allow create: if request.auth != null;
      allow update: if request.auth != null &&
        (request.auth.uid == resource.data.userId ||
         request.auth.uid == resource.data.driverId);
    }

    // Drivers collection
    match /drivers/{driverId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == driverId;
    }
  }
}
```

#### Realtime Database Rules
Update `database.rules.json`:
```json
{
  "rules": {
    "active_rides": {
      "$rideId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "driver_locations": {
      "$driverId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $driverId"
      }
    },
    "ride_acceptances": {
      "$rideId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

### 6. Build Configuration

#### A. Update build.gradle
Ensure all API keys are properly configured:
```gradle
android {
    defaultConfig {
        manifestPlaceholders = [
            GOOGLE_MAPS_API_KEY: "YOUR_API_KEY"
        ]
    }
}
```

#### B. ProGuard Rules
Add to `proguard-rules.pro`:
```
-keep class com.daxido.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.stripe.** { *; }
-keep class com.razorpay.** { *; }
```

### 7. Testing Checklist

Before going to production, test:

#### Core Features
- [ ] User registration and login
- [ ] Driver registration and verification
- [ ] Ride booking flow
- [ ] Driver allocation (ML-based)
- [ ] Real-time tracking
- [ ] Payment processing
- [ ] SOS functionality
- [ ] Notifications (FCM)

#### Edge Cases
- [ ] Network failure handling
- [ ] Location permission denial
- [ ] Payment failures
- [ ] Driver cancellation
- [ ] User cancellation
- [ ] App backgrounding during ride

### 8. Production Deployment

#### A. Generate Signed APK
1. Build → Generate Signed Bundle/APK
2. Create keystore (keep it safe!)
3. Build release APK/AAB

#### B. Play Store Submission
1. Create app in [Google Play Console](https://play.google.com/console)
2. Upload AAB file
3. Fill app details
4. Set up pricing (free)
5. Submit for review

### 9. Monitoring & Analytics

#### A. Firebase Crashlytics
```kotlin
// Already integrated, just ensure it's enabled
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
```

#### B. Performance Monitoring
```kotlin
// Track critical user journeys
val trace = Firebase.performance.newTrace("ride_booking")
trace.start()
// ... ride booking logic
trace.stop()
```

#### C. Analytics Events
```kotlin
// Track important events
Firebase.analytics.logEvent("ride_completed") {
    param("vehicle_type", vehicleType.name)
    param("distance", distance)
    param("fare", fare)
}
```

### 10. Post-Launch Tasks

1. **Monitor Cloud Functions** - Check execution logs
2. **Database Usage** - Monitor read/write quotas
3. **API Usage** - Check Google Maps API quotas
4. **User Feedback** - Implement in-app feedback
5. **Performance** - Monitor app performance metrics

### Environment Variables Template

Create `.env` file (never commit!):
```bash
GOOGLE_MAPS_API_KEY=AIza...
FIREBASE_WEB_API_KEY=AIza...
RAZORPAY_KEY_ID=rzp_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
FIREBASE_PROJECT_ID=daxido-production
FIREBASE_DATABASE_URL=https://daxido-production.firebaseio.com
```

### Support & Troubleshooting

Common issues and solutions:

1. **API Key Issues**
   - Ensure keys are not restricted incorrectly
   - Add SHA-1 fingerprint for debug and release

2. **Firebase Connection**
   - Check google-services.json is latest
   - Verify Firebase project settings

3. **Maps Not Loading**
   - Verify Maps API is enabled
   - Check API key restrictions

4. **Push Notifications**
   - Ensure FCM is properly configured
   - Check device token registration

### 📞 Need Help?

- Documentation: [Firebase Docs](https://firebase.google.com/docs)
- Maps: [Google Maps Platform](https://developers.google.com/maps)
- Community: Create issues on GitHub

---

**Ready for Production! 🚀**

Once all steps are completed, your Daxido app will be fully functional with:
- ✅ Real-time ride tracking
- ✅ ML-based driver allocation
- ✅ Precision location services
- ✅ Secure payment processing
- ✅ Emergency SOS system
- ✅ Push notifications
- ✅ Cloud Functions backend