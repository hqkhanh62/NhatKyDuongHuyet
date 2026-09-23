package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.RealtimePredictor
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.AIRepository
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.CloudInsightResult
import com.example.nhatkyduonghuyet.domain.health.GlucoseMetrics
import com.example.nhatkyduonghuyet.domain.scanner.AutoImportPipeline
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

import com.example.nhatkyduonghuyet.data.repository.MedicationRepository
import com.example.nhatkyduonghuyet.viewmodel.MedicationUiState

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: LogRepository,
    private val medicationRepository: MedicationRepository,
    private val realtimePredictor: RealtimePredictor,
    private val detectRisk: DetectRiskPattern,
    private val aiRepo: AIRepository,
    private val geminiUseCase: GeminiAnalysisUseCase
) : ViewModel() {

    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            val result = PdfExportHelper.exportReportToPdf(context, uiState.value)
            result.onSuccess { file ->
                PdfExportHelper.shareFile(context, file)
            }.onFailure {
                // Could add a toast or error state here if needed
            }
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
                    _geminiInsight.value = GeminiInsightUiState.Unavailable(result.reason)
                }
            }
        }
    }

    /**
     * Điểm vào thứ hai của luồng quét (dialog nhập nhanh, widget…).
     * Toàn bộ quyết định Auto Clean / ngày giờ / buổi / ô trước-sau ăn dùng
     * chung [AutoImportPipeline] với màn hình quét toàn màn hình, để hai lối nhập
     * dữ liệu không bao giờ ghi ra kết quả khác nhau.
     */
    fun onGlucoseScanned(result: ScannedGlucoseResult) {
        viewModelScope.launch {
            val now = Date()
            val systemDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            val systemTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

            val cleaned = AutoImportPipeline.clean(result.value, result.fields.errorCode)
            if (cleaned !is AutoImportPipeline.CleanResult.Accepted) return@launch

            val entries = repo.getAllLogs().first()
            val draft = AutoImportPipeline.draft(
                value = cleaned.value,
                fields = result.fields,
                systemDate = systemDate,
                systemTime = systemTime,
                existingForDate = entries,
                valueSource = result.source,
                confidence = result.confidence
            )
            when (val plan = AutoImportPipeline.plan(draft, entries)) {
                is AutoImportPipeline.ImportPlan.Duplicate -> Unit
                is AutoImportPipeline.ImportPlan.Insert -> repo.insertLog(plan.entry)
                is AutoImportPipeline.ImportPlan.Update -> repo.insertLog(plan.entry)
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

    private fun getMedicationsUiState(): Flow<List<MedicationUiState>> = medicationRepository.getAllMedications().flatMapLatest { meds ->
        if (meds.isEmpty()) {
            flowOf(emptyList<MedicationUiState>())
        } else {
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val flows = meds.map { med ->
                combine(
                    medicationRepository.getLogsForToday(med.id),
                    medicationRepository.getCountSince(med.id, startOfMonth)
                ) { logsToday, monthlyCount ->
                    MedicationUiState(
                        medication = med,
                        isTakenMorning = logsToday.any { it.session == "MORNING" },
                        isTakenNoon = logsToday.any { it.session == "NOON" },
                        isTakenAfternoon = logsToday.any { it.session == "AFTERNOON" },
                        isTakenEvening = logsToday.any { it.session == "EVENING" },
                        isTakenBedtime = logsToday.any { it.session == "BEDTIME" },
                        countThisMonth = monthlyCount
                    )
                }
            }
            combine(flows) { it.toList() }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getAllLogs(),
        getMedicationsUiState(),
        _timeFilter,
        _realtimePrediction,
        _multiStepForecast,
        _forecastStatus,
        _geminiInsight
    ) { args: Array<Any?> ->
        DashboardInput(
            allEntries = args[0] as List<LogEntry>,
            medications = args[1] as List<MedicationUiState>,
            filter = args[2] as DashboardTimeFilter,
            realtime = args[3] as PredictionResult?,
            multiStep = args[4] as MultiStepResult?,
            forecastStatus = args[5] as String?,
            gemini = args[6] as GeminiInsightUiState
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
            medications = input.medications,
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

    /** Trung bình ngày: dùng chung định nghĩa với luồng quét camera (GlucoseMetrics). */
    private fun dailyMeasuredAverages(entries: List<LogEntry>): Map<String, Float> =
        GlucoseMetrics.dailyMeasuredAverages(entries)

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
            .filter { GlucoseMetrics.isValidMmol(it) }
        val max = values.maxOrNull() ?: 0.0
        val average = values.average().takeIf { it.isFinite() } ?: 0.0
        val highRate = if (values.isEmpty()) 0 else values.count { it > 10.0 } * 100 / values.size
        val weightedAverage = GlucoseMetrics.weightedAverage(dailyAverages.values.toList())
        val hba1c = GlucoseMetrics.estimateHba1c(weightedAverage)
        return Quad(max, average, highRate, hba1c)
    }

    private fun getComparison(current: Double, previous: Double): ComparisonData? {
        if (previous <= 0.0) return null
        val diff = current - previous
        return ComparisonData(diff, diff / previous * 100, diff <= 0)
    }

    private fun validMeasurementsInChronologicalOrder(entries: List<LogEntry>): List<Float> =
        GlucoseMetrics.chronologicalMeasurements(entries)

    private data class DashboardInput(
        val allEntries: List<LogEntry>,
        val medications: List<MedicationUiState> = emptyList(),
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