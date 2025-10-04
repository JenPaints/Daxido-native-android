#!/bin/bash

# Daxido Ride-Hailing App Setup Script
# This script sets up the complete Firebase backend and deploys cloud functions

set -e

echo "🚗 Setting up Daxido Ride-Hailing App..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Firebase CLI is installed
check_firebase_cli() {
    if ! command -v firebase &> /dev/null; then
        print_error "Firebase CLI is not installed. Please install it first:"
        echo "npm install -g firebase-tools"
        exit 1
    fi
    print_success "Firebase CLI is installed"
}

# Check if Node.js is installed
check_node() {
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install it first:"
        echo "Visit: https://nodejs.org/"
        exit 1
    fi
    print_success "Node.js is installed"
}

# Initialize Firebase project
init_firebase() {
    print_status "Initializing Firebase project..."
    
    if [ ! -f "firebase.json" ]; then
        firebase init --project daxido-ride-hailing
    else
        print_warning "Firebase project already initialized"
    fi
}

# Install dependencies
install_dependencies() {
    print_status "Installing cloud functions dependencies..."
    cd functions
    npm install
    cd ..
    print_success "Dependencies installed"
}

# Deploy Firestore rules and indexes
deploy_firestore() {
    print_status "Deploying Firestore rules and indexes..."
    firebase deploy --only firestore:rules,firestore:indexes
    print_success "Firestore rules and indexes deployed"
}

# Deploy cloud functions
deploy_functions() {
    print_status "Deploying cloud functions..."
    firebase deploy --only functions
    print_success "Cloud functions deployed"
}

# Deploy Realtime Database rules
deploy_rtdb() {
    print_status "Deploying Realtime Database rules..."
    firebase deploy --only database
    print_success "Realtime Database rules deployed"
}

# Setup Firebase project configuration
setup_firebase_config() {
    print_status "Setting up Firebase project configuration..."
    
    # Create firebase.json if it doesn't exist
    if [ ! -f "firebase.json" ]; then
        cat > firebase.json << EOF
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "functions": {
    "source": "functions",
    "runtime": "nodejs18"
  },
  "database": {
    "rules": "database.rules.json"
  },
  "hosting": {
    "public": "public",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ]
  }
}
EOF
    fi
    
    # Create database.rules.json for Realtime Database
    if [ ! -f "database.rules.json" ]; then
        cat > database.rules.json << EOF
{
  "rules": {
    "active_rides": {
      "\$rideId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "drivers_available": {
      "\$driverId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == \$driverId"
      }
    },
    "driver_locations": {
      "\$driverId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == \$driverId"
      }
    },
    "ride_notifications": {
      "\$rideId": {
        ".read": "auth != null",
        ".write": false
      }
    },
    "driver_responses": {
      "\$rideId": {
        "\$driverId": {
          ".read": "auth != null",
          ".write": "auth != null && auth.uid == \$driverId"
        }
      }
    },
    "zones": {
      "\$zoneId": {
        ".read": "auth != null",
        ".write": false
      }
    }
  }
}
EOF
    fi
    
    print_success "Firebase configuration files created"
}

# Create environment configuration
create_env_config() {
    print_status "Creating environment configuration..."
    
    if [ ! -f ".env" ]; then
        cat > .env << EOF
# Firebase Configuration
FIREBASE_PROJECT_ID=daxido-ride-hailing
FIREBASE_WEB_API_KEY=your_web_api_key_here
FIREBASE_APP_ID=your_app_id_here

# Google Maps API Key
GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here

# Payment Gateway Keys
RAZORPAY_KEY_ID=your_razorpay_key_id_here
RAZORPAY_KEY_SECRET=your_razorpay_key_secret_here
STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key_here

# Realtime Database URL
FIREBASE_DATABASE_URL=https://daxido-ride-hailing-default-rtdb.firebaseio.com
EOF
        print_warning "Please update .env file with your actual API keys"
    else
        print_warning ".env file already exists"
    fi
}

# Setup Android app configuration
setup_android_config() {
    print_status "Setting up Android app configuration..."
    
    # Update google-services.json template
    if [ -f "app/google-services.json.template" ]; then
        print_warning "Please replace google-services.json with your actual Firebase configuration"
        print_warning "Download it from: https://console.firebase.google.com/project/daxido-ride-hailing/settings/general"
    fi
}

# Deploy everything
deploy_all() {
    print_status "Deploying all Firebase services..."
    
    # Deploy in order
    deploy_firestore
    deploy_functions
    deploy_rtdb
    
    print_success "All Firebase services deployed successfully!"
}

# Test cloud functions
test_functions() {
    print_status "Testing cloud functions..."
    
    # Test allocateDriver function
    print_status "Testing allocateDriver function..."
    firebase functions:shell --only allocateDriver
    
    print_success "Cloud functions testing completed"
}

# Main execution
main() {
    echo "🚗 Daxido Ride-Hailing App Setup"
    echo "================================="
    
    # Pre-flight checks
    check_firebase_cli
    check_node
    
    # Setup
    setup_firebase_config
    create_env_config
    setup_android_config
    init_firebase
    install_dependencies
    
    # Deploy
    deploy_all
    
    # Test
    test_functions
    
    echo ""
    print_success "🎉 Daxido Ride-Hailing App setup completed!"
    echo ""
    echo "Next steps:"
    echo "1. Update .env file with your actual API keys"
    echo "2. Replace google-services.json with your Firebase configuration"
    echo "3. Update AppConfig.kt with your actual project details"
    echo "4. Build and run the Android app"
    echo ""
    echo "For more information, visit: https://github.com/your-repo/daxido-native-android"
}

# Run main function
main "$@"
