package com.daxido.core.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.daxido.core.config.AppConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Sign-In service for handling Google authentication
 */
@Singleton
class GoogleSignInService @Inject constructor(
    private val context: Context
) {
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(AppConfig.GOOGLE_OAUTH_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        
        GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Get the Google Sign-In intent for starting the authentication flow
     */
    fun getSignInIntent() = googleSignInClient.signInIntent
    
    /**
     * Handle the result from Google Sign-In activity
     */
    fun handleSignInResult(task: Task<GoogleSignInAccount>): GoogleSignInResult {
        return try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d("GoogleSignInService", "Google Sign-In successful: ${account.email}")
                GoogleSignInResult.Success(account)
            } else {
                Log.e("GoogleSignInService", "Google Sign-In failed: account is null")
                GoogleSignInResult.Failure("Account is null")
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignInService", "Google Sign-In failed with API exception", e)
            GoogleSignInResult.Failure("Sign-in failed: ${e.message}")
        } catch (e: Exception) {
            Log.e("GoogleSignInService", "Google Sign-In failed with exception", e)
            GoogleSignInResult.Failure("Sign-in failed: ${e.message}")
        }
    }
    
    /**
     * Sign out from Google
     */
    fun signOut() {
        googleSignInClient.signOut()
    }
    
    /**
     * Get the current signed-in account
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * Check if user is already signed in
     */
    fun isSignedIn(): Boolean {
        return getCurrentAccount() != null
    }
}

/**
 * Result of Google Sign-In operation
 */
sealed class GoogleSignInResult {
    data class Success(val account: GoogleSignInAccount) : GoogleSignInResult()
    data class Failure(val error: String) : GoogleSignInResult()
}
