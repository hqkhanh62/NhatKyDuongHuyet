package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GeminiAnalysisUseCase @Inject constructor() {

    // IMPORTANT: Replace with your actual API Key or secure it in local.properties
    private val apiKey = "YOUR_GEMINI_API_KEY_HERE"
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    suspend fun getAnalysis(historyData: String, forecastData: MultiStepResult?): String? = withContext(Dispatchers.IO) {
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

            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
