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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b(\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B\u00bf\u0001\u0012\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\b\b\u0002\u0010\t\u001a\u00020\u0006\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\b\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\b\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0006\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\b\u0012\u000e\b\u0002\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u0003\u0012\u000e\b\u0002\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00110\u0003\u0012\u000e\b\u0002\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u0003\u0012\b\b\u0002\u0010\u0015\u001a\u00020\u0016\u0012\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u0018\u0012\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u001a\u00a2\u0006\u0002\u0010\u001bJ\u000f\u00102\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000f\u00103\u001a\b\u0012\u0004\u0012\u00020\u00110\u0003H\u00c6\u0003J\u000f\u00104\u001a\b\u0012\u0004\u0012\u00020\u00110\u0003H\u00c6\u0003J\u000f\u00105\u001a\b\u0012\u0004\u0012\u00020\u00140\u0003H\u00c6\u0003J\t\u00106\u001a\u00020\u0016H\u00c6\u0003J\u000b\u00107\u001a\u0004\u0018\u00010\u0018H\u00c6\u0003J\u000b\u00108\u001a\u0004\u0018\u00010\u001aH\u00c6\u0003J\t\u00109\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010:\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\t\u0010;\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010<\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\t\u0010=\u001a\u00020\fH\u00c6\u0003J\u000b\u0010>\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\t\u0010?\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010@\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\u00c3\u0001\u0010A\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b2\b\b\u0002\u0010\t\u001a\u00020\u00062\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\b2\b\b\u0002\u0010\u000b\u001a\u00020\f2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\b2\b\b\u0002\u0010\u000e\u001a\u00020\u00062\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\b2\u000e\b\u0002\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u00032\u000e\b\u0002\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00110\u00032\u000e\b\u0002\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u00032\b\b\u0002\u0010\u0015\u001a\u00020\u00162\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u00182\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u00c6\u0001J\u0013\u0010B\u001a\u00020C2\b\u0010D\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010E\u001a\u00020\fH\u00d6\u0001J\t\u0010F\u001a\u00020\u0014H\u00d6\u0001R\u0011\u0010\t\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u0013\u0010\n\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0011\u0010\u0015\u001a\u00020\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010#R\u0011\u0010\u000e\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001dR\u0013\u0010\u000f\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u001fR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010(R\u0013\u0010\r\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010\u001fR\u0017\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010#R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001dR\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u001fR\u0013\u0010\u0019\u001a\u0004\u0018\u00010\u001a\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010.R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00110\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010#R\u0013\u0010\u0017\u001a\u0004\u0018\u00010\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u00101\u00a8\u0006G"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardUiState;", "", "entries", "", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "max", "", "maxCompare", "Lcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;", "avg", "avgCompare", "highRate", "", "highRateCompare", "hba1c", "hba1cCompare", "currentPeriodPoints", "Lcom/example/nhatkyduonghuyet/ui/dashboard/ChartPointPro;", "previousPeriodPoints", "insights", "", "currentFilter", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "realtimePrediction", "Lcom/example/nhatkyduonghuyet/ai/PredictionResult;", "multiStepForecast", "Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;", "(Ljava/util/List;DLcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;DLcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;ILcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;DLcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;Ljava/util/List;Ljava/util/List;Ljava/util/List;Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;Lcom/example/nhatkyduonghuyet/ai/PredictionResult;Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;)V", "getAvg", "()D", "getAvgCompare", "()Lcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;", "getCurrentFilter", "()Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "getCurrentPeriodPoints", "()Ljava/util/List;", "getEntries", "getHba1c", "getHba1cCompare", "getHighRate", "()I", "getHighRateCompare", "getInsights", "getMax", "getMaxCompare", "getMultiStepForecast", "()Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;", "getPreviousPeriodPoints", "getRealtimePrediction", "()Lcom/example/nhatkyduonghuyet/ai/PredictionResult;", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class DashboardUiState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries = null;
    private final double max = 0.0;
    @org.jetbrains.annotations.Nullable()
    private final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData maxCompare = null;
    private final double avg = 0.0;
    @org.jetbrains.annotations.Nullable()
    private final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData avgCompare = null;
    private final int highRate = 0;
    @org.jetbrains.annotations.Nullable()
    private final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData highRateCompare = null;
    private final double hba1c = 0.0;
    @org.jetbrains.annotations.Nullable()
    private final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData hba1cCompare = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> currentPeriodPoints = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> previousPeriodPoints = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> insights = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter currentFilter = null;
    @org.jetbrains.annotations.Nullable()
    private final com.example.nhatkyduonghuyet.ai.PredictionResult realtimePrediction = null;
    @org.jetbrains.annotations.Nullable()
    private final com.example.nhatkyduonghuyet.ai.MultiStepResult multiStepForecast = null;
    
    public DashboardUiState(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries, double max, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData maxCompare, double avg, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData avgCompare, int highRate, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData highRateCompare, double hba1c, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData hba1cCompare, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> currentPeriodPoints, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> previousPeriodPoints, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> insights, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter currentFilter, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ai.PredictionResult realtimePrediction, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ai.MultiStepResult multiStepForecast) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> getEntries() {
        return null;
    }
    
    public final double getMax() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData getMaxCompare() {
        return null;
    }
    
    public final double getAvg() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData getAvgCompare() {
        return null;
    }
    
    public final int getHighRate() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData getHighRateCompare() {
        return null;
    }
    
    public final double getHba1c() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData getHba1cCompare() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> getCurrentPeriodPoints() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> getPreviousPeriodPoints() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getInsights() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter getCurrentFilter() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ai.PredictionResult getRealtimePrediction() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ai.MultiStepResult getMultiStepForecast() {
        return null;
    }
    
    public DashboardUiState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> component11() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component12() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter component13() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ai.PredictionResult component14() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ai.MultiStepResult component15() {
        return null;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData component3() {
        return null;
    }
    
    public final double component4() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData component5() {
        return null;
    }
    
    public final int component6() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData component7() {
        return null;
    }
    
    public final double component8() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries, double max, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData maxCompare, double avg, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData avgCompare, int highRate, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData highRateCompare, double hba1c, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData hba1cCompare, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> currentPeriodPoints, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.ui.dashboard.ChartPointPro> previousPeriodPoints, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> insights, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter currentFilter, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ai.PredictionResult realtimePrediction, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ai.MultiStepResult multiStepForecast) {
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