# Google Maps API Setup for Daxido App

## Step 1: Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project: `daxido-maps`
3. Enable billing (required for Maps API)

## Step 2: Enable APIs
Enable these APIs in Google Cloud Console:

### Required APIs:
- **Maps SDK for Android** - Core maps functionality
- **Places API** - Location search and autocomplete
- **Directions API** - Route calculation
- **Distance Matrix API** - Distance/time calculations
- **Geocoding API** - Address to coordinates conversion
- **Geolocation API** - Device location services

## Step 3: Create API Key
1. Go to APIs & Services → Credentials
2. Click "Create Credentials" → API Key
3. Restrict the key:
   - Application restrictions: Android apps
   - Package name: `com.daxido`
   - SHA-1 certificate fingerprint: (get from keystore)

## Step 4: Get SHA-1 Fingerprint
For debug builds:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

For release builds:
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
```

## Step 5: Configure API Key
Add to `local.properties`:
```properties
MAPS_API_KEY=your_actual_api_key_here
```

## Step 6: Test Maps Integration
- Run the app
- Check if maps load correctly
- Test location services
- Verify route calculation works

## Step 7: Production Considerations
- Set up API quotas and limits
- Monitor API usage
- Set up billing alerts
- Consider implementing caching for routes
