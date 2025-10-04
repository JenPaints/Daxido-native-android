package com.daxido.core.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for interacting with Google AI SDK (Gemini API)
 * Provides AI-powered features for the Daxido ride-sharing app
 */
@Singleton
class FirebaseAiLogicService @Inject constructor() {
    
    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = com.daxido.BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024
            }
        )
    }
    
    companion object {
        private const val TAG = "FirebaseAiLogicService"
    }
    
    /**
     * Generate AI-powered route suggestions based on user preferences and traffic
     */
    suspend fun generateRouteSuggestions(
        origin: String,
        destination: String,
        preferences: String = "",
        trafficConditions: String = ""
    ): Result<String> {
        return try {
            val prompt = buildString {
                appendLine("You are an AI assistant for Daxido ride-sharing app.")
                appendLine("Generate smart route suggestions for a ride from '$origin' to '$destination'.")
                
                if (preferences.isNotEmpty()) {
                    appendLine("User preferences: $preferences")
                }
                
                if (trafficConditions.isNotEmpty()) {
                    appendLine("Current traffic conditions: $trafficConditions")
                }
                
                appendLine()
                appendLine("Provide:")
                appendLine("1. Primary route recommendation with reasoning")
                appendLine("2. Alternative route options")
                appendLine("3. Estimated time and distance for each route")
                appendLine("4. Traffic considerations and tips")
                appendLine()
                appendLine("Format your response in a user-friendly way for a mobile app.")
            }
            
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }
            val result = response.text ?: "Unable to generate route suggestions at this time."
            
            Log.d(TAG, "Route suggestions generated successfully")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating route suggestions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate dynamic pricing recommendations based on demand and conditions
     */
    suspend fun generatePricingRecommendations(
        baseFare: Double,
        demandLevel: String,
        weatherConditions: String = "",
        timeOfDay: String = ""
    ): Result<String> {
        return try {
            val prompt = buildString {
                appendLine("You are an AI pricing assistant for Daxido ride-sharing app.")
                appendLine("Analyze pricing for a ride with base fare: ₹$baseFare")
                appendLine("Current demand level: $demandLevel")
                
                if (weatherConditions.isNotEmpty()) {
                    appendLine("Weather conditions: $weatherConditions")
                }
                
                if (timeOfDay.isNotEmpty()) {
                    appendLine("Time of day: $timeOfDay")
                }
                
                appendLine()
                appendLine("Provide:")
                appendLine("1. Recommended fare adjustment with reasoning")
                appendLine("2. Surge pricing recommendations if applicable")
                appendLine("3. Alternative pricing strategies")
                appendLine("4. Customer communication suggestions")
                appendLine()
                appendLine("Format your response for a driver dashboard.")
            }
            
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }
            val result = response.text ?: "Unable to generate pricing recommendations at this time."
            
            Log.d(TAG, "Pricing recommendations generated successfully")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating pricing recommendations", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate intelligent driver matching suggestions
     */
    suspend fun generateDriverMatchingSuggestions(
        riderLocation: String,
        riderPreferences: String = "",
        availableDrivers: String = ""
    ): Result<String> {
        return try {
            val prompt = buildString {
                appendLine("You are an AI driver matching assistant for Daxido ride-sharing app.")
                appendLine("Help match a rider at location: $riderLocation")
                
                if (riderPreferences.isNotEmpty()) {
                    appendLine("Rider preferences: $riderPreferences")
                }
                
                if (availableDrivers.isNotEmpty()) {
                    appendLine("Available drivers: $availableDrivers")
                }
                
                appendLine()
                appendLine("Provide:")
                appendLine("1. Driver matching criteria and reasoning")
                appendLine("2. Optimal driver selection strategy")
                appendLine("3. Factors to consider (distance, rating, vehicle type)")
                appendLine("4. Recommendations for improving matching")
                appendLine()
                appendLine("Format your response for a driver allocation system.")
            }
            
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }
            val result = response.text ?: "Unable to generate driver matching suggestions at this time."
            
            Log.d(TAG, "Driver matching suggestions generated successfully")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating driver matching suggestions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate customer support responses for common queries
     */
    suspend fun generateCustomerSupportResponse(
        userQuery: String,
        userContext: String = "",
        previousMessages: String = ""
    ): Result<String> {
        return try {
            val prompt = buildString {
                appendLine("You are a helpful AI customer support assistant for Daxido ride-sharing app.")
                appendLine("User query: $userQuery")
                
                if (userContext.isNotEmpty()) {
                    appendLine("User context: $userContext")
                }
                
                if (previousMessages.isNotEmpty()) {
                    appendLine("Previous conversation: $previousMessages")
                }
                
                appendLine()
                appendLine("Provide:")
                appendLine("1. A helpful and empathetic response")
                appendLine("2. Clear solution or next steps")
                appendLine("3. Additional helpful information")
                appendLine("4. Escalation suggestions if needed")
                appendLine()
                appendLine("Keep the response concise and mobile-friendly.")
            }
            
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }
            val result = response.text ?: "I'm here to help! Please contact our support team for assistance."
            
            Log.d(TAG, "Customer support response generated successfully")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating customer support response", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate demand forecasting insights
     */
    suspend fun generateDemandForecast(
        location: String,
        timeRange: String,
        historicalData: String = "",
        events: String = ""
    ): Result<String> {
        return try {
            val prompt = buildString {
                appendLine("You are an AI demand forecasting assistant for Daxido ride-sharing app.")
                appendLine("Analyze demand for location: $location")
                appendLine("Time range: $timeRange")
                
                if (historicalData.isNotEmpty()) {
                    appendLine("Historical data: $historicalData")
                }
                
                if (events.isNotEmpty()) {
                    appendLine("Special events: $events")
                }
                
                appendLine()
                appendLine("Provide:")
                appendLine("1. Demand forecast with confidence levels")
                appendLine("2. Peak hours and low-demand periods")
                appendLine("3. Driver allocation recommendations")
                appendLine("4. Pricing strategy suggestions")
                appendLine()
                appendLine("Format your response for a driver dashboard.")
            }
            
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }
            val result = response.text ?: "Unable to generate demand forecast at this time."
            
            Log.d(TAG, "Demand forecast generated successfully")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating demand forecast", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate general AI response for any prompt
     */
    suspend fun generateResponse(prompt: String): Result<String> {
        return try {
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }
            val result = response.text ?: "Unable to generate response at this time."
            
            Log.d(TAG, "General response generated successfully")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating general response", e)
            Result.failure(e)
        }
    }
}
