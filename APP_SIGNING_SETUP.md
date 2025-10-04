# App Signing Setup for Daxido

## Step 1: Generate Release Keystore
```bash
# Navigate to your project root
cd /Users/shakthi./Downloads/Daxido-native-android

# Generate keystore (replace with your details)
keytool -genkey -v -keystore daxido-release-key.keystore -alias daxido-key -keyalg RSA -keysize 2048 -validity 10000

# Enter details:
# Keystore password: [your secure password]
# Key password: [your secure password]
# Name: [Your Name]
# Organization: Daxido
# City: [Your City]
# State: [Your State]
# Country: [Your Country Code, e.g., US]
```

## Step 2: Secure Keystore Storage
```bash
# Move keystore to secure location
mkdir -p ~/.android/keystores
mv daxido-release-key.keystore ~/.android/keystores/

# Set proper permissions
chmod 600 ~/.android/keystores/daxido-release-key.keystore
```

## Step 3: Update Build Configuration
The keystore is already configured in `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../daxido-release-key.keystore")
        storePassword = "daxido123"  // Change this!
        keyAlias = "daxido-key"
        keyPassword = "daxido123"    // Change this!
    }
}
```

## Step 4: Environment Variables (Recommended)
Create `keystore.properties`:
```properties
storePassword=your_actual_store_password
keyPassword=your_actual_key_password
keyAlias=daxido-key
storeFile=../daxido-release-key.keystore
```

Update `build.gradle.kts`:
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

signingConfigs {
    create("release") {
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
    }
}
```

## Step 5: Test Release Build
```bash
# Build release APK
./gradlew assembleRelease

# Build release AAB (for Play Store)
./gradlew bundleRelease
```

## Step 6: Verify Signing
```bash
# Check APK signature
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Check AAB signature
bundletool verify --bundle=app/build/outputs/bundle/release/app-release.aab
```

## Security Notes:
- Never commit keystore files to version control
- Use strong passwords (20+ characters)
- Store keystore passwords securely
- Consider using Google Play App Signing
- Backup your keystore safely
