package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.BuildConfig
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
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

    suspend fun getAnalysis(historyData: String, forecastData: MultiStepResult?): CloudInsightResult =
        withContext(Dispatchers.IO) {
            if (BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
                return@withContext CloudInsightResult.Failure("Chưa cấu hình API Key cho Gemini.")
            }

            try {
                val forecastInfo = forecastData?.let {
                    "Dự báo 24h tới (từ model LSTM): ${it.hourlyForecasts.joinToString(", ")} mmol/L. " +
                    "Khoảng dự kiến: ${String.format("%.1f", it.minExpected)} - ${String.format("%.1f", it.maxExpected)} mmol/L."
                } ?: "Chưa có dữ liệu dự báo LSTM."

                val prompt = """
                    Bạn là một chuyên gia về nội tiết và tiểu đường. 
                    Dưới đây là lịch sử đo đường huyết của người dùng (đơn vị: mmol/L):
                    ${historyData}
                    
                    Dữ liệu bổ trợ từ model LSTM chuyên biệt:
                    ${forecastInfo}
                    
                    Hãy phân tích xu hướng dựa trên cả lịch sử và dự báo LSTM, sau đó đưa ra 3 lời khuyên ngắn gọn, thiết thực nhất về:
                    1. Chế độ ăn uống.
                    2. Vận động.
                    3. Cảnh báo rủi ro (nếu có).
                    
                    Yêu cầu: Trả lời bằng tiếng Việt, súc tích, chuyên nghiệp. Trình bày dạng danh sách gạch đầu dòng.
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        text(prompt)
                    }
                )
                
                val resultText = response.text
                if (resultText != null) {
                    CloudInsightResult.Success(resultText)
                } else {
                    CloudInsightResult.Failure("Gemini không trả về kết quả.")
                }
            } catch (e: Exception) {
                CloudInsightResult.Failure("Lỗi kết nối Gemini: ${e.localizedMessage ?: "Vui lòng thử lại sau"}")
            }
        }
}
