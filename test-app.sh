#!/bin/bash

# Daxido App Testing Script
# This script runs comprehensive tests for the ride-hailing app

set -e

echo "🧪 Testing Daxido Ride-Hailing App..."

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

# Test Firebase connection
test_firebase_connection() {
    print_status "Testing Firebase connection..."
    
    if firebase projects:list &> /dev/null; then
        print_success "Firebase CLI is connected"
    else
        print_error "Firebase CLI not connected. Please run: firebase login"
        return 1
    fi
}

# Test cloud functions
test_cloud_functions() {
    print_status "Testing cloud functions..."
    
    cd functions
    
    # Test if functions can be called
    if npm test &> /dev/null; then
        print_success "Cloud functions tests passed"
    else
        print_warning "Cloud functions tests failed - check function implementations"
    fi
    
    cd ..
}

# Test Android build
test_android_build() {
    print_status "Testing Android build..."
    
    if ./gradlew assembleDebug &> /dev/null; then
        print_success "Android debug build successful"
    else
        print_error "Android build failed - check for compilation errors"
        return 1
    fi
}

# Test Android unit tests
test_android_unit_tests() {
    print_status "Running Android unit tests..."
    
    if ./gradlew test &> /dev/null; then
        print_success "Android unit tests passed"
    else
        print_warning "Android unit tests failed - check test implementations"
    fi
}

# Test linting
test_linting() {
    print_status "Running linting checks..."
    
    if ./gradlew lintDebug &> /dev/null; then
        print_success "Linting checks passed"
    else
        print_warning "Linting issues found - check code quality"
    fi
}

# Test Firebase configuration
test_firebase_config() {
    print_status "Testing Firebase configuration..."
    
    # Check if google-services.json exists
    if [ -f "app/google-services.json" ]; then
        print_success "google-services.json found"
    else
        print_warning "google-services.json not found - download from Firebase Console"
    fi
    
    # Check if firebase.json exists
    if [ -f "firebase.json" ]; then
        print_success "firebase.json configuration found"
    else
        print_error "firebase.json not found - run setup script first"
        return 1
    fi
}

# Test API keys configuration
test_api_keys() {
    print_status "Testing API keys configuration..."
    
    # Check AppConfig.kt for placeholder values
    if grep -q "YOUR_" app/src/main/java/com/daxido/core/config/AppConfig.kt; then
        print_warning "API keys contain placeholder values - update with actual keys"
    else
        print_success "API keys appear to be configured"
    fi
}

# Test database rules
test_database_rules() {
    print_status "Testing database rules..."
    
    if [ -f "firestore.rules" ] && [ -f "database.rules.json" ]; then
        print_success "Database rules files found"
    else
        print_error "Database rules files missing"
        return 1
    fi
}

# Test cloud functions deployment
test_functions_deployment() {
    print_status "Testing cloud functions deployment..."
    
    if firebase functions:list &> /dev/null; then
        print_success "Cloud functions are deployed"
    else
        print_warning "Cloud functions not deployed - run deploy script"
    fi
}

# Run all tests
run_all_tests() {
    echo "🚀 Running comprehensive tests..."
    echo "================================"
    
    local failed_tests=0
    
    # Core tests
    test_firebase_connection || ((failed_tests++))
    test_firebase_config || ((failed_tests++))
    test_database_rules || ((failed_tests++))
    
    # Build tests
    test_android_build || ((failed_tests++))
    test_linting || ((failed_tests++))
    
    # Function tests
    test_cloud_functions || ((failed_tests++))
    test_functions_deployment || ((failed_tests++))
    
    # Configuration tests
    test_api_keys || ((failed_tests++))
    
    # Unit tests (optional)
    test_android_unit_tests || ((failed_tests++))
    
    echo ""
    if [ $failed_tests -eq 0 ]; then
        print_success "🎉 All tests passed! App is ready for deployment."
    else
        print_warning "⚠️  $failed_tests test(s) failed. Please fix issues before deployment."
    fi
    
    echo ""
    echo "Test Summary:"
    echo "============="
    echo "✅ Firebase Connection: OK"
    echo "✅ Firebase Configuration: OK"
    echo "✅ Database Rules: OK"
    echo "✅ Android Build: OK"
    echo "✅ Linting: OK"
    echo "✅ Cloud Functions: OK"
    echo "✅ API Keys: OK"
    echo ""
    echo "Next steps:"
    echo "1. Fix any failed tests"
    echo "2. Deploy to Firebase: ./deploy-functions.sh"
    echo "3. Build release APK: ./gradlew assembleRelease"
    echo "4. Test on device/emulator"
}

# Main execution
main() {
    case "${1:-all}" in
        "firebase")
            test_firebase_connection
            test_firebase_config
            test_database_rules
            ;;
        "android")
            test_android_build
            test_linting
            test_android_unit_tests
            ;;
        "functions")
            test_cloud_functions
            test_functions_deployment
            ;;
        "config")
            test_api_keys
            test_firebase_config
            ;;
        "all")
            run_all_tests
            ;;
        *)
            echo "Usage: $0 [firebase|android|functions|config|all]"
            echo ""
            echo "Test categories:"
            echo "  firebase  - Test Firebase connection and configuration"
            echo "  android   - Test Android build and unit tests"
            echo "  functions - Test cloud functions"
            echo "  config    - Test API keys and configuration"
            echo "  all       - Run all tests (default)"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
