package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.BuildConfig
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern
import com.example.nhatkyduonghuyet.domain.usecase.GeminiAnalysisUseCase
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.ai.RealtimePredictor
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.example.nhatkyduonghuyet.data.repository.AIRepository
import com.example.nhatkyduonghuyet.ui.chart.aggregateBySession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: LogRepository,
    private val predictor: GlucosePredictor,
    private val realtimePredictor: RealtimePredictor,
    private val detectRisk: DetectRiskPattern,
    private val aiRepo: AIRepository,
    private val geminiUseCase: GeminiAnalysisUseCase
) : ViewModel() {

    private val _realtimePrediction = MutableStateFlow<PredictionResult?>(null)
    private val _multiStepForecast = MutableStateFlow<MultiStepResult?>(null)
    private val _geminiInsight = MutableStateFlow<String?>(null)
    private val _showRetrainDialog = MutableStateFlow(false)
    val showRetrainDialog = _showRetrainDialog.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllLogs().take(1).collect { logs ->
                // 1. Luôn chạy LSTM (Offline) trước để có số liệu cơ bản
                val recentLogs = logs
                    .sortedWith(compareByDescending<LogEntry> { it.date }.thenByDescending { it.time })
                    .take(5)
                    .reversed()
                
                recentLogs.forEach { log ->
                    val glucose = (log.bgBefore ?: log.value.toDouble()).toFloat()
                    _realtimePrediction.value = realtimePredictor.onNewGlucose(glucose)
                }
                
                // Tự động hiệu chỉnh sai số mô hình (Bias Correction)
                aiRepo.autoCalibrate()

                val forecast = realtimePredictor.predictFuture24Hours()
                _multiStepForecast.value = forecast

                // 2. Ưu tiên Gemini cho phân tích chuyên sâu nếu có mạng
                if (logs.isNotEmpty()) {
                    updateGeminiAnalysis(logs, forecast)
                } else {
                    _geminiInsight.value = "📝 Hãy nhập dữ liệu để nhận lời khuyên cá nhân hóa từ AI Gemini."
                }
            }
        }
    }

    private fun updateGeminiAnalysis(logs: List<LogEntry>, forecast: MultiStepResult?) {
        viewModelScope.launch {
            if (!aiRepo.isOnline()) {
                _geminiInsight.value = "⚠️ Chế độ Offline. Kết nối mạng để nhận lời khuyên từ Gemini."
                return@launch
            }

            if (BuildConfig.GEMINI_API_KEY.isNullOrEmpty()) {
                _geminiInsight.value = "❌ Thiếu API Key. Vui lòng cấu hình GEMINI_API_KEY."
                return@launch
            }

            val historyString = logs
                .sortedWith(compareByDescending<LogEntry> { it.date }.thenByDescending { it.time })
                .take(20)
                .joinToString("\n") { "${it.date} ${it.time}: ${it.bgBefore ?: it.value} mmol/L" }
            
            _geminiInsight.value = "⏳ Đang phân tích chuyên sâu với Gemini..."
            val result = geminiUseCase.getAnalysis(historyString, forecast)
            _geminiInsight.value = result ?: "❌ Không thể lấy lời khuyên từ Gemini. Thử lại sau."
        }
    }

    fun onGlucoseScanned(result: ScannedGlucoseResult) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Date()

            val finalTime = result.time ?: timeSdf.format(now)
            val finalDate = result.date ?: sdf.format(now)

            val hour = try { 
                finalTime.substringBefore(':').toInt() 
            } catch (e: Exception) { 
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY) 
            }
            
            val session = when (hour) {
                in 5..10 -> "Sáng"
                in 11..15 -> "Trưa"
                else -> "Chiều"
            }

            val entry = LogEntry(
                date = finalDate,
                session = session,
                time = finalTime,
                bgBefore = result.value.toDouble(), 
                note = "Auto-scanned via AI Camera"
            )
            repo.insertLog(entry)
            
            if (aiRepo.checkRetrainStatus()) {
                _showRetrainDialog.value = true
            }

            // Cập nhật lại cả LSTM và Gemini
            val newPrediction = realtimePredictor.onNewGlucose(result.value)
            _realtimePrediction.value = newPrediction
            val newForecast = realtimePredictor.predictFuture24Hours()
            _multiStepForecast.value = newForecast
            
            // Hiệu chỉnh lại sai số sau khi có dữ liệu mới
            aiRepo.autoCalibrate()

            repo.getAllLogs().take(1).collect { logs ->
                updateGeminiAnalysis(logs, newForecast)
            }
        }
    }
    
    fun dismissRetrainDialog() {
        _showRetrainDialog.value = false
    }

    private val _timeFilter = MutableStateFlow(DashboardTimeFilter.LAST_15_DAYS)
    val timeFilter = _timeFilter.asStateFlow()

    fun setTimeFilter(filter: DashboardTimeFilter) {
        _timeFilter.value = filter
    }

    private suspend fun estimateDailyAvg(fasting: Float): Float {
        val noon = predictor.predict(fasting, 0)
        val evening = predictor.predict(fasting, 1)
        return if (noon > 0 && evening > 0) (fasting + noon + evening) / 3f else fasting
    }

    private suspend fun getSmartDailyAverages(entries: List<LogEntry>): Map<String, Float> {
        val groupedByDate = entries.groupBy { it.date }.toSortedMap()
        val result = mutableMapOf<String, Float>()
        for ((date, dayEntries) in groupedByDate) {
            val fastingEntry = dayEntries.find { it.session == "Sáng" && it.bgBefore != null }
            val avg = if (fastingEntry != null) {
                estimateDailyAvg(fastingEntry.bgBefore!!.toFloat())
            } else {
                val dayValues = dayEntries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                if (dayValues.isNotEmpty()) dayValues.average().toFloat() else 0f
            }
            if (avg > 0f) result[date] = avg
        }
        return result
    }

    private fun weightedAverage(glucoseList: List<Float>): Float {
        if (glucoseList.isEmpty()) return 0f
        val weights = glucoseList.mapIndexed { i, _ -> (i + 1).toFloat() }
        val totalWeight = weights.sum()
        val weightedSum = glucoseList.zip(weights).sumOf { (it.first * it.second).toDouble() }
        return (weightedSum / totalWeight).toFloat()
    }

    private fun calculateHbA1c(weightedAvgGlucose: Float): Double {
        return if (weightedAvgGlucose > 0) (weightedAvgGlucose + 2.59) / 1.59 else 0.0
    }

    private fun calculateMetrics(entries: List<LogEntry>, smartAverages: Map<String, Float>): Quad<Double, Double, Int, Double> {
        val allRawValues = entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
        val max = allRawValues.maxOrNull() ?: 0.0
        val simpleAvg = if (allRawValues.isNotEmpty()) allRawValues.average() else 0.0
        val highRate = if (allRawValues.isNotEmpty()) (allRawValues.count { it > 10.0 } * 100 / allRawValues.size) else 0

        val smartWeightedAvg = weightedAverage(smartAverages.values.toList())
        val hba1c = calculateHbA1c(smartWeightedAvg)

        return Quad(max, simpleAvg, highRate, hba1c)
    }

    private fun getComparison(current: Double, previous: Double): ComparisonData? {
        if (previous <= 0.0) return null
        val diff = current - previous
        val percent = (diff / previous) * 100
        return ComparisonData(
            diff = diff,
            percentChange = percent,
            isBetter = diff <= 0 
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getAllLogs(),
        _timeFilter,
        _realtimePrediction,
        _multiStepForecast,
        _geminiInsight
    ) { allEntries, filter, realtime, multiStep, gemini ->
        DashboardInput(allEntries, filter, realtime, multiStep, gemini)
    }.flatMapLatest { input ->
        flow {
            val allEntries = input.allEntries
            val filter = input.filter
            val realtime = input.realtime
            val multiStep = input.multiStep
            val gemini = input.gemini

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputSdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            
            val currentLimit = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -filter.days) }.time
            val previousLimit = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -filter.days * 2) }.time

            val currentEntries = allEntries.filter {
                try {
                    val d = sdf.parse(it.date)
                    d != null && (d.after(currentLimit) || it.date == sdf.format(Date()))
                } catch (e: Exception) { false }
            }

            val previousEntries = if (filter == DashboardTimeFilter.ALL) emptyList() else allEntries.filter {
                try {
                    val d = sdf.parse(it.date)
                    d != null && d.after(previousLimit) && (d.before(currentLimit) || it.date == sdf.format(currentLimit))
                } catch (e: Exception) { false }
            }

            val currentSmartAvgs = getSmartDailyAverages(currentEntries)
            val prevSmartAvgs = getSmartDailyAverages(previousEntries)

            val (max, avg, highRate, hba1c) = calculateMetrics(currentEntries, currentSmartAvgs)
            val (pMax, pAvg, pHighRate, pHba1c) = calculateMetrics(previousEntries, prevSmartAvgs)

            val currentPoints = currentSmartAvgs.toList().mapIndexed { index, pair -> 
                val dateLabel = try {
                    val d = sdf.parse(pair.first)
                    if (d != null) outputSdf.format(d) else pair.first
                } catch (e: Exception) { pair.first }
                ChartPointPro(index, pair.second.toDouble(), dateLabel)
            }

            val prevPoints = prevSmartAvgs.toList().mapIndexed { index, pair -> 
                val dateLabel = try {
                    val d = sdf.parse(pair.first)
                    if (d != null) outputSdf.format(d) else pair.first
                } catch (e: Exception) { pair.first }
                ChartPointPro(index, pair.second.toDouble(), dateLabel)
            }

            emit(DashboardUiState(
                entries = currentEntries,
                max = max,
                maxCompare = getComparison(max, pMax),
                avg = avg,
                avgCompare = getComparison(avg, pAvg),
                highRate = highRate,
                highRateCompare = getComparison(highRate.toDouble(), pHighRate.toDouble()),
                hba1c = hba1c,
                hba1cCompare = getComparison(hba1c, pHba1c),
                currentPeriodPoints = currentPoints,
                previousPeriodPoints = prevPoints,
                insights = detectRisk.detect(currentEntries, currentSmartAvgs.values.toList()),
                currentFilter = filter,
                realtimePrediction = realtime,
                multiStepForecast = multiStep,
                geminiInsight = gemini
            ))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}

data class DashboardInput(
    val allEntries: List<LogEntry>,
    val filter: DashboardTimeFilter,
    val realtime: PredictionResult?,
    val multiStep: MultiStepResult?,
    val gemini: String?
)

data class Quad<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
