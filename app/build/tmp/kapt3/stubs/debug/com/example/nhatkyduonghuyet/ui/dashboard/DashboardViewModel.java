package com.example.nhatkyduonghuyet.ui.dashboard;

import androidx.lifecycle.ViewModel;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.data.repository.LogRepository;
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern;
import com.example.nhatkyduonghuyet.ml.GlucosePredictor;
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult;
import com.example.nhatkyduonghuyet.ai.RealtimePredictor;
import com.example.nhatkyduonghuyet.ai.PredictionResult;
import com.example.nhatkyduonghuyet.ai.MultiStepResult;
import com.example.nhatkyduonghuyet.data.repository.AIRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0098\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u0010\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020\"H\u0002JB\u0010#\u001a\u001a\u0012\u0004\u0012\u00020 \u0012\u0004\u0012\u00020 \u0012\u0004\u0012\u00020%\u0012\u0004\u0012\u00020 0$2\f\u0010&\u001a\b\u0012\u0004\u0012\u00020(0\'2\u0012\u0010)\u001a\u000e\u0012\u0004\u0012\u00020+\u0012\u0004\u0012\u00020\"0*H\u0002J\u0006\u0010,\u001a\u00020-J\u0010\u0010.\u001a\u00020\"2\u0006\u0010/\u001a\u00020\"H\u0002J\u001a\u00100\u001a\u0004\u0018\u0001012\u0006\u00102\u001a\u00020 2\u0006\u00103\u001a\u00020 H\u0002J\"\u00104\u001a\u000e\u0012\u0004\u0012\u00020+\u0012\u0004\u0012\u00020\"0*2\f\u0010&\u001a\b\u0012\u0004\u0012\u00020(0\'H\u0002J\u000e\u00105\u001a\u00020-2\u0006\u00106\u001a\u000207J\u000e\u00108\u001a\u00020-2\u0006\u00109\u001a\u00020\u0015J\u0016\u0010:\u001a\u00020\"2\f\u0010;\u001a\b\u0012\u0004\u0012\u00020\"0\'H\u0002R\u0016\u0010\r\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00110\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00130\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00150\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0019R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001d0\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0019\u00a8\u0006<"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel;", "Landroidx/lifecycle/ViewModel;", "repo", "Lcom/example/nhatkyduonghuyet/data/repository/LogRepository;", "predictor", "Lcom/example/nhatkyduonghuyet/ml/GlucosePredictor;", "realtimePredictor", "Lcom/example/nhatkyduonghuyet/ai/RealtimePredictor;", "detectRisk", "Lcom/example/nhatkyduonghuyet/domain/usecase/DetectRiskPattern;", "aiRepo", "Lcom/example/nhatkyduonghuyet/data/repository/AIRepository;", "(Lcom/example/nhatkyduonghuyet/data/repository/LogRepository;Lcom/example/nhatkyduonghuyet/ml/GlucosePredictor;Lcom/example/nhatkyduonghuyet/ai/RealtimePredictor;Lcom/example/nhatkyduonghuyet/domain/usecase/DetectRiskPattern;Lcom/example/nhatkyduonghuyet/data/repository/AIRepository;)V", "_multiStepForecast", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;", "_realtimePrediction", "Lcom/example/nhatkyduonghuyet/ai/PredictionResult;", "_showRetrainDialog", "", "_timeFilter", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "showRetrainDialog", "Lkotlinx/coroutines/flow/StateFlow;", "getShowRetrainDialog", "()Lkotlinx/coroutines/flow/StateFlow;", "timeFilter", "getTimeFilter", "uiState", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardUiState;", "getUiState", "calculateHbA1c", "", "weightedAvgGlucose", "", "calculateMetrics", "Lcom/example/nhatkyduonghuyet/ui/dashboard/Quad;", "", "entries", "", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "smartAverages", "", "", "dismissRetrainDialog", "", "estimateDailyAvg", "fasting", "getComparison", "Lcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;", "current", "previous", "getSmartDailyAverages", "onGlucoseScanned", "result", "Lcom/example/nhatkyduonghuyet/ml/ScannedGlucoseResult;", "setTimeFilter", "filter", "weightedAverage", "glucoseList", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class DashboardViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.data.repository.LogRepository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.ml.GlucosePredictor predictor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.ai.RealtimePredictor realtimePredictor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern detectRisk = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.data.repository.AIRepository aiRepo = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.ai.PredictionResult> _realtimePrediction = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.ai.MultiStepResult> _multiStepForecast = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _showRetrainDialog = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> showRetrainDialog = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter> _timeFilter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter> timeFilter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState> uiState = null;
    
    @javax.inject.Inject()
    public DashboardViewModel(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.repository.LogRepository repo, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ml.GlucosePredictor predictor, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ai.RealtimePredictor realtimePredictor, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern detectRisk, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.repository.AIRepository aiRepo) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getShowRetrainDialog() {
        return null;
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
    
    private final float estimateDailyAvg(float fasting) {
        return 0.0F;
    }
    
    private final java.util.Map<java.lang.String, java.lang.Float> getSmartDailyAverages(java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries) {
        return null;
    }
    
    private final float weightedAverage(java.util.List<java.lang.Float> glucoseList) {
        return 0.0F;
    }
    
    private final double calculateHbA1c(float weightedAvgGlucose) {
        return 0.0;
    }
    
    private final com.example.nhatkyduonghuyet.ui.dashboard.Quad<java.lang.Double, java.lang.Double, java.lang.Integer, java.lang.Double> calculateMetrics(java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries, java.util.Map<java.lang.String, java.lang.Float> smartAverages) {
        return null;
    }
    
    private final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData getComparison(double current, double previous) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState> getUiState() {
        return null;
    }
}