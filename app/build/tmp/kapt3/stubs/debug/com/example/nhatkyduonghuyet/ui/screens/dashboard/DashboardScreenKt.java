package com.example.nhatkyduonghuyet.ui.screens.dashboard;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.tooling.preview.Preview;
import com.example.nhatkyduonghuyet.ui.components.DonutChartData;
import com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint;
import com.example.nhatkyduonghuyet.viewmodel.StatsViewModel;
import com.example.nhatkyduonghuyet.viewmodel.TimeFilter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000V\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a8\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\tH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\n\u0010\u000b\u001a\b\u0010\f\u001a\u00020\u0001H\u0007\u001a \u0010\r\u001a\u00020\u00012\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\t2\b\b\u0002\u0010\u000f\u001a\u00020\u0010H\u0007\u001a*\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00032\b\b\u0002\u0010\u0015\u001a\u00020\u0016H\u0007\u001a;\u0010\u0017\u001a\u00020\u00012\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u001a0\u00192\b\u0010\u001b\u001a\u0004\u0018\u00010\u00132\u0014\u0010\u001c\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u0013\u0012\u0004\u0012\u00020\u00010\u001dH\u0007\u00a2\u0006\u0002\u0010\u001e\u001a@\u0010\u001f\u001a\u00020\u00012\u0006\u0010 \u001a\u00020\u00032\u0006\u0010!\u001a\u00020\u00032\b\b\u0002\u0010\"\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\u0015\u001a\u00020\u0016H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b#\u0010$\u001a$\u0010%\u001a\u00020\u00012\u0006\u0010&\u001a\u00020\'2\u0012\u0010(\u001a\u000e\u0012\u0004\u0012\u00020\'\u0012\u0004\u0012\u00020\u00010\u001dH\u0007\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006)"}, d2 = {"ChartToggleRow", "", "label", "", "checked", "", "color", "Landroidx/compose/ui/graphics/Color;", "onToggle", "Lkotlin/Function0;", "ChartToggleRow-9LQNqLg", "(Ljava/lang/String;ZJLkotlin/jvm/functions/Function0;)V", "DashboardPreview", "DashboardScreen", "onViewDetails", "viewModel", "Lcom/example/nhatkyduonghuyet/viewmodel/StatsViewModel;", "GlucoseAiCard", "prediction", "", "risk", "modifier", "Landroidx/compose/ui/Modifier;", "PremiumChartSection", "chartData", "", "Lcom/example/nhatkyduonghuyet/viewmodel/MultiSeriesPoint;", "selectedX", "onValueSelected", "Lkotlin/Function1;", "(Ljava/util/List;Ljava/lang/Float;Lkotlin/jvm/functions/Function1;)V", "StatCard", "title", "value", "unit", "StatCard-42QJj7c", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JLandroidx/compose/ui/Modifier;)V", "TimeFilterDropdown", "currentFilter", "Lcom/example/nhatkyduonghuyet/viewmodel/TimeFilter;", "onFilterSelected", "app_debug"})
public final class DashboardScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void DashboardScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onViewDetails, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.StatsViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void GlucoseAiCard(@org.jetbrains.annotations.NotNull()
    java.lang.String label, float prediction, @org.jetbrains.annotations.NotNull()
    java.lang.String risk, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void PremiumChartSection(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint> chartData, @org.jetbrains.annotations.Nullable()
    java.lang.Float selectedX, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Float, kotlin.Unit> onValueSelected) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void TimeFilterDropdown(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.TimeFilter currentFilter, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.nhatkyduonghuyet.viewmodel.TimeFilter, kotlin.Unit> onFilterSelected) {
    }
    
    @androidx.compose.ui.tooling.preview.Preview(showBackground = true)
    @androidx.compose.runtime.Composable()
    public static final void DashboardPreview() {
    }
}