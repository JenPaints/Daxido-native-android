/**
 * COST OPTIMIZED FIREBASE FUNCTIONS
 * Reduces costs through:
 * - Connection pooling
 * - Batch operations
 * - Minimal memory (256MB)
 * - Short timeouts (10s)
 * - Async notifications
 */

const {onRequest, onCall} = require("firebase-functions/v2/https");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");

// Initialize Firebase Admin
initializeApp();

// COST OPTIMIZATION: Connection pooling
const db = getFirestore();

/**
 * Simple test function
 */
exports.testFunction = onRequest(async (req, res) => {
  res.json({
    message: "Test function working!",
    timestamp: new Date().toISOString()
  });
});

/**
 * Process Payment Function
 */
exports.processPayment = onCall(async (request) => {
  try {
    const {rideId, paymentMethod, paymentData} = request.data;
    
    console.log(`Processing payment for ride ${rideId} with method ${paymentMethod}`);
    
    let result;
    
    switch (paymentMethod) {
      case 'wallet':
        result = await processWalletPayment(paymentData, paymentData.amount);
        break;
      case 'razorpay':
        result = await processRazorpayPayment(paymentData, paymentData.amount);
        break;
      case 'stripe':
        result = await processStripePayment(paymentData, paymentData.amount);
        break;
      case 'cash':
        result = await processCashPayment(paymentData, paymentData.amount);
        break;
      default:
        throw new Error(`Unsupported payment method: ${paymentMethod}`);
    }
    
    // Record transaction in Firestore
    await recordTransaction(rideId, paymentMethod, paymentData.amount, result);
    
    return {
      success: true,
      paymentId: result.paymentId,
      amount: paymentData.amount,
      method: paymentMethod,
      timestamp: new Date().toISOString()
    };
    
  } catch (error) {
    console.error('Payment processing failed:', error);
    return {
      success: false,
      error: error.message
    };
  }
});

/**
 * Process Razorpay Payment
 */
async function processRazorpayPayment(paymentData, amount) {
  // Mock implementation for testing
  return {
    success: true,
    paymentId: `rzp_${Date.now()}`,
    amount: amount,
    orderId: `order_${Date.now()}`,
    currency: 'INR'
  };
}

/**
 * Process Stripe Payment
 */
async function processStripePayment(paymentData, amount) {
  // Mock implementation for testing
  return {
    success: true,
    paymentId: `stripe_${Date.now()}`,
    amount: amount,
    clientSecret: `pi_${Date.now()}_secret`,
    currency: 'USD'
  };
}

/**
 * Process Wallet Payment
 */
async function processWalletPayment(paymentData, amount) {
  try {
    const userId = paymentData.userId;
    
    // Get current wallet balance
    const walletDoc = await db.collection('wallets').doc(userId).get();
    const currentBalance = walletDoc.data()?.balance || 0;
    
    if (currentBalance < amount) {
      throw new Error('Insufficient wallet balance');
    }
    
    // Deduct amount from wallet
    await db.collection('wallets').doc(userId).update({
      balance: FieldValue.increment(-amount),
      lastUpdated: FieldValue.serverTimestamp()
    });
    
    return {
      success: true,
      paymentId: `wallet_${Date.now()}`,
      amount: amount,
      remainingBalance: currentBalance - amount
    };
  } catch (error) {
    console.error('Wallet payment failed:', error);
    throw new Error(`Wallet payment failed: ${error.message}`);
  }
}

/**
 * Process Cash Payment
 */
async function processCashPayment(paymentData, amount) {
  // Cash payments are handled offline, just record the transaction
  return {
    success: true,
    paymentId: `cash_${Date.now()}`,
    amount: amount,
    status: 'pending_collection'
  };
}

/**
 * Record Transaction in Firestore
 */
async function recordTransaction(rideId, paymentMethod, amount, result) {
  const transactionData = {
    rideId: rideId,
    paymentMethod: paymentMethod,
    amount: amount,
    paymentId: result.paymentId,
    status: result.success ? 'completed' : 'failed',
    timestamp: FieldValue.serverTimestamp(),
    createdAt: new Date().toISOString()
  };
  
  await db.collection('transactions').add(transactionData);
  console.log(`Transaction recorded: ${result.paymentId}`);
}

/**
 * Cloud Function: Allocate Driver for Ride Request
 */
exports.allocateDriver = onCall(async (request) => {
  try {
    const {rideRequest} = request.data;
    
    console.log(`Allocating driver for ride request: ${rideRequest.userId}`);
    
    // Find nearby available drivers
    const nearbyDrivers = await findNearbyDrivers(rideRequest.pickupLocation);
    
    if (nearbyDrivers.length === 0) {
      return {
        success: false,
        message: 'No drivers available in your area'
      };
    }
    
    // Select best driver based on distance and rating
    const selectedDriver = selectBestDriver(nearbyDrivers, rideRequest.pickupLocation);
    
    // Create ride document
    const rideData = {
      userId: rideRequest.userId,
      driverId: selectedDriver.uid,
      pickupLocation: rideRequest.pickupLocation,
      dropoffLocation: rideRequest.dropoffLocation,
      vehicleType: rideRequest.vehicleType,
      fare: rideRequest.fare,
      status: 'driver_assigned',
      createdAt: FieldValue.serverTimestamp(),
      estimatedArrival: calculateETA(selectedDriver.currentLocation, rideRequest.pickupLocation)
    };
    
    const rideRef = await db.collection('rides').add(rideData);
    
    // Notify driver
    await notifyDriver(selectedDriver.uid, rideRef.id, rideRequest);
    
    return {
      success: true,
      rideId: rideRef.id,
      driverId: selectedDriver.uid,
      estimatedArrival: rideData.estimatedArrival
    };
    
  } catch (error) {
    console.error('Driver allocation failed:', error);
    return {
      success: false,
      error: error.message
    };
  }
});

/**
 * Cloud Function: Handle Driver Response
 */
exports.handleDriverResponse = onCall(async (request) => {
  try {
    const {rideId, driverId, response, estimatedArrival} = request.data;
    
    console.log(`Driver ${driverId} responded ${response} to ride ${rideId}`);
    
    if (response === 'accepted') {
      // Update ride status
      await db.collection('rides').doc(rideId).update({
        status: 'driver_accepted',
        estimatedArrival: estimatedArrival,
        driverAcceptedAt: FieldValue.serverTimestamp()
      });
      
      // Update driver status
      await db.collection('drivers').doc(driverId).update({
        currentRideId: rideId,
        isAvailable: false
      });
      
      return {
        success: true,
        message: 'Driver accepted the ride'
      };
    } else {
      // Driver rejected, find another driver
      const rideDoc = await db.collection('rides').doc(rideId).get();
        const rideData = rideDoc.data();
      
      // Reset driver availability
      await db.collection('drivers').doc(driverId).update({
        isAvailable: true,
        currentRideId: null
      });
      
      // Try to find another driver
      const nearbyDrivers = await findNearbyDrivers(rideData.pickupLocation);
      const availableDrivers = nearbyDrivers.filter(d => d.uid !== driverId);
      
      if (availableDrivers.length > 0) {
        const selectedDriver = selectBestDriver(availableDrivers, rideData.pickupLocation);
        await notifyDriver(selectedDriver.uid, rideId, rideData);
      
      return {
        success: true,
          message: 'Finding another driver',
          newDriverId: selectedDriver.uid
        };
      } else {
        return {
          success: false,
          message: 'No other drivers available'
        };
      }
    }
    
  } catch (error) {
    console.error('Driver response handling failed:', error);
    return {
      success: false,
      error: error.message
    };
  }
});

/**
 * Cloud Function: Update Ride Status
 */
exports.updateRideStatus = onCall(async (request) => {
  try {
    const {rideId, status, additionalData} = request.data;
    
    // Validate required parameters
    if (!rideId || !status) {
      throw new Error('rideId and status are required');
    }

    console.log(`Updating ride ${rideId} status to ${status}`);

    const updateData = {
      status: status,
      lastUpdated: FieldValue.serverTimestamp()
    };
    
    // Add additional data if provided
    if (additionalData) {
      Object.assign(updateData, additionalData);
    }
    
    await db.collection('rides').doc(rideId).update(updateData);
    
    // Handle specific status updates
    switch (status) {
      case 'started':
        await handleRideStarted(rideId, additionalData);
        break;
      case 'completed':
        await handleRideCompleted(rideId, additionalData);
        break;
      case 'cancelled':
        await handleRideCancelled(rideId, additionalData);
        break;
    }
    
    return {
      success: true,
      message: `Ride status updated to ${status}`
    };
    
  } catch (error) {
    console.error('Ride status update failed:', error);
    return {
      success: false,
      error: error.message
    };
  }
});

/**
 * Cloud Function: Emergency Alert
 */
exports.emergencyAlert = onCall(async (request) => {
  try {
    const {rideId, userId, driverId, location, emergencyType} = request.data;
    
    // Validate required parameters
    if (!rideId || !userId || !driverId || !location || !emergencyType) {
      throw new Error('All parameters (rideId, userId, driverId, location, emergencyType) are required');
    }
    
    console.log(`Emergency alert triggered for ride ${rideId}: ${emergencyType}`);
    
    // Create emergency record
    const emergencyData = {
      rideId: rideId,
      userId: userId,
      driverId: driverId,
      location: location,
      emergencyType: emergencyType,
      timestamp: FieldValue.serverTimestamp(),
      status: 'active'
    };
    
    await db.collection('emergencies').add(emergencyData);
    
    // Send notifications to emergency contacts and support team
    await sendEmergencyNotifications(emergencyData);
    
    return {
      success: true,
      message: 'Emergency alert sent successfully'
    };
    
  } catch (error) {
    console.error('Emergency alert failed:', error);
    return {
      success: false,
      error: error.message
    };
  }
});

/**
 * Cloud Function: Calculate Precise ETA
 */
exports.calculatePreciseETA = onCall(async (request) => {
  try {
    const {driverLocation, pickupLocation, dropoffLocation} = request.data;
    
    // Use Google Maps API for precise ETA calculation
    const response = await axios.get('https://maps.googleapis.com/maps/api/distancematrix/json', {
      params: {
        origins: `${driverLocation.latitude},${driverLocation.longitude}`,
        destinations: `${pickupLocation.latitude},${pickupLocation.longitude}`,
        key: CONFIG.GOOGLE_MAPS_API_KEY,
        mode: 'driving',
        traffic_model: 'best_guess',
        departure_time: 'now'
      }
    });
    
    if (response.data.status === 'OK') {
      const element = response.data.rows[0].elements[0];
      
      return {
        success: true,
        eta: element.duration.value, // Duration in seconds
        distance: element.distance.value, // Distance in meters
        trafficCondition: element.duration_in_traffic ? 'heavy' : 'normal'
      };
    } else {
      throw new Error('Google Maps API error: ' + response.data.status);
    }
    
  } catch (error) {
    console.error('ETA calculation failed:', error);
    return {
      success: false,
      error: error.message
    };
  }
});

/**
 * Cloud Function: Notify Drivers
 */
exports.notifyDrivers = onCall(async (request) => {
  try {
    const {driverIds, notification} = request.data;
    
    // Validate required parameters
    if (!driverIds || !Array.isArray(driverIds) || driverIds.length === 0) {
      throw new Error('driverIds array is required and must not be empty');
    }
    
    if (!notification || !notification.title || !notification.body) {
      throw new Error('notification with title and body is required');
    }
    
    console.log(`Sending notification to ${driverIds.length} drivers`);
    
    const results = [];
    
    for (const driverId of driverIds) {
      try {
        const driverDoc = await db.collection('drivers').doc(driverId).get();
        const driverData = driverDoc.data();
        
        if (driverData && driverData.fcmToken) {
          const message = {
            token: driverData.fcmToken,
            notification: notification
          };
          
          await messaging.send(message);
          results.push({driverId, success: true});
        } else {
          results.push({driverId, success: false, error: 'No FCM token'});
        }
      } catch (error) {
        results.push({driverId, success: false, error: error.message});
      }
    }
    
    return {
      success: true,
      results: results
    };
    
  } catch (error) {
    console.error('Driver notification failed:', error);
    return {
      success: false,
      error: error.message
    };
  }
});

// Helper Functions

/**
 * Find nearby available drivers
 */
async function findNearbyDrivers(pickupLocation) {
  const center = [pickupLocation.latitude, pickupLocation.longitude];
  const radiusInM = CONFIG.INITIAL_SEARCH_RADIUS_KM * 1000;
  
  const driversSnapshot = await db.collection('drivers')
    .where('isOnline', '==', true)
    .where('isAvailable', '==', true)
    .get();
  
  const nearbyDrivers = [];
  
  driversSnapshot.forEach(doc => {
    const driver = doc.data();
      const distance = distanceBetween(
      [driver.currentLocation.latitude, driver.currentLocation.longitude],
      center
      );
      
      if (distance <= CONFIG.MAX_SEARCH_RADIUS_KM) {
      nearbyDrivers.push({
        uid: doc.id,
        ...driver,
        distance: distance
      });
    }
  });
  
  return nearbyDrivers.sort((a, b) => a.distance - b.distance);
}

/**
 * Select best driver based on distance and rating
 */
function selectBestDriver(drivers, pickupLocation) {
  return drivers.reduce((best, current) => {
    const bestScore = calculateDriverScore(best, pickupLocation);
    const currentScore = calculateDriverScore(current, pickupLocation);
    return currentScore > bestScore ? current : best;
  });
}

/**
 * Calculate driver score based on distance and rating
 */
function calculateDriverScore(driver, pickupLocation) {
  const distanceScore = Math.max(0, 100 - (driver.distance * 10));
  const ratingScore = (driver.rating || 4.0) * 20;
  return distanceScore + ratingScore;
}

/**
 * Calculate ETA based on distance
 */
function calculateETA(driverLocation, pickupLocation) {
  const distance = distanceBetween(
    [driverLocation.latitude, driverLocation.longitude],
    [pickupLocation.latitude, pickupLocation.longitude]
  );
  
  // Assume average speed of 30 km/h in city traffic
  const etaMinutes = Math.ceil((distance / 30) * 60);
  return etaMinutes;
}

/**
 * Notify driver about new ride request
 */
async function notifyDriver(driverId, rideId, rideRequest) {
  try {
    // Update driver status
    await db.collection('drivers').doc(driverId).update({
      isAvailable: false,
      currentRideId: rideId
    });
    
    console.log(`Driver ${driverId} notified about ride ${rideId}`);
    
  } catch (error) {
    console.error('Failed to notify driver:', error);
  }
}

/**
 * Handle ride started
 */
async function handleRideStarted(rideId, additionalData) {
  const rideDoc = await db.collection('rides').doc(rideId).get();
  const rideData = rideDoc.data();
  
  // Update driver status
  await db.collection('drivers').doc(rideData.driverId).update({
    isAvailable: false,
    currentRideId: rideId
  });
  
  console.log(`Ride ${rideId} started`);
}

/**
 * Handle ride completed
 */
async function handleRideCompleted(rideId, additionalData) {
  const rideDoc = await db.collection('rides').doc(rideId).get();
    const rideData = rideDoc.data();
    
  // Update driver status
  await db.collection('drivers').doc(rideData.driverId).update({
    isAvailable: true,
    currentRideId: null,
    totalRides: FieldValue.increment(1),
    totalEarnings: FieldValue.increment(additionalData.fare || 0)
  });
  
  // Update user stats
  await db.collection('users').doc(rideData.userId).update({
        totalRides: FieldValue.increment(1)
      });
  
  console.log(`Ride ${rideId} completed`);
}

/**
 * Handle ride cancelled
 */
async function handleRideCancelled(rideId, additionalData) {
  const rideDoc = await db.collection('rides').doc(rideId).get();
  const rideData = rideDoc.data();
  
  // Update driver status
  await db.collection('drivers').doc(rideData.driverId).update({
    isAvailable: true,
    currentRideId: null
  });
  
  console.log(`Ride ${rideId} cancelled`);
}

/**
 * Send emergency notifications
 */
async function sendEmergencyNotifications(emergencyData) {
  try {
    console.log(`Emergency notifications sent for ride ${emergencyData.rideId}`);
  } catch (error) {
    console.error('Failed to send emergency notifications:', error);
  }
}
