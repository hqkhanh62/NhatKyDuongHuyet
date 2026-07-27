package com.example.nhatkyduonghuyet.data.repository;

import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006J\u0012\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u0006J\u0012\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\t0\u0006J\u001a\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u00062\u0006\u0010\u000e\u001a\u00020\u000fJ\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u0006J\u0016\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0015J\u0016\u0010\u0016\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0015R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/example/nhatkyduonghuyet/data/repository/LogRepository;", "", "dao", "Lcom/example/nhatkyduonghuyet/data/local/dao/LogEntryDao;", "(Lcom/example/nhatkyduonghuyet/data/local/dao/LogEntryDao;)V", "getAdvancedStats", "Lkotlinx/coroutines/flow/Flow;", "Lcom/example/nhatkyduonghuyet/data/model/AdvancedStatsEntity;", "getAllEntries", "", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "getDailyAverage", "Lcom/example/nhatkyduonghuyet/data/model/DailyAvgRow;", "getEntries", "date", "", "getTotalCount", "", "update", "", "entry", "(Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "upsert", "app_debug"})
public final class LogRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao dao = null;
    
    public LogRepository(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao dao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Integer> getTotalCount() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity> getAdvancedStats() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.model.DailyAvgRow>> getDailyAverage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getAllEntries() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getEntries(@org.jetbrains.annotations.NotNull()
    java.lang.String date) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object update(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry entry, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry entry, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}