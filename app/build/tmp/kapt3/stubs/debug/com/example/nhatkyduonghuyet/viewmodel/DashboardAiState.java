package com.example.nhatkyduonghuyet.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.nhatkyduonghuyet.ai.PredictionOutcome;
import com.example.nhatkyduonghuyet.data.repository.AIRepository;
import com.example.nhatkyduonghuyet.domain.repository.LogRepository;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0011\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B1\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\bJ\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0011\u001a\u00020\u0005H\u00c6\u0003J\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0005H\u00c6\u0003J:\u0010\u0014\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0007\u001a\u00020\u0005H\u00c6\u0001\u00a2\u0006\u0002\u0010\u0015J\u0013\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0019\u001a\u00020\u001aH\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u0005H\u00d6\u0001R\u0015\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\n\n\u0002\u0010\u000b\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0015\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\n\n\u0002\u0010\u000b\u001a\u0004\b\u000e\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\r\u00a8\u0006\u001c"}, d2 = {"Lcom/example/nhatkyduonghuyet/viewmodel/DashboardAiState;", "", "morningPrediction", "", "morningRisk", "", "afternoonPrediction", "afternoonRisk", "(Ljava/lang/Float;Ljava/lang/String;Ljava/lang/Float;Ljava/lang/String;)V", "getAfternoonPrediction", "()Ljava/lang/Float;", "Ljava/lang/Float;", "getAfternoonRisk", "()Ljava/lang/String;", "getMorningPrediction", "getMorningRisk", "component1", "component2", "component3", "component4", "copy", "(Ljava/lang/Float;Ljava/lang/String;Ljava/lang/Float;Ljava/lang/String;)Lcom/example/nhatkyduonghuyet/viewmodel/DashboardAiState;", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
public final class DashboardAiState {
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Float morningPrediction = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String morningRisk = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Float afternoonPrediction = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String afternoonRisk = null;
    
    public DashboardAiState(@org.jetbrains.annotations.Nullable()
    java.lang.Float morningPrediction, @org.jetbrains.annotations.NotNull()
    java.lang.String morningRisk, @org.jetbrains.annotations.Nullable()
    java.lang.Float afternoonPrediction, @org.jetbrains.annotations.NotNull()
    java.lang.String afternoonRisk) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Float getMorningPrediction() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMorningRisk() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Float getAfternoonPrediction() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAfternoonRisk() {
        return null;
    }
    
    public DashboardAiState() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Float component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Float component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.nhatkyduonghuyet.viewmodel.DashboardAiState copy(@org.jetbrains.annotations.Nullable()
    java.lang.Float morningPrediction, @org.jetbrains.annotations.NotNull()
    java.lang.String morningRisk, @org.jetbrains.annotations.Nullable()
    java.lang.Float afternoonPrediction, @org.jetbrains.annotations.NotNull()
    java.lang.String afternoonRisk) {
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