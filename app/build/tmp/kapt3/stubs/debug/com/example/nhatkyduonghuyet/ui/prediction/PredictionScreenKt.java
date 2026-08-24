package com.example.nhatkyduonghuyet.ui.prediction;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.navigation.NavController;
import com.example.nhatkyduonghuyet.ai.PredictionOutcome;
import com.example.nhatkyduonghuyet.ml.GlucosePredictor;
import java.util.Locale;
import com.example.nhatkyduonghuyet.R;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u0010\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0003\u001a@\u0010\u0004\u001a\u00020\u00012\u0006\u0010\u0005\u001a\u00020\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00072\u0006\u0010\b\u001a\u00020\t2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u0010\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\rH\u0003\u001a\u0018\u0010\u000e\u001a\u00020\u00012\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0007\u001a\u0010\u0010\u0013\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\rH\u0007\u001a\u0013\u0010\u0014\u001a\u00020\u00152\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u0016\u00a8\u0006\u0017"}, d2 = {"PredictionErrorCard", "", "message", "", "PredictionInputCard", "fasting", "onFastingChange", "Lkotlin/Function1;", "type", "", "onTypeChange", "PredictionResultCard", "value", "", "PredictionScreen", "navController", "Landroidx/navigation/NavController;", "predictor", "Lcom/example/nhatkyduonghuyet/ml/GlucosePredictor;", "getInsight", "getRiskColor", "Landroidx/compose/ui/graphics/Color;", "(F)J", "app_debug"})
public final class PredictionScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void PredictionScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ml.GlucosePredictor predictor) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PredictionInputCard(java.lang.String fasting, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onFastingChange, int type, kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> onTypeChange) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PredictionResultCard(float value) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PredictionErrorCard(java.lang.String message) {
    }
    
    public static final long getRiskColor(float value) {
        return 0L;
    }
    
    @androidx.compose.runtime.Composable()
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String getInsight(float value) {
        return null;
    }
}