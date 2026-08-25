package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.example.nhatkyduonghuyet.ai.Normalizer
import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.RealtimePredictor
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.AIRepository
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.CloudInsightResult
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern
import com.example.nhatkyduonghuyet.domain.usecase.GeminiAnalysisUseCase
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

import android.content.Context
import com.example.nhatkyduonghuyet.util.PdfExportHelper

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: LogRepository,
    private val realtimePredictor: RealtimePredictor,
    private val detectRisk: DetectRiskPattern,
    private val aiRepo: AIRepository,
    private val geminiUseCase: GeminiAnalysisUseCase
) : ViewModel() {

    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            PdfExportHelper.exportReportToPdf(context, uiState.value)
        }
    }

    private val _realtimePrediction = MutableStateFlow<PredictionResult?>(null)
    private val _multiStepForecast = MutableStateFlow<MultiStepResult?>(null)
    private val _forecastStatus = MutableStateFlow<String?>(null)
    private val _geminiInsight = MutableStateFlow<GeminiInsightUiState>(GeminiInsightUiState.Idle)
    private val _showRetrainDialog = MutableStateFlow(false)
    private var insightRequestJob: Job? = null
    private var lastInsightFingerprint: String? = null

    val showRetrainDialog: StateFlow<Boolean> = _showRetrainDialog

    init {
        viewModelScope.launch {
            repo.getAllLogs()
                .map(::validMeasurementsInChronologicalOrder)
                .distinctUntilChanged()
                .collectLatest { measurements ->
                    when (val forecast = realtimePredictor.refresh(measurements)) {
                        is PredictionOutcome.Success -> {
                            _realtimePrediction.value = forecast.value.nextPrediction
                            _multiStepForecast.value = forecast.value.future
                            _forecastStatus.value = null
                        }
                        is PredictionOutcome.Failure -> {
                            _realtimePrediction.value = null
                            _multiStepForecast.value = null
                            _forecastStatus.value = forecast.reason
                        }
                    }
                }
        }
    }

    fun requestGeminiAnalysis() {
        if (insightRequestJob?.isActive == true) return

        insightRequestJob = viewModelScope.launch {
            val logs = repo.getAllLogs().first()
            if (logs.isEmpty()) {
                _geminiInsight.value = GeminiInsightUiState.Unavailable("Hãy nhập dữ liệu trước khi yêu cầu phân tích AI.")
                return@launch
            }

            val history = logs
                .sortedWith(compareByDescending<LogEntry> { it.date }.thenByDescending { it.time ?: "" })
                .take(MAX_CLOUD_HISTORY_ROWS)
                .joinToString("\n") { "${it.date} ${it.time ?: "--:--"}: ${it.bgBefore ?: it.bgAfter ?: "không có"} mmol/L" }
            val fingerprint = "$history|${_multiStepForecast.value}"

            if (fingerprint == lastInsightFingerprint && _geminiInsight.value is GeminiInsightUiState.Content) {
                return@launch
            }

            _geminiInsight.value = GeminiInsightUiState.Loading
            val isEnglish = Locale.getDefault().language == "en"
            when (val result = geminiUseCase.getAnalysis(history, _multiStepForecast.value, isEnglish)) {
                is CloudInsightResult.Success -> {
                    lastInsightFingerprint = fingerprint
                    _geminiInsight.value = GeminiInsightUiState.Content(result.insight)
                }
                is CloudInsightResult.Failure -> {
                    _geminiInsight.value = GeminiInsightUiState.Unavailable("Lỗi kết nối AI: ${result.reason}")
                }
            }
        }
    }

    fun onGlucoseScanned(result: ScannedGlucoseResult) {
        viewModelScope.launch {
            val now = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val hf = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            val finalTime = result.time ?: hf.format(now)
            var finalDate = result.date ?: sdf.format(now)
            
            // Validate date format yyyy-MM-dd
            try {
                val parts = finalDate.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    
                    if (month > 12 && day <= 12) {
                        // Swapped month and day (yyyy-dd-MM)
                        finalDate = "%04d-%02d-%02d".format(year, day, month)
                    } else if (month > 12) {
                        // Still invalid month, fallback to today
                        finalDate = sdf.format(now)
                    }
                }
            } catch (e: Exception) {
                finalDate = sdf.format(now)
            }

            val hour = finalTime.substringBefore(':').toIntOrNull()
                ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            var session = "Sáng"
            val scannedVal = result.value.toDouble()
            
            when {
                hour < 10 -> session = "Sáng"
                hour in 10..15 -> session = "Trưa"
                hour in 16..19 -> session = "Chiều"
                else -> session = "Tối"
            }

            val existingEntries = repo.getLogsByDate(finalDate).first()
            
            // Prevention of duplicate identical scans
            val isDuplicate = existingEntries.any { 
                it.session == session && 
                (it.bgBefore == scannedVal || it.bgAfter == scannedVal) &&
                it.time == finalTime 
            }
            if (isDuplicate) return@launch

            val existing = existingEntries.find { it.session == session }

            if (existing != null) {
                // Update existing record
                val updated = if (hour % 2 == 0) { // Simple heuristic or just check which one is null
                     if (existing.bgBefore == null) existing.copy(bgBefore = scannedVal, time = finalTime)
                     else existing.copy(bgAfter = scannedVal, time = finalTime)
                } else {
                     if (existing.bgAfter == null) existing.copy(bgAfter = scannedVal, time = finalTime)
                     else existing.copy(bgBefore = scannedVal, time = finalTime)
                }
                repo.insertLog(updated)
            } else {
                // Create new record
                repo.insertLog(
                    LogEntry(
                        date = finalDate,
                        session = session,
                        time = finalTime,
                        bgBefore = scannedVal,
                        note = "Auto-scanned via AI Camera"
                    )
                )
            }

            _geminiInsight.value = GeminiInsightUiState.Idle
            lastInsightFingerprint = null
            insightRequestJob?.cancel()

            if (aiRepo.checkRetrainStatus()) {
                _showRetrainDialog.value = true
            }
            aiRepo.autoCalibrate()
        }
    }

    fun dismissRetrainDialog() {
        _showRetrainDialog.value = false
    }

    private val _timeFilter = MutableStateFlow(DashboardTimeFilter.LAST_15_DAYS)
    val timeFilter: StateFlow<DashboardTimeFilter> = _timeFilter

    fun setTimeFilter(filter: DashboardTimeFilter) {
        _timeFilter.value = filter
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        listOf(
            repo.getAllLogs(),
            _timeFilter,
            _realtimePrediction,
            _multiStepForecast,
            _forecastStatus,
            _geminiInsight
        )
    ) { args: Array<Any?> ->
        DashboardInput(
            allEntries = args[0] as List<LogEntry>,
            filter = args[1] as DashboardTimeFilter,
            realtime = args[2] as PredictionResult?,
            multiStep = args[3] as MultiStepResult?,
            forecastStatus = args[4] as String?,
            gemini = args[5] as GeminiInsightUiState
        )
    }.map(::buildUiState)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private fun buildUiState(input: DashboardInput): DashboardUiState {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputSdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val (currentEntries, previousEntries) = filterEntries(input.allEntries, input.filter, sdf)
        val currentDailyAverages = dailyMeasuredAverages(currentEntries)
        val previousDailyAverages = dailyMeasuredAverages(previousEntries)
        val (max, avg, highRate, hba1c) = calculateMetrics(currentEntries, currentDailyAverages)
        val (previousMax, previousAvg, previousHighRate, previousHba1c) = calculateMetrics(previousEntries, previousDailyAverages)

        return DashboardUiState(
            entries = currentEntries,
            max = max,
            maxCompare = getComparison(max, previousMax),
            avg = avg,
            avgCompare = getComparison(avg, previousAvg),
            highRate = highRate,
            highRateCompare = getComparison(highRate.toDouble(), previousHighRate.toDouble()),
            hba1c = hba1c,
            hba1cCompare = getComparison(hba1c, previousHba1c),
            currentPeriodPoints = chartPoints(currentDailyAverages, sdf, outputSdf),
            previousPeriodPoints = chartPoints(previousDailyAverages, sdf, outputSdf),
            insights = detectRisk.detect(currentEntries, currentDailyAverages.values.toList()),
            currentFilter = input.filter,
            realtimePrediction = input.realtime,
            multiStepForecast = input.multiStep,
            forecastStatus = input.forecastStatus,
            geminiInsight = input.gemini
        )
    }

    private fun filterEntries(
        allEntries: List<LogEntry>,
        filter: DashboardTimeFilter,
        sdf: SimpleDateFormat
    ): Pair<List<LogEntry>, List<LogEntry>> {
        if (filter == DashboardTimeFilter.ALL) return allEntries to emptyList()

        val currentLimit = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -filter.days) }.time
        val previousLimit = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -filter.days * 2) }.time
        val current = allEntries.filter { entry ->
            runCatching { sdf.parse(entry.date) }
                .getOrNull()
                ?.let { !it.before(currentLimit) } == true
        }
        val previous = allEntries.filter { entry ->
            runCatching { sdf.parse(entry.date) }
                .getOrNull()
                ?.let { !it.before(previousLimit) && it.before(currentLimit) } == true
        }
        return current to previous
    }

    private fun dailyMeasuredAverages(entries: List<LogEntry>): Map<String, Float> =
        entries.groupBy { it.date }
            .toSortedMap()
            .mapValues { (_, dayEntries) ->
                dayEntries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                    .filter { it.isFinite() && it in Normalizer.MIN_GLUCOSE_MMOL.toDouble()..Normalizer.MAX_GLUCOSE_MMOL.toDouble() }
                    .average()
                    .toFloat()
            }
            .filterValues { it.isFinite() && it > 0f }

    private fun chartPoints(
        dailyAverages: Map<String, Float>,
        inputSdf: SimpleDateFormat,
        outputSdf: SimpleDateFormat
    ): List<ChartPointPro> = dailyAverages.entries.mapIndexed { index, (date, value) ->
        val label = runCatching { inputSdf.parse(date)?.let(outputSdf::format) ?: date }.getOrDefault(date)
        ChartPointPro(index, value.toDouble(), label)
    }

    private fun calculateMetrics(
        entries: List<LogEntry>,
        dailyAverages: Map<String, Float>
    ): Quad<Double, Double, Int, Double> {
        val values = entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
            .filter { it.isFinite() && it in Normalizer.MIN_GLUCOSE_MMOL.toDouble()..Normalizer.MAX_GLUCOSE_MMOL.toDouble() }
        val max = values.maxOrNull() ?: 0.0
        val average = values.average().takeIf { it.isFinite() } ?: 0.0
        val highRate = if (values.isEmpty()) 0 else values.count { it > 10.0 } * 100 / values.size
        val weightedAverage = weightedAverage(dailyAverages.values.toList())
        val hba1c = if (weightedAverage > 0f) (weightedAverage + 2.59) / 1.59 else 0.0
        return Quad(max, average, highRate, hba1c)
    }

    private fun weightedAverage(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val weights = values.indices.map { (it + 1).toFloat() }
        return (values.zip(weights).sumOf { (value, weight) -> (value * weight).toDouble() } / weights.sum()).toFloat()
    }

    private fun getComparison(current: Double, previous: Double): ComparisonData? {
        if (previous <= 0.0) return null
        val diff = current - previous
        return ComparisonData(diff, diff / previous * 100, diff <= 0)
    }

    private fun validMeasurementsInChronologicalOrder(entries: List<LogEntry>): List<Float> =
        entries.asSequence()
            .filter { it.session != "AI Prediction" }
            .sortedWith(compareBy<LogEntry> { it.date }.thenBy { it.time ?: "" })
            .mapNotNull { entry -> entry.bgBefore?.toFloat()?.takeIf(Normalizer::isValidGlucose) }
            .toList()

    private data class DashboardInput(
        val allEntries: List<LogEntry>,
        val filter: DashboardTimeFilter,
        val realtime: PredictionResult?,
        val multiStep: MultiStepResult?,
        val forecastStatus: String?,
        val gemini: GeminiInsightUiState
    )

    private data class Quad<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private companion object {
        const val MAX_CLOUD_HISTORY_ROWS = 20
    }
}