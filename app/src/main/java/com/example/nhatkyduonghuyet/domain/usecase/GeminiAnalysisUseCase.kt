package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.BuildConfig
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject

sealed interface CloudInsightResult {
    data class Success(val insight: String) : CloudInsightResult
    data class Failure(val reason: String) : CloudInsightResult
}

/**
 * Calls the app-owned insight backend. The backend, not this Android app, owns
 * the Gemini credential and is responsible for authentication, consent and rate limiting.
 */
class GeminiAnalysisUseCase @Inject constructor() {

    suspend fun getAnalysis(historyData: String, forecastData: MultiStepResult?): CloudInsightResult =
        withContext(Dispatchers.IO) {
            val endpoint = BuildConfig.GEMINI_BACKEND_URL.trim()
            if (endpoint.isEmpty()) {
                return@withContext CloudInsightResult.Failure(
                    "Phân tích AI chưa được cấu hình bởi nhà cung cấp dịch vụ."
                )
            }

            val uri = try {
                URI(endpoint)
            } catch (_: Exception) {
                return@withContext CloudInsightResult.Failure("Địa chỉ máy chủ phân tích AI không hợp lệ.")
            }
            if (uri.scheme != "https") {
                return@withContext CloudInsightResult.Failure("Máy chủ phân tích AI phải sử dụng HTTPS.")
            }

            try {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val requestBody = JSONObject().apply {
                        put("history", historyData)
                        put("forecast", forecastData?.toJson())
                    }.toString()
                    connection.outputStream.use { output ->
                        output.write(requestBody.toByteArray(StandardCharsets.UTF_8))
                    }

                    if (connection.responseCode !in 200..299) {
                        return@withContext CloudInsightResult.Failure(
                            "Máy chủ phân tích AI hiện không khả dụng (HTTP ${connection.responseCode})."
                        )
                    }

                    val response = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    val insight = JSONObject(response).optString("insight").trim()
                    if (insight.isBlank()) {
                        CloudInsightResult.Failure("Máy chủ phân tích AI không trả về nội dung hợp lệ.")
                    } else {
                        CloudInsightResult.Success(insight)
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                CloudInsightResult.Failure("Không thể kết nối máy chủ phân tích AI. Vui lòng thử lại sau.")
            }
        }

    private fun MultiStepResult.toJson(): JSONObject = JSONObject().apply {
        put("hourlyForecasts", JSONArray(hourlyForecasts))
        put("minExpected", String.format(Locale.US, "%.2f", minExpected).toDouble())
        put("maxExpected", String.format(Locale.US, "%.2f", maxExpected).toDouble())
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 20_000
    }
}