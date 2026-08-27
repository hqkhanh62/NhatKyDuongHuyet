package com.example.nhatkyduonghuyet.data.remote

import com.example.nhatkyduonghuyet.ai.MultiStepResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GeminiBackendResult {
    data class Success(val insight: String) : GeminiBackendResult
    data class Failure(val code: String, val message: String? = null) : GeminiBackendResult
}

@Singleton
class GeminiBackendClient @Inject constructor() {

    suspend fun generateInsight(
        endpoint: String,
        historyData: String,
        forecastData: MultiStepResult?,
        language: String
    ): GeminiBackendResult = withContext(Dispatchers.IO) {
        if (endpoint.isBlank() || !endpoint.startsWith("https://")) {
            return@withContext GeminiBackendResult.Failure("BACKEND_NOT_CONFIGURED")
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        try {
            val payload = JSONObject().apply {
                put("history", historyData)
                put("language", language)
                put("forecast", forecastData?.toJson() ?: JSONObject.NULL)
            }

            connection.outputStream.use { output ->
                output.writer(Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                }
            }

            val responseBody = readResponse(connection)
            val responseJson = runCatching { JSONObject(responseBody) }.getOrNull()
            val insight = responseJson?.optString("insight")?.trim().orEmpty()

            when {
                connection.responseCode in 200..299 && insight.isNotBlank() -> {
                    GeminiBackendResult.Success(insight)
                }
                connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    GeminiBackendResult.Failure("UNAUTHORIZED")
                }
                connection.responseCode == HTTP_RATE_LIMITED -> {
                    GeminiBackendResult.Failure("RATE_LIMITED")
                }
                else -> {
                    GeminiBackendResult.Failure(
                        code = "BACKEND_ERROR",
                        message = responseJson?.optString("message")
                    )
                }
            }
        } catch (error: Exception) {
            GeminiBackendResult.Failure("NETWORK_ERROR", error.message)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    private fun MultiStepResult.toJson(): JSONObject = JSONObject().apply {
        put("hourlyForecasts", JSONArray(hourlyForecasts.toList()))
        put("minExpected", minExpected)
        put("maxExpected", maxExpected)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 20_000
        const val HTTP_RATE_LIMITED = 429
    }
}
