package com.daxido.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.daxido.user.presentation.location.LocationSearchScreen
import com.daxido.user.presentation.location.LocationSearchType
import com.daxido.user.presentation.auth.LoginScreen
import com.daxido.user.presentation.auth.OtpVerificationScreen
import com.daxido.user.presentation.auth.SignupScreen
import com.daxido.user.presentation.home.HomeMapScreen
import com.daxido.user.presentation.onboarding.OnboardingScreen
import com.daxido.user.presentation.onboarding.SplashScreen
import com.daxido.user.presentation.profile.ProfileScreen
import com.daxido.user.presentation.ride.RideBookingScreen
import com.daxido.user.presentation.ride.RideTrackingScreen
import com.daxido.user.presentation.wallet.WalletScreen
import com.daxido.user.presentation.notifications.NotificationsScreen
import com.daxido.user.presentation.support.SupportScreen
import com.daxido.driver.presentation.home.DriverHomeScreen
import com.daxido.driver.presentation.onboarding.DriverOnboardingScreen
import com.daxido.driver.presentation.profile.DriverProfileScreen
import com.daxido.driver.presentation.earnings.EarningsDashboard
import com.daxido.driver.presentation.incentives.IncentivesScreen
import com.daxido.driver.presentation.performance.PerformanceMetricsScreen
import com.daxido.driver.presentation.documents.DocumentManagerScreen
import com.daxido.driver.presentation.training.TrainingCenterScreen
import com.daxido.driver.presentation.scheduler.AvailabilitySchedulerScreen
import com.daxido.driver.presentation.ride.RideRequestScreen
import com.daxido.user.presentation.settings.SettingsScreen
import com.daxido.user.presentation.ai.AiAssistantScreen
import com.daxido.driver.presentation.navigation.DriverNavigationScreen
import com.daxido.user.presentation.mode.ModeSelectionScreen
import com.daxido.admin.presentation.dashboard.AdminDashboardScreen
import com.daxido.admin.presentation.live.LiveRidesScreen
import com.daxido.admin.presentation.users.UserManagementScreen
import com.daxido.admin.presentation.drivers.DriverManagementScreen
import com.daxido.admin.presentation.financial.FinancialOverviewScreen

@Composable
fun DaxidoNavHost(
    navController: NavHostController,
    startDestination: String = Route.SPLASH,
    onGoogleSignIn: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.SPLASH) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Route.ONBOARDING) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToModeSelection = {
                    navController.navigate(Route.MODE_SELECTION) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onNavigateToAuth = {
                    navController.navigate(Route.AUTH_GRAPH) {
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.MODE_SELECTION) {
            ModeSelectionScreen(
                onSelectUserMode = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.MODE_SELECTION) { inclusive = true }
                    }
                },
                onSelectDriverMode = {
                    navController.navigate(Route.DRIVER_HOME) {
                        popUpTo(Route.MODE_SELECTION) { inclusive = true }
                    }
                },
                onSelectAdminMode = {
                    navController.navigate(Route.ADMIN_DASHBOARD) {
                        popUpTo(Route.MODE_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        navigation(
            startDestination = Route.LOGIN,
            route = Route.AUTH_GRAPH
        ) {
            composable(Route.LOGIN) {
                LoginScreen(
                    onNavigateToSignup = {
                        navController.navigate(Route.SIGNUP)
                    },
                    onNavigateToOtp = { phoneNumber ->
                        navController.navigate("${Route.OTP_VERIFICATION}/$phoneNumber")
                    },
                    onGoogleSignIn = onGoogleSignIn
                )
            }

            composable(Route.SIGNUP) {
                SignupScreen(
                    onNavigateToOtp = { phoneNumber ->
                        navController.navigate("${Route.OTP_VERIFICATION}/$phoneNumber")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("${Route.OTP_VERIFICATION}/{phoneNumber}") { backStackEntry ->
                val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
                OtpVerificationScreen(
                    phoneNumber = phoneNumber,
                    onVerificationSuccess = {
                        navController.navigate(Route.HOME) {
                            popUpTo(Route.AUTH_GRAPH) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Route.HOME) {
            HomeMapScreen(
                onNavigateToLocationSearch = {
                    navController.navigate("${Route.LOCATION_SEARCH}/PICKUP")
                },
                onNavigateToRideBooking = { pickupName, dropoffName, pickupAddress, dropoffAddress, pickupLat, pickupLng, dropoffLat, dropoffLng ->
                    navController.navigate(Route.RIDE_BOOKING)
                },
                onNavigateToRideTracking = { rideId ->
                    navController.navigate("${Route.RIDE_TRACKING}/$rideId")
                },
                onNavigateToAiAssistant = {
                    navController.navigate(Route.AI_ASSISTANT)
                }
            )
        }

        composable("${Route.LOCATION_SEARCH}/{searchType}") { backStackEntry ->
            val searchTypeString = backStackEntry.arguments?.getString("searchType") ?: "PICKUP"
            val searchType = LocationSearchType.valueOf(searchTypeString)
            LocationSearchScreen(
                searchType = searchType,
                onLocationSelected = { location ->
                    // Handle location selection based on search type
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.RIDE_BOOKING) {
            RideBookingScreen(
                pickupLocation = "Current Location",
                dropoffLocation = "Destination",
                vehicleType = "CAR",
                rideId = "ride_${System.currentTimeMillis()}",
                pickupLat = 0.0,
                pickupLng = 0.0,
                dropoffLat = 0.0,
                dropoffLng = 0.0,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRideTracking = { rideId ->
                    navController.navigate("${Route.RIDE_TRACKING}/$rideId")
                }
            )
        }

        composable("${Route.RIDE_TRACKING}/{rideId}") { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId") ?: ""
            RideTrackingScreen(
                rideId = rideId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEndRide = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.PROFILE) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSwitchToDriverMode = {
                    navController.navigate(Route.DRIVER_HOME) {
                        popUpTo(Route.HOME) { inclusive = true }
                    }
                },
                onNavigateToSupport = {
                    navController.navigate(Route.SUPPORT)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.SETTINGS)
                }
            )
        }

        composable(Route.WALLET) {
            WalletScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddMoney = {
                    // Navigate to add money screen
                },
                onNavigateToTransactionHistory = {
                    // Navigate to transaction history screen
                }
            )
        }

        composable(Route.NOTIFICATIONS) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.SUPPORT) {
            SupportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = {
                    // Navigate to chat screen
                },
                onNavigateToFAQ = {
                    // Navigate to FAQ screen
                },
                onNavigateToReportIssue = {
                    // Navigate to report issue screen
                }
            )
        }

        composable(Route.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.AI_ASSISTANT) {
            AiAssistantScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.TRIP_HISTORY) {
            com.daxido.user.presentation.history.TripHistoryScreen(
                onTripClick = { tripId -> /* TODO: Navigate to trip details */ },
                onRebookTrip = { trip -> /* TODO: Rebook trip */ },
                onDownloadInvoice = { tripId -> /* TODO: Download invoice */ },
                onReportIssue = { tripId -> /* TODO: Report issue */ },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Driver App Navigation
        composable(Route.DRIVER_HOME) {
            DriverHomeScreen(
                onNavigateToEarnings = {
                    navController.navigate(Route.DRIVER_EARNINGS)
                },
                onNavigateToProfile = {
                    navController.navigate(Route.DRIVER_PROFILE)
                },
                onNavigateToRideRequest = { requestId ->
                    navController.navigate("${Route.DRIVER_RIDE_REQUEST}/$requestId")
                },
                onNavigateToNavigation = { tripId ->
                    navController.navigate("${Route.DRIVER_NAVIGATION}/$tripId")
                }
            )
        }

        composable(Route.DRIVER_ONBOARDING) {
            DriverOnboardingScreen(
                onNavigateToDocuments = {
                    navController.navigate(Route.DRIVER_DOCUMENTS)
                },
                onNavigateToVehicleDetails = { /* TODO: Implement vehicle details */ },
                onNavigateToBankDetails = { /* TODO: Implement bank details */ },
                onNavigateToTraining = {
                    navController.navigate(Route.DRIVER_TRAINING)
                },
                onComplete = {
                    navController.navigate(Route.DRIVER_HOME) {
                        popUpTo(Route.DRIVER_ONBOARDING) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.DRIVER_PROFILE) {
            DriverProfileScreen(
                onBack = {
                    navController.popBackStack()
                },
                onEditProfile = { /* TODO: Implement edit profile */ },
                onManageDocuments = {
                    navController.navigate(Route.DRIVER_DOCUMENTS)
                },
                onManageVehicle = { /* TODO: Implement vehicle management */ },
                onChangePassword = { /* TODO: Implement change password */ },
                onLogout = { /* TODO: Implement logout */ },
                onNavigateToSettings = { /* TODO: Implement settings */ },
                onSwitchToUserMode = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.DRIVER_HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.DRIVER_EARNINGS) {
            EarningsDashboard(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToWithdrawal = {
                    // Navigate to withdrawal screen
                },
                onNavigateToTransactionHistory = {
                    // Navigate to transaction history screen
                }
            )
        }

        composable(Route.DRIVER_INCENTIVES) {
            IncentivesScreen(
                onBack = {
                    navController.popBackStack()
                },
                onViewDetails = { incentive -> /* TODO: Navigate to incentive details */ },
                onClaimReward = { rewardId -> /* TODO: Claim reward */ }
            )
        }

        composable(Route.DRIVER_PERFORMANCE) {
            PerformanceMetricsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onViewAchievements = { /* TODO: Navigate to achievements */ },
                onViewTraining = {
                    navController.navigate(Route.DRIVER_TRAINING)
                }
            )
        }

        composable(Route.DRIVER_DOCUMENTS) {
            DocumentManagerScreen(
                onBack = {
                    navController.popBackStack()
                },
                onUploadDocument = { documentType -> /* TODO: Upload document */ },
                onViewDocument = { document -> /* TODO: View document */ },
                onUpdateDocument = { document -> /* TODO: Update document */ }
            )
        }

        composable(Route.DRIVER_TRAINING) {
            TrainingCenterScreen(
                onBack = {
                    navController.popBackStack()
                },
                onStartModule = { module -> /* TODO: Start training module */ },
                onViewCertificate = { /* TODO: View certificate */ },
                onContactSupport = { /* TODO: Contact support */ }
            )
        }

        composable(Route.DRIVER_SCHEDULER) {
            AvailabilitySchedulerScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSaveSchedule = { schedule -> /* TODO: Save schedule */ },
                onQuickToggle = { isEnabled -> /* TODO: Toggle availability */ }
            )
        }

        composable("${Route.DRIVER_RIDE_REQUEST}/{requestId}") { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RideRequestScreen(
                requestId = requestId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNavigation = { tripId ->
                    navController.navigate("${Route.DRIVER_NAVIGATION}/$tripId") {
                        popUpTo(Route.DRIVER_HOME)
                    }
                }
            )
        }

        composable("${Route.DRIVER_NAVIGATION}/{tripId}") { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            DriverNavigationScreen(
                tripId = tripId,
                currentLocation = com.google.android.gms.maps.model.LatLng(12.9716, 77.5946),
                destination = com.google.android.gms.maps.model.LatLng(12.9356, 77.6068),
                customerName = "John Doe",
                isPickupPhase = true,
                onNavigationAction = { action ->
                    // Handle navigation actions
                },
                onEndNavigation = {
                    navController.navigate(Route.DRIVER_HOME) {
                        popUpTo(Route.DRIVER_HOME) { inclusive = true }
                    }
                }
            )
        }

        // Admin Dashboard Navigation
        composable(Route.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onNavigateToLiveRides = {
                    navController.navigate(Route.ADMIN_LIVE_RIDES)
                },
                onNavigateToUsers = {
                    navController.navigate(Route.ADMIN_USERS)
                },
                onNavigateToDrivers = {
                    navController.navigate(Route.ADMIN_DRIVERS)
                },
                onNavigateToFinancial = {
                    navController.navigate(Route.ADMIN_FINANCIAL)
                },
                onNavigateToPromos = {
                    navController.navigate(Route.ADMIN_PROMOS)
                },
                onNavigateToSupport = {
                    navController.navigate(Route.ADMIN_SUPPORT)
                },
                onNavigateToEmergency = {
                    navController.navigate(Route.ADMIN_EMERGENCY)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.ADMIN_SETTINGS)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Route.ADMIN_ANALYTICS)
                }
            )
        }

        composable(Route.ADMIN_LIVE_RIDES) {
            LiveRidesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRideClick = { rideId ->
                    navController.navigate("${Route.RIDE_TRACKING}/$rideId")
                }
            )
        }

        composable(Route.ADMIN_USERS) {
            UserManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUserClick = { userId ->
                    // Navigate to user details if needed
                }
            )
        }

        composable(Route.ADMIN_DRIVERS) {
            DriverManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDriverClick = { driverId ->
                    // Navigate to driver details if needed
                }
            )
        }

        composable(Route.ADMIN_FINANCIAL) {
            FinancialOverviewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

object Route {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val AUTH_GRAPH = "auth_graph"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val OTP_VERIFICATION = "otp_verification"
    const val HOME = "home"
    const val LOCATION_SEARCH = "location_search"
    const val RIDE_BOOKING = "ride_booking"
    const val RIDE_TRACKING = "ride_tracking"
    const val PROFILE = "profile"
    const val WALLET = "wallet"
    const val NOTIFICATIONS = "notifications"
    const val SUPPORT = "support"
    const val TRIP_HISTORY = "trip_history"
    const val SETTINGS = "settings"
    const val AI_ASSISTANT = "ai_assistant"
    
    // Driver App Routes
    const val DRIVER_HOME = "driver_home"
    const val DRIVER_ONBOARDING = "driver_onboarding"
    const val DRIVER_PROFILE = "driver_profile"
    const val DRIVER_EARNINGS = "driver_earnings"
    const val DRIVER_INCENTIVES = "driver_incentives"
    const val DRIVER_PERFORMANCE = "driver_performance"
    const val DRIVER_DOCUMENTS = "driver_documents"
    const val DRIVER_TRAINING = "driver_training"
    const val DRIVER_SCHEDULER = "driver_scheduler"
    const val DRIVER_RIDE_REQUEST = "driver_ride_request"
    const val DRIVER_NAVIGATION = "driver_navigation"
    const val MODE_SELECTION = "mode_selection"

    // Admin Dashboard Routes
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_LIVE_RIDES = "admin_live_rides"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_DRIVERS = "admin_drivers"
    const val ADMIN_FINANCIAL = "admin_financial"
    const val ADMIN_PROMOS = "admin_promos"
    const val ADMIN_SUPPORT = "admin_support"
    const val ADMIN_EMERGENCY = "admin_emergency"
    const val ADMIN_SETTINGS = "admin_settings"
    const val ADMIN_ANALYTICS = "admin_analytics"
}