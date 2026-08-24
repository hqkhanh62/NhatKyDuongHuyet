package com.example.nhatkyduonghuyet.ui.dashboard;

import androidx.lifecycle.ViewModel;
import com.example.nhatkyduonghuyet.ai.MultiStepResult;
import com.example.nhatkyduonghuyet.ai.Normalizer;
import com.example.nhatkyduonghuyet.ai.PredictionOutcome;
import com.example.nhatkyduonghuyet.ai.PredictionResult;
import com.example.nhatkyduonghuyet.ai.RealtimePredictor;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.data.repository.AIRepository;
import com.example.nhatkyduonghuyet.domain.repository.LogRepository;
import com.example.nhatkyduonghuyet.domain.usecase.CloudInsightResult;
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern;
import com.example.nhatkyduonghuyet.domain.usecase.GeminiAnalysisUseCase;
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.SharingStarted;
import kotlinx.coroutines.flow.StateFlow;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.inject.Inject;
import android.content.Context;
import com.example.nhatkyduonghuyet.util.PdfExportHelper;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00c6\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\t\b\u0007\u0018\u0000 O2\u00020\u0001:\u0003OPQB/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u0010\u0010&\u001a\u00020$2\u0006\u0010\'\u001a\u00020(H\u0002JB\u0010)\u001a\u001a\u0012\u0004\u0012\u00020+\u0012\u0004\u0012\u00020+\u0012\u0004\u0012\u00020,\u0012\u0004\u0012\u00020+0*2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020/0.2\u0012\u00100\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020201H\u0002J2\u00103\u001a\b\u0012\u0004\u0012\u0002040.2\u0012\u00100\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u000202012\u0006\u00105\u001a\u0002062\u0006\u00107\u001a\u000206H\u0002J\"\u00108\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u000202012\f\u0010-\u001a\b\u0012\u0004\u0012\u00020/0.H\u0002J\u0006\u00109\u001a\u00020:J\u000e\u0010;\u001a\u00020:2\u0006\u0010<\u001a\u00020=J>\u0010>\u001a\u001a\u0012\n\u0012\b\u0012\u0004\u0012\u00020/0.\u0012\n\u0012\b\u0012\u0004\u0012\u00020/0.0?2\f\u0010@\u001a\b\u0012\u0004\u0012\u00020/0.2\u0006\u0010A\u001a\u00020\u00192\u0006\u0010B\u001a\u000206H\u0002J\u001a\u0010C\u001a\u0004\u0018\u00010D2\u0006\u0010E\u001a\u00020+2\u0006\u0010F\u001a\u00020+H\u0002J\u000e\u0010G\u001a\u00020:2\u0006\u0010H\u001a\u00020IJ\u0006\u0010J\u001a\u00020:J\u000e\u0010K\u001a\u00020:2\u0006\u0010A\u001a\u00020\u0019J\u001c\u0010L\u001a\b\u0012\u0004\u0012\u0002020.2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020/0.H\u0002J\u0016\u0010M\u001a\u0002022\f\u0010N\u001a\b\u0012\u0004\u0012\u0002020.H\u0002R\u0016\u0010\r\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0012\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00130\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0014\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00190\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u001bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00170\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0017\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00190\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010 R\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020$0\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010 \u00a8\u0006R"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel;", "Landroidx/lifecycle/ViewModel;", "repo", "Lcom/example/nhatkyduonghuyet/domain/repository/LogRepository;", "realtimePredictor", "Lcom/example/nhatkyduonghuyet/ai/RealtimePredictor;", "detectRisk", "Lcom/example/nhatkyduonghuyet/domain/usecase/DetectRiskPattern;", "aiRepo", "Lcom/example/nhatkyduonghuyet/data/repository/AIRepository;", "geminiUseCase", "Lcom/example/nhatkyduonghuyet/domain/usecase/GeminiAnalysisUseCase;", "(Lcom/example/nhatkyduonghuyet/domain/repository/LogRepository;Lcom/example/nhatkyduonghuyet/ai/RealtimePredictor;Lcom/example/nhatkyduonghuyet/domain/usecase/DetectRiskPattern;Lcom/example/nhatkyduonghuyet/data/repository/AIRepository;Lcom/example/nhatkyduonghuyet/domain/usecase/GeminiAnalysisUseCase;)V", "_forecastStatus", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_geminiInsight", "Lcom/example/nhatkyduonghuyet/ui/dashboard/GeminiInsightUiState;", "_multiStepForecast", "Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;", "_realtimePrediction", "Lcom/example/nhatkyduonghuyet/ai/PredictionResult;", "_showRetrainDialog", "", "_timeFilter", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "insightRequestJob", "Lkotlinx/coroutines/Job;", "lastInsightFingerprint", "showRetrainDialog", "Lkotlinx/coroutines/flow/StateFlow;", "getShowRetrainDialog", "()Lkotlinx/coroutines/flow/StateFlow;", "timeFilter", "getTimeFilter", "uiState", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardUiState;", "getUiState", "buildUiState", "input", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel$DashboardInput;", "calculateMetrics", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel$Quad;", "", "", "entries", "", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "dailyAverages", "", "", "chartPoints", "Lcom/example/nhatkyduonghuyet/ui/dashboard/ChartPointPro;", "inputSdf", "Ljava/text/SimpleDateFormat;", "outputSdf", "dailyMeasuredAverages", "dismissRetrainDialog", "", "exportToPdf", "context", "Landroid/content/Context;", "filterEntries", "Lkotlin/Pair;", "allEntries", "filter", "sdf", "getComparison", "Lcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;", "current", "previous", "onGlucoseScanned", "result", "Lcom/example/nhatkyduonghuyet/ml/ScannedGlucoseResult;", "requestGeminiAnalysis", "setTimeFilter", "validMeasurementsInChronologicalOrder", "weightedAverage", "values", "Companion", "DashboardInput", "Quad", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class DashboardViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.domain.repository.LogRepository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.ai.RealtimePredictor realtimePredictor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern detectRisk = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.data.repository.AIRepository aiRepo = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.domain.usecase.GeminiAnalysisUseCase geminiUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.ai.PredictionResult> _realtimePrediction = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.ai.MultiStepResult> _multiStepForecast = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _forecastStatus = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.ui.dashboard.GeminiInsightUiState> _geminiInsight = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _showRetrainDialog = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job insightRequestJob;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String lastInsightFingerprint;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> showRetrainDialog = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter> _timeFilter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter> timeFilter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState> uiState = null;
    @java.lang.Deprecated()
    public static final int MAX_CLOUD_HISTORY_ROWS = 20;
    @org.jetbrains.annotations.NotNull()
    private static final com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public DashboardViewModel(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.domain.repository.LogRepository repo, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ai.RealtimePredictor realtimePredictor, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern detectRisk, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.repository.AIRepository aiRepo, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.domain.usecase.GeminiAnalysisUseCase geminiUseCase) {
        super();
    }
    
    public final void exportToPdf(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getShowRetrainDialog() {
        return null;
    }
    
    public final void requestGeminiAnalysis() {
    }
    
    public final void onGlucoseScanned(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult result) {
    }
    
    public final void dismissRetrainDialog() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter> getTimeFilter() {
        return null;
    }
    
    public final void setTimeFilter(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter filter) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState> getUiState() {
        return null;
    }
    
    private final com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState buildUiState(com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel.DashboardInput input) {
        return null;
    }
    
    private final kotlin.Pair<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>, java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> filterEntries(java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> allEntries, com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter filter, java.text.SimpleDateFormat sdf) {
        return null;
    }
    
    private final java.util.Map<java.lang.String, java.lang.Float> dailyMeasuredAverages(java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries) {
        return null;
    }
    
    private final java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> chartPoints(java.util.Map<java.lang.String, java.lang.Float> dailyAverages, java.text.SimpleDateFormat inputSdf, java.text.SimpleDateFormat outputSdf) {
        return null;
    }
    
    private final com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel.Quad<java.lang.Double, java.lang.Double, java.lang.Integer, java.lang.Double> calculateMetrics(java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries, java.util.Map<java.lang.String, java.lang.Float> dailyAverages) {
        return null;
    }
    
    private final float weightedAverage(java.util.List<java.lang.Float> values) {
        return 0.0F;
    }
    
    private final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData getComparison(double current, double previous) {
        return null;
    }
    
    private final java.util.List<java.lang.Float> validMeasurementsInChronologicalOrder(java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel$Companion;", "", "()V", "MAX_CLOUD_HISTORY_ROWS", "", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0015\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0082\b\u0018\u00002\u00020\u0001BA\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\b\u0010\t\u001a\u0004\u0018\u00010\n\u0012\b\u0010\u000b\u001a\u0004\u0018\u00010\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u00a2\u0006\u0002\u0010\u000fJ\u000f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010\u001e\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\u000b\u0010\u001f\u001a\u0004\u0018\u00010\nH\u00c6\u0003J\u000b\u0010 \u001a\u0004\u0018\u00010\fH\u00c6\u0003J\t\u0010!\u001a\u00020\u000eH\u00c6\u0003JQ\u0010\"\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f2\b\b\u0002\u0010\r\u001a\u00020\u000eH\u00c6\u0001J\u0013\u0010#\u001a\u00020$2\b\u0010%\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010&\u001a\u00020\'H\u00d6\u0001J\t\u0010(\u001a\u00020\fH\u00d6\u0001R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0013\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001b\u00a8\u0006)"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel$DashboardInput;", "", "allEntries", "", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "filter", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "realtime", "Lcom/example/nhatkyduonghuyet/ai/PredictionResult;", "multiStep", "Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;", "forecastStatus", "", "gemini", "Lcom/example/nhatkyduonghuyet/ui/dashboard/GeminiInsightUiState;", "(Ljava/util/List;Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;Lcom/example/nhatkyduonghuyet/ai/PredictionResult;Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;Ljava/lang/String;Lcom/example/nhatkyduonghuyet/ui/dashboard/GeminiInsightUiState;)V", "getAllEntries", "()Ljava/util/List;", "getFilter", "()Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "getForecastStatus", "()Ljava/lang/String;", "getGemini", "()Lcom/example/nhatkyduonghuyet/ui/dashboard/GeminiInsightUiState;", "getMultiStep", "()Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;", "getRealtime", "()Lcom/example/nhatkyduonghuyet/ai/PredictionResult;", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    static final class DashboardInput {
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> allEntries = null;
        @org.jetbrains.annotations.NotNull()
        private final com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter filter = null;
        @org.jetbrains.annotations.Nullable()
        private final com.example.nhatkyduonghuyet.ai.PredictionResult realtime = null;
        @org.jetbrains.annotations.Nullable()
        private final com.example.nhatkyduonghuyet.ai.MultiStepResult multiStep = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String forecastStatus = null;
        @org.jetbrains.annotations.NotNull()
        private final com.example.nhatkyduonghuyet.ui.dashboard.GeminiInsightUiState gemini = null;
        
        public DashboardInput(@org.jetbrains.annotations.NotNull()
        java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> allEntries, @org.jetbrains.annotations.NotNull()
        com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter filter, @org.jetbrains.annotations.Nullable()
        com.example.nhatkyduonghuyet.ai.PredictionResult realtime, @org.jetbrains.annotations.Nullable()
        com.example.nhatkyduonghuyet.ai.MultiStepResult multiStep, @org.jetbrains.annotations.Nullable()
        java.lang.String forecastStatus, @org.jetbrains.annotations.NotNull()
        com.example.nhatkyduonghuyet.ui.dashboard.GeminiInsightUiState gemini) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> getAllEntries() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter getFilter() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.nhatkyduonghuyet.ai.PredictionResult getRealtime() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.nhatkyduonghuyet.ai.MultiStepResult getMultiStep() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getForecastStatus() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nhatkyduonghuyet.ui.dashboard.GeminiInsightUiState getGemini() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.nhatkyduonghuyet.ai.PredictionResult component3() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.nhatkyduonghuyet.ai.MultiStepResult component4() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nhatkyduonghuyet.ui.dashboard.GeminiInsightUiState component6() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel.DashboardInput copy(@org.jetbrains.annotations.NotNull()
        java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> allEntries, @org.jetbrains.annotations.NotNull()
        com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter filter, @org.jetbrains.annotations.Nullable()
        com.example.nhatkyduonghuyet.ai.PredictionResult realtime, @org.jetbrains.annotations.Nullable()
        com.example.nhatkyduonghuyet.ai.MultiStepResult multiStep, @org.jetbrains.annotations.Nullable()
        java.lang.String forecastStatus, @org.jetbrains.annotations.NotNull()
        com.example.nhatkyduonghuyet.ui.dashboard.GeminiInsightUiState gemini) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0000\n\u0002\b\u0012\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0082\b\u0018\u0000*\u0006\b\u0000\u0010\u0001 \u0001*\u0006\b\u0001\u0010\u0002 \u0001*\u0006\b\u0002\u0010\u0003 \u0001*\u0006\b\u0003\u0010\u0004 \u00012\u00020\u0005B%\u0012\u0006\u0010\u0006\u001a\u00028\u0000\u0012\u0006\u0010\u0007\u001a\u00028\u0001\u0012\u0006\u0010\b\u001a\u00028\u0002\u0012\u0006\u0010\t\u001a\u00028\u0003\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u0011\u001a\u00028\u0000H\u00c6\u0003\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\u0012\u001a\u00028\u0001H\u00c6\u0003\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\u0013\u001a\u00028\u0002H\u00c6\u0003\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\u0014\u001a\u00028\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\fJN\u0010\u0015\u001a\u001a\u0012\u0004\u0012\u00028\u0000\u0012\u0004\u0012\u00028\u0001\u0012\u0004\u0012\u00028\u0002\u0012\u0004\u0012\u00028\u00030\u00002\b\b\u0002\u0010\u0006\u001a\u00028\u00002\b\b\u0002\u0010\u0007\u001a\u00028\u00012\b\b\u0002\u0010\b\u001a\u00028\u00022\b\b\u0002\u0010\t\u001a\u00028\u0003H\u00c6\u0001\u00a2\u0006\u0002\u0010\u0016J\u0013\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u0005H\u00d6\u0003J\t\u0010\u001a\u001a\u00020\u001bH\u00d6\u0001J\t\u0010\u001c\u001a\u00020\u001dH\u00d6\u0001R\u0013\u0010\u0006\u001a\u00028\u0000\u00a2\u0006\n\n\u0002\u0010\r\u001a\u0004\b\u000b\u0010\fR\u0013\u0010\t\u001a\u00028\u0003\u00a2\u0006\n\n\u0002\u0010\r\u001a\u0004\b\u000e\u0010\fR\u0013\u0010\u0007\u001a\u00028\u0001\u00a2\u0006\n\n\u0002\u0010\r\u001a\u0004\b\u000f\u0010\fR\u0013\u0010\b\u001a\u00028\u0002\u00a2\u0006\n\n\u0002\u0010\r\u001a\u0004\b\u0010\u0010\f\u00a8\u0006\u001e"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel$Quad;", "A", "B", "C", "D", "", "first", "second", "third", "fourth", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", "getFirst", "()Ljava/lang/Object;", "Ljava/lang/Object;", "getFourth", "getSecond", "getThird", "component1", "component2", "component3", "component4", "copy", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel$Quad;", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
    static final class Quad<A extends java.lang.Object, B extends java.lang.Object, C extends java.lang.Object, D extends java.lang.Object> {
        private final A first = null;
        private final B second = null;
        private final C third = null;
        private final D fourth = null;
        
        public Quad(A first, B second, C third, D fourth) {
            super();
        }
        
        public final A getFirst() {
            return null;
        }
        
        public final B getSecond() {
            return null;
        }
        
        public final C getThird() {
            return null;
        }
        
        public final D getFourth() {
            return null;
        }
        
        public final A component1() {
            return null;
        }
        
        public final B component2() {
            return null;
        }
        
        public final C component3() {
            return null;
        }
        
        public final D component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel.Quad<A, B, C, D> copy(A first, B second, C third, D fourth) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}