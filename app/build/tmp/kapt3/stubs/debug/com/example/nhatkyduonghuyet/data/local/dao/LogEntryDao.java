package com.example.nhatkyduonghuyet.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity;
import com.example.nhatkyduonghuyet.data.model.DailyAvgRow;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bH\'J\u0014\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\bH\'J\u0014\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u000b0\bH\'J\u001c\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u000b0\b2\u0006\u0010\u000f\u001a\u00020\fH\'J\u0014\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u000b0\bH\'J\u001c\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u000b0\b2\u0006\u0010\u000f\u001a\u00020\fH\'J\u000e\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\bH\'J\u0016\u0010\u0015\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0017\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0018"}, d2 = {"Lcom/example/nhatkyduonghuyet/data/local/dao/LogEntryDao;", "", "delete", "", "entry", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "(Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAdvancedStats", "Lkotlinx/coroutines/flow/Flow;", "Lcom/example/nhatkyduonghuyet/data/model/AdvancedStatsEntity;", "getAllDates", "", "", "getAllLogEntries", "getByDate", "date", "getDailyAverage", "Lcom/example/nhatkyduonghuyet/data/model/DailyAvgRow;", "getEntriesForDate", "getTotalCount", "", "insert", "update", "upsert", "app_debug"})
@androidx.room.Dao()
public abstract interface LogEntryDao {
    
    @androidx.room.Query(value = "SELECT * FROM log_entries WHERE date = :date")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getByDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry entry, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Upsert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry entry, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object update(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry entry, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.data.local.entity.LogEntry entry, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM log_entries")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Integer> getTotalCount();
    
    @androidx.room.Query(value = "SELECT * FROM log_entries WHERE date = :date")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getEntriesForDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date);
    
    @androidx.room.Query(value = "SELECT * FROM log_entries ORDER BY date DESC, time DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry>> getAllLogEntries();
    
    @androidx.room.Query(value = "SELECT DISTINCT date FROM log_entries ORDER BY date DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<java.lang.String>> getAllDates();
    
    @androidx.room.Query(value = "SELECT COUNT(*) as totalValid FROM log_entries")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity> getAdvancedStats();
    
    @androidx.room.Query(value = "SELECT date, AVG(bgBefore) as averageValue FROM log_entries GROUP BY date")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.example.nhatkyduonghuyet.data.model.DailyAvgRow>> getDailyAverage();
}