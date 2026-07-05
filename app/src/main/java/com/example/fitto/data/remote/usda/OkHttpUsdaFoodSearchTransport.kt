package com.example.fitto.data.remote.usda

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal class OkHttpUsdaFoodSearchTransport(
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient()
) : UsdaFoodSearchTransport {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun search(
        query: String,
        pageSize: Int,
        pageNumber: Int
    ): String? {
        val url = "https://api.nal.usda.gov/fdc/v1/foods/search"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("api_key", apiKey.ifBlank { "DEMO_KEY" })
            .build()

        val requestJson = JSONObject()
            .put("query", query)
            .put("pageSize", pageSize.coerceIn(1, 20))
            .put("pageNumber", pageNumber.coerceAtLeast(1))
            .put(
                "dataType",
                JSONArray()
                    .put("Foundation")
                    .put("SR Legacy")
                    .put("Survey (FNDDS)")
                    .put("Branded")
            )

        Log.d(TAG, "USDA request query=$query")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "USDA response code=${response.code}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "USDA request failed. code=${response.code}")
                    return null
                }

                val body = response.body?.string().orEmpty()
                Log.d(TAG, "USDA body length=${body.length}")
                body.takeIf { it.isNotBlank() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "USDA request exception", exception)
            null
        }
    }

    private companion object {
        const val TAG = "UsdaFoodClient"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()
        }
    }
}
