#!/usr/bin/env node

/**
 * Daxido Payment Flow Testing Script
 * This script tests the complete payment flow end-to-end
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

// Test configuration
const TEST_CONFIG = {
  BASE_URL: 'https://us-central1-daxido-native.cloudfunctions.net',
  TEST_USER_ID: 'test_user_001',
  TEST_DRIVER_ID: 'test_driver_001',
  TEST_RIDE_ID: 'test_ride_001'
};

// Test scenarios
const PAYMENT_SCENARIOS = [
  {
    name: 'Wallet Payment - Sufficient Balance',
    paymentMethod: 'wallet',
    amount: 150,
    expectedResult: 'success'
  },
  {
    name: 'Wallet Payment - Insufficient Balance',
    paymentMethod: 'wallet',
    amount: 10000,
    expectedResult: 'failure'
  },
  {
    name: 'Razorpay Payment - Valid Amount',
    paymentMethod: 'razorpay',
    amount: 200,
    expectedResult: 'success'
  },
  {
    name: 'Stripe Payment - Valid Amount',
    paymentMethod: 'stripe',
    amount: 300,
    expectedResult: 'success'
  },
  {
    name: 'Cash Payment',
    paymentMethod: 'cash',
    amount: 250,
    expectedResult: 'success'
  }
];

// Utility functions
const log = (message, type = 'info') => {
  const timestamp = new Date().toISOString();
  const prefix = type === 'error' ? '❌' : type === 'success' ? '✅' : 'ℹ️';
  console.log(`${prefix} [${timestamp}] ${message}`);
};

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Test setup
async function setupTestEnvironment() {
  log('Setting up test environment...');
  
  try {
    // Create test user
    await db.collection('users').doc(TEST_CONFIG.TEST_USER_ID).set({
      uid: TEST_CONFIG.TEST_USER_ID,
      email: 'testuser@daxido.com',
      phoneNumber: '+919876543210',
      displayName: 'Test User',
      isVerified: true,
      createdAt: admin.firestore.Timestamp.now()
    });
    
    // Create test wallet with sufficient balance
    await db.collection('wallets').doc(TEST_CONFIG.TEST_USER_ID).set({
      balance: 5000,
      lastUpdated: admin.firestore.Timestamp.now()
    });
    
    // Create test driver
    await db.collection('drivers').doc(TEST_CONFIG.TEST_DRIVER_ID).set({
      uid: TEST_CONFIG.TEST_DRIVER_ID,
      email: 'testdriver@daxido.com',
      phoneNumber: '+919876543211',
      displayName: 'Test Driver',
      isVerified: true,
      isOnline: true,
      isAvailable: true,
      createdAt: admin.firestore.Timestamp.now()
    });
    
    // Create test ride
    await db.collection('rides').doc(TEST_CONFIG.TEST_RIDE_ID).set({
      rideId: TEST_CONFIG.TEST_RIDE_ID,
      userId: TEST_CONFIG.TEST_USER_ID,
      driverId: TEST_CONFIG.TEST_DRIVER_ID,
      status: 'PENDING',
      fare: 200,
      createdAt: admin.firestore.Timestamp.now()
    });
    
    log('Test environment setup completed', 'success');
  } catch (error) {
    log(`Failed to setup test environment: ${error.message}`, 'error');
    throw error;
  }
}

// Test Firebase Functions
async function testFirebaseFunction(functionName, data) {
  try {
    log(`Testing Firebase function: ${functionName}`);
    
    const response = await axios.post(`${FUNCTIONS_BASE_URL}/${functionName}`, {
      data: data
    });
    
    log(`Function ${functionName} executed successfully`, 'success');
    return { success: true, data: response.data };
  } catch (error) {
    log(`Function ${functionName} failed: ${JSON.stringify(error.response?.data || error.message)}`, 'error');
    return { success: false, error: error.response?.data || error.message };
  }
}

// Test payment processing
async function testPaymentProcessing(scenario) {
  log(`Testing scenario: ${scenario.name}`);
  
  const paymentData = {
    rideId: TEST_CONFIG.TEST_RIDE_ID,
    userId: TEST_CONFIG.TEST_USER_ID,
    amount: scenario.amount,
    paymentMethod: scenario.paymentMethod
  };
  
  const result = await testFirebaseFunction('processPayment', {
    rideId: TEST_CONFIG.TEST_RIDE_ID,
    paymentMethod: scenario.paymentMethod,
    paymentData: paymentData
  });
  
  // Verify result matches expectation
  const isSuccess = result.success && result.data?.result?.success;
  const matchesExpectation = (scenario.expectedResult === 'success') === isSuccess;
  
  if (matchesExpectation) {
    log(`✅ ${scenario.name} - PASSED`, 'success');
  } else {
    log(`❌ ${scenario.name} - FAILED (Expected: ${scenario.expectedResult}, Got: ${isSuccess ? 'success' : 'failure'})`, 'error');
  }
  
  return {
    scenario: scenario.name,
    passed: matchesExpectation,
    result: result
  };
}

// Test wallet operations
async function testWalletOperations() {
  log('Testing wallet operations...');
  
  const results = [];
  
  // Test wallet balance check
  try {
    const walletDoc = await db.collection('wallets').doc(TEST_CONFIG.TEST_USER_ID).get();
    const balance = walletDoc.data()?.balance || 0;
    
    if (balance > 0) {
      log('✅ Wallet balance check - PASSED', 'success');
      results.push({ test: 'Wallet balance check', passed: true });
    } else {
      log('❌ Wallet balance check - FAILED', 'error');
      results.push({ test: 'Wallet balance check', passed: false });
    }
  } catch (error) {
    log(`❌ Wallet balance check failed: ${error.message}`, 'error');
    results.push({ test: 'Wallet balance check', passed: false });
  }
  
  // Test wallet transaction recording
  try {
    const transactionId = `TXN_TEST_${Date.now()}`;
    await db.collection('transactions').doc(transactionId).set({
      transactionId: transactionId,
      userId: TEST_CONFIG.TEST_USER_ID,
      amount: 100,
      type: 'WALLET_RECHARGE',
      status: 'COMPLETED',
      timestamp: admin.firestore.Timestamp.now()
    });
    
    log('✅ Wallet transaction recording - PASSED', 'success');
    results.push({ test: 'Wallet transaction recording', passed: true });
  } catch (error) {
    log(`❌ Wallet transaction recording failed: ${error.message}`, 'error');
    results.push({ test: 'Wallet transaction recording', passed: false });
  }
  
  return results;
}

// Test ride allocation
async function testRideAllocation() {
  log('Testing ride allocation...');
  
  const rideRequest = {
    userId: TEST_CONFIG.TEST_USER_ID,
    pickupLocation: {
      latitude: 12.9716,
      longitude: 77.5946,
      address: 'Test Pickup Location'
    },
    dropoffLocation: {
      latitude: 12.9356,
      longitude: 77.6068,
      address: 'Test Dropoff Location'
    },
    vehicleType: 'CAR',
    fare: 200
  };
  
  const result = await testFirebaseFunction('allocateDriver', { rideRequest });
  
  if (result.success) {
    log('✅ Ride allocation - PASSED', 'success');
    return { test: 'Ride allocation', passed: true };
  } else {
    log('❌ Ride allocation - FAILED', 'error');
    return { test: 'Ride allocation', passed: false };
  }
}

// Test driver response handling
async function testDriverResponse() {
  log('Testing driver response handling...');
  
  const responseData = {
    rideId: TEST_CONFIG.TEST_RIDE_ID,
    driverId: TEST_CONFIG.TEST_DRIVER_ID,
    response: 'accepted',
    estimatedArrival: 5
  };
  
  const result = await testFirebaseFunction('handleDriverResponse', responseData);
  
  if (result.success) {
    log('✅ Driver response handling - PASSED', 'success');
    return { test: 'Driver response handling', passed: true };
  } else {
    log('❌ Driver response handling - FAILED', 'error');
    return { test: 'Driver response handling', passed: false };
  }
}

// Test ride status updates
async function testRideStatusUpdates() {
  log('Testing ride status updates...');
  
  const statusUpdates = [
    { status: 'started', additionalData: { driverId: TEST_CONFIG.TEST_DRIVER_ID } },
    { status: 'in_progress', additionalData: { currentLocation: { latitude: 12.9716, longitude: 77.5946 } } },
    { status: 'completed', additionalData: { fare: 200 } }
  ];
  
  const results = [];
  
  for (const update of statusUpdates) {
    const result = await testFirebaseFunction('updateRideStatus', {
      rideId: TEST_CONFIG.TEST_RIDE_ID,
      status: update.status,
      additionalData: update.additionalData
    });
    
    if (result.success) {
      log(`✅ Ride status update (${update.status}) - PASSED`, 'success');
      results.push({ test: `Ride status update (${update.status})`, passed: true });
    } else {
      log(`❌ Ride status update (${update.status}) - FAILED`, 'error');
      results.push({ test: `Ride status update (${update.status})`, passed: false });
    }
    
    await sleep(1000); // Wait between updates
  }
  
  return results;
}

// Test emergency alert
async function testEmergencyAlert() {
  log('Testing emergency alert...');
  
  const emergencyData = {
    rideId: TEST_CONFIG.TEST_RIDE_ID,
    userId: TEST_CONFIG.TEST_USER_ID,
    driverId: TEST_CONFIG.TEST_DRIVER_ID,
    location: { latitude: 12.9716, longitude: 77.5946 },
    emergencyType: 'safety_concern'
  };
  
  const result = await testFirebaseFunction('emergencyAlert', emergencyData);
  
  if (result.success) {
    log('✅ Emergency alert - PASSED', 'success');
    return { test: 'Emergency alert', passed: true };
  } else {
    log('❌ Emergency alert - FAILED', 'error');
    return { test: 'Emergency alert', passed: false };
  }
}

// Generate test report
function generateTestReport(results) {
  const totalTests = results.length;
  const passedTests = results.filter(r => r.passed).length;
  const failedTests = totalTests - passedTests;
  const successRate = ((passedTests / totalTests) * 100).toFixed(2);
  
  console.log('\n📊 PAYMENT FLOW TEST REPORT');
  console.log('='.repeat(50));
  console.log(`Total Tests: ${totalTests}`);
  console.log(`Passed: ${passedTests}`);
  console.log(`Failed: ${failedTests}`);
  console.log(`Success Rate: ${successRate}%`);
  console.log('='.repeat(50));
  
  console.log('\n📋 DETAILED RESULTS:');
  results.forEach(result => {
    const status = result.passed ? '✅ PASS' : '❌ FAIL';
    console.log(`${status} ${result.test || result.scenario}`);
  });
  
  if (failedTests > 0) {
    console.log('\n⚠️  FAILED TESTS:');
    results.filter(r => !r.passed).forEach(result => {
      console.log(`❌ ${result.test || result.scenario}`);
      if (result.result?.error) {
        console.log(`   Error: ${result.result.error}`);
      }
    });
  }
  
  console.log('\n🎯 RECOMMENDATIONS:');
  if (successRate >= 90) {
    console.log('✅ Payment flow is working excellently!');
    console.log('✅ Ready for production deployment');
  } else if (successRate >= 70) {
    console.log('⚠️  Payment flow has some issues that need attention');
    console.log('⚠️  Review failed tests before production deployment');
  } else {
    console.log('❌ Payment flow has significant issues');
    console.log('❌ Do not deploy to production until issues are resolved');
  }
}

// Main test execution
async function runPaymentFlowTests() {
  log('🚀 Starting Payment Flow Tests...\n');
  
  const allResults = [];
  
  try {
    // Setup test environment
    await setupTestEnvironment();
    
    // Test wallet operations
    const walletResults = await testWalletOperations();
    allResults.push(...walletResults);
    
    // Test payment processing scenarios
    for (const scenario of PAYMENT_SCENARIOS) {
      const result = await testPaymentProcessing(scenario);
      allResults.push(result);
      await sleep(1000); // Wait between tests
    }
    
    // Test ride allocation
    const allocationResult = await testRideAllocation();
    allResults.push(allocationResult);
    
    // Test driver response
    const responseResult = await testDriverResponse();
    allResults.push(responseResult);
    
    // Test ride status updates
    const statusResults = await testRideStatusUpdates();
    allResults.push(...statusResults);
    
    // Test emergency alert
    const emergencyResult = await testEmergencyAlert();
    allResults.push(emergencyResult);
    
    // Generate test report
    generateTestReport(allResults);
    
  } catch (error) {
    log(`❌ Test execution failed: ${error.message}`, 'error');
    process.exit(1);
  }
}

// Run the tests
if (require.main === module) {
  runPaymentFlowTests()
    .then(() => {
      log('\n✅ Payment flow tests completed!', 'success');
      process.exit(0);
    })
    .catch((error) => {
      log(`❌ Payment flow tests failed: ${error.message}`, 'error');
      process.exit(1);
    });
}

module.exports = { runPaymentFlowTests };
