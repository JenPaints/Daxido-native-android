package com.daxido.user.presentation.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.*
import com.daxido.data.repository.RideRepository
import com.daxido.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideBookingViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RideBookingUiState())
    val uiState: StateFlow<RideBookingUiState> = _uiState.asStateFlow()

    fun setPickupLocation(location: Location) {
        _uiState.update { it.copy(pickupLocation = location) }
        calculateFareEstimate()
    }

    fun setDropLocation(location: Location) {
        _uiState.update { it.copy(dropLocation = location) }
        calculateFareEstimate()
    }

    fun selectVehicleType(vehicleType: VehicleType) {
        _uiState.update { it.copy(selectedVehicleType = vehicleType) }
        calculateFareEstimate()
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.update { it.copy(selectedPaymentMethod = paymentMethod) }
    }

    fun loadPaymentMethods() {
        viewModelScope.launch {
            val paymentMethods = paymentRepository.getPaymentMethods()
            _uiState.update { it.copy(paymentMethods = paymentMethods) }
        }
    }

    fun loadWalletBalance() {
        viewModelScope.launch {
            val balance = paymentRepository.getWalletBalanceValue()
            _uiState.update { it.copy(walletBalance = balance) }
        }
    }

    fun updatePromoCode(code: String) {
        _uiState.update { it.copy(promoCode = code, promoCodeError = null) }
    }

    fun applyPromoCode() {
        viewModelScope.launch {
            val code = _uiState.value.promoCode.trim()
            if (code.isEmpty()) {
                _uiState.update { it.copy(promoCodeError = "Please enter a promo code") }
                return@launch
            }

            _uiState.update { it.copy(isApplyingPromo = true) }

            val result = paymentRepository.validatePromoCode(code)
            result.fold(
                onSuccess = { discount ->
                    _uiState.update {
                        it.copy(
                            appliedPromo = discount,
                            isApplyingPromo = false,
                            promoCodeError = null
                        )
                    }
                    // Recalculate fare with discount
                    calculateFareEstimate()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isApplyingPromo = false,
                            promoCodeError = error.message ?: "Invalid promo code"
                        )
                    }
                }
            )
        }
    }

    fun removePromoCode() {
        _uiState.update {
            it.copy(
                promoCode = "",
                appliedPromo = null,
                promoCodeError = null
            )
        }
        calculateFareEstimate()
    }

    fun calculateFareEstimate() {
        viewModelScope.launch {
            val pickup = _uiState.value.pickupLocation
            val drop = _uiState.value.dropLocation
            val vehicleType = _uiState.value.selectedVehicleType

            if (pickup != null && drop != null && vehicleType != null) {
                var fare = rideRepository.calculateFare(pickup, drop, vehicleType)

                // Apply promo discount if available
                _uiState.value.appliedPromo?.let { promo ->
                    val discountAmount = when (promo.discountType) {
                        "PERCENTAGE" -> fare.total * (promo.discountValue / 100)
                        "FIXED" -> promo.discountValue.coerceAtMost(fare.total)
                        else -> 0.0
                    }

                    fare = fare.copy(
                        discount = discountAmount,
                        total = (fare.total - discountAmount).coerceAtLeast(0.0)
                    )
                }

                _uiState.update { it.copy(estimatedFare = fare) }
            }
        }
    }

    fun confirmBooking() {
        viewModelScope.launch {
            val state = _uiState.value
            val pickup = state.pickupLocation
            val drop = state.dropLocation
            val vehicleType = state.selectedVehicleType
            val paymentMethod = state.selectedPaymentMethod

            if (pickup != null && drop != null && vehicleType != null && paymentMethod != null) {
                _uiState.update { it.copy(isBooking = true) }

                val rideResult = rideRepository.createRideRequest(
                    pickup,
                    drop,
                    vehicleType,
                    paymentMethod
                )

                rideResult.fold(
                    onSuccess = { ride ->
                        _uiState.update { 
                            it.copy(
                                ride = ride,
                                isBooking = false
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                isBooking = false,
                                errorMessage = error.message
                            ) 
                        }
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class RideBookingUiState(
    val pickupLocation: Location? = null,
    val dropLocation: Location? = null,
    val selectedVehicleType: VehicleType? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val availableVehicleTypes: List<VehicleType> = listOf(
        VehicleType.BIKE,
        VehicleType.AUTO,
        VehicleType.CAR,
        VehicleType.PREMIUM
    ),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val estimatedFare: Fare? = null,
    val estimatedTime: Int? = null,
    val estimatedDistance: Double? = null,
    val routePolyline: String? = null,
    val ride: Ride? = null,
    val isBooking: Boolean = false,
    val errorMessage: String? = null,
    val walletBalance: Double = 0.0,
    val promoCode: String = "",
    val appliedPromo: PromoDiscount? = null,
    val isApplyingPromo: Boolean = false,
    val promoCodeError: String? = null
)

data class PromoDiscount(
    val code: String,
    val discountType: String, // "PERCENTAGE" or "FIXED"
    val discountValue: Double,
    val maxDiscount: Double? = null,
    val description: String
)
