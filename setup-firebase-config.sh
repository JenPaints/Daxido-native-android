#!/bin/bash

# Daxido Firebase Configuration Script

echo "🔧 Setting up Firebase Functions configuration..."
echo ""

# Set environment variables for Cloud Functions
firebase functions:config:set \
  google.maps_key="AIzaSyB7Vt9F-vl99jZYfd_ZUhVJHezNvY1IFWU" \
  razorpay.key_id="rzp_test_RInmVTmTZUOSlf" \
  razorpay.key_secret="ln3JjfNR5DeWKWuIHPyMOsiK" \
  app.name="Daxido" \
  app.version="1.0.0"

echo ""
echo "✅ Configuration set successfully!"
echo ""
echo "To verify configuration, run:"
echo "  firebase functions:config:get"
echo ""
echo "To deploy functions with new config:"
echo "  cd functions && firebase deploy --only functions"