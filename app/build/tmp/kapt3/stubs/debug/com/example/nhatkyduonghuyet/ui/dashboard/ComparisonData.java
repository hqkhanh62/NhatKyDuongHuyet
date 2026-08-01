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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\f\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B#\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\t\u0010\f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0006H\u00c6\u0003J\'\u0010\u000f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0010\u001a\u00020\u00062\b\u0010\u0011\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0012\u001a\u00020\u0013H\u00d6\u0001J\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\t\u00a8\u0006\u0016"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/dashboard/ComparisonData;", "", "diff", "", "percentChange", "isBetter", "", "(DDZ)V", "getDiff", "()D", "()Z", "getPercentChange", "component1", "component2", "component3", "copy", "equals", "other", "hashCode", "", "toString", "", "app_debug"})
public final class ComparisonData {
    private final double diff = 0.0;
    private final double percentChange = 0.0;
    private final boolean isBetter = false;
    
    public ComparisonData(double diff, double percentChange, boolean isBetter) {
        super();
    }
    
    public final double getDiff() {
        return 0.0;
    }
    
    public final double getPercentChange() {
        return 0.0;
    }
    
    public final boolean isBetter() {
        return false;
    }
    
    public ComparisonData() {
        super();
    }
    
    public final double component1() {
        return 0.0;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    public final boolean component3() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData copy(double diff, double percentChange, boolean isBetter) {
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