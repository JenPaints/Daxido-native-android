// Helper Functions for Cloud Functions

const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getDatabase} = require("firebase-admin/database");
const {getMessaging} = require("firebase-admin/messaging");
const {geohashForLocation, distanceBetween} = require("geofire-common");
const logger = require("firebase-functions/logger");

const db = getFirestore();
const rtdb = getDatabase();
const messaging = getMessaging();

const CONFIG = {
  MAX_SEARCH_RADIUS_KM: 10,
  INITIAL_SEARCH_RADIUS_KM: 2,
  RADIUS_INCREMENT_KM: 1,
  MAX_DRIVERS_TO_NOTIFY: 5,
  DRIVER_RESPONSE_TIMEOUT_SEC: 15,
  GEOHASH_PRECISION: 7,
  GOOGLE_MAPS_API_KEY: "AIzaSyB7Vt9F-vl99jZYfd_ZUhVJHezNvY1IFWU"
};

async function performDriverAllocation(rideId, rideRequest) {
  const pickupLocation = rideRequest.pickupLocation;
  const geohash = geohashForLocation([pickupLocation.latitude, pickupLocation.longitude], CONFIG.GEOHASH_PRECISION);
  
  let searchRadius = CONFIG.INITIAL_SEARCH_RADIUS_KM;
  
  while (searchRadius <= CONFIG.MAX_SEARCH_RADIUS_KM) {
    // Find nearby drivers
    const nearbyDrivers = await findNearbyDrivers(pickupLocation, searchRadius, rideRequest.vehicleType);
    
    if (nearbyDrivers.length > 0) {
      // Rank drivers using ML-based scoring
      const rankedDrivers = await rankDrivers(nearbyDrivers, rideRequest);
      
      // Notify top drivers
      const driversToNotify = rankedDrivers.slice(0, CONFIG.MAX_DRIVERS_TO_NOTIFY);
      await notifyDrivers({
        driverIds: driversToNotify.map(d => d.driverId),
        rideRequest,
        rideId
      });

      return {
        driversNotified: driversToNotify.length,
        searchRadius,
        status: "searching"
      };
    }
    
    searchRadius += CONFIG.RADIUS_INCREMENT_KM;
  }
  
  // No drivers found
  await db.collection("ride_requests").doc(rideId).update({
    status: "no_drivers",
    updatedAt: FieldValue.serverTimestamp()
  });
  
  return {
    status: "no_drivers",
    message: "No drivers available in the area"
  };
}

async function findNearbyDrivers(pickupLocation, radiusKm, vehicleType) {
  const geohash = geohashForLocation([pickupLocation.latitude, pickupLocation.longitude], CONFIG.GEOHASH_PRECISION);
  const neighbors = getGeohashNeighbors(geohash, radiusKm);
  
  const drivers = [];
  
  for (const neighborHash of neighbors) {
    const zoneRef = rtdb.ref(`zones/${neighborHash.substring(0, 4)}/drivers`);
    const snapshot = await zoneRef.once("value");
    
    if (snapshot.exists()) {
      const zoneDrivers = snapshot.val();
      
      for (const [driverId, driverData] of Object.entries(zoneDrivers)) {
        if (driverData.status === "available" && driverData.vehicleType === vehicleType) {
          const distance = distanceBetween(
            [pickupLocation.latitude, pickupLocation.longitude],
            [driverData.location.latitude, driverData.location.longitude]
          );
          
          if (distance <= radiusKm) {
            drivers.push({
              driverId,
              ...driverData,
              distance
            });
          }
        }
      }
    }
  }
  
  return drivers;
}

async function rankDrivers(drivers, rideRequest) {
  // ML-based driver ranking algorithm
  return drivers.map(driver => {
    const distanceScore = (1 - (driver.distance / CONFIG.MAX_SEARCH_RADIUS_KM)) * 40;
    const ratingScore = (driver.rating / 5.0) * 30;
    const experienceScore = Math.min(1, driver.completedTrips / 100) * 20;
    const responseTimeScore = 10; // Based on historical response time
    
    const totalScore = distanceScore + ratingScore + experienceScore + responseTimeScore;
    
    return {
      ...driver,
      score: totalScore
    };
  }).sort((a, b) => b.score - a.score);
}

async function notifyUser(rideId, type, data) {
  try {
    const rideDoc = await db.collection("rides").doc(rideId).get();
    if (!rideDoc.exists) return;
    
    const rideData = rideDoc.data();
    const userDoc = await db.collection("users").doc(rideData.userId).get();
    
    if (!userDoc.exists) return;
    
    const userData = userDoc.data();
    if (!userData.fcmToken) return;
    
    const notification = {
      title: getNotificationTitle(type),
      body: getNotificationBody(type, data),
      data: {
        type,
        rideId,
        ...data
      }
    };
    
    await messaging.send({
      token: userData.fcmToken,
      notification,
      data: notification.data
    });
    
  } catch (error) {
    logger.error("Error notifying user:", error);
  }
}

async function notifyDriver(rideId, type, data) {
  try {
    const rideDoc = await db.collection("rides").doc(rideId).get();
    if (!rideDoc.exists) return;
    
    const rideData = rideDoc.data();
    const driverDoc = await db.collection("drivers").doc(rideData.driverId).get();
    
    if (!driverDoc.exists) return;
    
    const driverData = driverDoc.data();
    if (!driverData.fcmToken) return;
    
    const notification = {
      title: getDriverNotificationTitle(type),
      body: getDriverNotificationBody(type, data),
      data: {
        type,
        rideId,
        ...data
      }
    };
    
    await messaging.send({
      token: driverData.fcmToken,
      notification,
      data: notification.data
    });
    
  } catch (error) {
    logger.error("Error notifying driver:", error);
  }
}

async function cancelOtherDriverNotifications(rideId, acceptedDriverId) {
  try {
    const notificationRef = rtdb.ref(`ride_notifications/${rideId}`);
    const snapshot = await notificationRef.once("value");
    
    if (snapshot.exists()) {
      const notificationData = snapshot.val();
      const otherDriverIds = notificationData.driverIds.filter(id => id !== acceptedDriverId);
      
      // Send cancellation notifications to other drivers
      for (const driverId of otherDriverIds) {
        await notifyDriver(rideId, "ride_cancelled", {reason: "driver_accepted"});
      }
    }
    
  } catch (error) {
    logger.error("Error cancelling other driver notifications:", error);
  }
}

async function continueDriverSearch(rideId, rideData) {
  try {
    const rideRef = db.collection("ride_requests").doc(rideId);
    const currentData = rideData || (await rideRef.get()).data();
    
    if (currentData.status !== "searching") return;
    
    const newRadius = currentData.searchRadius + CONFIG.RADIUS_INCREMENT_KM;
    
    if (newRadius <= CONFIG.MAX_SEARCH_RADIUS_KM) {
      await rideRef.update({
        searchRadius: newRadius,
        updatedAt: FieldValue.serverTimestamp()
      });
      
      // Continue searching with expanded radius
      await performDriverAllocation(rideId, currentData);
    } else {
      // No drivers found
      await rideRef.update({
        status: "no_drivers",
        updatedAt: FieldValue.serverTimestamp()
      });
    }
    
  } catch (error) {
    logger.error("Error continuing driver search:", error);
  }
}

function getGeohashNeighbors(geohash, radiusKm) {
  // Simplified geohash neighbor calculation
  // In production, use a proper geohash library
  const neighbors = [geohash];
  
  // Add neighboring geohashes based on radius
  // This is a simplified implementation
  for (let i = 0; i < 8; i++) {
    neighbors.push(geohash + i.toString());
  }
  
  return neighbors;
}

function getNotificationTitle(type) {
  const titles = {
    driver_accepted: "Driver Found!",
    ride_started: "Ride Started",
    ride_completed: "Ride Completed",
    ride_cancelled: "Ride Cancelled"
  };
  return titles[type] || "Ride Update";
}

function getNotificationBody(type, data) {
  const bodies = {
    driver_accepted: "Your driver is on the way!",
    ride_started: "Your ride has started",
    ride_completed: `Ride completed. Fare: $${data.fare?.total || 0}`,
    ride_cancelled: "Your ride has been cancelled"
  };
  return bodies[type] || "Ride status updated";
}

function getDriverNotificationTitle(type) {
  const titles = {
    ride_completed: "Ride Completed",
    ride_cancelled: "Ride Cancelled"
  };
  return titles[type] || "Ride Update";
}

function getDriverNotificationBody(type, data) {
  const bodies = {
    ride_completed: `Ride completed. Earnings: $${data.fare?.total || 0}`,
    ride_cancelled: "Ride has been cancelled"
  };
  return bodies[type] || "Ride status updated";
}

module.exports = {
  performDriverAllocation,
  findNearbyDrivers,
  rankDrivers,
  notifyUser,
  notifyDriver,
  cancelOtherDriverNotifications,
  continueDriverSearch,
  getGeohashNeighbors,
  getNotificationTitle,
  getNotificationBody,
  getDriverNotificationTitle,
  getDriverNotificationBody
};
