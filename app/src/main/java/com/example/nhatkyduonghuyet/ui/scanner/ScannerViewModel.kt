package com.example.nhatkyduonghuyet.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.ai.Normalizer
import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.RealtimePredictor
import com.example.nhatkyduonghuyet.data.repository.AIRepository
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.scan.ScanAutoImportPipeline
import com.example.nhatkyduonghuyet.scan.ScanImportDraft
import com.example.nhatkyduonghuyet.scan.ScanSaveResult
import com.example.nhatkyduonghuyet.scan.ScanStabilityTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Phases of the Pro AI scan flow. */
enum class ScannerPhase {
    /** Live camera stream, waiting for a stable reading. */
    SCANNING,

    /** Stable reading found — review banner is shown for confirmation. */
    REVIEW,

    /** Auto-save in progress. */
    SAVING,

    /** Saved — forecast + warning banner is shown. */
    SAVED
}

data class ScannerUiState(
    val phase: ScannerPhase = ScannerPhase.SCANNING,
    /** Draft under review (or just saved). Null while scanning. */
    val draft: ScanImportDraft? = null,
    val saveResult: ScanSaveResult? = null,
    /** Post-save LSTM forecast (Scan → Save → Forecast → Warning). */
    val postSaveForecast: PredictionResult? = null,
    val forecastMessage: String? = null,
    /** True when the AI retrain threshold was just reached. */
    val retrainReady: Boolean = false,
    val saveError: String? = null,
    /** Frames since the last candidate — drives the "hold still" hint. */
    val framesWithoutResult: Int = 0,
    /** How many raw scans were auto-cleaned as noise. */
    val rejectedNoiseCount: Int = 0
)

/**
 * Orchestrates the Real-time AI Loop for the Pro scanner:
 *
 * Frame → [GlucoseScanner] → [ScanStabilityTracker] → review banner →
 * [ScanAutoImportPipeline] auto-save → LSTM forecast refresh → risk alert.
 *
 * The camera analyzer stays dumb (pixels in, callbacks out); every product
 * decision — stability, meter-vs-system date/time, session, risk, HbA1c —
 * lives here and in the testable `scan` package.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    val scanner: GlucoseScanner,
    private val pipeline: ScanAutoImportPipeline,
    private val repository: LogRepository,
    private val realtimePredictor: RealtimePredictor,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val stability = ScanStabilityTracker()

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Called by the camera analyzer for every processed frame (250 ms
     * cadence). Null means "no readable value in this frame".
     */
    fun onFrameResult(result: ScannedGlucoseResult?) {
        val current = _uiState.value
        if (current.phase != ScannerPhase.SCANNING) return

        if (result == null) {
            _uiState.update { it.copy(framesWithoutResult = it.framesWithoutResult + 1) }
            return
        }

        val stableValue = stability.add(result.value)
        if (stableValue == null) {
            // Still settling — keep the live stream running.
            _uiState.update { it.copy(framesWithoutResult = 0) }
            return
        }

        val draft = pipeline.buildDraft(result.copy(value = stableValue))
        if (draft == null) {
            // Auto Clean dropped OCR noise outside 2.0–30.0: keep scanning.
            stability.reset()
            _uiState.update {
                it.copy(
                    framesWithoutResult = 0,
                    rejectedNoiseCount = it.rejectedNoiseCount + 1
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                phase = ScannerPhase.REVIEW,
                draft = draft,
                framesWithoutResult = 0
            )
        }
    }

    /** User confirmed the review banner — auto-save + full AI forecast flow. */
    fun confirmSave() {
        val draft = _uiState.value.draft ?: return
        if (_uiState.value.phase != ScannerPhase.REVIEW) return

        _uiState.update { it.copy(phase = ScannerPhase.SAVING, saveError = null) }

        viewModelScope.launch {
            when (val result = pipeline.confirmAndSave(draft)) {
                is ScanSaveResult.Saved -> {
                    _uiState.update { it.copy(saveResult = result) }
                    refreshPostSaveForecast()
                    runBackgroundAiMaintenance()
                    _uiState.update { it.copy(phase = ScannerPhase.SAVED) }
                }
                is ScanSaveResult.Duplicate -> {
                    // Same scan pressed twice: surface the existing row instead
                    // of writing a second identical entry.
                    _uiState.update {
                        it.copy(
                            phase = ScannerPhase.SAVED,
                            saveResult = result,
                            forecastMessage = "Bản ghi này đã tồn tại — không lưu trùng."
                        )
                    }
                }
                is ScanSaveResult.Rejected -> {
                    _uiState.update {
                        it.copy(
                            phase = ScannerPhase.REVIEW,
                            saveError = result.reason
                        )
                    }
                }
            }
        }
    }

    /** User rejected the banner — resume the live scan loop. */
    fun rescan() {
        stability.reset()
        _uiState.update {
            ScannerUiState(
                rejectedNoiseCount = it.rejectedNoiseCount
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(saveError = null) }
    }

    /**
     * Real-time AI Loop, second half: recompute the LSTM forecast from the
     * freshly saved history so the success banner can warn immediately.
     */
    private suspend fun refreshPostSaveForecast() {
        val history = try {
            repository.getAllLogs().first()
                .asSequence()
                .filter { it.session != "AI Prediction" }
                .sortedWith(compareBy({ it.date }, { it.time ?: "" }))
                .flatMap { listOfNotNull(it.bgBefore, it.bgAfter).map(Double::toFloat) }
                .filter(Normalizer::isValidGlucose)
                .toList()
        } catch (_: Exception) {
            emptyList()
        }

        when (val outcome = realtimePredictor.predictStateless(history)) {
            is PredictionOutcome.Success -> _uiState.update {
                it.copy(postSaveForecast = outcome.value, forecastMessage = null)
            }
            is PredictionOutcome.Failure -> _uiState.update {
                it.copy(postSaveForecast = null, forecastMessage = outcome.reason)
            }
        }
    }

    private suspend fun runBackgroundAiMaintenance() {
        try {
            if (aiRepository.checkRetrainStatus()) {
                _uiState.update { it.copy(retrainReady = true) }
            }
            aiRepository.autoCalibrate()
        } catch (_: Exception) {
            // AI maintenance must never break the scan flow.
        }
    }
}
