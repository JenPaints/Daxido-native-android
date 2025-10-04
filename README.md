# 🚗 Daxido - Native Android Ride-Hailing App

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![Google Maps](https://img.shields.io/badge/Maps-Google%20Maps-red.svg)](https://developers.google.com/maps)

A comprehensive ride-hailing application built with modern Android development practices, featuring real-time driver allocation, payment integration, and advanced safety features.

## 🌟 Features

### 🚖 Core Ride-Hailing
- **Real-time Driver Matching** - Advanced algorithm for optimal driver allocation
- **Live Ride Tracking** - Real-time location updates and ETA calculations
- **Multiple Vehicle Types** - Auto, Bike, Premium options
- **Ride Pooling** - Shared rides for cost optimization
- **Scheduled Rides** - Book rides in advance

### 💳 Payment Integration
- **Razorpay** - Indian payment gateway integration
- **Stripe** - International payment processing
- **UPI Payments** - Direct bank transfers
- **Digital Wallet** - In-app wallet system
- **Promo Codes** - Discount and referral system

### 🛡️ Safety & Security
- **Live Dashcam** - Real-time video streaming during rides
- **Emergency Alerts** - Instant SOS functionality
- **Driver Verification** - Comprehensive background checks
- **Ride Safety Score** - AI-powered safety monitoring
- **Biometric Authentication** - Secure app access

### 🎯 Advanced Features
- **AI Assistant** - Intelligent ride recommendations
- **Multi-stop Rides** - Multiple destinations in one trip
- **Corporate Accounts** - Business ride management
- **Real-time Notifications** - Push notifications and SMS
- **Offline Support** - Core functionality without internet

## 🏗️ Architecture

### Tech Stack
- **Frontend**: Android (Kotlin) with Jetpack Compose
- **Backend**: Firebase Cloud Functions (Node.js)
- **Database**: Cloud Firestore + Realtime Database
- **Authentication**: Firebase Auth (Phone, Email, Google)
- **Maps**: Google Maps SDK
- **Payments**: Razorpay + Stripe
- **Analytics**: Firebase Analytics + Crashlytics

### Project Structure
```
app/
├── src/main/java/com/daxido/
│   ├── core/                    # Core business logic
│   │   ├── auth/               # Authentication services
│   │   ├── location/           # Location services
│   │   ├── maps/               # Maps integration
│   │   ├── payment/            # Payment processing
│   │   └── services/           # Core services
│   ├── user/                   # User-facing features
│   │   ├── presentation/       # UI screens
│   │   └── data/               # User data management
│   ├── driver/                 # Driver-specific features
│   │   ├── presentation/       # Driver UI screens
│   │   └── services/           # Driver services
│   ├── admin/                  # Admin dashboard
│   │   ├── presentation/       # Admin UI
│   │   └── data/               # Admin data
│   └── di/                     # Dependency injection
├── src/main/res/               # Resources
└── build.gradle.kts           # Build configuration

functions/                      # Firebase Cloud Functions
├── index.js                   # Main functions file
├── helpers.js                 # Utility functions
└── package.json               # Node.js dependencies
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Node.js 14+ and npm
- Firebase CLI (`npm install -g firebase-tools`)
- Google Cloud Platform account
- Firebase project

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/daxido-native-android.git
   cd daxido-native-android
   ```

2. **Setup Firebase**
   ```bash
   # Install Firebase CLI
   npm install -g firebase-tools
   
   # Login to Firebase
   firebase login
   
   # Setup Firebase project
   firebase use --add
   ```

3. **Configure Android App**
   - Download `google-services.json` from Firebase Console
   - Place it in `app/` directory
   - Update package name in `app/build.gradle.kts`

4. **Setup Google Maps**
   - Get API key from Google Cloud Console
   - Update `GOOGLE_MAPS_API_KEY` in `AppConfig.kt`

5. **Configure Payment Gateways**
   - Setup Razorpay account and get API keys
   - Setup Stripe account and get API keys
   - Update keys in `AppConfig.kt`

6. **Deploy Cloud Functions**
   ```bash
   firebase deploy --only functions
   ```

7. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

## 📱 Screenshots

### User App
- **Home Screen** - Map view with vehicle selection
- **Ride Booking** - Pickup/destination selection
- **Live Tracking** - Real-time driver location
- **Payment** - Multiple payment options
- **Profile** - User settings and history

### Driver App
- **Driver Dashboard** - Earnings and performance
- **Ride Requests** - Accept/decline rides
- **Navigation** - Turn-by-turn directions
- **Earnings** - Daily/weekly/monthly reports

### Admin Dashboard
- **Live Rides** - Real-time ride monitoring
- **Driver Management** - Driver verification and management
- **Financial Overview** - Revenue and analytics
- **Emergency Monitor** - Safety alerts and responses

## 🔧 Configuration

### Environment Variables
Create a `local.properties` file in the root directory:
```properties
# Firebase Configuration
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_API_KEY=your-api-key

# Google Maps
GOOGLE_MAPS_API_KEY=your-maps-api-key

# Payment Gateways
RAZORPAY_KEY_ID=your-razorpay-key
STRIPE_PUBLISHABLE_KEY=your-stripe-key

# Other Services
GEMINI_API_KEY=your-gemini-key
```

### Firebase Rules
The project includes comprehensive security rules for:
- Firestore database
- Cloud Storage
- Realtime Database

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
# Test Firebase Functions
node functions/test-index.js

# Test Payment Flow
node payment-flow-test.js

# Performance Tests
node performance-test.js
```

### Manual Testing
```bash
# Run complete test suite
./run-complete-tests.sh

# Test specific components
./test-app.sh
```

## 📊 Analytics & Monitoring

### Firebase Analytics
- User behavior tracking
- Ride completion rates
- Payment success rates
- Feature usage analytics

### Crashlytics
- Real-time crash reporting
- Performance monitoring
- Custom error tracking

### Custom Metrics
- Driver allocation efficiency
- Ride completion times
- Payment processing times
- User satisfaction scores

## 🔒 Security

### Data Protection
- End-to-end encryption for sensitive data
- Secure API communication
- Biometric authentication
- Certificate pinning

### Privacy Compliance
- GDPR compliant data handling
- User consent management
- Data retention policies
- Right to deletion

## 🚀 Deployment

### Android App
```bash
# Build release APK
./gradlew assembleRelease

# Build AAB for Play Store
./gradlew bundleRelease
```

### Firebase Functions
```bash
# Deploy all functions
firebase deploy --only functions

# Deploy specific function
firebase deploy --only functions:functionName
```

### Production Checklist
- [ ] Update API keys for production
- [ ] Configure production Firebase project
- [ ] Enable security rules
- [ ] Setup monitoring and alerts
- [ ] Test payment gateways
- [ ] Verify Google Maps integration

## 📈 Performance Optimization

### Cost Optimization
- Connection pooling for Firebase
- Batch operations for database
- Minimal memory usage (256MB)
- Short function timeouts (10s)
- Async notifications

### Caching Strategy
- Location data caching
- Route optimization
- Image compression
- Offline data storage

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation
- [Setup Guide](SETUP_GUIDE.md)
- [API Documentation](API_CONFIGURATION_REPORT.md)
- [Security Guide](SECURITY_SETUP.md)

### Troubleshooting
- [Common Issues](FIXES_SUMMARY.md)
- [Firebase Setup](FIREBASE_SETUP.md)
- [Deployment Issues](DEPLOYMENT_FIX_FINAL.md)

### Contact
- **Email**: support@daxido.com
- **Website**: https://daxido.com
- **Documentation**: [Wiki](https://github.com/yourusername/daxido-native-android/wiki)

## 🙏 Acknowledgments

- Firebase team for excellent backend services
- Google Maps team for comprehensive mapping APIs
- Razorpay and Stripe for payment processing
- Android community for best practices and libraries

---

**Made with ❤️ by the Daxido Team**

*Transforming urban mobility, one ride at a time.*