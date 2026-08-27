package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.BuildConfig
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.example.nhatkyduonghuyet.data.remote.GeminiBackendClient
import com.example.nhatkyduonghuyet.data.remote.GeminiBackendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface CloudInsightResult {
    data class Success(val insight: String) : CloudInsightResult
    data class Failure(val reason: String) : CloudInsightResult
}

class GeminiAnalysisUseCase @Inject constructor(
    private val backendClient: GeminiBackendClient
) {

    suspend fun getAnalysis(
        historyData: String,
        forecastData: MultiStepResult?,
        isEnglish: Boolean = false
    ): CloudInsightResult = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.GEMINI_BACKEND_URL.trim()
        if (endpoint.isBlank()) {
            return@withContext CloudInsightResult.Failure(
                if (isEnglish) "AI backend is not configured." else "Chưa cấu hình máy chủ AI."
            )
        }

        val historyText = historyData.ifBlank {
            if (isEnglish) "No history available." else "Không có dữ liệu lịch sử."
        }

        when (val result = backendClient.generateInsight(
            endpoint = endpoint,
            historyData = historyText,
            forecastData = forecastData,
            language = if (isEnglish) "en" else "vi"
        )) {
            is GeminiBackendResult.Success -> CloudInsightResult.Success(result.insight)
            is GeminiBackendResult.Failure -> CloudInsightResult.Failure(
                friendlyError(result.code, isEnglish)
            )
        }
    }

    private fun friendlyError(code: String, isEnglish: Boolean): String = when (code) {
        "BACKEND_NOT_CONFIGURED" -> if (isEnglish) {
            "AI backend is not configured."
        } else {
            "Chưa cấu hình máy chủ AI."
        }
        "UNAUTHORIZED" -> if (isEnglish) {
            "Your session is not authorized for AI analysis."
        } else {
            "Phiên đăng nhập không có quyền phân tích AI."
        }
        "RATE_LIMITED" -> if (isEnglish) {
            "AI usage limit reached. Please try again later."
        } else {
            "Đã đạt giới hạn sử dụng AI. Vui lòng thử lại sau."
        }
        "BACKEND_ERROR" -> if (isEnglish) {
            "The AI service is temporarily unavailable."
        } else {
            "Dịch vụ AI tạm thời không khả dụng."
        }
        else -> if (isEnglish) {
            "Unable to connect to the AI service. Please check your network and try again."
        } else {
            "Không thể kết nối dịch vụ AI. Hãy kiểm tra mạng và thử lại."
        }
    }
}
