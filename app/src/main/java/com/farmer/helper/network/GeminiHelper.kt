package com.farmer.helper.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object GeminiHelper {
    private const val API_KEY = "AIzaSyDk22pCuUhHHQHvaunXX8XxJtkgdxPeNeM"

    suspend fun getResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // Build proper prompt JSON
                val messageContent = JSONObject().put("type", "text").put("text", userMessage)
                val messagesArray = JSONArray().put(JSONObject().put("author", "user").put("content", JSONArray().put(messageContent)))
                val prompt = JSONObject().put("messages", messagesArray)

                val json = JSONObject()
                    .put("model", "gemini-1.5-flash")
                    .put("temperature", 0.7)
                    .put("candidateCount", 1)
                    .put("topP", 0.95)
                    .put("prompt", prompt)

                val body = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    return@withContext "Error: ${response.code} ${response.message}"
                }

                val jsonResponse = JSONObject(responseBody)

                // Handle API errors
                if (jsonResponse.has("error")) {
                    val errorMsg = jsonResponse.getJSONObject("error").optString("message", "Unknown error")
                    return@withContext "Error: $errorMsg"
                }

                // Extract AI response
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .optString("text", "")
                    if (content.isNotBlank()) return@withContext content
                }

                return@withContext "No response from Gemini."
            } catch (e: Exception) {
                return@withContext "Error: ${e.message}"
            }
        }
    }
}
