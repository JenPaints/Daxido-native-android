# 🔍 **Crashlytics Troubleshooting Guide**

## 🚨 **Issue: Crash Reports Not Appearing in Firebase Console**

Based on your logs, Firebase Crashlytics is initializing properly, but reports aren't showing up in the console. Here's how to fix this:

---

## 🔧 **Step 1: Enable Debug Logging**

### **Run the Debug Script:**
```bash
./debug-crashlytics.sh
```

### **Or Manually Enable Debug Logging:**
```bash
# Enable Crashlytics debug logging
adb shell setprop log.tag.FirebaseCrashlytics DEBUG

# Enable Firebase Analytics debug logging  
adb shell setprop log.tag.FA DEBUG

# Enable faster debug mode
adb shell setprop debug.firebase.analytics.app com.daxido

# Clear and monitor logs
adb logcat -c
adb logcat | grep -E "(FirebaseCrashlytics|FA|FirebaseApp|Crashlytics)"
```

---

## 🔧 **Step 2: Check Firebase Console Settings**

### **1. Verify Crashlytics is Enabled:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **daxido-native**
3. Navigate to **Crashlytics** in the left sidebar
4. Make sure Crashlytics is **enabled** for your app

### **2. Check App Registration:**
1. Go to **Project Settings** → **General**
2. Under **Your apps**, verify:
   - **Package name**: `com.daxido`
   - **SHA-1 fingerprint**: `9b6f31ba463731e8d059f196345a965c9f36a9a6`
   - **App ID**: `1:781620504101:android:e97af82cf91efc444e41cc`

---

## 🔧 **Step 3: Test Crash Reporting**

### **Method 1: Use the Test Panel**
1. **Run the app**
2. **Tap the red 🚨 button** (bottom-right corner)
3. **Tap "Show Crash Warning"**
4. **Tap "🚨 FORCE TEST CRASH"**
5. **App will crash** (this is intentional!)
6. **Restart the app** to send the crash report

### **Method 2: Programmatic Test**
```kotlin
// In your MainActivity or any ViewModel
crashlyticsManager.log("Testing crash reporting")
crashlyticsManager.setCustomKey("test_key", "test_value")
crashlyticsManager.forceTestCrash()
```

---

## 🔧 **Step 4: Check Common Issues**

### **Issue 1: Debug vs Release Build**
- **Debug builds** may not report crashes immediately
- **Release builds** report crashes more reliably
- **Solution**: Test with release build

### **Issue 2: Network Connectivity**
- Crashlytics needs internet connection
- Reports are queued when offline
- **Solution**: Ensure device has internet connection

### **Issue 3: App Not Restarted**
- Crash reports are sent when app restarts
- **Solution**: Always restart app after crash

### **Issue 4: Firebase Project Configuration**
- Wrong project ID or package name
- **Solution**: Verify google-services.json matches Firebase project

---

## 🔧 **Step 5: Build Release Version**

### **Create Release Build:**
```bash
# Build release version
./gradlew assembleRelease

# Install release version
adb install app/build/outputs/apk/release/app-release.apk
```

### **Test with Release Build:**
1. **Install release APK**
2. **Run the app**
3. **Test crash functionality**
4. **Check Firebase Console**

---

## 🔧 **Step 6: Verify Crashlytics Integration**

### **Check Logs for These Messages:**
```
✅ FirebaseCrashlytics: Initializing Firebase Crashlytics 19.0.3 for com.daxido
✅ FirebaseApp: FirebaseApp initialization successful
✅ FA: App measurement initialized, version: 136000
✅ FirebaseCrashlytics: No version control information found
```

### **Look for Error Messages:**
```
❌ FirebaseCrashlytics: Failed to initialize
❌ FirebaseApp: FirebaseApp initialization failed
❌ FA: App measurement initialization failed
```

---

## 🔧 **Step 7: Manual Verification**

### **1. Check Firebase Console:**
- Go to **Crashlytics** dashboard
- Look for **"No crashes reported"** or crash reports
- Check **"Non-fatals"** tab for error logs

### **2. Check App Logs:**
```bash
# Monitor logs while testing
adb logcat | grep -E "(FirebaseCrashlytics|FA|FirebaseApp)"
```

### **3. Test Custom Logging:**
```kotlin
// Test custom logging
crashlyticsManager.log("Custom test message")
crashlyticsManager.setCustomKey("test_timestamp", System.currentTimeMillis())
```

---

## 🔧 **Step 8: Advanced Troubleshooting**

### **1. Check Firebase Project Status:**
- Ensure project is active
- Check billing status (if applicable)
- Verify API quotas

### **2. Check Device/Emulator:**
- Use physical device for testing
- Ensure Google Play Services is updated
- Check device time/date settings

### **3. Check Network:**
- Ensure device can reach Firebase servers
- Check firewall/proxy settings
- Test with different network

---

## 🎯 **Expected Behavior**

### **After Successful Setup:**
1. **App starts** → Crashlytics initializes
2. **Crash occurs** → App crashes
3. **App restarts** → Crash report sent to Firebase
4. **2-5 minutes later** → Report appears in Firebase Console

### **Firebase Console Should Show:**
- **Crash reports** with stack traces
- **Custom logs** and key-value pairs
- **User sessions** and device information
- **Non-fatal errors** (if any)

---

## 🚨 **If Still Not Working**

### **1. Check Firebase Console:**
- Go to **Project Settings** → **General**
- Verify **google-services.json** is correct
- Check **SHA-1 fingerprint** matches

### **2. Re-enable Crashlytics:**
- Go to **Crashlytics** in Firebase Console
- **Disable** and **re-enable** Crashlytics
- Wait 5-10 minutes for changes to take effect

### **3. Check Firebase Status:**
- Visit [Firebase Status Page](https://status.firebase.google.com/)
- Check for any service outages

### **4. Contact Support:**
- Use Firebase Console support
- Provide logs and project details

---

## 🎊 **Success Indicators**

### **✅ Working Correctly:**
- Crashlytics initializes without errors
- Test crashes are reported to Firebase Console
- Custom logs appear in Firebase Console
- Non-fatal errors are logged properly

### **❌ Still Not Working:**
- No crash reports in Firebase Console after 10 minutes
- Error messages in logs
- Firebase Console shows "No crashes reported"

---

## 📞 **Next Steps**

1. **Run the debug script**: `./debug-crashlytics.sh`
2. **Test with release build**
3. **Check Firebase Console settings**
4. **Verify network connectivity**
5. **Contact Firebase support if needed**

**The issue is likely related to debug build limitations or Firebase Console configuration. Try the release build first! 🚀**
