package com.example.nhatkyduonghuyet.ui.detail;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.tooling.preview.Preview;
import androidx.core.content.ContextCompat;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.navigation.NavController;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.ml.GlucoseScanner;
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult;
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel;
import com.google.mlkit.vision.common.InputImage;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000N\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u001a2\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a\b\u0010\t\u001a\u00020\u0001H\u0007\u001a(\u0010\n\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0002\u001a\u00020\u0003H\u0007\u001a<\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00102\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u00142\b\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u0012\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001af\u0010\u0017\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00102\u0012\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u00010\u00072\u0006\u0010\u001a\u001a\u00020\u00102\b\b\u0002\u0010\u001b\u001a\u00020\u001c2\b\b\u0002\u0010\u001d\u001a\u00020\u001e2\u0012\u0010\u001f\u001a\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u00010\u00072\u0010\b\u0002\u0010 \u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0005H\u0007\u00a8\u0006!"}, d2 = {"CameraScannerDialog", "", "scanner", "Lcom/example/nhatkyduonghuyet/ml/GlucoseScanner;", "onDismiss", "Lkotlin/Function0;", "onResult", "Lkotlin/Function1;", "", "DayDetailReviewPreview", "DayDetailScreen", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/example/nhatkyduonghuyet/viewmodel/LogEntryViewModel;", "selectedDate", "", "SessionEntryCard", "sessionName", "logEntryState", "Landroidx/compose/runtime/MutableState;", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "onSave", "SmartInputTextField", "value", "onValueChange", "label", "modifier", "Landroidx/compose/ui/Modifier;", "keyboardOptions", "Landroidx/compose/foundation/text/KeyboardOptions;", "onVoiceResult", "onCameraClick", "app_debug"})
public final class DayDetailScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void DayDetailScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel viewModel, @org.jetbrains.annotations.NotNull()
    java.lang.String selectedDate, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ml.GlucoseScanner scanner) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void SessionEntryCard(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionName, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.MutableState<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> logEntryState, @org.jetbrains.annotations.Nullable()
    com.example.nhatkyduonghuyet.ml.GlucoseScanner scanner, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.nhatkyduonghuyet.data.local.entity.LogEntry, kotlin.Unit> onSave) {
    }
    
    @androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "DayDetail Review")
    @androidx.compose.runtime.Composable()
    public static final void DayDetailReviewPreview() {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void SmartInputTextField(@org.jetbrains.annotations.NotNull()
    java.lang.String value, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onValueChange, @org.jetbrains.annotations.NotNull()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    androidx.compose.foundation.text.KeyboardOptions keyboardOptions, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onVoiceResult, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function0<kotlin.Unit> onCameraClick) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.camera.core.ExperimentalGetImage.class})
    @androidx.compose.runtime.Composable()
    public static final void CameraScannerDialog(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ml.GlucoseScanner scanner, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Float, kotlin.Unit> onResult) {
    }
}