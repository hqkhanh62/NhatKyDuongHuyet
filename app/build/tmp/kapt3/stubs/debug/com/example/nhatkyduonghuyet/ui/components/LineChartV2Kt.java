package com.example.nhatkyduonghuyet.ui.components;

import androidx.compose.runtime.Composable;
import androidx.compose.ui.Modifier;
import com.example.nhatkyduonghuyet.ui.chart.SessionPoint;
import com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint;
import com.patrykandpatrick.vico.core.axis.AxisPosition;
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter;
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider;
import com.patrykandpatrick.vico.core.entry.FloatEntry;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\u001a@\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u00052\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0005H\u0003\u001aR\u0010\u000b\u001a\u00020\u00012\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\u00052\b\b\u0002\u0010\u000e\u001a\u00020\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u000f2\b\b\u0002\u0010\u0011\u001a\u00020\u000f2\b\b\u0002\u0010\u0012\u001a\u00020\u000f2\b\b\u0002\u0010\u0013\u001a\u00020\u000f2\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u0007\u001a>\u0010\u000b\u001a\u00020\u00012\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u00052\b\b\u0002\u0010\u0016\u001a\u00020\u000f2\b\b\u0002\u0010\u0017\u001a\u00020\u000f2\b\b\u0002\u0010\u0018\u001a\u00020\u000f2\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u0007\u00a8\u0006\u0019"}, d2 = {"CommonLineChart", "", "modifier", "Landroidx/compose/ui/Modifier;", "seriesList", "", "Lcom/patrykandpatrick/vico/core/entry/FloatEntry;", "lineColors", "Landroidx/compose/ui/graphics/Color;", "dateLabels", "", "LineChartV2", "points", "Lcom/example/nhatkyduonghuyet/ui/chart/SessionPoint;", "showMorning", "", "showNoon", "showEvening", "showNight", "showDailyAvg", "data", "Lcom/example/nhatkyduonghuyet/viewmodel/MultiSeriesPoint;", "showBefore", "showAfter", "showDaily", "app_debug"})
public final class LineChartV2Kt {
    
    @androidx.compose.runtime.Composable()
    public static final void LineChartV2(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.ui.chart.SessionPoint> points, boolean showMorning, boolean showNoon, boolean showEvening, boolean showNight, boolean showDailyAvg, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void LineChartV2(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint> data, boolean showBefore, boolean showAfter, boolean showDaily, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CommonLineChart(androidx.compose.ui.Modifier modifier, java.util.List<? extends java.util.List<com.patrykandpatrick.vico.core.entry.FloatEntry>> seriesList, java.util.List<androidx.compose.ui.graphics.Color> lineColors, java.util.List<java.lang.String> dateLabels) {
    }
}