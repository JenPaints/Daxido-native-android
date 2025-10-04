#!/bin/bash

# Daxido Cloud Functions Deployment Script
# This script deploys all cloud functions to Firebase

set -e

echo "🚀 Deploying Daxido Cloud Functions..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
if ! command -v firebase &> /dev/null; then
    print_error "Firebase CLI is not installed. Please install it first:"
    echo "npm install -g firebase-tools"
    exit 1
fi

# Check if we're in the right directory
if [ ! -f "functions/package.json" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Check if user is logged in to Firebase
if ! firebase projects:list &> /dev/null; then
    print_error "Please login to Firebase first:"
    echo "firebase login"
    exit 1
fi

print_status "Installing dependencies..."
cd functions
npm install
cd ..

print_status "Deploying Firestore rules and indexes..."
firebase deploy --only firestore:rules,firestore:indexes

print_status "Deploying Realtime Database rules..."
firebase deploy --only database

print_status "Deploying Cloud Functions..."
firebase deploy --only functions

print_success "🎉 All cloud functions deployed successfully!"

echo ""
echo "Deployed Functions:"
echo "=================="
echo "✅ allocateDriver - Intelligent driver matching"
echo "✅ calculatePreciseETA - Google Maps ETA calculation"
echo "✅ notifyDrivers - Push notifications to drivers"
echo "✅ handleDriverResponse - Process driver responses"
echo "✅ updateRideStatus - Handle ride lifecycle"
echo "✅ processPayment - Secure payment processing"
echo "✅ emergencyAlert - Emergency response system"
echo ""
echo "Next steps:"
echo "1. Test the functions using Firebase Functions shell"
echo "2. Update your Android app with the function URLs"
echo "3. Configure Firebase Authentication"
echo "4. Set up Google Maps API keys"
echo ""
echo "For testing: firebase functions:shell"