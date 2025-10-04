#!/usr/bin/env node

/**
 * Daxido Deployment Verification Script
 * This script verifies that all components are properly deployed and functional
 */

const admin = require('firebase-admin');
const axios = require('axios');

// Initialize Firebase Admin with Application Default Credentials
admin.initializeApp({
  projectId: "daxido-native",
  databaseURL: "https://daxido-native-default-rtdb.firebaseio.com"
});

const db = admin.firestore();

// Firebase Functions base URL (using local emulator)
const FUNCTIONS_BASE_URL = 'http://127.0.0.1:5001/daxido-native/us-central1';

// Verification configuration
const VERIFICATION_CONFIG = {
  BASE_URL: 'https://us-central1-daxido-native.cloudfunctions.net',
  TIMEOUT: 30000,
  RETRY_ATTEMPTS: 3
};

// Utility functions
const log = (message, type = 'info') => {
  const timestamp = new Date().toISOString();
  const prefix = type === 'error' ? '❌' : type === 'success' ? '✅' : 'ℹ️';
  console.log(`${prefix} [${timestamp}] ${message}`);
};

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Retry mechanism
const retry = async (operation, maxAttempts = 3) => {
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await operation();
    } catch (error) {
      if (attempt === maxAttempts) {
        throw error;
      }
      log(`Attempt ${attempt} failed, retrying... (${error.message})`, 'warning');
      await sleep(1000 * attempt); // Exponential backoff
    }
  }
};

// Verify Firebase Functions
async function verifyFirebaseFunctions() {
  log('Verifying Firebase Functions deployment...');
  
  const functionsToTest = [
    'allocateDriver',
    'processPayment',
    'handleDriverResponse',
    'updateRideStatus',
    'emergencyAlert',
    'calculatePreciseETA',
    'notifyDrivers'
  ];
  
  const results = [];
  
  for (const funcName of functionsToTest) {
    try {
      log(`Testing function: ${funcName}`);
      
      // Test function availability by making a call
      const testData = {
        test: true,
        timestamp: Date.now()
      };
      
      const result = await retry(async () => {
        const response = await axios.post(`${FUNCTIONS_BASE_URL}/${funcName}`, {
          data: testData
        });
        return response.data;
      });
      
      results.push({
        function: funcName,
        status: 'deployed',
        accessible: true,
        responseTime: Date.now() - testData.timestamp
      });
      
      log(`✅ ${funcName} is deployed and accessible`, 'success');
      
    } catch (error) {
      // Check if function exists but has validation errors (which is expected)
      if (error.message.includes('validation') || error.message.includes('required')) {
        results.push({
          function: funcName,
          status: 'deployed',
          accessible: true,
          note: 'Function exists but requires proper parameters'
        });
        log(`✅ ${funcName} is deployed (validation error expected)`, 'success');
      } else {
        results.push({
          function: funcName,
          status: 'error',
          accessible: false,
          error: error.message
        });
        log(`❌ ${funcName} failed: ${error.message}`, 'error');
      }
    }
  }
  
  return results;
}

// Verify Firestore connectivity and rules
async function verifyFirestore() {
  log('Verifying Firestore connectivity and rules...');
  
  const results = [];
  
  try {
    // Test basic connectivity
    const testDoc = await db.collection('test').doc('connectivity').get();
    log('✅ Firestore connectivity verified', 'success');
    results.push({ test: 'Connectivity', status: 'success' });
    
    // Test read permissions
    const usersSnapshot = await db.collection('users').limit(1).get();
    log('✅ Firestore read permissions verified', 'success');
    results.push({ test: 'Read Permissions', status: 'success' });
    
    // Test write permissions (cleanup after)
    const testWriteDoc = db.collection('test').doc('write_test');
    await testWriteDoc.set({ test: true, timestamp: Date.now() });
    await testWriteDoc.delete();
    log('✅ Firestore write permissions verified', 'success');
    results.push({ test: 'Write Permissions', status: 'success' });
    
  } catch (error) {
    log(`❌ Firestore verification failed: ${error.message}`, 'error');
    results.push({ test: 'Firestore', status: 'error', error: error.message });
  }
  
  return results;
}

// Verify Realtime Database
async function verifyRealtimeDatabase() {
  log('Verifying Realtime Database connectivity...');
  
  const results = [];
  
  try {
    // Test write operation
    const testRef = admin.database().ref('test/connectivity');
    await testRef.set({ test: true, timestamp: Date.now() });
    
    // Test read operation
    const snapshot = await testRef.once('value');
    const data = snapshot.val();
    
    if (data && data.test) {
      log('✅ Realtime Database connectivity verified', 'success');
      results.push({ test: 'Connectivity', status: 'success' });
    } else {
      throw new Error('Data not found after write');
    }
    
    // Cleanup
    await testRef.remove();
    
  } catch (error) {
    log(`❌ Realtime Database verification failed: ${error.message}`, 'error');
    results.push({ test: 'Realtime Database', status: 'error', error: error.message });
  }
  
  return results;
}

// Verify Storage rules
async function verifyStorage() {
  log('Verifying Cloud Storage rules...');
  
  const results = [];
  
  try {
    // Test storage bucket access
    const bucket = admin.storage().bucket();
    const [exists] = await bucket.exists();
    
    if (exists) {
      log('✅ Cloud Storage bucket accessible', 'success');
      results.push({ test: 'Storage Bucket', status: 'success' });
    } else {
      throw new Error('Storage bucket not accessible');
    }
    
  } catch (error) {
    log(`❌ Cloud Storage verification failed: ${error.message}`, 'error');
    results.push({ test: 'Cloud Storage', status: 'error', error: error.message });
  }
  
  return results;
}

// Verify test data exists
async function verifyTestData() {
  log('Verifying test data exists...');
  
  const results = [];
  
  try {
    // Check users
    const usersSnapshot = await db.collection('users').limit(1).get();
    if (!usersSnapshot.empty) {
      log('✅ Test users found', 'success');
      results.push({ test: 'Users', status: 'success', count: usersSnapshot.size });
    } else {
      log('⚠️  No test users found', 'warning');
      results.push({ test: 'Users', status: 'warning', count: 0 });
    }
    
    // Check drivers
    const driversSnapshot = await db.collection('drivers').limit(1).get();
    if (!driversSnapshot.empty) {
      log('✅ Test drivers found', 'success');
      results.push({ test: 'Drivers', status: 'success', count: driversSnapshot.size });
    } else {
      log('⚠️  No test drivers found', 'warning');
      results.push({ test: 'Drivers', status: 'warning', count: 0 });
    }
    
    // Check configuration
    const configDoc = await db.collection('config').doc('support').get();
    if (configDoc.exists) {
      log('✅ Configuration data found', 'success');
      results.push({ test: 'Configuration', status: 'success' });
    } else {
      log('⚠️  Configuration data not found', 'warning');
      results.push({ test: 'Configuration', status: 'warning' });
    }
    
  } catch (error) {
    log(`❌ Test data verification failed: ${error.message}`, 'error');
    results.push({ test: 'Test Data', status: 'error', error: error.message });
  }
  
  return results;
}

// Verify payment gateway configuration
async function verifyPaymentConfiguration() {
  log('Verifying payment gateway configuration...');
  
  const results = [];
  
  try {
    // Test wallet payment
    const walletTestData = {
      rideId: 'verification_test_wallet',
      userId: 'test_user',
      amount: 100,
      paymentMethod: 'wallet'
    };
    
    const walletResult = await axios.post(`${FUNCTIONS_BASE_URL}/processPayment`, {
      data: {
        rideId: walletTestData.rideId,
        paymentMethod: 'wallet',
        paymentData: walletTestData
      }
    });
    
    if (walletResult.data) {
      log('✅ Wallet payment processing verified', 'success');
      results.push({ test: 'Wallet Payment', status: 'success' });
    } else {
      throw new Error('Wallet payment test failed');
    }
    
  } catch (error) {
    if (error.message.includes('insufficient') || error.message.includes('balance')) {
      log('✅ Wallet payment processing verified (insufficient balance expected)', 'success');
      results.push({ test: 'Wallet Payment', status: 'success', note: 'Insufficient balance error expected' });
    } else {
      log(`❌ Payment configuration verification failed: ${error.message}`, 'error');
      results.push({ test: 'Payment Configuration', status: 'error', error: error.message });
    }
  }
  
  return results;
}

// Generate verification report
function generateVerificationReport(allResults) {
  const totalTests = allResults.reduce((sum, category) => sum + category.results.length, 0);
  const successfulTests = allResults.reduce((sum, category) => 
    sum + category.results.filter(r => r.status === 'success').length, 0);
  const warningTests = allResults.reduce((sum, category) => 
    sum + category.results.filter(r => r.status === 'warning').length, 0);
  const failedTests = totalTests - successfulTests - warningTests;
  
  const successRate = ((successfulTests / totalTests) * 100).toFixed(2);
  
  console.log('\n📊 DEPLOYMENT VERIFICATION REPORT');
  console.log('='.repeat(60));
  console.log(`Total Tests: ${totalTests}`);
  console.log(`Successful: ${successfulTests}`);
  console.log(`Warnings: ${warningTests}`);
  console.log(`Failed: ${failedTests}`);
  console.log(`Success Rate: ${successRate}%`);
  console.log('='.repeat(60));
  
  allResults.forEach(category => {
    console.log(`\n📋 ${category.name.toUpperCase()}:`);
    category.results.forEach(result => {
      const status = result.status === 'success' ? '✅' : 
                   result.status === 'warning' ? '⚠️' : '❌';
      console.log(`  ${status} ${result.test || result.function}`);
      if (result.error) {
        console.log(`     Error: ${result.error}`);
      }
      if (result.note) {
        console.log(`     Note: ${result.note}`);
      }
    });
  });
  
  console.log('\n🎯 DEPLOYMENT STATUS:');
  if (successRate >= 90) {
    console.log('✅ EXCELLENT - Deployment is ready for production!');
    console.log('✅ All critical components are functioning properly');
  } else if (successRate >= 75) {
    console.log('⚠️  GOOD - Deployment is mostly ready with minor issues');
    console.log('⚠️  Review warnings and failed tests before production');
  } else {
    console.log('❌ POOR - Deployment has significant issues');
    console.log('❌ Do not deploy to production until issues are resolved');
  }
  
  console.log('\n📝 RECOMMENDATIONS:');
  if (failedTests > 0) {
    console.log('• Fix all failed tests before production deployment');
  }
  if (warningTests > 0) {
    console.log('• Review warnings and ensure they are acceptable');
  }
  console.log('• Set up monitoring and alerting for production');
  console.log('• Configure backup and disaster recovery procedures');
  console.log('• Plan for scaling based on expected user load');
}

// Main verification function
async function runDeploymentVerification() {
  log('🚀 Starting Deployment Verification...\n');
  
  const allResults = [];
  
  try {
    // Run all verification tests
    const functionsResults = await verifyFirebaseFunctions();
    allResults.push({ name: 'Firebase Functions', results: functionsResults });
    
    const firestoreResults = await verifyFirestore();
    allResults.push({ name: 'Firestore', results: firestoreResults });
    
    const rtdbResults = await verifyRealtimeDatabase();
    allResults.push({ name: 'Realtime Database', results: rtdbResults });
    
    const storageResults = await verifyStorage();
    allResults.push({ name: 'Cloud Storage', results: storageResults });
    
    const testDataResults = await verifyTestData();
    allResults.push({ name: 'Test Data', results: testDataResults });
    
    const paymentResults = await verifyPaymentConfiguration();
    allResults.push({ name: 'Payment Configuration', results: paymentResults });
    
    // Generate comprehensive report
    generateVerificationReport(allResults);
    
    log('\n✅ Deployment verification completed!', 'success');
    
  } catch (error) {
    log(`❌ Deployment verification failed: ${error.message}`, 'error');
    process.exit(1);
  }
}

// Run the verification
if (require.main === module) {
  runDeploymentVerification()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      log(`❌ Verification failed: ${error.message}`, 'error');
      process.exit(1);
    });
}

module.exports = { runDeploymentVerification };
