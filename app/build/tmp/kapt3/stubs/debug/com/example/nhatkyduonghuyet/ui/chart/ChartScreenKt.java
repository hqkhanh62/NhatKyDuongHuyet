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

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000L\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u0018\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u0007\u001a\u0010\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\bH\u0007\u001a,\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u0010\u0010\u0011\u001a8\u0010\u0012\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u000e\u001a\u00020\u000f2\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u0016H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u0017\u0010\u0018\u001a\u001a\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\b0\u001a2\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u001c0\u001a\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001d"}, d2 = {"ChartScreen", "", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/example/nhatkyduonghuyet/viewmodel/LogEntryViewModel;", "DayInfoCard", "day", "Lcom/example/nhatkyduonghuyet/ui/chart/SessionPoint;", "MiniStat", "label", "", "value", "", "color", "Landroidx/compose/ui/graphics/Color;", "MiniStat-mxwnekA", "(Ljava/lang/String;Ljava/lang/Double;J)V", "SessionFilterChip", "selected", "", "onClick", "Lkotlin/Function0;", "SessionFilterChip-9LQNqLg", "(Ljava/lang/String;ZJLkotlin/jvm/functions/Function0;)V", "aggregateBySession", "", "entries", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "app_debug"})
public final class ChartScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class, androidx.compose.foundation.layout.ExperimentalLayoutApi.class})
    @androidx.compose.runtime.Composable()
    public static final void ChartScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void DayInfoCard(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.chart.SessionPoint day) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.util.List<com.example.nhatkyduonghuyet.ui.chart.SessionPoint> aggregateBySession(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries) {
        return null;
    }
}