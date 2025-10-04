const {onRequest} = require("firebase-functions/v2/https");
const {initializeApp} = require("firebase-admin/app");

// Initialize Firebase Admin
initializeApp();

/**
 * Simple test function
 */
exports.testFunction = onRequest(async (req, res) => {
  res.json({
    message: "Test function working!",
    timestamp: new Date().toISOString()
  });
});
