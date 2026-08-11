package com.example.nhatkyduonghuyet.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.domain.repository.LogRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0012\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\n0\u0016J\b\u0010\u0017\u001a\u00020\u0007H\u0002J&\u0010\u0018\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u001a0\u00190\n0\t2\u0006\u0010\u001b\u001a\u00020\u0007J\u0014\u0010\u001c\u001a\u00020\u001d2\f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u000e0\nJ\u000e\u0010\u001f\u001a\u00020\u001d2\u0006\u0010\u001b\u001a\u00020\u0007J\u000e\u0010 \u001a\u00020\u001d2\u0006\u0010!\u001a\u00020\u000eR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\n0\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u001d\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\n0\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\fR\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\fR#\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\n0\t\u00a2\u0006\u000e\n\u0000\u0012\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0015\u0010\fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\""}, d2 = {"Lcom/example/nhatkyduonghuyet/viewmodel/LogEntryViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/example/nhatkyduonghuyet/domain/repository/LogRepository;", "(Lcom/example/nhatkyduonghuyet/domain/repository/LogRepository;)V", "_currentDate", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "allDates", "Lkotlinx/coroutines/flow/StateFlow;", "", "getAllDates", "()Lkotlinx/coroutines/flow/StateFlow;", "allLogEntries", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "getAllLogEntries", "currentDate", "getCurrentDate", "entriesForSelectedDate", "getEntriesForSelectedDate$annotations", "()V", "getEntriesForSelectedDate", "Lkotlinx/coroutines/flow/Flow;", "getCurrentDateFormatted", "getDailyChartData", "Lkotlin/Pair;", "", "date", "importLogEntries", "", "entries", "selectDate", "upsertLogEntry", "logEntry", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class LogEntryViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.domain.repository.LogRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _currentDate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> currentDate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> allLogEntries = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<java.lang.String>> allDates = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> entriesForSelectedDate = null;
    
    @javax.inject.Inject()
    public LogEntryViewModel(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.domain.repository.LogRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getCurrentDate() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getAllLogEntries() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<java.lang.String>> getAllDates() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getEntriesForSelectedDate() {
        return null;
    }
    
    @kotlin.OptIn(markerClass = {kotlinx.coroutines.ExperimentalCoroutinesApi.class})
    @java.lang.Deprecated()
    public static void getEntriesForSelectedDate$annotations() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getAllLogEntries() {
        return null;
    }
    
    public final void selectDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date) {
    }
    
    public final void upsertLogEntry(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry logEntry) {
    }
    
    public final void importLogEntries(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> entries) {
    }
    
    private final java.lang.String getCurrentDateFormatted() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<kotlin.Pair<java.lang.String, java.lang.Double>>> getDailyChartData(@org.jetbrains.annotations.NotNull()
    java.lang.String date) {
        return null;
    }
}