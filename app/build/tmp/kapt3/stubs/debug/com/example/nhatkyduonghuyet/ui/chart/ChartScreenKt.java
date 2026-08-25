package com.example.nhatkyduonghuyet.ui.chart;

import android.view.MotionEvent;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000R\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\"\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0007\u001a\u0010\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\nH\u0007\u001a,\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u0012\u0010\u0013\u001a8\u0010\u0014\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0010\u001a\u00020\u00112\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u0018H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u0019\u0010\u001a\u001a\u001a\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\n0\u001c2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\u001c\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001f"}, d2 = {"ChartScreen", "", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/example/nhatkyduonghuyet/viewmodel/LogEntryViewModel;", "dashboardViewModel", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel;", "DayInfoCard", "day", "Lcom/example/nhatkyduonghuyet/ui/chart/SessionPoint;", "MiniStat", "label", "", "value", "", "color", "Landroidx/compose/ui/graphics/Color;", "MiniStat-mxwnekA", "(Ljava/lang/String;Ljava/lang/Double;J)V", "SessionFilterChip", "selected", "", "onClick", "Lkotlin/Function0;", "SessionFilterChip-9LQNqLg", "(Ljava/lang/String;ZJLkotlin/jvm/functions/Function0;)V", "aggregateBySession", "", "entries", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "app_debug"})
public final class ChartScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class, androidx.compose.foundation.layout.ExperimentalLayoutApi.class})
    @androidx.compose.runtime.Composable()
    public static final void ChartScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel viewModel, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel dashboardViewModel) {
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