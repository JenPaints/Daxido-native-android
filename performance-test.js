#!/usr/bin/env node

/**
 * Daxido Performance Testing Script
 * This script performs load testing and performance analysis
 */

const admin = require('firebase-admin');
const axios = require('axios');
const { performance } = require('perf_hooks');

// Initialize Firebase Admin with Application Default Credentials
admin.initializeApp({
  projectId: "daxido-native",
  databaseURL: "https://daxido-native-default-rtdb.firebaseio.com"
});

const db = admin.firestore();

// Firebase Functions base URL (using local emulator)
const FUNCTIONS_BASE_URL = 'http://127.0.0.1:5001/daxido-native/us-central1';

// Performance test configuration
const PERFORMANCE_CONFIG = {
  CONCURRENT_USERS: 50,
  REQUESTS_PER_USER: 10,
  TEST_DURATION_MS: 60000, // 1 minute
  BASE_URL: 'https://us-central1-daxido-native.cloudfunctions.net',
  TIMEOUT_MS: 30000
};

// Performance metrics
let metrics = {
  totalRequests: 0,
  successfulRequests: 0,
  failedRequests: 0,
  responseTimes: [],
  errors: [],
  startTime: null,
  endTime: null
};

// Utility functions
const log = (message, type = 'info') => {
  const timestamp = new Date().toISOString();
  const prefix = type === 'error' ? '❌' : type === 'success' ? '✅' : 'ℹ️';
  console.log(`${prefix} [${timestamp}] ${message}`);
};

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Performance measurement
const measurePerformance = async (operation, operationName) => {
  const startTime = performance.now();
  try {
    const result = await operation();
    const endTime = performance.now();
    const responseTime = endTime - startTime;
    
    metrics.totalRequests++;
    metrics.successfulRequests++;
    metrics.responseTimes.push(responseTime);
    
    return { success: true, result, responseTime };
  } catch (error) {
    const endTime = performance.now();
    const responseTime = endTime - startTime;
    
    metrics.totalRequests++;
    metrics.failedRequests++;
    metrics.responseTimes.push(responseTime);
    metrics.errors.push({ operation: operationName, error: error.message, timestamp: new Date() });
    
    return { success: false, error: error.message, responseTime };
  }
};

// Test operations
const testFirestoreRead = async () => {
  const userId = `test_user_${Math.floor(Math.random() * 1000)}`;
  const doc = await db.collection('users').doc(userId).get();
  return doc.exists;
};

const testFirestoreWrite = async () => {
  const userId = `perf_test_user_${Date.now()}_${Math.random()}`;
  await db.collection('users').doc(userId).set({
    uid: userId,
    email: `${userId}@daxido.com`,
    createdAt: admin.firestore.Timestamp.now()
  });
  return userId;
};

const testFirestoreQuery = async () => {
  const snapshot = await db.collection('users')
    .limit(10)
    .get();
  return snapshot.docs.length;
};

const testFirebaseFunction = async () => {
  const rideRequest = {
    userId: `perf_user_${Math.floor(Math.random() * 1000)}`,
    pickupLocation: {
      latitude: 12.9716 + (Math.random() - 0.5) * 0.1,
      longitude: 77.5946 + (Math.random() - 0.5) * 0.1,
      address: 'Performance Test Location'
    },
    dropoffLocation: {
      latitude: 12.9356 + (Math.random() - 0.5) * 0.1,
      longitude: 77.6068 + (Math.random() - 0.5) * 0.1,
      address: 'Performance Test Destination'
    },
    vehicleType: 'CAR',
    fare: Math.floor(Math.random() * 500) + 100
  };
  
  const response = await axios.post(`${FUNCTIONS_BASE_URL}/allocateDriver`, {
    data: { rideRequest }
  });
  return response.data;
};

const testPaymentProcessing = async () => {
  const paymentData = {
    rideId: `perf_ride_${Date.now()}`,
    userId: `perf_user_${Math.floor(Math.random() * 1000)}`,
    amount: Math.floor(Math.random() * 500) + 100,
    paymentMethod: 'wallet'
  };
  
  const response = await axios.post(`${FUNCTIONS_BASE_URL}/processPayment`, {
    data: {
      rideId: paymentData.rideId,
      paymentMethod: 'wallet',
      paymentData: paymentData
    }
  });
  return response.data;
};

const testRealtimeDatabaseWrite = async () => {
  const driverId = `perf_driver_${Math.floor(Math.random() * 1000)}`;
  const locationData = {
    latitude: 12.9716 + (Math.random() - 0.5) * 0.1,
    longitude: 77.5946 + (Math.random() - 0.5) * 0.1,
    timestamp: Date.now()
  };
  
  await admin.database().ref(`driver_locations/${driverId}`).set(locationData);
  return driverId;
};

const testRealtimeDatabaseRead = async () => {
  const snapshot = await admin.database().ref('driver_locations').limitToFirst(5).once('value');
  return snapshot.val();
};

// Load test scenarios
const LOAD_TEST_SCENARIOS = [
  {
    name: 'Firestore Read Operations',
    operation: testFirestoreRead,
    weight: 0.3
  },
  {
    name: 'Firestore Write Operations',
    operation: testFirestoreWrite,
    weight: 0.2
  },
  {
    name: 'Firestore Query Operations',
    operation: testFirestoreQuery,
    weight: 0.2
  },
  {
    name: 'Firebase Function Calls',
    operation: testFirebaseFunction,
    weight: 0.15
  },
  {
    name: 'Payment Processing',
    operation: testPaymentProcessing,
    weight: 0.1
  },
  {
    name: 'Realtime Database Write',
    operation: testRealtimeDatabaseWrite,
    weight: 0.03
  },
  {
    name: 'Realtime Database Read',
    operation: testRealtimeDatabaseRead,
    weight: 0.02
  }
];

// Simulate concurrent users
async function simulateUser(userId, duration) {
  const userMetrics = {
    userId: userId,
    requests: 0,
    successfulRequests: 0,
    failedRequests: 0,
    responseTimes: [],
    errors: []
  };
  
  const endTime = Date.now() + duration;
  
  while (Date.now() < endTime) {
    // Select operation based on weights
    const random = Math.random();
    let cumulativeWeight = 0;
    let selectedScenario = null;
    
    for (const scenario of LOAD_TEST_SCENARIOS) {
      cumulativeWeight += scenario.weight;
      if (random <= cumulativeWeight) {
        selectedScenario = scenario;
        break;
      }
    }
    
    if (selectedScenario) {
      const result = await measurePerformance(
        selectedScenario.operation,
        selectedScenario.name
      );
      
      userMetrics.requests++;
      if (result.success) {
        userMetrics.successfulRequests++;
      } else {
        userMetrics.failedRequests++;
        userMetrics.errors.push({
          operation: selectedScenario.name,
          error: result.error,
          timestamp: new Date()
        });
      }
      userMetrics.responseTimes.push(result.responseTime);
    }
    
    // Random delay between requests (100-500ms)
    await sleep(Math.floor(Math.random() * 400) + 100);
  }
  
  return userMetrics;
}

// Run load test
async function runLoadTest() {
  log('🚀 Starting Performance Load Test...\n');
  
  metrics.startTime = Date.now();
  
  try {
    // Create concurrent user simulations
    const userPromises = [];
    
    for (let i = 0; i < PERFORMANCE_CONFIG.CONCURRENT_USERS; i++) {
      const userPromise = simulateUser(`user_${i}`, PERFORMANCE_CONFIG.TEST_DURATION_MS);
      userPromises.push(userPromise);
    }
    
    log(`Simulating ${PERFORMANCE_CONFIG.CONCURRENT_USERS} concurrent users for ${PERFORMANCE_CONFIG.TEST_DURATION_MS / 1000} seconds...`);
    
    // Wait for all users to complete
    const userResults = await Promise.all(userPromises);
    
    metrics.endTime = Date.now();
    
    // Aggregate user metrics
    const aggregatedMetrics = {
      totalUsers: userResults.length,
      totalRequests: userResults.reduce((sum, user) => sum + user.requests, 0),
      totalSuccessfulRequests: userResults.reduce((sum, user) => sum + user.successfulRequests, 0),
      totalFailedRequests: userResults.reduce((sum, user) => sum + user.failedRequests, 0),
      allResponseTimes: userResults.flatMap(user => user.responseTimes),
      allErrors: userResults.flatMap(user => user.errors)
    };
    
    return aggregatedMetrics;
    
  } catch (error) {
    log(`❌ Load test failed: ${error.message}`, 'error');
    throw error;
  }
}

// Calculate performance statistics
function calculateStats(responseTimes) {
  if (responseTimes.length === 0) return null;
  
  const sorted = responseTimes.sort((a, b) => a - b);
  const sum = responseTimes.reduce((a, b) => a + b, 0);
  
  return {
    count: responseTimes.length,
    min: sorted[0],
    max: sorted[sorted.length - 1],
    avg: sum / responseTimes.length,
    median: sorted[Math.floor(sorted.length / 2)],
    p95: sorted[Math.floor(sorted.length * 0.95)],
    p99: sorted[Math.floor(sorted.length * 0.99)]
  };
}

// Generate performance report
function generatePerformanceReport(metrics) {
  const duration = (metrics.endTime - metrics.startTime) / 1000; // seconds
  const requestsPerSecond = metrics.totalRequests / duration;
  const successRate = ((metrics.successfulRequests / metrics.totalRequests) * 100).toFixed(2);
  
  const responseTimeStats = calculateStats(metrics.responseTimes);
  
  console.log('\n📊 PERFORMANCE TEST REPORT');
  console.log('='.repeat(60));
  console.log(`Test Duration: ${duration.toFixed(2)} seconds`);
  console.log(`Concurrent Users: ${PERFORMANCE_CONFIG.CONCURRENT_USERS}`);
  console.log(`Total Requests: ${metrics.totalRequests}`);
  console.log(`Successful Requests: ${metrics.successfulRequests}`);
  console.log(`Failed Requests: ${metrics.failedRequests}`);
  console.log(`Success Rate: ${successRate}%`);
  console.log(`Requests per Second: ${requestsPerSecond.toFixed(2)}`);
  console.log('='.repeat(60));
  
  if (responseTimeStats) {
    console.log('\n⏱️  RESPONSE TIME STATISTICS:');
    console.log(`Average Response Time: ${responseTimeStats.avg.toFixed(2)}ms`);
    console.log(`Median Response Time: ${responseTimeStats.median.toFixed(2)}ms`);
    console.log(`95th Percentile: ${responseTimeStats.p95.toFixed(2)}ms`);
    console.log(`99th Percentile: ${responseTimeStats.p99.toFixed(2)}ms`);
    console.log(`Min Response Time: ${responseTimeStats.min.toFixed(2)}ms`);
    console.log(`Max Response Time: ${responseTimeStats.max.toFixed(2)}ms`);
  }
  
  // Error analysis
  if (metrics.errors.length > 0) {
    console.log('\n❌ ERROR ANALYSIS:');
    const errorGroups = {};
    metrics.errors.forEach(error => {
      const key = `${error.operation}: ${error.error}`;
      errorGroups[key] = (errorGroups[key] || 0) + 1;
    });
    
    Object.entries(errorGroups).forEach(([error, count]) => {
      console.log(`${error} (${count} occurrences)`);
    });
  }
  
  // Performance recommendations
  console.log('\n🎯 PERFORMANCE RECOMMENDATIONS:');
  
  if (successRate >= 95) {
    console.log('✅ Excellent success rate!');
  } else if (successRate >= 90) {
    console.log('⚠️  Good success rate, but monitor for improvements');
  } else {
    console.log('❌ Poor success rate, investigate and fix issues');
  }
  
  if (responseTimeStats) {
    if (responseTimeStats.avg < 1000) {
      console.log('✅ Excellent average response time');
    } else if (responseTimeStats.avg < 3000) {
      console.log('⚠️  Acceptable response time, consider optimization');
    } else {
      console.log('❌ Poor response time, optimization required');
    }
    
    if (responseTimeStats.p95 < 2000) {
      console.log('✅ Good 95th percentile response time');
    } else if (responseTimeStats.p95 < 5000) {
      console.log('⚠️  Acceptable 95th percentile, monitor closely');
    } else {
      console.log('❌ Poor 95th percentile, optimization required');
    }
  }
  
  if (requestsPerSecond > 100) {
    console.log('✅ High throughput achieved');
  } else if (requestsPerSecond > 50) {
    console.log('⚠️  Moderate throughput, consider scaling');
  } else {
    console.log('❌ Low throughput, scaling required');
  }
  
  // Database performance recommendations
  console.log('\n💾 DATABASE OPTIMIZATION RECOMMENDATIONS:');
  console.log('• Ensure proper Firestore indexes are created');
  console.log('• Use composite indexes for complex queries');
  console.log('• Implement caching for frequently accessed data');
  console.log('• Consider using Firestore offline persistence');
  console.log('• Monitor Firestore usage quotas');
  
  // Firebase Functions recommendations
  console.log('\n⚡ FIREBASE FUNCTIONS OPTIMIZATION:');
  console.log('• Use connection pooling for external APIs');
  console.log('• Implement proper error handling and retries');
  console.log('• Consider using Cloud Run for CPU-intensive tasks');
  console.log('• Monitor function execution time and memory usage');
  console.log('• Use Firebase Functions emulator for local testing');
}

// Stress test specific operations
async function runStressTest() {
  log('🔥 Starting Stress Test...\n');
  
  const stressTestOperations = [
    { name: 'High-frequency Firestore writes', operation: testFirestoreWrite, iterations: 100 },
    { name: 'Concurrent payment processing', operation: testPaymentProcessing, iterations: 50 },
    { name: 'Rapid ride allocation', operation: testFirebaseFunction, iterations: 30 }
  ];
  
  const stressResults = [];
  
  for (const test of stressTestOperations) {
    log(`Running stress test: ${test.name}`);
    
    const startTime = performance.now();
    const promises = [];
    
    for (let i = 0; i < test.iterations; i++) {
      promises.push(measurePerformance(test.operation, test.name));
    }
    
    const results = await Promise.all(promises);
    const endTime = performance.now();
    
    const successful = results.filter(r => r.success).length;
    const failed = results.filter(r => !r.success).length;
    const avgResponseTime = results.reduce((sum, r) => sum + r.responseTime, 0) / results.length;
    
    stressResults.push({
      test: test.name,
      iterations: test.iterations,
      successful: successful,
      failed: failed,
      successRate: (successful / test.iterations * 100).toFixed(2),
      avgResponseTime: avgResponseTime.toFixed(2),
      duration: (endTime - startTime).toFixed(2)
    });
    
    log(`✅ ${test.name} completed: ${successful}/${test.iterations} successful`, 'success');
  }
  
  console.log('\n🔥 STRESS TEST RESULTS:');
  console.log('='.repeat(60));
  stressResults.forEach(result => {
    console.log(`\n${result.test}:`);
    console.log(`  Iterations: ${result.iterations}`);
    console.log(`  Success Rate: ${result.successRate}%`);
    console.log(`  Avg Response Time: ${result.avgResponseTime}ms`);
    console.log(`  Duration: ${result.duration}ms`);
  });
  
  return stressResults;
}

// Main performance testing function
async function runPerformanceTests() {
  log('🚀 Starting Comprehensive Performance Tests...\n');
  
  try {
    // Run load test
    const loadTestMetrics = await runLoadTest();
    
    // Run stress test
    const stressTestResults = await runStressTest();
    
    // Generate comprehensive report
    generatePerformanceReport(loadTestMetrics);
    
    log('\n✅ Performance tests completed successfully!', 'success');
    
  } catch (error) {
    log(`❌ Performance tests failed: ${error.message}`, 'error');
    process.exit(1);
  }
}

// Run the performance tests
if (require.main === module) {
  runPerformanceTests()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      log(`❌ Performance tests failed: ${error.message}`, 'error');
      process.exit(1);
    });
}

module.exports = { runPerformanceTests };
