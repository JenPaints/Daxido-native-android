# 🔧 Add Debug and Staging Apps to Firebase

## Issue
Your `google-services.json` only has `com.daxido`, but the app uses:
- `com.daxido` (release)
- `com.daxido.debug` (debug builds)
- `com.daxido.staging` (staging builds)

## Solution: Add Apps in Firebase Console

### Step 1: Go to Firebase Console
1. Open: https://console.firebase.google.com
2. Select project: **daxido-native**
3. Click on **Project Settings** (⚙️ gear icon)

### Step 2: Add Debug App
1. Scroll to **Your apps** section
2. Click **Add app** → Select **Android** icon
3. Enter details:
   - **Android package name**: `com.daxido.debug`
   - **App nickname**: Daxido Debug
4. Click **Register app**
5. **Skip** the download step (we'll download new file later)
6. Click **Continue to console**

### Step 3: Add Staging App
1. Click **Add app** → Select **Android** icon again
2. Enter details:
   - **Android package name**: `com.daxido.staging`
   - **App nickname**: Daxido Staging
3. Click **Register app**
4. **Skip** the download step
5. Click **Continue to console**

### Step 4: Add SHA-1 for Debug App
1. Get your debug SHA-1:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```

2. In Firebase Console → Project Settings
3. Find **com.daxido.debug** app
4. Click **Add fingerprint**
5. Paste the SHA1
6. Click **Save**

### Step 5: Download Updated google-services.json
1. In Project Settings, scroll to **Your apps**
2. Find any of your apps
3. Click **google-services.json** download button
4. This will download the file with ALL THREE apps configured
5. Replace the current file:
   ```bash
   mv ~/Downloads/google-services.json /Users/shakthi./Downloads/Daxido-native-android/app/google-services.json
   ```

### Step 6: Verify and Build
```bash
cd /Users/shakthi./Downloads/Daxido-native-android

# Check the file has all 3 apps
cat app/google-services.json | grep -c "package_name"
# Should output: 3

# Build
./gradlew clean assembleDebug
```

---

## Quick Alternative: Build Without Debug Suffix

If you want to test immediately without adding apps:

```bash
cd /Users/shakthi./Downloads/Daxido-native-android

# Build release variant (uses com.daxido - already configured)
./gradlew assembleRelease

# Or install directly
./gradlew installRelease
```

---

## After Adding Apps

Once you've added both apps and downloaded the new `google-services.json`:

✅ All three variants will work:
```bash
./gradlew assembleDebug      # com.daxido.debug
./gradlew assembleStaging     # com.daxido.staging
./gradlew assembleRelease     # com.daxido
```

---

**Do this now to continue testing!** 🚀
