package com.daxido.core.data.repository

import com.daxido.core.models.User
import com.daxido.core.optimization.CacheManager
import com.daxido.core.optimization.FirestoreOptimizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val cacheManager: CacheManager,
    private val firestoreOptimizer: FirestoreOptimizer
) {

    /**
     * COST OPTIMIZATION: Get user profile with caching
     * Saves ~70% of Firestore reads for profile fetches
     */
    suspend fun getUserProfile(userId: String? = null): Result<User> {
        return try {
            val uid = userId ?: auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            // 1. Try cache first (saves Firestore read $$)
            cacheManager.getCachedUserProfile()?.let {
                return Result.success(it)
            }

            // 2. Fetch from Firestore only if not cached
            val doc = firestore.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java)
                ?: return Result.failure(Exception("User not found"))

            // 3. Cache result for 24 hours
            cacheManager.cacheUserProfile(user)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile and invalidate cache
     */
    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            // Update Firestore
            firestore.collection("users").document(uid).set(user).await()

            // Update cache
            cacheManager.cacheUserProfile(user)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * COST OPTIMIZATION: Clear all caches
     */
    suspend fun clearCache(): Result<Unit> {
        return try {
            cacheManager.clearAllCache()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout and clear cache
     */
    suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            cacheManager.clearAllCache()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check for app update
     */
    suspend fun checkForAppUpdate(): Result<Boolean> {
        return try {
            // Implement version check logic
            Result.success(false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}