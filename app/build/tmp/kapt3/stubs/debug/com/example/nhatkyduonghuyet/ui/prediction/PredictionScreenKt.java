package com.example.nhatkyduonghuyet.ui.prediction;

import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.navigation.NavController;
import com.example.nhatkyduonghuyet.ml.GlucosePredictor;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000(\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u0018\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u0007\u001a\u000e\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t\u001a\u0013\u0010\n\u001a\u00020\u000b2\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\f\u00a8\u0006\r"}, d2 = {"PredictionScreen", "", "navController", "Landroidx/navigation/NavController;", "predictor", "Lcom/example/nhatkyduonghuyet/ml/GlucosePredictor;", "getInsight", "", "value", "", "getRiskColor", "Landroidx/compose/ui/graphics/Color;", "(F)J", "app_debug"})
public final class PredictionScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void PredictionScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ml.GlucosePredictor predictor) {
    }
    
    public static final long getRiskColor(float value) {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String getInsight(float value) {
        return null;
    }
}