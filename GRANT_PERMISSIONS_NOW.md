# 🚀 Grant Cloud Build Permissions - DO THIS NOW

## ⚡ Quick 3-Minute Fix

Firebase console is now open in your browser!

---

## 📋 Step-by-Step Instructions

### **Step 1: Navigate to IAM Page**

In the Firebase console that just opened, click on:
- **Settings** (gear icon, top left)
- Then click **"Users and permissions"** tab
- Or directly go to: https://console.firebase.google.com/project/daxido-native/settings/iam

---

### **Step 2: Add Cloud Build Service Account**

1. Click the blue **"Add member"** button

2. In **"New principals"** field, paste this email:
   ```
   781620504101@cloudbuild.gserviceaccount.com
   ```

3. Click **"Select a role"** dropdown

4. Search and add these 3 roles (one at a time):

   **Role 1:**
   - Search: `Cloud Functions Admin`
   - Select: **Cloud Functions Admin**
   - Click "Add another role" to add more

   **Role 2:**
   - Search: `Cloud Build`
   - Select: **Cloud Build Service Account**
   - Click "Add another role"

   **Role 3:**
   - Search: `Service Account User`
   - Select: **Service Account User**

5. Click **"Save"** button

---

### **Step 3: Wait 60 Seconds**

⏰ IAM permissions need 1-2 minutes to propagate. Grab some water!

---

### **Step 4: Deploy Functions**

Come back to terminal and run:

```bash
               
```

---

## ✅ Expected Success Output

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

Project Console: https://console.firebase.google.com/project/daxido-native/overview
```

---

## 🎯 Summary

**What you're doing:** Giving the Cloud Build robot permission to deploy your functions

**Service Account:** `781620504101@cloudbuild.gserviceaccount.com`

**Roles needed:**
1. ✅ Cloud Functions Admin (to deploy functions)
2. ✅ Cloud Build Service Account (to build containers)
3. ✅ Service Account User (to act as service account)

**Time required:** 3 minutes (including wait time)

---

## 🔍 Troubleshooting

### If you don't see "Add member" button:
- You might not have Owner/Editor permissions
- Ask project owner (nayaz@daxido.com) to grant you permissions

### If roles don't appear in dropdown:
- Make sure you're searching in the right project (daxido-native)
- Try typing the full role name: `roles/cloudfunctions.admin`

### If deployment still fails after 2 minutes:
- Check that all 3 roles were added
- Wait another minute and retry
- Check Cloud Console: https://console.cloud.google.com/iam-admin/iam?project=781620504101

---

## 🎉 After Success

Once deployed, your functions will be available at:
```
https://us-central1-daxido-native.cloudfunctions.net/[FUNCTION_NAME]
```

Test with:
```bash
curl https://us-central1-daxido-native.cloudfunctions.net/testFunction
```

---

**Ready?** Go to the Firebase console tab in your browser and follow the steps above! 🚀
