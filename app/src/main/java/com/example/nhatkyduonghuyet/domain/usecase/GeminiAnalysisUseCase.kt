package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.BuildConfig
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface CloudInsightResult {
    data class Success(val insight: String) : CloudInsightResult
    data class Failure(val reason: String) : CloudInsightResult
}

class GeminiAnalysisUseCase @Inject constructor() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun getAnalysis(historyData: String, forecastData: MultiStepResult?, isEnglish: Boolean = false): CloudInsightResult =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey == "" || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                val errorMsg = if (isEnglish) "API Key not configured." else "Chưa cấu hình API Key cho Gemini."
                return@withContext CloudInsightResult.Failure(errorMsg)
            }

            try {
                val forecastInfo = if (isEnglish) {
                    forecastData?.let {
                        "Next 24h forecast (LSTM): ${it.hourlyForecasts.joinToString(", ")} mmol/L. " +
                        "Range: ${String.format("%.1f", it.minExpected)} - ${String.format("%.1f", it.maxExpected)} mmol/L."
                    } ?: "No LSTM forecast data available."
                } else {
                    forecastData?.let {
                        "Dự báo 24h tới (từ model LSTM): ${it.hourlyForecasts.joinToString(", ")} mmol/L. " +
                        "Khoảng dự kiến: ${String.format("%.1f", it.minExpected)} - ${String.format("%.1f", it.maxExpected)} mmol/L."
                    } ?: "Chưa có dữ liệu dự báo LSTM."
                }

                val historyText = if (historyData == "") {
                    if (isEnglish) "No history available." else "Không có dữ liệu lịch sử."
                } else historyData

                val prompt = if (isEnglish) {
                    "You are an expert in endocrinology and diabetes. " +
                    "User's glucose log (mmol/L): " + historyText + " " +
                    "LSTM Forecast data: " + forecastInfo + " " +
                    "Analyze trends and provide 3 concise tips on Diet, Activity, and Risks. " +
                    "Answer in English, professional, bulleted list."
                } else {
                    "Bạn là một chuyên gia về nội tiết và tiểu đường. " +
                    "Dưới đây là lịch sử đo đường huyết (mmol/L): " + historyText + " " +
                    "Dữ liệu bổ trợ từ model LSTM: " + forecastInfo + " " +
                    "Hãy phân tích xu hướng và đưa ra 3 lời khuyên ngắn gọn về Chế độ ăn, Vận động và Rủi ro. " +
                    "Trả lời bằng tiếng Việt, súc tích, dạng danh sách."
                }

                if (prompt == "") {
                    return@withContext CloudInsightResult.Failure("Prompt is empty")
                }

                val response = generativeModel.generateContent(prompt)
                
                val resultText = response.text
                if (resultText != null && resultText != "") {
                    CloudInsightResult.Success(resultText)
                } else {
                    CloudInsightResult.Failure(if (isEnglish) "Gemini returned no result." else "Gemini không trả về kết quả.")
                }
            } catch (e: Exception) {
                CloudInsightResult.Failure(if (isEnglish) "AI Connection Error: ${e.localizedMessage}" else "Lỗi kết nối Gemini: ${e.localizedMessage ?: "Vui lòng thử lại sau"}")
            }
        }
}
