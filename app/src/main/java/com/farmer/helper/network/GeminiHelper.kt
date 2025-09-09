package com.farmer.helper.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object GeminiHelper {
    private const val API_KEY = "AIzaSyDk22pCuUhHHQHvaunXX8XxJtkgdxPeNeM" // replace with your actual key

    suspend fun getResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // ✅ Build minimal valid JSON
                val userContent = JSONObject()
                    .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))

                val json = JSONObject()
                    .put("contents", JSONArray().put(userContent))

                val requestBodyString = json.toString()

                // 🔍 Log the request JSON
                Log.d("GeminiHelper", "Request JSON: $requestBodyString")

                val requestBody = requestBodyString
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("GeminiHelper", "API call failed: ${response.code} - ${response.message}")
                    return@withContext "Error: ${response.code} ${response.message}"
                }

                // 🔍 Log the raw response
                Log.d("GeminiHelper", "Response body: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) return@withContext text
                    }
                }

                return@withContext "No response from Gemini."
            } catch (e: Exception) {
                Log.e("GeminiHelper", "Exception in getResponse", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }
}
