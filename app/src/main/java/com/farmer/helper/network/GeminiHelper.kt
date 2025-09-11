package com.farmer.helper.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val API_KEY = "AIzaSyDj5PAGNIjX0_y6juEje5Gi0nw5JQ1LbsM" // replace with your real key
    private const val MODEL = "gemini-1.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // Existing text-only query
    suspend fun getResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestJson = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                    ))

                val requestBody = requestJson.toString()
                    .toRequestBody("application/json".toMediaType())

                val url = "${BASE_URL}${MODEL}:generateContent?key=$API_KEY"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("GeminiHelper", "➡️ Request JSON: $requestJson")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("GeminiHelper", "❌ API call failed: ${response.code} - ${response.message}")
                    return@withContext "Error ${response.code}: ${response.message}"
                }

                Log.d("GeminiHelper", "✅ Response body: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "")
                    }
                }

                return@withContext "No response from Gemini."
            } catch (e: Exception) {
                Log.e("GeminiHelper", "⚠️ Exception in getResponse", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }

    // ✅ New function: send text + image
    suspend fun sendQueryWithImage(context: Context, userMessage: String, imageUri: Uri?): String {
        return withContext(Dispatchers.IO) {
            try {
                val partsArray = JSONArray()
                // Add text part
                partsArray.put(JSONObject().put("text", userMessage))

                // Add image part if provided
                imageUri?.let { uri ->
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    val base64Image = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    if (base64Image != null) {
                        partsArray.put(JSONObject().put("image", base64Image))
                    }
                }

                val requestJson = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("parts", partsArray)
                    ))

                val requestBody = requestJson.toString()
                    .toRequestBody("application/json".toMediaType())

                val url = "${BASE_URL}${MODEL}:generateContent?key=$API_KEY"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("GeminiHelper", "➡️ Request JSON (image query): $requestJson")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("GeminiHelper", "❌ API call failed: ${response.code} - ${response.message}")
                    return@withContext "Error ${response.code}: ${response.message}"
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "")
                    }
                }

                return@withContext "No response from Gemini."
            } catch (e: Exception) {
                Log.e("GeminiHelper", "⚠️ Exception in sendQueryWithImage", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }
}
