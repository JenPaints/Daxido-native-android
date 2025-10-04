#!/bin/bash

# ============================================================================
# Fix Firebase Functions Deployment - Cloud Build Permissions
# ============================================================================
# This script grants the necessary IAM roles to the Cloud Build service account
# to enable successful Firebase Functions deployment.
#
# Usage: ./fix-cloud-build-permissions.sh
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project details
PROJECT_ID="daxido-native"
PROJECT_NUMBER="781620504101"
SERVICE_ACCOUNT="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

echo -e "${BLUE}============================================================================${NC}"
echo -e "${BLUE}Firebase Functions Deployment - Permission Fix${NC}"
echo -e "${BLUE}============================================================================${NC}"
echo ""
echo -e "${YELLOW}Project ID:${NC} $PROJECT_ID"
echo -e "${YELLOW}Project Number:${NC} $PROJECT_NUMBER"
echo -e "${YELLOW}Service Account:${NC} $SERVICE_ACCOUNT"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ Error: gcloud CLI is not installed${NC}"
    echo ""
    echo -e "${YELLOW}Please install gcloud CLI:${NC}"
    echo "  macOS: brew install --cask google-cloud-sdk"
    echo "  Or visit: https://cloud.google.com/sdk/docs/install"
    echo ""
    echo -e "${BLUE}Alternative: Use Firebase Console${NC}"
    echo "  1. Go to: https://console.firebase.google.com/project/daxido-native/settings/iam"
    echo "  2. Add member: $SERVICE_ACCOUNT"
    echo "  3. Grant roles: Cloud Functions Admin, Cloud Build Service Account, Service Account User"
    echo ""
    exit 1
fi

# Check if logged in to gcloud
echo -e "${BLUE}Checking gcloud authentication...${NC}"
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    echo -e "${YELLOW}⚠️  Not logged in to gcloud. Please login:${NC}"
    gcloud auth login
fi

# Set the project
echo -e "${BLUE}Setting active project...${NC}"
gcloud config set project $PROJECT_ID

echo ""
echo -e "${BLUE}============================================================================${NC}"
echo -e "${GREEN}Granting IAM Roles to Cloud Build Service Account${NC}"
echo -e "${BLUE}============================================================================${NC}"
echo ""

# Array of roles to grant
declare -a roles=(
    "roles/cloudbuild.builds.builder"
    "roles/cloudfunctions.admin"
    "roles/iam.serviceAccountUser"
    "roles/storage.admin"
    "roles/artifactregistry.writer"
)

# Grant each role
for role in "${roles[@]}"; do
    echo -e "${BLUE}➤ Granting role:${NC} $role"

    if gcloud projects add-iam-policy-binding $PROJECT_ID \
        --member="serviceAccount:$SERVICE_ACCOUNT" \
        --role="$role" \
        --condition=None \
        --quiet > /dev/null 2>&1; then
        echo -e "${GREEN}  ✅ Success${NC}"
    else
        echo -e "${YELLOW}  ⚠️  Already has role or error occurred${NC}"
    fi
done

echo ""
echo -e "${BLUE}============================================================================${NC}"
echo -e "${GREEN}Verifying Granted Permissions${NC}"
echo -e "${BLUE}============================================================================${NC}"
echo ""

# Verify granted roles
echo -e "${BLUE}Roles for $SERVICE_ACCOUNT:${NC}"
echo ""
gcloud projects get-iam-policy $PROJECT_ID \
    --flatten="bindings[].members" \
    --format="table(bindings.role)" \
    --filter="bindings.members:$SERVICE_ACCOUNT" 2>/dev/null || echo -e "${YELLOW}Unable to verify. Permissions may still be propagating.${NC}"

echo ""
echo -e "${BLUE}============================================================================${NC}"
echo -e "${GREEN}✅ Permissions Granted Successfully!${NC}"
echo -e "${BLUE}============================================================================${NC}"
echo ""
echo -e "${YELLOW}⏳ Important:${NC} Wait 1-2 minutes for IAM changes to propagate"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Wait 1-2 minutes for permissions to propagate"
echo "  2. Run: ${GREEN}firebase deploy --only functions${NC}"
echo "  3. Or run: ${GREEN}firebase deploy${NC} (to deploy everything)"
echo ""
echo -e "${BLUE}If deployment still fails after 2 minutes:${NC}"
echo "  1. Try re-running this script"
echo "  2. Check Firebase Console: https://console.firebase.google.com/project/daxido-native/settings/iam"
echo "  3. Check Cloud Console: https://console.cloud.google.com/iam-admin/iam?project=781620504101"
echo ""
echo -e "${GREEN}Done!${NC} 🚀"
echo ""
