package com.daxido.core.maps

import android.content.Context
import android.util.Log
import com.daxido.core.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Places API service for location search and autocomplete
 */
@Singleton
class PlacesApiService @Inject constructor(
    private val context: Context
) {
    
    private val apiKey = AppConfig.GOOGLE_PLACES_API_KEY
    private val baseUrl = "https://maps.googleapis.com/maps/api/place"
    
    /**
     * Search for places using autocomplete
     */
    fun searchPlaces(query: String): Flow<List<PlaceSearchResult>> = flow {
        try {
            val results = withContext(Dispatchers.IO) {
                searchPlacesHttp(query)
            }
            emit(results)
        } catch (e: Exception) {
            Log.e("PlacesApiService", "Error searching places", e)
            emit(emptyList())
        }
    }
    
    private suspend fun searchPlacesHttp(query: String): List<PlaceSearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/autocomplete/json?input=$encodedQuery&key=$apiKey&types=establishment"
        
        return try {
            val response = makeHttpRequest(url)
            parseAutocompleteResponse(response)
        } catch (e: Exception) {
            Log.e("PlacesApiService", "HTTP request failed", e)
            emptyList()
        }
    }
    
    /**
     * Get place details by place ID
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails? {
        return try {
            val url = "$baseUrl/details/json?place_id=$placeId&key=$apiKey&fields=place_id,name,formatted_address,geometry,types,formatted_phone_number,rating,price_level"
            val response = withContext(Dispatchers.IO) { makeHttpRequest(url) }
            parsePlaceDetailsResponse(response)
        } catch (e: Exception) {
            Log.e("PlacesApiService", "Error fetching place details", e)
            null
        }
    }
    
    /**
     * Search for nearby places
     */
    fun searchNearbyPlaces(
        latitude: Double,
        longitude: Double,
        radius: Int = 1000,
        type: String? = null
    ): Flow<List<PlaceDetails>> = flow {
        try {
            val results = withContext(Dispatchers.IO) {
                searchNearbyPlacesHttp(latitude, longitude, radius, type)
            }
            emit(results)
        } catch (e: Exception) {
            Log.e("PlacesApiService", "Error searching nearby places", e)
            emit(emptyList())
        }
    }
    
    private suspend fun searchNearbyPlacesHttp(
        latitude: Double,
        longitude: Double,
        radius: Int,
        type: String?
    ): List<PlaceDetails> {
        val location = "$latitude,$longitude"
        val typeParam = type?.let { "&type=$it" } ?: ""
        val url = "$baseUrl/nearbysearch/json?location=$location&radius=$radius&key=$apiKey$typeParam"
        
        return try {
            val response = makeHttpRequest(url)
            parseNearbyPlacesResponse(response)
        } catch (e: Exception) {
            Log.e("PlacesApiService", "HTTP request failed", e)
            emptyList()
        }
    }
    
    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            
            reader.close()
            inputStream.close()
            connection.disconnect()
            
            return response.toString()
        } else {
            connection.disconnect()
            throw Exception("HTTP error: $responseCode")
        }
    }
    
    private fun parseAutocompleteResponse(response: String): List<PlaceSearchResult> {
        return try {
            val json = JSONObject(response)
            val predictionsArray = json.getJSONArray("predictions")
            val results = mutableListOf<PlaceSearchResult>()
            
            for (i in 0 until predictionsArray.length()) {
                val prediction = predictionsArray.getJSONObject(i)
                val result = PlaceSearchResult(
                    placeId = prediction.getString("place_id"),
                    primaryText = prediction.getJSONObject("structured_formatting").getString("main_text"),
                    secondaryText = prediction.getJSONObject("structured_formatting").optString("secondary_text", ""),
                    fullText = prediction.getString("description")
                )
                results.add(result)
            }
            
            results
        } catch (e: Exception) {
            Log.e("PlacesApiService", "Error parsing autocomplete response", e)
            emptyList()
        }
    }
    
    private fun parsePlaceDetailsResponse(response: String): PlaceDetails? {
        return try {
            val json = JSONObject(response)
            val result = json.getJSONObject("result")
            val geometry = result.getJSONObject("geometry")
            val location = geometry.getJSONObject("location")
            
            PlaceDetails(
                placeId = result.getString("place_id"),
                name = result.getString("name"),
                address = result.getString("formatted_address"),
                latitude = location.getDouble("lat"),
                longitude = location.getDouble("lng"),
                types = parseTypes(result.optJSONArray("types")),
                phoneNumber = result.optString("formatted_phone_number", null),
                rating = result.optDouble("rating").takeIf { !it.isNaN() }?.toFloat(),
                priceLevel = result.optInt("price_level").takeIf { it > 0 }
            )
        } catch (e: Exception) {
            Log.e("PlacesApiService", "Error parsing place details response", e)
            null
        }
    }
    
    private fun parseNearbyPlacesResponse(response: String): List<PlaceDetails> {
        return try {
            val json = JSONObject(response)
            val resultsArray = json.getJSONArray("results")
            val results = mutableListOf<PlaceDetails>()
            
            for (i in 0 until resultsArray.length()) {
                val result = resultsArray.getJSONObject(i)
                val geometry = result.getJSONObject("geometry")
                val location = geometry.getJSONObject("location")
                
                val placeDetails = PlaceDetails(
                    placeId = result.getString("place_id"),
                    name = result.getString("name"),
                    address = result.optString("vicinity", ""),
                    latitude = location.getDouble("lat"),
                    longitude = location.getDouble("lng"),
                    types = parseTypes(result.optJSONArray("types")),
                    phoneNumber = null,
                    rating = result.optDouble("rating").takeIf { !it.isNaN() }?.toFloat(),
                    priceLevel = result.optInt("price_level").takeIf { it > 0 }
                )
                results.add(placeDetails)
            }
            
            results
        } catch (e: Exception) {
            Log.e("PlacesApiService", "Error parsing nearby places response", e)
            emptyList()
        }
    }
    
    private fun parseTypes(typesArray: JSONArray?): List<String> {
        if (typesArray == null) return emptyList()
        
        val types = mutableListOf<String>()
        for (i in 0 until typesArray.length()) {
            types.add(typesArray.getString(i))
        }
        return types
    }
}

/**
 * Data class for place search results
 */
data class PlaceSearchResult(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

/**
 * Data class for place details
 */
data class PlaceDetails(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val types: List<String>,
    val phoneNumber: String?,
    val rating: Float?,
    val priceLevel: Int?
)
