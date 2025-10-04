#!/usr/bin/env node

/**
 * Daxido Test Data Setup Script
 * This script populates Firestore with comprehensive test data for testing
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin with Application Default Credentials
admin.initializeApp({
  projectId: "daxido-native",
  databaseURL: "https://daxido-native-default-rtdb.firebaseio.com"
});

const db = admin.firestore();

// Test data configuration
const TEST_CONFIG = {
  USERS_COUNT: 50,
  DRIVERS_COUNT: 20,
  RIDES_COUNT: 100,
  TRANSACTIONS_COUNT: 200,
  NOTIFICATIONS_COUNT: 50
};

// Sample data generators
const generateUserData = (id) => ({
  uid: `user_${id}`,
  email: `user${id}@daxido.com`,
  phoneNumber: `+91${9000000000 + parseInt(id)}`,
  displayName: `User ${id}`,
  photoURL: null,
  isDriver: false,
  isVerified: true,
  createdAt: admin.firestore.Timestamp.now(),
  lastActiveAt: admin.firestore.Timestamp.now(),
  preferences: {
    language: 'en',
    notifications: true,
    theme: 'light'
  },
  emergencyContacts: [
    {
      name: 'Emergency Contact 1',
      phone: '+919876543210',
      relationship: 'Family'
    }
  ],
  walletBalance: Math.floor(Math.random() * 5000) + 1000, // 1000-6000
  totalRides: Math.floor(Math.random() * 50),
  rating: (Math.random() * 2 + 3).toFixed(1), // 3.0-5.0
  joinedAt: admin.firestore.Timestamp.now()
});

const generateDriverData = (id) => ({
  uid: `driver_${id}`,
  email: `driver${id}@daxido.com`,
  phoneNumber: `+91${8000000000 + parseInt(id)}`,
  displayName: `Driver ${id}`,
  photoURL: null,
  isDriver: true,
  isVerified: true,
  isOnline: Math.random() > 0.3, // 70% online
  isAvailable: Math.random() > 0.4, // 60% available
  createdAt: admin.firestore.Timestamp.now(),
  lastActiveAt: admin.firestore.Timestamp.now(),
  
  // Driver specific data
  licenseNumber: `DL${id.toString().padStart(10, '0')}`,
  vehicleType: ['CAR', 'BIKE', 'AUTO'][Math.floor(Math.random() * 3)],
  vehicleNumber: `KA${Math.floor(Math.random() * 100)}${String.fromCharCode(65 + Math.floor(Math.random() * 26))}${Math.floor(Math.random() * 10000)}`,
  vehicleModel: ['Maruti Swift', 'Honda City', 'Toyota Innova', 'Bajaj Pulsar', 'TVS Apache'][Math.floor(Math.random() * 5)],
  
  // Location data
  currentLocation: {
    latitude: 12.9716 + (Math.random() - 0.5) * 0.1,
    longitude: 77.5946 + (Math.random() - 0.5) * 0.1
  },
  lastLocationUpdate: admin.firestore.Timestamp.now(),
  
  // Earnings data
  totalEarnings: Math.floor(Math.random() * 50000) + 10000,
  todayEarnings: Math.floor(Math.random() * 2000) + 500,
  weeklyEarnings: Math.floor(Math.random() * 10000) + 2000,
  monthlyEarnings: Math.floor(Math.random() * 30000) + 5000,
  todaysRides: Math.floor(Math.random() * 15),
  todaysHours: Math.floor(Math.random() * 12) + 1,
  
  // Performance data
  rating: (Math.random() * 2 + 3).toFixed(1), // 3.0-5.0
  totalRides: Math.floor(Math.random() * 200) + 50,
  completedTrips: Math.floor(Math.random() * 180) + 40,
  cancellationRate: (Math.random() * 0.1).toFixed(2), // 0-10%
  
  // Documents
  documents: {
    license: {
      status: 'verified',
      uploadedAt: admin.firestore.Timestamp.now(),
      expiryDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000) // 1 year from now
    },
    rc: {
      status: 'verified',
      uploadedAt: admin.firestore.Timestamp.now(),
      expiryDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000)
    },
    insurance: {
      status: 'verified',
      uploadedAt: admin.firestore.Timestamp.now(),
      expiryDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000)
    }
  },
  
  // Bank details
  bankDetails: {
    accountNumber: `${Math.floor(Math.random() * 10000000000)}`,
    ifscCode: `SBIN${Math.floor(Math.random() * 1000000)}`,
    accountHolderName: `Driver ${id}`,
    bankName: 'State Bank of India'
  }
});

const generateRideData = (id, userId, driverId) => ({
  rideId: `ride_${id}`,
  userId: userId,
  driverId: driverId,
  status: ['COMPLETED', 'CANCELLED', 'IN_PROGRESS', 'PENDING'][Math.floor(Math.random() * 4)],
  
  // Location data
  pickupLocation: {
    latitude: 12.9716 + (Math.random() - 0.5) * 0.1,
    longitude: 77.5946 + (Math.random() - 0.5) * 0.1,
    address: `Pickup Address ${id}`,
    name: `Location ${id}`
  },
  dropoffLocation: {
    latitude: 12.9356 + (Math.random() - 0.5) * 0.1,
    longitude: 77.6068 + (Math.random() - 0.5) * 0.1,
    address: `Dropoff Address ${id}`,
    name: `Destination ${id}`
  },
  
  // Ride details
  vehicleType: ['CAR', 'BIKE', 'AUTO'][Math.floor(Math.random() * 3)],
  fare: Math.floor(Math.random() * 500) + 100, // 100-600
  distance: Math.floor(Math.random() * 20) + 5, // 5-25 km
  duration: Math.floor(Math.random() * 60) + 15, // 15-75 minutes
  
  // Timestamps
  createdAt: admin.firestore.Timestamp.now(),
  startedAt: admin.firestore.Timestamp.now(),
  completedAt: admin.firestore.Timestamp.now(),
  
  // Payment
  paymentMethod: ['CASH', 'CARD', 'UPI', 'WALLET'][Math.floor(Math.random() * 4)],
  paymentStatus: ['PENDING', 'COMPLETED', 'FAILED'][Math.floor(Math.random() * 3)],
  
  // Rating
  rating: Math.floor(Math.random() * 5) + 1,
  feedback: `Great ride experience ${id}`
});

const generateTransactionData = (id, userId) => ({
  transactionId: `TXN_${id}`,
  userId: userId,
  amount: Math.floor(Math.random() * 1000) + 50, // 50-1050
  type: ['RIDE_FARE', 'WALLET_RECHARGE', 'REFUND', 'BONUS'][Math.floor(Math.random() * 4)],
  status: ['COMPLETED', 'PENDING', 'FAILED'][Math.floor(Math.random() * 3)],
  description: `Transaction ${id}`,
  timestamp: admin.firestore.Timestamp.now(),
  paymentMethod: ['CASH', 'CARD', 'UPI', 'WALLET'][Math.floor(Math.random() * 4)]
});

const generateNotificationData = (id, userId) => ({
  id: `notification_${id}`,
  userId: userId,
  title: `Notification ${id}`,
  body: `This is notification body ${id}`,
  type: ['RIDE_UPDATE', 'PROMOTION', 'SYSTEM', 'PAYMENT'][Math.floor(Math.random() * 4)],
  isRead: Math.random() > 0.5,
  createdAt: admin.firestore.Timestamp.now(),
  data: {
    rideId: `ride_${Math.floor(Math.random() * 100)}`,
    action: 'view'
  }
});

// Main function to populate test data
async function populateTestData() {
  console.log('🚀 Starting Daxido Test Data Population...\n');
  
  try {
    // 1. Create Users
    console.log('👥 Creating users...');
    const users = [];
    for (let i = 1; i <= TEST_CONFIG.USERS_COUNT; i++) {
      const userData = generateUserData(i);
      users.push(userData);
      
      await db.collection('users').doc(userData.uid).set(userData);
      
      // Create user wallet
      await db.collection('wallets').doc(userData.uid).set({
        balance: userData.walletBalance,
        lastUpdated: admin.firestore.Timestamp.now()
      });
      
      if (i % 10 === 0) console.log(`Created ${i}/${TEST_CONFIG.USERS_COUNT} users`);
    }
    console.log(`✅ Created ${TEST_CONFIG.USERS_COUNT} users\n`);
    
    // 2. Create Drivers
    console.log('🚗 Creating drivers...');
    const drivers = [];
    for (let i = 1; i <= TEST_CONFIG.DRIVERS_COUNT; i++) {
      const driverData = generateDriverData(i);
      drivers.push(driverData);
      
      await db.collection('drivers').doc(driverData.uid).set(driverData);
      
      // Create driver earnings subcollection
      const earningsRef = db.collection('drivers').doc(driverData.uid).collection('earnings');
      for (let j = 1; j <= 10; j++) {
        await earningsRef.add({
          amount: Math.floor(Math.random() * 500) + 100,
          type: 'RIDE_FARE',
          description: `Ride fare ${j}`,
          date: new Date(Date.now() - j * 24 * 60 * 60 * 1000).toISOString(),
          timestamp: admin.firestore.Timestamp.now()
        });
      }
      
      if (i % 5 === 0) console.log(`Created ${i}/${TEST_CONFIG.DRIVERS_COUNT} drivers`);
    }
    console.log(`✅ Created ${TEST_CONFIG.DRIVERS_COUNT} drivers\n`);
    
    // 3. Create Rides
    console.log('🚕 Creating rides...');
    for (let i = 1; i <= TEST_CONFIG.RIDES_COUNT; i++) {
      const randomUser = users[Math.floor(Math.random() * users.length)];
      const randomDriver = drivers[Math.floor(Math.random() * drivers.length)];
      
      const rideData = generateRideData(i, randomUser.uid, randomDriver.uid);
      await db.collection('rides').doc(rideData.rideId).set(rideData);
      
      if (i % 20 === 0) console.log(`Created ${i}/${TEST_CONFIG.RIDES_COUNT} rides`);
    }
    console.log(`✅ Created ${TEST_CONFIG.RIDES_COUNT} rides\n`);
    
    // 4. Create Transactions
    console.log('💳 Creating transactions...');
    for (let i = 1; i <= TEST_CONFIG.TRANSACTIONS_COUNT; i++) {
      const randomUser = users[Math.floor(Math.random() * users.length)];
      const transactionData = generateTransactionData(i, randomUser.uid);
      await db.collection('transactions').doc(transactionData.transactionId).set(transactionData);
      
      if (i % 50 === 0) console.log(`Created ${i}/${TEST_CONFIG.TRANSACTIONS_COUNT} transactions`);
    }
    console.log(`✅ Created ${TEST_CONFIG.TRANSACTIONS_COUNT} transactions\n`);
    
    // 5. Create Notifications
    console.log('🔔 Creating notifications...');
    for (let i = 1; i <= TEST_CONFIG.NOTIFICATIONS_COUNT; i++) {
      const randomUser = users[Math.floor(Math.random() * users.length)];
      const notificationData = generateNotificationData(i, randomUser.uid);
      await db.collection('notifications').add(notificationData);
      
      if (i % 10 === 0) console.log(`Created ${i}/${TEST_CONFIG.NOTIFICATIONS_COUNT} notifications`);
    }
    console.log(`✅ Created ${TEST_CONFIG.NOTIFICATIONS_COUNT} notifications\n`);
    
    // 6. Create Configuration Data
    console.log('⚙️ Creating configuration data...');
    
    // Popular places
    await db.collection('config').doc('popularPlaces').set({
      places: [
        {
          name: 'India Gate',
          address: 'Rajpath, New Delhi, Delhi 110001',
          latitude: 28.6139,
          longitude: 77.2090
        },
        {
          name: 'Gateway of India',
          address: 'Apollo Bandar, Colaba, Mumbai, Maharashtra 400001',
          latitude: 19.0760,
          longitude: 72.8777
        },
        {
          name: 'Cubbon Park',
          address: 'Kasturba Rd, Bengaluru, Karnataka 560001',
          latitude: 12.9716,
          longitude: 77.5946
        },
        {
          name: 'Victoria Memorial',
          address: 'Victoria Memorial Hall, Kolkata, West Bengal 700071',
          latitude: 22.5726,
          longitude: 88.3639
        }
      ]
    });
    
    // Support configuration
    await db.collection('config').doc('support').set({
      phoneNumber: '+91 1800 123 4567',
      emailAddress: 'support@daxido.com',
      workingHours: '24/7',
      responseTime: 'Within 2 hours'
    });
    
    // FAQs
    const faqs = [
      {
        question: 'How do I book a ride?',
        answer: 'Simply open the app, enter your pickup and destination locations, select your preferred vehicle type, and tap Book Ride.',
        category: 'booking',
        order: 1
      },
      {
        question: 'How is the fare calculated?',
        answer: 'Fare is calculated based on distance, time, vehicle type, and current demand.',
        category: 'pricing',
        order: 2
      },
      {
        question: 'Can I cancel my ride?',
        answer: 'Yes, you can cancel your ride before the driver arrives.',
        category: 'cancellation',
        order: 3
      },
      {
        question: 'What payment methods are accepted?',
        answer: 'We accept cash, credit/debit cards, UPI, and wallet payments.',
        category: 'payment',
        order: 4
      },
      {
        question: 'How do I track my ride?',
        answer: 'Once your ride is confirmed, you can track your driver\'s real-time location on the map.',
        category: 'tracking',
        order: 5
      }
    ];
    
    for (const faq of faqs) {
      await db.collection('faqs').add(faq);
    }
    
    console.log('✅ Created configuration data\n');
    
    // 7. Create Driver Incentives
    console.log('🎁 Creating driver incentives...');
    for (const driver of drivers) {
      const incentivesRef = db.collection('drivers').doc(driver.uid).collection('incentives');
      
      const incentives = [
        {
          title: 'Complete 10 rides today',
          amount: 100,
          description: 'Complete 10 rides today to earn bonus',
          isCompleted: Math.random() > 0.7,
          createdAt: admin.firestore.Timestamp.now()
        },
        {
          title: 'Peak hour bonus',
          amount: 50,
          description: 'Drive during peak hours (6-9 PM)',
          isCompleted: Math.random() > 0.8,
          createdAt: admin.firestore.Timestamp.now()
        },
        {
          title: 'Weekly target',
          amount: 500,
          description: 'Complete weekly target of 50 rides',
          isCompleted: Math.random() > 0.9,
          createdAt: admin.firestore.Timestamp.now()
        }
      ];
      
      for (const incentive of incentives) {
        await incentivesRef.add(incentive);
      }
    }
    console.log('✅ Created driver incentives\n');
    
    console.log('🎉 Test data population completed successfully!');
    console.log('\n📊 Summary:');
    console.log(`- Users: ${TEST_CONFIG.USERS_COUNT}`);
    console.log(`- Drivers: ${TEST_CONFIG.DRIVERS_COUNT}`);
    console.log(`- Rides: ${TEST_CONFIG.RIDES_COUNT}`);
    console.log(`- Transactions: ${TEST_CONFIG.TRANSACTIONS_COUNT}`);
    console.log(`- Notifications: ${TEST_CONFIG.NOTIFICATIONS_COUNT}`);
    console.log(`- Configuration: Popular places, Support info, FAQs`);
    console.log(`- Driver incentives: ${TEST_CONFIG.DRIVERS_COUNT * 3}`);
    
  } catch (error) {
    console.error('❌ Error populating test data:', error);
    process.exit(1);
  }
}

// Run the script
if (require.main === module) {
  populateTestData()
    .then(() => {
      console.log('\n✅ Test data setup completed successfully!');
      process.exit(0);
    })
    .catch((error) => {
      console.error('❌ Test data setup failed:', error);
      process.exit(1);
    });
}

module.exports = { populateTestData };
