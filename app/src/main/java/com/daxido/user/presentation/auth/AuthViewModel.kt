package com.daxido.user.presentation.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.User
import com.daxido.data.repository.AuthRepository
import com.daxido.core.auth.GoogleSignInService
import com.daxido.core.auth.GoogleSignInResult
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AuthViewModel"

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInService: GoogleSignInService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var verificationId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    init {
        // Load verification ID from SharedPreferences on initialization
        verificationId = prefs.getString("verification_id", null)
        if (verificationId != null) {
            _uiState.value = _uiState.value.copy(otpSent = true)
        }
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            Log.d(TAG, "sendOtp: Sending OTP to $phoneNumber")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d(TAG, "onVerificationCompleted: Auto-verification completed")
                        // Auto-verification completed - sign in automatically
                        signInWithCredential(credential)
                    }

                    override fun onVerificationFailed(exception: FirebaseException) {
                        Log.e(TAG, "onVerificationFailed: ${exception.message}", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Verification failed: ${exception.message}"
                        )
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        Log.d(TAG, "onCodeSent: OTP sent successfully. VerificationId: $verificationId")
                        this@AuthViewModel.verificationId = verificationId
                        // Save verification ID to SharedPreferences
                        prefs.edit().putString("verification_id", verificationId).apply()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            otpSent = true,
                            errorMessage = null
                        )
                    }
                }

                authRepository.sendOtp(phoneNumber, activity, callbacks)
            } catch (e: Exception) {
                Log.e(TAG, "sendOtp: Failed to send OTP", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to send OTP: ${e.message}"
                )
            }
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            Log.d(TAG, "verifyOtp: Verifying OTP")
            _uiState.value = _uiState.value.copy(isVerifying = true, errorMessage = null)

            try {
                val currentVerificationId = this@AuthViewModel.verificationId
                if (currentVerificationId == null) {
                    Log.e(TAG, "verifyOtp: Verification ID is null")
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        errorMessage = "Verification ID not found. Please request OTP again."
                    )
                    return@launch
                }

                Log.d(TAG, "verifyOtp: Attempting to verify with verificationId: $currentVerificationId")
                val result = authRepository.verifyOtp(currentVerificationId, otp)
                result.fold(
                    onSuccess = { user ->
                        Log.d(TAG, "verifyOtp: OTP verified successfully for user: ${user.id}")
                        // Clear verification ID after successful verification
                        this@AuthViewModel.verificationId = null
                        prefs.edit().remove("verification_id").apply()
                        _uiState.value = _uiState.value.copy(
                            isVerifying = false,
                            isAuthenticated = true,
                            currentUser = user,
                            errorMessage = null
                        )
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "verifyOtp: Verification failed", exception)
                        _uiState.value = _uiState.value.copy(
                            isVerifying = false,
                            errorMessage = "Invalid OTP. Please try again."
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "verifyOtp: Exception during verification", e)
                _uiState.value = _uiState.value.copy(
                    isVerifying = false,
                    errorMessage = "Verification failed: ${e.message}"
                )
            }
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        if (firebaseUser != null) {
                            // Create or fetch user from Firestore
                            viewModelScope.launch {
                                val user = User(
                                    id = firebaseUser.uid,
                                    phoneNumber = firebaseUser.phoneNumber ?: "",
                                    email = firebaseUser.email,
                                    firstName = "",
                                    lastName = ""
                                )
                                // Clear verification ID after successful auto-verification
                                this@AuthViewModel.verificationId = null
                                prefs.edit().remove("verification_id").apply()
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    currentUser = user,
                                    errorMessage = null
                                )
                            }
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Sign-in failed: ${task.exception?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sign-in failed: ${e.message}"
                )
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            Log.d(TAG, "signInWithGoogle: Starting Google Sign-In")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // The actual Google Sign-In will be handled in the Activity
                // This method just sets up the loading state
                Log.d(TAG, "signInWithGoogle: Google Sign-In initiated")
            } catch (e: Exception) {
                Log.e(TAG, "signInWithGoogle: Exception during Google Sign-In", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Google Sign-In failed: ${e.message}"
                )
            }
        }
    }

    fun handleGoogleSignInResult(result: GoogleSignInResult) {
        viewModelScope.launch {
            when (result) {
                is GoogleSignInResult.Success -> {
                    Log.d(TAG, "handleGoogleSignInResult: Google Sign-In successful")
                    try {
                        val authResult = authRepository.signInWithGoogle(result.account.idToken!!)
                        authResult.fold(
                            onSuccess = { user ->
                                Log.d(TAG, "handleGoogleSignInResult: Firebase authentication successful")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    currentUser = user,
                                    errorMessage = null
                                )
                            },
                            onFailure = { error ->
                                Log.e(TAG, "handleGoogleSignInResult: Firebase authentication failed", error)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Authentication failed: ${error.message}"
                                )
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "handleGoogleSignInResult: Exception during Firebase authentication", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Authentication failed: ${e.message}"
                        )
                    }
                }
                is GoogleSignInResult.Failure -> {
                    Log.e(TAG, "handleGoogleSignInResult: Google Sign-In failed: ${result.error}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Google Sign-In failed: ${result.error}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetOtpState() {
        this@AuthViewModel.verificationId = null
        prefs.edit().remove("verification_id").apply()
        _uiState.value = _uiState.value.copy(otpSent = false, verificationId = null)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "logout: Logging out user")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Sign out from Firebase
                authRepository.signOut()

                // Clear local data
                this@AuthViewModel.verificationId = null
                prefs.edit().clear().apply()

                // Reset UI state
                _uiState.value = AuthUiState()

                Log.d(TAG, "logout: User logged out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "logout: Failed to logout", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Logout failed: ${e.message}"
                )
            }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isVerifying: Boolean = false,
    val isAuthenticated: Boolean = false,
    val otpSent: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    val verificationId: String? = null
)
