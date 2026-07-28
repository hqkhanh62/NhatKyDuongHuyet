package com.example.nhatkyduonghuyet.ui.chart;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.navigation.NavController;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel;
import java.text.SimpleDateFormat;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0019\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001BG\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0006\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0006\u0012\b\u0010\n\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0016\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0003H\u00c6\u0003J\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u0019\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\rJ^\u0010\u001d\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u0006H\u00c6\u0001\u00a2\u0006\u0002\u0010\u001eJ\u0013\u0010\u001f\u001a\u00020 2\b\u0010!\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\"\u001a\u00020#H\u00d6\u0001J\t\u0010$\u001a\u00020\u0003H\u00d6\u0001R\u0015\u0010\n\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u000e\u001a\u0004\b\f\u0010\rR\u0015\u0010\b\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u000e\u001a\u0004\b\u000f\u0010\rR\u0015\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u000e\u001a\u0004\b\u0010\u0010\rR\u0015\u0010\t\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u000e\u001a\u0004\b\u0011\u0010\rR\u0015\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u000e\u001a\u0004\b\u0012\u0010\rR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0014\u00a8\u0006%"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/chart/SessionPoint;", "", "fullDate", "", "dateLabel", "avgMorning", "", "avgNoon", "avgEvening", "avgNight", "avgDaily", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;)V", "getAvgDaily", "()Ljava/lang/Double;", "Ljava/lang/Double;", "getAvgEvening", "getAvgMorning", "getAvgNight", "getAvgNoon", "getDateLabel", "()Ljava/lang/String;", "getFullDate", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;)Lcom/example/nhatkyduonghuyet/ui/chart/SessionPoint;", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
public final class SessionPoint {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String fullDate = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String dateLabel = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double avgMorning = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double avgNoon = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double avgEvening = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double avgNight = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double avgDaily = null;
    
    public SessionPoint(@org.jetbrains.annotations.NotNull()
    java.lang.String fullDate, @org.jetbrains.annotations.NotNull()
    java.lang.String dateLabel, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgMorning, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgNoon, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgEvening, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgNight, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgDaily) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFullDate() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDateLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getAvgMorning() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getAvgNoon() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getAvgEvening() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getAvgNight() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getAvgDaily() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component5() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.nhatkyduonghuyet.ui.chart.SessionPoint copy(@org.jetbrains.annotations.NotNull()
    java.lang.String fullDate, @org.jetbrains.annotations.NotNull()
    java.lang.String dateLabel, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgMorning, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgNoon, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgEvening, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgNight, @org.jetbrains.annotations.Nullable()
    java.lang.Double avgDaily) {
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