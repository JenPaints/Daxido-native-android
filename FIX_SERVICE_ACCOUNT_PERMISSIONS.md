# 🔧 Fix Firebase Admin SDK Service Account Permissions

## 🚨 **Current Issue**
Firebase functions deployment failing with "Precondition failed" errors because the Firebase Admin SDK service account lacks required permissions.

## 📋 **Required IAM Roles**

The service account `firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com` needs these roles:

### **1. Cloud Functions Admin**
- **Role:** `roles/cloudfunctions.admin`
- **Purpose:** Deploy, update, and manage Cloud Functions

### **2. Service Account User**
- **Role:** `roles/iam.serviceAccountUser`
- **Purpose:** Act as a service account

### **3. Cloud Build Editor**
- **Role:** `roles/cloudbuild.builds.editor`
- **Purpose:** Build function containers

### **4. Artifact Registry Writer**
- **Role:** `roles/artifactregistry.writer`
- **Purpose:** Push container images

### **5. Cloud Run Admin**
- **Role:** `roles/run.admin`
- **Purpose:** Deploy to Cloud Run (Functions v2)

### **6. Storage Admin**
- **Role:** `roles/storage.admin`
- **Purpose:** Access Cloud Storage for function artifacts

---

## 🛠️ **Solution: Grant Permissions**

### **Option 1: Using Google Cloud Console (Recommended)**

1. **Go to IAM & Admin:**
   ```
   https://console.cloud.google.com/iam-admin/iam?project=daxido-native
   ```

2. **Find the Service Account:**
   - Search for: `firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com`
   - Click the pencil icon to edit

3. **Add Required Roles:**
   Click "ADD ANOTHER ROLE" and add each role:
   - `Cloud Functions Admin`
   - `Service Account User`
   - `Cloud Build Editor`
   - `Artifact Registry Writer`
   - `Cloud Run Admin`
   - `Storage Admin`

4. **Save Changes**

### **Option 2: Using gcloud CLI**

Run these commands in your terminal:

```bash
# Set project
gcloud config set project daxido-native

# Grant Cloud Functions Admin
gcloud projects add-iam-policy-binding daxido-native \
  --member="serviceAccount:firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com" \
  --role="roles/cloudfunctions.admin"

# Grant Service Account User
gcloud projects add-iam-policy-binding daxido-native \
  --member="serviceAccount:firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

# Grant Cloud Build Editor
gcloud projects add-iam-policy-binding daxido-native \
  --member="serviceAccount:firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com" \
  --role="roles/cloudbuild.builds.editor"

# Grant Artifact Registry Writer
gcloud projects add-iam-policy-binding daxido-native \
  --member="serviceAccount:firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

# Grant Cloud Run Admin
gcloud projects add-iam-policy-binding daxido-native \
  --member="serviceAccount:firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com" \
  --role="roles/run.admin"

# Grant Storage Admin
gcloud projects add-iam-policy-binding daxido-native \
  --member="serviceAccount:firebase-adminsdk-fbsvc@daxido-native.iam.gserviceaccount.com" \
  --role="roles/storage.admin"
```

---

## ⏰ **Wait for Propagation**

IAM permissions take **2-3 minutes** to propagate. Wait before retrying deployment.

---

## 🚀 **Retry Deployment**

After granting permissions, retry the deployment:

```bash
# Deploy all functions
firebase deploy --only functions

# Or deploy functions one by one (if quota issues)
firebase deploy --only functions:testFunction
firebase deploy --only functions:processPayment
firebase deploy --only functions:allocateDriver
firebase deploy --only functions:handleDriverResponse
firebase deploy --only functions:updateRideStatus
firebase deploy --only functions:emergencyAlert
firebase deploy --only functions:calculatePreciseETA
firebase deploy --only functions:notifyDrivers
```

---

## ✅ **Expected Success Output**

After permissions are granted, you should see:

```
✔  functions[testFunction(us-central1)] Successful update operation.
✔  functions[processPayment(us-central1)] Successful update operation.
✔  functions[allocateDriver(us-central1)] Successful update operation.
✔  functions[handleDriverResponse(us-central1)] Successful update operation.
✔  functions[updateRideStatus(us-central1)] Successful update operation.
✔  functions[emergencyAlert(us-central1)] Successful update operation.
✔  functions[calculatePreciseETA(us-central1)] Successful update operation.
✔  functions[notifyDrivers(us-central1)] Successful update operation.

✔  Deploy complete!
```

---

## 🔍 **Troubleshooting**

### **If permissions don't work:**
1. Verify you have Owner/Editor role on the project
2. Check that the service account email is correct
3. Wait 5 minutes for IAM propagation
4. Try deploying one function at a time

### **If still getting errors:**
1. Check Cloud Console for detailed error logs
2. Verify all required APIs are enabled
3. Ensure billing is enabled on the project

---

## 📞 **Quick Links**

- **IAM Console:** https://console.cloud.google.com/iam-admin/iam?project=daxido-native
- **Firebase Console:** https://console.firebase.google.com/project/daxido-native
- **Cloud Functions Console:** https://console.cloud.google.com/functions?project=daxido-native

---

**Ready?** Go to the IAM console and grant the required permissions! 🚀
