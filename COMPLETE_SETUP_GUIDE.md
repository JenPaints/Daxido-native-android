# 🚗 Daxido Ride-Hailing App

A complete, production-ready ride-hailing application built with Android (Kotlin) and Firebase backend. Features real-time driver allocation, live tracking, payment processing, and comprehensive safety features.

## ✨ Features

### 🚕 Core Functionality
- **Real-time Driver Allocation**: Intelligent ML-based driver matching with geohash optimization
- **Live Ride Tracking**: Real-time location updates using Firebase Realtime Database
- **Dynamic Pricing**: Surge pricing based on demand and supply
- **Multiple Vehicle Types**: Support for different vehicle categories (Economy, Premium, XL)
- **Route Optimization**: Google Maps integration with traffic-aware routing

### 💳 Payment & Billing
- **Multiple Payment Methods**: Credit cards, digital wallets, UPI
- **Secure Payment Processing**: Integration with Razorpay and Stripe
- **Fare Calculation**: Dynamic pricing with base fare, distance, time, and surge multipliers
- **Payment History**: Complete transaction tracking and receipts

### 🛡️ Safety & Security
- **Emergency Features**: One-tap emergency alerts with location sharing
- **Driver Verification**: Comprehensive background checks and document verification
- **Real-time Monitoring**: Live tracking with safety checkpoints
- **Emergency Contacts**: Automatic notification to trusted contacts
- **Incident Reporting**: Built-in reporting system for safety issues

### 📱 User Experience
- **Intuitive UI**: Modern Material Design with dark/light themes
- **Offline Support**: Core functionality works without internet
- **Push Notifications**: Real-time updates for ride status
- **Multi-language Support**: Localization for global markets
- **Accessibility**: Full accessibility support for all users

## 🏗️ Architecture

### Frontend (Android)
- **Language**: Kotlin
- **Architecture**: MVVM with Clean Architecture
- **UI Framework**: Jetpack Compose
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Compose
- **State Management**: StateFlow and Flow

### Backend (Firebase)
- **Database**: Firestore + Realtime Database
- **Authentication**: Firebase Auth
- **Cloud Functions**: Node.js with Firebase Functions v2
- **Storage**: Firebase Storage for images
- **Messaging**: Firebase Cloud Messaging
- **Analytics**: Firebase Analytics

### Key Components
```
📱 Android App
├── 🎨 UI Layer (Compose)
├── 🧠 ViewModels (MVVM)
├── 📊 Repositories (Data Layer)
├── 🔧 Services (Location, Payment, etc.)
└── 🌐 Network Layer (Firebase)

☁️ Firebase Backend
├── 🔥 Firestore (User data, rides, payments)
├── ⚡ Realtime Database (Live tracking)
├── 🔧 Cloud Functions (Business logic)
├── 🔐 Authentication (User management)
└── 📱 FCM (Push notifications)
```

## 🚀 Quick Start

### Prerequisites
- Android Studio Arctic Fox or later
- Node.js 18+ and npm
- Firebase CLI
- Google Maps API Key
- Firebase Project

### 1. Clone and Setup
```bash
git clone https://github.com/your-repo/daxido-native-android.git
cd daxido-native-android
chmod +x setup-complete.sh
./setup-complete.sh
```

### 2. Firebase Configuration
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Enable Authentication, Firestore, Realtime Database, and Cloud Functions
3. Download `google-services.json` and place it in `app/` directory
4. Update `AppConfig.kt` with your project details

### 3. API Keys Setup
Update the following in `app/src/main/java/com/daxido/core/config/AppConfig.kt`:
```kotlin
const val GOOGLE_MAPS_API_KEY = "your_google_maps_api_key"
const val FIREBASE_PROJECT_ID = "your_firebase_project_id"
const val RAZORPAY_KEY_ID = "your_razorpay_key"
const val STRIPE_PUBLISHABLE_KEY = "your_stripe_key"
```

### 4. Deploy Backend
```bash
# Install dependencies
cd functions
npm install
cd ..

# Deploy everything
firebase deploy
```

### 5. Build and Run
```bash
# Open in Android Studio
# Sync project with Gradle files
# Build and run on device/emulator
```

## 📋 Detailed Setup Guide

### Firebase Project Setup

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Click "Create a project"
   - Name: `daxido-ride-hailing`
   - Enable Google Analytics (optional)

2. **Add Android App**
   - Click "Add app" → Android
   - Package name: `com.daxido`
   - Download `google-services.json`
   - Place in `app/` directory

3. **Enable Services**
   ```bash
   # Authentication
   - Go to Authentication → Sign-in method
   - Enable Email/Password and Phone authentication
   
   # Firestore Database
   - Go to Firestore Database → Create database
   - Start in test mode (rules will be updated)
   
   # Realtime Database
   - Go to Realtime Database → Create database
   - Choose location closest to your users
   
   # Cloud Functions
   - Go to Functions → Get started
   - Upgrade to Blaze plan (required for external API calls)
   ```

### Google Maps Setup

1. **Get API Key**
   - Go to [Google Cloud Console](https://console.cloud.google.com)
   - Enable Maps SDK for Android
   - Create API key
   - Restrict key to Android apps

2. **Enable APIs**
   - Maps SDK for Android
   - Directions API
   - Places API
   - Geocoding API

### Payment Gateway Setup

#### Razorpay
1. Create account at [Razorpay](https://razorpay.com)
2. Get API keys from Dashboard → Settings → API Keys
3. Update in `AppConfig.kt`

#### Stripe (Optional)
1. Create account at [Stripe](https://stripe.com)
2. Get publishable key from Dashboard
3. Update in `AppConfig.kt`

## 🔧 Cloud Functions

The app includes comprehensive cloud functions for:

### Core Functions
- `allocateDriver`: Intelligent driver matching with ML scoring
- `calculatePreciseETA`: Google Maps integration for accurate ETAs
- `notifyDrivers`: Push notifications to available drivers
- `handleDriverResponse`: Process driver acceptance/rejection
- `updateRideStatus`: Handle ride lifecycle transitions
- `processPayment`: Secure payment processing
- `emergencyAlert`: Emergency response system

### Database Triggers
- `onDriverLocationUpdate`: Real-time driver location processing
- `onDriverResponseTimeout`: Handle driver response timeouts

### Deploy Functions
```bash
cd functions
npm install
firebase deploy --only functions
```

## 📊 Database Schema

### Firestore Collections

#### Users
```javascript
{
  userId: string,
  email: string,
  phone: string,
  name: string,
  profileImage: string,
  emergencyContacts: [{
    name: string,
    phone: string,
    relationship: string
  }],
  preferences: {
    language: string,
    notifications: boolean,
    darkMode: boolean
  },
  createdAt: timestamp,
  updatedAt: timestamp
}
```

#### Drivers
```javascript
{
  driverId: string,
  email: string,
  phone: string,
  name: string,
  profileImage: string,
  licenseNumber: string,
  vehicleInfo: {
    make: string,
    model: string,
    year: number,
    color: string,
    plateNumber: string
  },
  location: {
    latitude: number,
    longitude: number,
    address: string
  },
  status: "available" | "busy" | "offline",
  rating: number,
  completedTrips: number,
  fcmToken: string,
  createdAt: timestamp,
  updatedAt: timestamp
}
```

#### Rides
```javascript
{
  rideId: string,
  userId: string,
  driverId: string,
  pickupLocation: {
    latitude: number,
    longitude: number,
    address: string
  },
  dropLocation: {
    latitude: number,
    longitude: number,
    address: string
  },
  status: "requested" | "accepted" | "started" | "completed" | "cancelled",
  vehicleType: "economy" | "premium" | "xl",
  fare: {
    base: number,
    distance: number,
    time: number,
    surge: number,
    total: number
  },
  paymentMethod: string,
  paymentStatus: "pending" | "completed" | "failed",
  createdAt: timestamp,
  updatedAt: timestamp
}
```

### Realtime Database Structure

```
active_rides/
  {rideId}/
    driver_location: {lat, lng, timestamp}
    user_location: {lat, lng, timestamp}
    status: string
    polyline: string

drivers_available/
  {driverId}/
    location: {lat, lng}
    status: string
    vehicleType: string
    lastUpdate: timestamp

zones/
  {zoneId}/
    drivers/
      {driverId}: {location, status, lastUpdate}

ride_notifications/
  {rideId}/
    driverIds: [string]
    sentAt: timestamp
    successCount: number
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Cloud Functions Tests
```bash
cd functions
npm test
```

## 📱 Screenshots

### User App
- **Home Screen**: Clean interface with pickup/drop selection
- **Ride Tracking**: Real-time driver location and ETA
- **Payment**: Secure payment processing
- **Profile**: User settings and ride history

### Driver App
- **Dashboard**: Earnings and trip statistics
- **Ride Requests**: Incoming ride notifications
- **Navigation**: Integrated turn-by-turn directions
- **Profile**: Driver information and documents

## 🔒 Security

### Data Protection
- All sensitive data encrypted in transit and at rest
- Firebase Security Rules enforce data access controls
- Payment data never stored locally
- Biometric authentication for sensitive operations

### Privacy
- Location data only shared during active rides
- User data anonymized for analytics
- GDPR compliant data handling
- Clear privacy policy and user consent

## 🚀 Deployment

### Android App
1. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

2. **Generate Signed Bundle**
   ```bash
   ./gradlew bundleRelease
   ```

3. **Upload to Play Store**
   - Follow [Play Console](https://play.google.com/console) guidelines
   - Complete store listing
   - Submit for review

### Backend
```bash
# Deploy all services
firebase deploy

# Deploy specific services
firebase deploy --only functions
firebase deploy --only firestore
firebase deploy --only database
```

## 📈 Monitoring & Analytics

### Firebase Analytics
- User behavior tracking
- Ride completion rates
- Driver performance metrics
- Revenue analytics

### Crashlytics
- Real-time crash reporting
- Performance monitoring
- User session tracking

### Custom Metrics
- Driver allocation success rate
- Average response time
- Payment success rate
- Emergency response time

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add comprehensive comments
- Write unit tests for new features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation
- [Firebase Documentation](https://firebase.google.com/docs)
- [Android Developer Guide](https://developer.android.com/guide)
- [Google Maps API](https://developers.google.com/maps/documentation)

### Community
- [GitHub Issues](https://github.com/your-repo/daxido-native-android/issues)
- [Discord Server](https://discord.gg/daxido)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/daxido)

### Professional Support
- Email: support@daxido.com
- Phone: +1-800-DAXIDO-1
- Enterprise: enterprise@daxido.com

## 🎯 Roadmap

### Version 2.0
- [ ] AI-powered route optimization
- [ ] Electric vehicle support
- [ ] Carpooling features
- [ ] Multi-city support
- [ ] Advanced analytics dashboard

### Version 2.1
- [ ] Voice commands
- [ ] AR navigation
- [ ] Predictive pricing
- [ ] Driver earnings optimization
- [ ] Carbon footprint tracking

## 🙏 Acknowledgments

- Firebase team for excellent backend services
- Google Maps team for comprehensive mapping APIs
- Android team for robust development platform
- Open source community for amazing libraries

---

**Made with ❤️ by the Daxido Team**

*Building the future of transportation, one ride at a time.*
