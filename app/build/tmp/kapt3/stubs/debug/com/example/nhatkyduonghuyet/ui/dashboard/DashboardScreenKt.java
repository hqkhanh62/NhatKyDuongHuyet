package com.example.nhatkyduonghuyet.ui.dashboard;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import com.example.nhatkyduonghuyet.R;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.TopAppBarDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.tooling.preview.Preview;
import com.example.nhatkyduonghuyet.ai.MultiStepResult;
import com.example.nhatkyduonghuyet.ai.PredictionResult;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000B\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\b\u0010\u0000\u001a\u00020\u0001H\u0007\u001a<\u0010\u0002\u001a\u00020\u00012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\b\b\u0002\u0010\u0007\u001a\u00020\bH\u0007\u001a\u0082\u0001\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u00102\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\u000e\b\u0002\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u0004H\u0007\u001a@\u0010\u0014\u001a\u00020\u00012\u0006\u0010\u0015\u001a\u00020\u00112\u0006\u0010\u0016\u001a\u00020\r2\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u00010\u00102\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u0010H\u0003\u001a\u0010\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0019\u001a\u00020\u001aH\u0003\u001a\u001e\u0010\u001b\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u001c2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00010\u0004H\u0007\u00a8\u0006\u001e"}, d2 = {"DashboardProPreview", "", "DashboardScreenPro", "onViewDetails", "Lkotlin/Function0;", "onNavigateToPrediction", "onNavigateToScanner", "viewModel", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel;", "DashboardScreenProContent", "state", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardUiState;", "showRetrain", "", "onDismissRetrain", "onTimeFilterSelected", "Lkotlin/Function1;", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "onRequestCloudInsight", "onExportPdf", "FilterMenu", "currentFilter", "expanded", "onExpandedChange", "ForecastStatusCard", "message", "", "GeminiInsightCard", "Lcom/example/nhatkyduonghuyet/ui/dashboard/GeminiInsightUiState;", "onRequest", "app_debug"})
public final class DashboardScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void DashboardScreenPro(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onViewDetails, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToPrediction, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToScanner, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel viewModel) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void DashboardScreenProContent(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState state, boolean showRetrain, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismissRetrain, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter, kotlin.Unit> onTimeFilterSelected, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRequestCloudInsight, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onViewDetails, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToPrediction, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToScanner, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onExportPdf) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void FilterMenu(com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter currentFilter, boolean expanded, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onExpandedChange, kotlin.jvm.functions.Function1<? super com.example.nhatkyduonghuyet.ui.dashboard.DashboardTimeFilter, kotlin.Unit> onTimeFilterSelected) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ForecastStatusCard(java.lang.String message) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void GeminiInsightCard(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ui.dashboard.GeminiInsightUiState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRequest) {
    }
    
    @androidx.compose.ui.tooling.preview.Preview(showBackground = true)
    @androidx.compose.runtime.Composable()
    public static final void DashboardProPreview() {
    }
}