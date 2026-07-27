package com.example.nhatkyduonghuyet.ui.chart;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.unit.Dp;
import androidx.navigation.NavController;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel;
import java.text.SimpleDateFormat;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000n\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u001a\u0018\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u0007\u001a`\u0010\u0006\u001a\u00020\u00012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u000b2\u0006\u0010\u000e\u001a\u00020\u000b2\u0006\u0010\u000f\u001a\u00020\u000b2\b\b\u0002\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u0013H\u0007\u001a,\u0010\u0016\u001a\u00020\u00012\u0006\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u00132\u0006\u0010\u001a\u001a\u00020\u001bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u001c\u0010\u001d\u001a8\u0010\u001e\u001a\u00020\u00012\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u001f\u001a\u00020\u000b2\u0006\u0010\u001a\u001a\u00020\u001b2\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00010!H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\"\u0010#\u001a\u001a\u0010$\u001a\b\u0012\u0004\u0012\u00020\t0\b2\f\u0010%\u001a\b\u0012\u0004\u0012\u00020&0\b\u001a\u0017\u0010\'\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u0013H\u0002\u00a2\u0006\u0002\u0010(\u001a^\u0010)\u001a\u00020\u0001*\u00020*2\u000e\u0010+\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00130\b2\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020-2\u0016\u0010/\u001a\u0012\u0012\u0006\u0012\u0004\u0018\u00010\u0013\u0012\u0006\u0012\u0004\u0018\u00010-002\b\b\u0002\u00101\u001a\u00020\u000b\u00f8\u0001\u0000\u00a2\u0006\u0004\b2\u00103\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u00064"}, d2 = {"ChartScreen", "", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/example/nhatkyduonghuyet/viewmodel/LogEntryViewModel;", "FlexibleLineChart", "points", "", "Lcom/example/nhatkyduonghuyet/ui/chart/SessionPoint;", "showMorning", "", "showNoon", "showEvening", "showNight", "showDailyAvg", "modifier", "Landroidx/compose/ui/Modifier;", "minY", "", "maxY", "yStep", "InfoText", "label", "", "value", "color", "Landroidx/compose/ui/graphics/Color;", "InfoText-mxwnekA", "(Ljava/lang/String;Ljava/lang/Double;J)V", "SessionFilterChip", "selected", "onClick", "Lkotlin/Function0;", "SessionFilterChip-9LQNqLg", "(Ljava/lang/String;ZJLkotlin/jvm/functions/Function0;)V", "aggregateBySession", "entries", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "formatDouble", "(Ljava/lang/Double;)Ljava/lang/String;", "drawDataLine", "Landroidx/compose/ui/graphics/drawscope/DrawScope;", "values", "xStart", "", "stepX", "valueToY", "Lkotlin/Function1;", "isDashed", "drawDataLine-cf5BqRc", "(Landroidx/compose/ui/graphics/drawscope/DrawScope;Ljava/util/List;JFFLkotlin/jvm/functions/Function1;Z)V", "app_debug"})
public final class ChartScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class, androidx.compose.foundation.layout.ExperimentalLayoutApi.class})
    @androidx.compose.runtime.Composable()
    public static final void ChartScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel viewModel) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.util.List<com.example.nhatkyduonghuyet.ui.chart.SessionPoint> aggregateBySession(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries) {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    public static final void FlexibleLineChart(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.ui.chart.SessionPoint> points, boolean showMorning, boolean showNoon, boolean showEvening, boolean showNight, boolean showDailyAvg, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, double minY, double maxY, double yStep) {
    }
    
    private static final java.lang.String formatDouble(java.lang.Double value) {
        return null;
    }
}