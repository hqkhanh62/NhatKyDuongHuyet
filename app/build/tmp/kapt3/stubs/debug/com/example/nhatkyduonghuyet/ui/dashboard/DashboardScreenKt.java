package com.example.nhatkyduonghuyet.ui.dashboard;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.tooling.preview.Preview;
import androidx.compose.ui.text.font.FontWeight;
import com.example.nhatkyduonghuyet.ui.dashboard.components.*;
import com.example.nhatkyduonghuyet.ai.PredictionResult;
import com.example.nhatkyduonghuyet.ai.MultiStepResult;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import androidx.compose.ui.Alignment;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000:\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\u001a\b\u0010\u0000\u001a\u00020\u0001H\u0007\u001a<\u0010\u0002\u001a\u00020\u00012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\b\b\u0002\u0010\u0007\u001a\u00020\bH\u0007\u001ad\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u00102\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00042\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u0004H\u0007\u001a\u0012\u0010\u0012\u001a\u00020\u00012\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0007\u00a8\u0006\u0015"}, d2 = {"DashboardProPreview", "", "DashboardScreenPro", "onViewDetails", "Lkotlin/Function0;", "onNavigateToPrediction", "onNavigateToScanner", "viewModel", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardViewModel;", "DashboardScreenProContent", "state", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardUiState;", "showRetrain", "", "onDismissRetrain", "onTimeFilterSelected", "Lkotlin/Function1;", "Lcom/example/nhatkyduonghuyet/ui/dashboard/DashboardTimeFilter;", "GeminiInsightCard", "insight", "", "app_debug"})
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
    kotlin.jvm.functions.Function0<kotlin.Unit> onViewDetails, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToPrediction, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToScanner) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void GeminiInsightCard(@org.jetbrains.annotations.Nullable()
    java.lang.String insight) {
    }
    
    @androidx.compose.ui.tooling.preview.Preview(showBackground = true)
    @androidx.compose.runtime.Composable()
    public static final void DashboardProPreview() {
    }
}