package com.example.nhatkyduonghuyet.data;

import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import kotlinx.coroutines.flow.Flow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0012\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006J\u0012\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u00070\u0006J\u001a\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u00070\u00062\u0006\u0010\f\u001a\u00020\bJ\u0016\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0010R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/example/nhatkyduonghuyet/data/LogEntryRepository;", "", "logEntryDao", "Lcom/example/nhatkyduonghuyet/data/local/dao/LogEntryDao;", "(Lcom/example/nhatkyduonghuyet/data/local/dao/LogEntryDao;)V", "getAllDates", "Lkotlinx/coroutines/flow/Flow;", "", "", "getAllLogEntries", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "getEntriesForDate", "date", "upsert", "", "logEntry", "(Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class LogEntryRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao logEntryDao = null;
    
    @javax.inject.Inject()
    public LogEntryRepository(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao logEntryDao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getEntriesForDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry logEntry, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<java.lang.String>> getAllDates() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getAllLogEntries() {
        return null;
    }
}