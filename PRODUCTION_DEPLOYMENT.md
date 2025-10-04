# Daxido App Production Deployment Guide

## Pre-Deployment Checklist

### ✅ Firebase Setup
- [ ] Firebase project created
- [ ] Authentication enabled (Phone, Email, Google)
- [ ] Firestore database created with security rules
- [ ] Realtime Database configured
- [ ] Storage bucket created
- [ ] `google-services.json` added to project
- [ ] Firebase security rules tested

### ✅ Google Maps Setup
- [ ] Google Cloud project created
- [ ] Maps APIs enabled (Maps SDK, Places, Directions, etc.)
- [ ] API key created and restricted
- [ ] SHA-1 fingerprints added for debug and release
- [ ] API key added to `local.properties`

### ✅ App Signing
- [ ] Release keystore generated
- [ ] Keystore passwords secured
- [ ] Signing configuration tested
- [ ] Release build verified

### ✅ Build Configuration
- [ ] Build variants configured (debug, staging, release)
- [ ] ProGuard rules tested
- [ ] App version updated
- [ ] Version code incremented

## Deployment Steps

### 1. Final Testing
```bash
# Test all build variants
./gradlew assembleDebug
./gradlew assembleStaging
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### 2. Generate Release Builds
```bash
# Generate APK for testing
./gradlew assembleRelease

# Generate AAB for Play Store
./gradlew bundleRelease
```

### 3. Google Play Console Setup
1. Create Google Play Developer account
2. Create new app in Play Console
3. Upload AAB file
4. Fill in store listing details
5. Set up pricing and distribution
6. Configure app content rating

### 4. Internal Testing
1. Upload to Internal Testing track
2. Add test users
3. Test all core features
4. Verify payment flows
5. Test location services

### 5. Production Release
1. Move to Production track
2. Set release percentage (start with 5%)
3. Monitor crash reports
4. Monitor user feedback
5. Gradually increase rollout

## Post-Deployment Monitoring

### Analytics Setup
- Google Analytics for user behavior
- Firebase Analytics for app usage
- Custom events for business metrics

### Crash Reporting
- Firebase Crashlytics for crash reports
- Set up alerts for critical crashes
- Monitor ANR (Application Not Responding) reports

### Performance Monitoring
- Firebase Performance Monitoring
- Monitor app startup time
- Track network performance
- Monitor battery usage

### User Feedback
- In-app feedback system
- Play Store reviews monitoring
- Support ticket system

## Rollback Plan
1. Keep previous version ready
2. Monitor key metrics
3. Set up automated alerts
4. Have rollback procedure documented

## Security Checklist
- [ ] API keys properly secured
- [ ] Firebase security rules tested
- [ ] User data encryption enabled
- [ ] Payment data PCI compliant
- [ ] Location data privacy compliant
- [ ] GDPR compliance (if applicable)

## Performance Optimization
- [ ] App size optimized
- [ ] Images compressed
- [ ] ProGuard rules optimized
- [ ] Network requests optimized
- [ ] Database queries optimized
