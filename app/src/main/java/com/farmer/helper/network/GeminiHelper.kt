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
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val API_KEY = "AIzaSyDk22pCuUhHHQHvaunXX8XxJtkgdxPeNeM" // replace with your actual key

    // ✅ Reuse OkHttp client with longer timeouts
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // connect to server
            .readTimeout(60, TimeUnit.SECONDS)    // wait for server response
            .writeTimeout(60, TimeUnit.SECONDS)   // send request body
            .build()
    }

    suspend fun getResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ Build valid JSON body
                val userContent = JSONObject()
                    .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))

                val json = JSONObject()
                    .put("contents", JSONArray().put(userContent))

                val requestBodyString = json.toString()

                // 🔍 Log request JSON
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

                // 🔍 Log raw response
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
