package com.daxido.driver.presentation.earnings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "EarningsViewModel"
    private val _uiState = MutableStateFlow(EarningsUiState())
    val uiState: StateFlow<EarningsUiState> = _uiState.asStateFlow()

    init {
        loadEarningsData()
    }

    fun loadEarningsData() {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid
            if (driverId == null) {
                Log.e(TAG, "Driver not authenticated")
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Get driver document
                val driverDoc = firestore.collection("drivers")
                    .document(driverId)
                    .get()
                    .await()

                val totalEarnings = driverDoc.getDouble("totalEarnings") ?: 0.0
                val todayEarnings = driverDoc.getDouble("todayEarnings") ?: 0.0
                val todaysRides = (driverDoc.getLong("todaysRides") ?: 0L).toInt()
                val todaysHours = driverDoc.getDouble("todaysHours") ?: 0.0
                val rating = (driverDoc.getDouble("rating") ?: 5.0).toFloat()

                // Get earnings breakdown
                val earningsSnapshot = firestore.collection("drivers")
                    .document(driverId)
                    .collection("earnings")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(30)
                    .get()
                    .await()

                val transactions = earningsSnapshot.documents.mapNotNull { doc ->
                    try {
                        EarningsTransaction(
                            id = doc.id,
                            amount = doc.getDouble("amount") ?: 0.0,
                            type = doc.getString("type") ?: "RIDE_FARE",
                            date = doc.getString("date") ?: "",
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction", e)
                        null
                    }
                }

                // Calculate weekly and monthly earnings
                val now = System.currentTimeMillis()
                val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
                val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000)

                val weeklyEarnings = transactions.filter { trans ->
                    try {
                        val transDate = trans.date.toLongOrNull() ?: 0L
                        transDate >= oneWeekAgo
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.amount }

                val monthlyEarnings = transactions.filter { trans ->
                    try {
                        val transDate = trans.date.toLongOrNull() ?: 0L
                        transDate >= oneMonthAgo
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.amount }

                // Get incentives
                val incentivesSnapshot = firestore.collection("drivers")
                    .document(driverId)
                    .collection("incentives")
                    .get()
                    .await()

                val incentives = incentivesSnapshot.documents.mapNotNull { doc ->
                    try {
                        Incentive(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            description = doc.getString("description") ?: "",
                            isCompleted = doc.getBoolean("isCompleted") ?: false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing incentive", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalEarnings = totalEarnings,
                    todayEarnings = todayEarnings,
                    weeklyEarnings = weeklyEarnings,
                    monthlyEarnings = monthlyEarnings,
                    todaysRides = todaysRides,
                    todaysHours = todaysHours,
                    todaysRating = rating,
                    transactions = transactions,
                    recentTransactions = transactions.take(10),
                    incentives = incentives,
                    availableIncentives = incentives.filter { !it.isCompleted }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error loading earnings data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load earnings: ${e.message}"
                )
            }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                val snapshot = firestore.collection("drivers")
                    .document(driverId)
                    .collection("earnings")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val transactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        EarningsTransaction(
                            id = doc.id,
                            amount = doc.getDouble("amount") ?: 0.0,
                            type = doc.getString("type") ?: "RIDE_FARE",
                            date = doc.getString("date") ?: "",
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(transactions = transactions)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transactions", e)
            }
        }
    }

    fun loadIncentives() {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                val snapshot = firestore.collection("drivers")
                    .document(driverId)
                    .collection("incentives")
                    .get()
                    .await()

                val incentives = snapshot.documents.mapNotNull { doc ->
                    try {
                        Incentive(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            description = doc.getString("description") ?: "",
                            isCompleted = doc.getBoolean("isCompleted") ?: false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing incentive", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    incentives = incentives,
                    availableIncentives = incentives.filter { !it.isCompleted }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading incentives", e)
            }
        }
    }

    fun selectTimeRange(timeRange: String) {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                val query = when (timeRange) {
                    "today" -> {
                        val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
                        firestore.collection("drivers")
                            .document(driverId)
                            .collection("earnings")
                            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                    }
                    "week" -> {
                        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                        firestore.collection("drivers")
                            .document(driverId)
                            .collection("earnings")
                            .whereGreaterThanOrEqualTo("timestamp", oneWeekAgo)
                    }
                    "month" -> {
                        val oneMonthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                        firestore.collection("drivers")
                            .document(driverId)
                            .collection("earnings")
                            .whereGreaterThanOrEqualTo("timestamp", oneMonthAgo)
                    }
                    else -> {
                        firestore.collection("drivers")
                            .document(driverId)
                            .collection("earnings")
                    }
                }

                val snapshot = query.orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val transactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        EarningsTransaction(
                            id = doc.id,
                            amount = doc.getDouble("amount") ?: 0.0,
                            type = doc.getString("type") ?: "RIDE_FARE",
                            date = doc.getString("date") ?: "",
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(transactions = transactions)
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering by time range", e)
            }
        }
    }

    fun selectTransactionType(type: String) {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                val snapshot = firestore.collection("drivers")
                    .document(driverId)
                    .collection("earnings")
                    .whereEqualTo("type", type)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val transactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        EarningsTransaction(
                            id = doc.id,
                            amount = doc.getDouble("amount") ?: 0.0,
                            type = doc.getString("type") ?: "RIDE_FARE",
                            date = doc.getString("date") ?: "",
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(transactions = transactions)
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering by transaction type", e)
            }
        }
    }

    fun claimIncentive(incentiveId: String) {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                // Update incentive as completed
                firestore.collection("drivers")
                    .document(driverId)
                    .collection("incentives")
                    .document(incentiveId)
                    .update("isCompleted", true, "claimedAt", com.google.firebase.Timestamp.now())
                    .await()

                // Get incentive amount
                val incentiveDoc = firestore.collection("drivers")
                    .document(driverId)
                    .collection("incentives")
                    .document(incentiveId)
                    .get()
                    .await()

                val amount = incentiveDoc.getDouble("amount") ?: 0.0
                val title = incentiveDoc.getString("title") ?: "Incentive"

                // Add to earnings
                val earningData = hashMapOf(
                    "amount" to amount,
                    "type" to "INCENTIVE",
                    "description" to "Claimed: $title",
                    "date" to System.currentTimeMillis().toString(),
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("drivers")
                    .document(driverId)
                    .collection("earnings")
                    .add(earningData)
                    .await()

                // Update driver's total earnings
                firestore.collection("drivers")
                    .document(driverId)
                    .update(
                        "totalEarnings", com.google.firebase.firestore.FieldValue.increment(amount),
                        "todayEarnings", com.google.firebase.firestore.FieldValue.increment(amount)
                    )
                    .await()

                // Reload data
                loadEarningsData()
                loadIncentives()

                Log.d(TAG, "Incentive claimed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error claiming incentive", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to claim incentive: ${e.message}"
                )
            }
        }
    }
}

data class EarningsUiState(
    val isLoading: Boolean = true,
    val totalEarnings: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val weeklyEarnings: Double = 0.0,
    val monthlyEarnings: Double = 0.0,
    val todaysRides: Int = 0,
    val todaysHours: Double = 0.0,
    val todaysRating: Float = 0.0f,
    val transactions: List<EarningsTransaction> = emptyList(),
    val recentTransactions: List<EarningsTransaction> = emptyList(),
    val incentives: List<Incentive> = emptyList(),
    val availableIncentives: List<Incentive> = emptyList(),
    val errorMessage: String? = null
)

data class EarningsTransaction(
    val id: String,
    val amount: Double,
    val type: String,
    val date: String,
    val description: String
)

data class Incentive(
    val id: String,
    val title: String,
    val amount: Double,
    val description: String,
    val isCompleted: Boolean = false
)
