package com.example.nhatkyduonghuyet.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.nhatkyduonghuyet.data.repository.LogRepository;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\u000bJ\u0006\u0010\'\u001a\u00020%J\u0006\u0010(\u001a\u00020%J\u0006\u0010)\u001a\u00020%R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u000e0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u001d\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0013R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00070\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0013R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00070\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0013R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00070\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0013R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001d0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0013R\u0017\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u000b0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u0013R\u0017\u0010!\u001a\b\u0012\u0004\u0012\u00020\"0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0013\u00a8\u0006*"}, d2 = {"Lcom/example/nhatkyduonghuyet/viewmodel/StatsViewModel;", "Landroidx/lifecycle/ViewModel;", "repo", "Lcom/example/nhatkyduonghuyet/data/repository/LogRepository;", "(Lcom/example/nhatkyduonghuyet/data/repository/LogRepository;)V", "_showAfter", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_showBefore", "_showDaily", "_timeFilter", "Lcom/example/nhatkyduonghuyet/viewmodel/TimeFilter;", "allEntries", "Lkotlinx/coroutines/flow/StateFlow;", "", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "chartData", "Lcom/example/nhatkyduonghuyet/viewmodel/MultiSeriesPoint;", "getChartData", "()Lkotlinx/coroutines/flow/StateFlow;", "filteredEntries", "getFilteredEntries", "showAfter", "getShowAfter", "showBefore", "getShowBefore", "showDaily", "getShowDaily", "stats", "Lcom/example/nhatkyduonghuyet/viewmodel/StatsUi;", "getStats", "timeFilter", "getTimeFilter", "totalCount", "", "getTotalCount", "setTimeFilter", "", "filter", "toggleAfter", "toggleBefore", "toggleDaily", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class StatsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.data.repository.LogRepository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nhatkyduonghuyet.viewmodel.TimeFilter> _timeFilter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.viewmodel.TimeFilter> timeFilter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _showBefore = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> showBefore = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _showAfter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> showAfter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _showDaily = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> showDaily = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> allEntries = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> filteredEntries = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> totalCount = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.viewmodel.StatsUi> stats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint>> chartData = null;
    
    @javax.inject.Inject()
    public StatsViewModel(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.repository.LogRepository repo) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.viewmodel.TimeFilter> getTimeFilter() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getShowBefore() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getShowAfter() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getShowDaily() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getFilteredEntries() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getTotalCount() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.nhatkyduonghuyet.viewmodel.StatsUi> getStats() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint>> getChartData() {
        return null;
    }
    
    public final void setTimeFilter(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.viewmodel.TimeFilter filter) {
    }
    
    public final void toggleBefore() {
    }
    
    public final void toggleAfter() {
    }
    
    public final void toggleDaily() {
    }
}