# 🔑 Firebase Service Account Key Setup Guide

## Step-by-Step Instructions

### 1. **Access Firebase Console**
- Go to: https://console.firebase.google.com/
- Select project: **daxido-native**

### 2. **Navigate to Project Settings**
- Click the gear icon (⚙️) next to "Project Overview"
- Select "Project settings"

### 3. **Go to Service Accounts Tab**
- Click on "Service accounts" tab
- You'll see "Firebase Admin SDK" section

### 4. **Generate New Private Key**
- Click "Generate new private key"
- Click "Generate key" in the confirmation dialog
- A JSON file will be downloaded (e.g., `daxido-native-firebase-adminsdk-xxxxx.json`)

### 5. **Place the File in Project Root**
```bash
# Navigate to your project directory
cd /Users/shakthi./Downloads/Daxido-native-android

# Move the downloaded file to project root
mv ~/Downloads/daxido-native-firebase-adminsdk-*.json ./firebase-service-account.json
```

### 6. **Verify the File Structure**
The service account key file should look like this:
```json
{
  "type": "service_account",
  "project_id": "daxido-native",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

### 7. **Test the Setup**
```bash
# Run a simple test to verify the service account works
node -e "
const admin = require('firebase-admin');
const serviceAccount = require('./firebase-service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://daxido-native-default-rtdb.firebaseio.com'
});
console.log('✅ Firebase Admin initialized successfully!');
"
```

## 🔒 **Security Notes**

### **Important Security Considerations:**
- ⚠️ **Never commit the service account key to version control**
- ⚠️ **Add `firebase-service-account.json` to `.gitignore`**
- ⚠️ **Keep the key file secure and private**
- ⚠️ **Rotate keys regularly in production**

### **Add to .gitignore:**
```bash
echo "firebase-service-account.json" >> .gitignore
echo "*.json" >> .gitignore
```

## 🚀 **Alternative: Environment Variables**

For production, use environment variables instead of files:

```bash
# Set environment variables
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/firebase-service-account.json"
export FIREBASE_PROJECT_ID="daxido-native"
export FIREBASE_DATABASE_URL="https://daxido-native-default-rtdb.firebaseio.com"
```

Then update the initialization code:
```javascript
// Initialize Firebase Admin with environment variables
admin.initializeApp({
  projectId: process.env.FIREBASE_PROJECT_ID,
  databaseURL: process.env.FIREBASE_DATABASE_URL
});
```

## 🧪 **Run Tests After Setup**

Once you have the service account key:

```bash
# Run test data setup
node test-data-setup.js

# Run payment flow tests
node payment-flow-test.js

# Run performance tests
node performance-test.js

# Run deployment verification
node verify-deployment.js
```

## 📞 **Troubleshooting**

### **Common Issues:**

1. **"Service account object must contain a string 'project_id' property"**
   - Solution: Make sure you're using the service account key, not the client config

2. **"Permission denied"**
   - Solution: Ensure the service account has proper permissions in Firebase

3. **"Authentication Error"**
   - Solution: Verify the service account key is valid and not expired

### **Need Help?**
- Check Firebase documentation: https://firebase.google.com/docs/admin/setup
- Verify project permissions in Firebase Console
- Ensure the service account has the necessary roles
