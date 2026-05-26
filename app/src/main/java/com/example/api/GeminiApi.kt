package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a meteorological analysis prompt to the Gemini API and returns the text response.
     * Falls back to a local intelligent message if the key is empty, invalid, or query fails.
     */
    suspend fun getStormAnalysis(
        stormName: String,
        pressureHpa: Int,
        windsMph: Int,
        cellDetails: String,
        isGridWarping: Boolean,
        activeLightningRate: Int,
        historicalContext: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is placeholder or empty. Falling back to local offline analysis.")
            return@withContext getLocalOfflineAnalysis(stormName, pressureHpa, windsMph, cellDetails, isGridWarping, historicalContext)
        }

        val historicalHeader = if (historicalContext != null) {
            "NOTE: This discussion is specifically simulating and reconstructing the HISTORICAL WEATHER EVENT: $historicalContext.\n\n"
        } else ""

        val prompt = """
            You are an elite, senior National Hurricane Center meteorologist and synoptic analyst.
            ${historicalHeader}Provide a professional, immersive, and scientifically detailed real-time weather briefing/synoptic discussion for the following active storm system:
            
            System Name: $stormName
            Central Pressure: ${pressureHpa} hPa (Atmospheric Dip: ${if (pressureHpa < 950) "EXTREME LOW" else "MODERATE PRESSURE DEPLETION"})
            Sustained Winds: ${windsMph} mph
            Core Active Cells: $cellDetails
            3D Grid Warping Display Status: ${if (isGridWarping) "ENABLED (indicating severe barobathic depression warping)" else "DISABLED"}
            Real-time Lightning Rate: $activeLightningRate strikes/min
            
            Structure your analysis with these exact sections (using elegant standard markdown headings, no self-praise or sales-pitch language):
            
            ### 1. SYNOPTIC SUMMARY
            Explain the fluid dynamics and the thermodynamic state of the atmospheric pressure fields driving this wind orbit. Cover the pressure gradient force and Coriolis parameters shaping the simulated streamflow.
            
            ### 2. CORE THERMAL CONVECTION & SHEAR
            Detail what the reflectivity coordinates (relative echo tops, precipitation dBZ profiles) indicate about the internal updraft velocities and convective instability.
            
            ### 3. ACTIONABLE ALERTS & RECOMMENDATIONS
            Output specific aviation, marine, or civil warnings (e.g. Gale Warnings, extreme wind shear hazards, potential storm surgence, or microburst dangers) in a concise warning bulletin style.
            
            Keep the tone professional, objective, and deeply scientific. Avoid marketing hype or flowery adjectives.
        """.trimIndent()

        try {
            // Build the JSON request body
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // Add temperature constraints for objective outputs
                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestBodyJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API failed with code ${response.code}: $errBody")
                    return@withContext "Meteorological uplink unavailable (HTTP ${response.code}). Loading standby physical report:\n\n${getLocalOfflineAnalysis(stormName, pressureHpa, windsMph, cellDetails, isGridWarping)}"
                }

                val resBody = response.body?.string() ?: return@withContext "Uplink returned empty atmospheric envelope."
                val jsonRes = JSONObject(resBody)
                val candidates = jsonRes.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
                "Uplink decoding failed. Displaying core barometric values: sustained at $windsMph mph with $pressureHpa hPa center."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Gemini API", e)
            return@withContext "Meteorological connection state warning: ${e.localizedMessage ?: "Network Timeout"}. Standby telemetry:\n\n${getLocalOfflineAnalysis(stormName, pressureHpa, windsMph, cellDetails, isGridWarping, historicalContext)}"
        }
    }

    private fun getLocalOfflineAnalysis(
        stormName: String,
        pressureHpa: Int,
        windsMph: Int,
        cellDetails: String,
        isGridWarping: Boolean,
        historicalContext: String? = null
    ): String {
        val stormSeverity = when {
            windsMph > 156 -> "Category 5 Extreme Super-Cyclone"
            windsMph > 111 -> "Category 3 Major Storm Front"
            windsMph > 74 -> "Category 1 Sustained Storm Force"
            else -> "Tropical Synoptic Depression"
        }
        
        val historicalPrefix = if (historicalContext != null) {
            "🔴 **HISTORICAL RECONSTRUCTION REPORT ACTIVE ($historicalContext)**\n\n"
        } else ""

        return """
            $historicalPrefix### 1. SYNOPTIC SUMMARY (LOCAL REPORT)
            Active storm system **$stormName** currently classified as a **$stormSeverity** with a measured core central pressure of **$pressureHpa hPa**.
            The horizontal pressure gradient force is triggering extreme wind flow velocities of up to **$windsMph mph**, resulting in high cyclonic vortex drag values.
            
            ### 2. CORE THERMAL CONVECTION & SHEAR
            Active cell echoes detect extreme convection fields:
            $cellDetails
            ${if (isGridWarping) "Severe atmospheric thinning is active on the 3D grid, reflecting a deeply pressed storm core with high vertical updraft speeds." else "Localized vertical convection indicates active thermal instability."}
            
            ### 3. ACTIONABLE ALERTS & RECOMMENDATIONS
            *   **AV WARNING**: Severe structural wind shear and microburst hazards near coordinate quadrants. 
            *   **MAR WARNING**: Extreme marine storm surge heights of 12-18ft expected adjacent to core pressure centers. Complete vessel evacuation advised.
            *   **CIV ALERTS**: High lightning flash incidence rate. Standby by storm shelters and avoid outer exposure.
        """.trimIndent()
    }
}
