package com.example.nhatkyduonghuyet.ui.home;

import android.net.Uri;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Modifier;
import androidx.navigation.NavController;
import com.example.nhatkyduonghuyet.ui.navigation.GlucoseScreen;
import com.example.nhatkyduonghuyet.util.CsvExportHelper;
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\"\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a$\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a\u0018\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0007\u00a8\u0006\u000b"}, d2 = {"DateItem", "", "date", "", "onClick", "Lkotlin/Function1;", "DateListScreen", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/example/nhatkyduonghuyet/viewmodel/LogEntryViewModel;", "app_debug"})
public final class DateListScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void DateListScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void DateItem(@org.jetbrains.annotations.NotNull()
    java.lang.String date, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onClick) {
    }
}