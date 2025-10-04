#!/bin/bash

# Daxido Firebase Setup Script
# This script helps you configure Firebase for the Daxido app

set -e

echo "🔥 Daxido Firebase Setup Script"
echo "================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Firebase CLI is installed
check_firebase_cli() {
    if ! command -v firebase &> /dev/null; then
        echo -e "${RED}❌ Firebase CLI not found${NC}"
        echo ""
        echo "Installing Firebase CLI..."
        curl -sL https://firebase.tools | bash
    else
        echo -e "${GREEN}✅ Firebase CLI found${NC}"
    fi
}

# Get SHA-1 fingerprint
get_sha1() {
    echo ""
    echo -e "${BLUE}📱 Getting SHA-1 Fingerprint...${NC}"
    echo ""

    echo "Debug keystore SHA-1:"
    keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep "SHA1:" || echo "Debug keystore not found"

    echo ""
    echo "Release keystore SHA-1:"
    if [ -f "daxido-release-key.keystore" ]; then
        keytool -list -v -keystore daxido-release-key.keystore -alias daxido-key -storepass daxido123 -keypass daxido123 2>/dev/null | grep "SHA1:" || echo "Release keystore found but couldn't read"
    else
        echo "Release keystore not found at: daxido-release-key.keystore"
    fi

    echo ""
    echo -e "${YELLOW}⚠️  Copy the SHA1 fingerprints above and add them to Firebase Console${NC}"
}

# Check google-services.json
check_google_services() {
    echo ""
    echo -e "${BLUE}📄 Checking google-services.json...${NC}"

    if [ -f "app/google-services.json" ]; then
        # Check if it's still the placeholder
        if grep -q "YOUR_PROJECT_NUMBER" app/google-services.json; then
            echo -e "${RED}❌ google-services.json is still a placeholder${NC}"
            echo ""
            echo "Please follow these steps:"
            echo "1. Go to https://console.firebase.google.com"
            echo "2. Select your project or create new one"
            echo "3. Click 'Add app' → Android"
            echo "4. Enter package name: com.daxido"
            echo "5. Download google-services.json"
            echo "6. Replace app/google-services.json with downloaded file"
            return 1
        else
            echo -e "${GREEN}✅ google-services.json looks valid${NC}"
            return 0
        fi
    else
        echo -e "${RED}❌ google-services.json not found${NC}"
        return 1
    fi
}

# Create Firebase indexes
create_firestore_indexes() {
    echo ""
    echo -e "${BLUE}📊 Creating firestore.indexes.json...${NC}"

    cat > firestore.indexes.json <<'EOF'
{
  "indexes": [
    {
      "collectionGroup": "rides",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "notifications",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "notifications",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "isRead", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "transactions",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "active_pools",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "vehicleType", "order": "ASCENDING" },
        { "fieldPath": "currentOccupancy", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
EOF

    echo -e "${GREEN}✅ firestore.indexes.json created${NC}"
}

# Create Firestore rules file
create_firestore_rules() {
    echo ""
    echo -e "${BLUE}🔒 Creating firestore.rules...${NC}"

    cat > firestore.rules <<'EOF'
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // Users collection
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId);

      match /paymentMethods/{methodId} {
        allow read, write: if isOwner(userId);
      }
    }

    // Rides collection
    match /rides/{rideId} {
      allow read: if isAuthenticated() &&
                     (resource.data.userId == request.auth.uid ||
                      resource.data.driverId == request.auth.uid);
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow update: if isAuthenticated() &&
                       (resource.data.userId == request.auth.uid ||
                        resource.data.driverId == request.auth.uid);
    }

    // Notifications collection
    match /notifications/{notificationId} {
      allow read: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow write: if isAuthenticated();
    }

    // Wallets collection
    match /wallets/{userId} {
      allow read: if isOwner(userId);
      allow write: if isOwner(userId);
    }

    // Transactions collection
    match /transactions/{transactionId} {
      allow read: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
    }

    // Active pools for ride pooling
    match /active_pools/{poolId} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated();
    }
  }
}
EOF

    echo -e "${GREEN}✅ firestore.rules created${NC}"
}

# Create Realtime Database rules file
create_database_rules() {
    echo ""
    echo -e "${BLUE}🔒 Creating database.rules.json...${NC}"

    cat > database.rules.json <<'EOF'
{
  "rules": {
    "drivers": {
      "$driverId": {
        ".read": true,
        ".write": "auth != null && auth.uid == $driverId",
        ".indexOn": ["isAvailable", "vehicleType"]
      }
    },
    "active_rides": {
      "$rideId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "ride_requests": {
      "$rideId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "sos_alerts": {
      "$rideId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
EOF

    echo -e "${GREEN}✅ database.rules.json created${NC}"
}

# Create Storage rules file
create_storage_rules() {
    echo ""
    echo -e "${BLUE}🔒 Creating storage.rules...${NC}"

    cat > storage.rules <<'EOF'
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // User files
    match /users/{userId}/{allPaths=**} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId) && request.resource.size < 10 * 1024 * 1024; // 10MB limit
    }

    // Driver files
    match /drivers/{driverId}/{allPaths=**} {
      allow read: if isAuthenticated();
      allow write: if isOwner(driverId) && request.resource.size < 10 * 1024 * 1024;
    }

    // Profile images
    match /profile_images/{userId}/{image} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId) &&
                      request.resource.contentType.matches('image/.*') &&
                      request.resource.size < 5 * 1024 * 1024; // 5MB limit
    }
  }
}
EOF

    echo -e "${GREEN}✅ storage.rules created${NC}"
}

# Deploy rules using Firebase CLI
deploy_rules() {
    echo ""
    echo -e "${BLUE}🚀 Deploying Firebase rules...${NC}"

    if ! command -v firebase &> /dev/null; then
        echo -e "${RED}❌ Firebase CLI not installed. Skipping deployment.${NC}"
        echo "Install it with: curl -sL https://firebase.tools | bash"
        return 1
    fi

    echo "Logging into Firebase..."
    firebase login

    echo ""
    echo "Deploying Firestore rules..."
    firebase deploy --only firestore:rules

    echo ""
    echo "Deploying Realtime Database rules..."
    firebase deploy --only database

    echo ""
    echo "Deploying Storage rules..."
    firebase deploy --only storage

    echo ""
    echo "Deploying Firestore indexes..."
    firebase deploy --only firestore:indexes

    echo -e "${GREEN}✅ All rules deployed successfully${NC}"
}

# Main setup flow
main() {
    echo ""
    echo "Starting Firebase setup..."
    echo ""

    # Step 1: Check Firebase CLI
    check_firebase_cli

    # Step 2: Get SHA-1
    get_sha1

    # Step 3: Check google-services.json
    if ! check_google_services; then
        echo ""
        echo -e "${YELLOW}⚠️  Please download google-services.json from Firebase Console first${NC}"
        echo ""
        read -p "Press Enter when you've added google-services.json..."
    fi

    # Step 4: Create rule files
    create_firestore_rules
    create_database_rules
    create_storage_rules
    create_firestore_indexes

    echo ""
    echo -e "${GREEN}✅ Setup files created successfully!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Make sure google-services.json is in place"
    echo "2. Run: firebase init (if not already initialized)"
    echo "3. Run: firebase deploy --only firestore:rules,database,storage"
    echo ""
    echo "Or run: ./setup-firebase.sh deploy"
    echo ""
}

# Check for deploy argument
if [ "$1" = "deploy" ]; then
    deploy_rules
else
    main
fi
