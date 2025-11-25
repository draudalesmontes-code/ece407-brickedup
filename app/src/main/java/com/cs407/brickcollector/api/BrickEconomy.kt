package com.cs407.brickcollector.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

data class SetData(
    val setNumber: String?,
    val name: String?,
    val theme: String?,
    val year: Int?,
    val piecesCount: Int?,
    val minifigsCount: Int?,
    val retired: Boolean?,
    val retailPriceUs: Double?,
    val currentValueNew: Double?,
    val currentValueUsed: Double?,
    val rollingGrowthLastyear: Double?,
    val priceEventsNew: List<PriceEvent>?
)

data class PriceEvent(
    val date: String,
    val value: Double
)

class BrickEconomyAPI(private val apiKey: String) {
    private val baseUrl = "https://www.brickeconomy.com/api/v1"
    private val client = OkHttpClient()

    /**
     * Get information about a LEGO set by its set number
     *
     * @param setNumber LEGO set number (e.g., "10236-1" or "10236")
     * @param currency Currency code (ISO 4217 format), defaults to USD
     * @return SetData object containing set information or null if error
     */
    suspend fun getSetInfo(setNumber: String, currency: String = "USD"): SetData? {
        return withContext(Dispatchers.IO) {
            val urlBuilder = StringBuilder("$baseUrl/set/$setNumber")
            if (currency != "USD") {
                urlBuilder.append("?currency=$currency")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "LegoPricingApp/1.0")
                .addHeader("x-apikey", apiKey)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        println("Error: HTTP ${response.code}")
                        return@withContext null
                    }

                    val responseBody = response.body?.string() ?: return@withContext null
                    val jsonObject = JSONObject(responseBody)
                    val data = jsonObject.optJSONObject("data") ?: return@withContext null

                    parseSetData(data)
                }
            } catch (e: IOException) {
                println("Error fetching set data: ${e.message}")
                null
            } catch (e: Exception) {
                println("Error parsing set data: ${e.message}")
                null
            }
        }
    }

    private fun parseSetData(data: JSONObject): SetData {
        val priceEvents = mutableListOf<PriceEvent>()
        val priceEventsArray = data.optJSONArray("price_events_new")
        if (priceEventsArray != null) {
            for (i in 0 until priceEventsArray.length()) {
                val event = priceEventsArray.getJSONObject(i)
                priceEvents.add(
                    PriceEvent(
                        date = event.optString("date", "N/A"),
                        value = event.optDouble("value", 0.0)
                    )
                )
            }
        }

        return SetData(
            setNumber = data.optString("set_number", null),
            name = data.optString("name", null),
            theme = data.optString("theme", null),
            year = if (data.has("year")) data.optInt("year") else null,
            piecesCount = if (data.has("pieces_count")) data.optInt("pieces_count") else null,
            minifigsCount = if (data.has("minifigs_count")) data.optInt("minifigs_count") else null,
            retired = if (data.has("retired")) data.optBoolean("retired") else null,
            retailPriceUs = if (data.has("retail_price_us")) data.optDouble("retail_price_us") else null,
            currentValueNew = if (data.has("current_value_new")) data.optDouble("current_value_new") else null,
            currentValueUsed = if (data.has("current_value_used")) data.optDouble("current_value_used") else null,
            rollingGrowthLastyear = if (data.has("rolling_growth_lastyear")) data.optDouble("rolling_growth_lastyear") else null,
            priceEventsNew = priceEvents.ifEmpty { null }
        )
    }

    /**
     * Format set information for display
     */
    fun formatSetInfo(setData: SetData?): String {
        if (setData == null) {
            return "No set data available"
        }

        return buildString {
            appendLine("Set: ${setData.setNumber ?: "N/A"} - ${setData.name ?: "N/A"}")
            appendLine("Theme: ${setData.theme ?: "N/A"}")
            appendLine("Year: ${setData.year ?: "N/A"}")
            appendLine("Pieces: ${setData.piecesCount ?: "N/A"}")
            appendLine("Minifigs: ${setData.minifigsCount ?: "N/A"}")
            appendLine("Retired: ${if (setData.retired == true) "Yes" else "No"}")
            appendLine("Original Price: $${setData.retailPriceUs ?: "N/A"}")
            appendLine("Current Value (New): $${setData.currentValueNew ?: "N/A"}")
            appendLine("Current Value (Used): $${setData.currentValueUsed ?: "N/A"}")
            append("Growth (Last Year): ${setData.rollingGrowthLastyear ?: "N/A"}%")
        }
    }
}

// Example usage in an Activity or ViewModel
suspend fun exampleUsage() {
    // Replace with your actual API key
    val apiKey = ""

    // Initialize the API client
    val legoApi = BrickEconomyAPI(apiKey)

    // Test with a popular set
    val setNumber = "10236-1" // Ewok Village

    println("Fetching data for set $setNumber...")
    val setData = legoApi.getSetInfo(setNumber)

    if (setData != null) {
        println("\n" + "=".repeat(50))
        println(legoApi.formatSetInfo(setData))
        println("=".repeat(50))

        // Show some additional price history
        val priceEvents = setData.priceEventsNew?.take(5) // Last 5 entries
        if (priceEvents != null && priceEvents.isNotEmpty()) {
            println("\nRecent Price History (New):")
            priceEvents.forEach { event ->
                println("  ${event.date}: $${event.value}")
            }
        }
    } else {
        println("Failed to fetch set data")
    }
}