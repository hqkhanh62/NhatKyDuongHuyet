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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: LogRepository,
    private val realtimePredictor: RealtimePredictor,
    private val detectRisk: DetectRiskPattern,
    private val aiRepo: AIRepository,
    private val geminiUseCase: GeminiAnalysisUseCase
) : ViewModel() {

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
            when (val result = geminiUseCase.getAnalysis(history, _multiStepForecast.value)) {
                is CloudInsightResult.Success -> {
                    lastInsightFingerprint = fingerprint
                    _geminiInsight.value = GeminiInsightUiState.Content(result.insight)
                }
                is CloudInsightResult.Failure -> {
                    _geminiInsight.value = GeminiInsightUiState.Unavailable(result.reason)
                }
            }
        }
    }

    fun onGlucoseScanned(result: ScannedGlucoseResult) {
        viewModelScope.launch {
            val now = Date()
            val finalTime = result.time ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            val finalDate = result.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            val hour = finalTime.substringBefore(':').toIntOrNull()
                ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val session = when (hour) {
                in 5..10 -> "Sáng"
                in 11..15 -> "Trưa"
                else -> "Chiều"
            }

            repo.insertLog(
                LogEntry(
                    date = finalDate,
                    session = session,
                    time = finalTime,
                    bgBefore = result.value.toDouble(),
                    note = "Auto-scanned via AI Camera"
                )
            )
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
        repo.getAllLogs(),
        _timeFilter,
        _realtimePrediction,
        _multiStepForecast,
        _forecastStatus,
        _geminiInsight
    ) { allEntries, filter, realtime, multiStep, forecastStatus, gemini ->
        DashboardInput(allEntries, filter, realtime, multiStep, forecastStatus, gemini)
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