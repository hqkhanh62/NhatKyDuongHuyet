package com.example.nhatkyduonghuyet.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.nhatkyduonghuyet.ai.PredictionOutcome;
import com.example.nhatkyduonghuyet.data.repository.AIRepository;
import com.example.nhatkyduonghuyet.domain.repository.LogRepository;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0017\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\rj\u0002\b\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/example/nhatkyduonghuyet/viewmodel/TimeFilter;", "", "days", "", "label", "", "(Ljava/lang/String;IILjava/lang/String;)V", "getDays", "()I", "getLabel", "()Ljava/lang/String;", "LAST_7_DAYS", "LAST_15_DAYS", "LAST_30_DAYS", "ALL", "app_debug"})
public enum TimeFilter {
    /*public static final*/ LAST_7_DAYS /* = new LAST_7_DAYS(0, null) */,
    /*public static final*/ LAST_15_DAYS /* = new LAST_15_DAYS(0, null) */,
    /*public static final*/ LAST_30_DAYS /* = new LAST_30_DAYS(0, null) */,
    /*public static final*/ ALL /* = new ALL(0, null) */;
    private final int days = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String label = null;
    
    TimeFilter(int days, java.lang.String label) {
    }
    
    public final int getDays() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.nhatkyduonghuyet.viewmodel.TimeFilter> getEntries() {
        return null;
    }
}