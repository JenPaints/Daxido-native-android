# Firebase Setup Guide for Daxido App

## Step 1: Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Project name: `daxido-ride-sharing`
4. Enable Google Analytics (recommended)

## Step 2: Add Android App
1. Click "Add app" → Android
2. Package name: `com.daxido`
3. App nickname: `Daxido Android`
4. Download `google-services.json`

## Step 3: Enable Services
Enable these Firebase services:

### Authentication
- Go to Authentication → Sign-in method
- Enable: Phone, Email/Password, Google
- Add your domain for OAuth

### Firestore Database
- Go to Firestore Database → Create database
- Start in test mode (we'll secure later)
- Choose location closest to your users

### Realtime Database
- Go to Realtime Database → Create database
- Start in test mode
- Choose location

### Storage
- Go to Storage → Get started
- Start in test mode

### Cloud Messaging
- Automatically enabled
- No additional setup needed

## Step 4: Security Rules
Update Firestore rules:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Rides are readable by participants
    match /rides/{rideId} {
      allow read: if request.auth != null && 
        (resource.data.riderId == request.auth.uid || 
         resource.data.driverId == request.auth.uid);
      allow write: if request.auth != null && 
        (resource.data.riderId == request.auth.uid || 
         resource.data.driverId == request.auth.uid);
    }
    
    // Drivers can manage their own data
    match /drivers/{driverId} {
      allow read, write: if request.auth != null && request.auth.uid == driverId;
    }
  }
}
```

## Step 5: Place google-services.json
Place the downloaded `google-services.json` in:
`app/google-services.json`

## Step 6: Test Connection
Run the app and check Firebase Console for:
- Authentication users
- Firestore data
- Realtime Database activity
