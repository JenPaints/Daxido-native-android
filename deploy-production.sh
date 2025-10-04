#!/bin/bash

# ============================================================================
# DAXIDO - PRODUCTION DEPLOYMENT SCRIPT
# ============================================================================
# This script automates the deployment process for Daxido Android App
#
# Usage:
#   ./deploy-production.sh [OPTIONS]
#
# Options:
#   --functions-only    Deploy only Firebase Functions
#   --rules-only        Deploy only Firebase Security Rules
#   --app-only          Build only Android App
#   --full              Full deployment (default)
#
# Prerequisites:
#   - Firebase CLI installed and logged in
#   - Android SDK configured
#   - local.properties configured with production keys
#   - Keystore file present
#
# ============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Deployment mode
DEPLOY_MODE="${1:---full}"

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

print_header() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}→ $1${NC}"
}

# ============================================================================
# PRE-DEPLOYMENT CHECKS
# ============================================================================

pre_deployment_checks() {
    print_header "PRE-DEPLOYMENT CHECKS"

    # Check Firebase CLI
    if ! command -v firebase &> /dev/null; then
        print_error "Firebase CLI not found. Install it with: npm install -g firebase-tools"
        exit 1
    fi
    print_success "Firebase CLI installed"

    # Check Firebase login
    if ! firebase projects:list &> /dev/null; then
        print_error "Not logged in to Firebase. Run: firebase login"
        exit 1
    fi
    print_success "Firebase authentication valid"

    # Check if we're in the right directory
    if [ ! -f "firebase.json" ]; then
        print_error "firebase.json not found. Run this script from project root."
        exit 1
    fi
    print_success "Project directory validated"

    # Check local.properties
    if [ ! -f "local.properties" ]; then
        print_error "local.properties not found. Configure API keys first."
        exit 1
    fi
    print_success "local.properties found"

    # Check for production keys
    if grep -q "rzp_test_" local.properties; then
        print_warning "Razorpay is in TEST mode. Switch to LIVE for production!"
        read -p "Continue anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi

    # Check Android SDK
    if [ ! -d "$HOME/Library/Android/sdk" ] && [ ! -d "$ANDROID_HOME" ]; then
        print_error "Android SDK not found. Install Android Studio first."
        exit 1
    fi
    print_success "Android SDK found"

    # Check gradlew
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found. This might not be an Android project."
        exit 1
    fi
    print_success "Gradle wrapper found"

    print_success "All pre-deployment checks passed!"
}

# ============================================================================
# DEPLOY FIREBASE FUNCTIONS
# ============================================================================

deploy_firebase_functions() {
    print_header "DEPLOYING FIREBASE FUNCTIONS"

    cd functions

    # Check if node_modules exist
    if [ ! -d "node_modules" ]; then
        print_info "Installing dependencies..."
        npm install
    else
        print_success "Dependencies already installed"
    fi

    # Run ESLint
    print_info "Running linter..."
    if npm run lint; then
        print_success "Code passed linting"
    else
        print_warning "Linting issues found. Review before deploying."
        read -p "Continue? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            cd ..
            exit 1
        fi
    fi

    cd ..

    # Deploy functions
    print_info "Deploying functions to Firebase..."
    if firebase deploy --only functions --project daxido-native; then
        print_success "Firebase Functions deployed successfully!"
    else
        print_error "Function deployment failed!"
        exit 1
    fi
}

# ============================================================================
# DEPLOY FIREBASE RULES
# ============================================================================

deploy_firebase_rules() {
    print_header "DEPLOYING FIREBASE SECURITY RULES"

    # Deploy Firestore rules
    print_info "Deploying Firestore rules..."
    if firebase deploy --only firestore:rules --project daxido-native; then
        print_success "Firestore rules deployed"
    else
        print_error "Firestore rules deployment failed!"
        exit 1
    fi

    # Deploy Firestore indexes
    print_info "Deploying Firestore indexes..."
    if firebase deploy --only firestore:indexes --project daxido-native; then
        print_success "Firestore indexes deployed"
    else
        print_error "Firestore indexes deployment failed!"
        exit 1
    fi

    # Deploy Realtime Database rules
    print_info "Deploying Realtime Database rules..."
    if firebase deploy --only database --project daxido-native; then
        print_success "Database rules deployed"
    else
        print_error "Database rules deployment failed!"
        exit 1
    fi

    # Deploy Storage rules
    print_info "Deploying Storage rules..."
    if firebase deploy --only storage --project daxido-native; then
        print_success "Storage rules deployed"
    else
        print_error "Storage rules deployment failed!"
        exit 1
    fi

    print_success "All Firebase rules deployed successfully!"
}

# ============================================================================
# BUILD ANDROID APP
# ============================================================================

build_android_app() {
    print_header "BUILDING ANDROID APP"

    # Clean build
    print_info "Cleaning previous builds..."
    ./gradlew clean

    # Build release APK
    print_info "Building release APK... (this may take a few minutes)"
    if ./gradlew assembleRelease; then
        print_success "Release APK built successfully!"

        # Find APK location
        APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
        if [ -n "$APK_PATH" ]; then
            APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
            print_info "APK Location: $APK_PATH"
            print_info "APK Size: $APK_SIZE"
        fi
    else
        print_error "Release build failed!"
        exit 1
    fi

    # Build release AAB (for Play Store)
    print_info "Building release AAB..."
    if ./gradlew bundleRelease; then
        print_success "Release AAB built successfully!"

        # Find AAB location
        AAB_PATH=$(find app/build/outputs/bundle/release -name "*.aab" | head -1)
        if [ -n "$AAB_PATH" ]; then
            AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
            print_info "AAB Location: $AAB_PATH"
            print_info "AAB Size: $AAB_SIZE"
        fi
    else
        print_error "Bundle build failed!"
        exit 1
    fi

    print_success "Android app built successfully!"
}

# ============================================================================
# POST-DEPLOYMENT TASKS
# ============================================================================

post_deployment_tasks() {
    print_header "POST-DEPLOYMENT TASKS"

    print_info "Generating deployment report..."

    # Create deployment report
    REPORT_FILE="deployment-report-$(date +%Y%m%d-%H%M%S).txt"

    cat > "$REPORT_FILE" << EOF
============================================================================
DAXIDO - DEPLOYMENT REPORT
============================================================================
Date: $(date)
Deployment Mode: $DEPLOY_MODE
============================================================================

FIREBASE FUNCTIONS:
$(firebase functions:list --project daxido-native 2>&1 || echo "Not deployed in this run")

ANDROID BUILD:
$(if [ "$DEPLOY_MODE" = "--full" ] || [ "$DEPLOY_MODE" = "--app-only" ]; then
    if [ -n "$APK_PATH" ]; then
        echo "APK: $APK_PATH ($APK_SIZE)"
    fi
    if [ -n "$AAB_PATH" ]; then
        echo "AAB: $AAB_PATH ($AAB_SIZE)"
    fi
else
    echo "Not built in this run"
fi)

============================================================================
NEXT STEPS:
============================================================================
1. Test the deployed functions with Firebase emulator
2. Upload AAB to Play Store Console
3. Configure Razorpay webhooks (if not done)
4. Enable Firebase App Check
5. Monitor Crashlytics for any issues
6. Set up monitoring alerts

============================================================================
EOF

    print_success "Deployment report generated: $REPORT_FILE"
}

# ============================================================================
# MAIN DEPLOYMENT FLOW
# ============================================================================

main() {
    print_header "DAXIDO PRODUCTION DEPLOYMENT"

    print_info "Deployment Mode: $DEPLOY_MODE"
    print_info "Firebase Project: daxido-native"
    print_info ""

    # Always run pre-deployment checks
    pre_deployment_checks

    # Deploy based on mode
    case "$DEPLOY_MODE" in
        --functions-only)
            deploy_firebase_functions
            ;;
        --rules-only)
            deploy_firebase_rules
            ;;
        --app-only)
            build_android_app
            ;;
        --full)
            deploy_firebase_functions
            deploy_firebase_rules
            build_android_app
            ;;
        *)
            print_error "Invalid deployment mode: $DEPLOY_MODE"
            echo "Usage: ./deploy-production.sh [--functions-only|--rules-only|--app-only|--full]"
            exit 1
            ;;
    esac

    # Post-deployment tasks
    post_deployment_tasks

    # Success message
    print_header "DEPLOYMENT COMPLETED SUCCESSFULLY!"
    print_success "All deployment tasks completed"
    print_info "Review the deployment report: $REPORT_FILE"
    print_info ""
    print_info "🎉 Daxido is ready for production!"
}

# Run main function
main
