package com.example.fitto.data.remote.logmeal

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

internal interface LogMealApiSource {
    val isConfigured: Boolean

    fun uploadForSegmentation(
        uploadFile: File,
        languageCode: String
    ): String?

    fun fetchNutrition(imageId: String): String?
}

internal class OkHttpLogMealApiSource(
    private val apiUserToken: String,
    private val client: OkHttpClient = defaultClient()
) : LogMealApiSource {

    override val isConfigured: Boolean
        get() = apiUserToken.isNotBlank()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun uploadForSegmentation(
        uploadFile: File,
        languageCode: String
    ): String? {
        val url = "https://api.logmeal.com/v2/image/segmentation/complete/v1.0"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("language", languageCode.toLogMealLanguage())
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "image",
                filename = uploadFile.name,
                body = uploadFile.asRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiUserToken")
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        Log.d(TAG, "LogMeal request started. file=${uploadFile.name} size=${uploadFile.length()}")

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "LogMeal response code=${response.code}")
                Log.d(TAG, "LogMeal body length=${body.length}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "LogMeal request failed. code=${response.code} body=$body")
                    return null
                }

                body.takeIf { it.isNotBlank() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "LogMeal request exception", exception)
            null
        }
    }

    override fun fetchNutrition(imageId: String): String? {
        val url = "https://api.logmeal.com/v2/nutrition/recipe/nutritionalInfo/v1.0"
            .toHttpUrl()
            .newBuilder()
            .build()

        val requestJson = JSONObject().put("imageId", imageId)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiUserToken")
            .header("Accept", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        Log.d(TAG, "LogMeal nutrition request started. imageId=$imageId")

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "LogMeal nutrition response code=${response.code}")
                Log.d(TAG, "LogMeal nutrition body length=${body.length}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "LogMeal nutrition failed. code=${response.code} body=$body")
                    return null
                }

                body.takeIf { it.isNotBlank() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "LogMeal nutrition request exception", exception)
            null
        }
    }

    private fun String.toLogMealLanguage(): String {
        return when (lowercase(Locale.ROOT)) {
            "tr", "tur" -> "tur"
            "es", "spa" -> "spa"
            "fr", "fre", "fra" -> "fre"
            "de", "ger", "deu" -> "ger"
            "it", "ita" -> "ita"
            "nl", "dut", "nld" -> "dut"
            else -> "eng"
        }
    }

    private companion object {
        const val TAG = "LogMealClient"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
