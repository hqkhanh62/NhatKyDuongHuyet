package com.example.nhatkyduonghuyet;

import android.content.Context;
import com.example.nhatkyduonghuyet.data.LogEntryRepository;
import com.example.nhatkyduonghuyet.data.local.AppDatabase;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001R\u0012\u0010\u0002\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0004\u0010\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/example/nhatkyduonghuyet/AppContainer;", "", "logEntryRepository", "Lcom/example/nhatkyduonghuyet/data/LogEntryRepository;", "getLogEntryRepository", "()Lcom/example/nhatkyduonghuyet/data/LogEntryRepository;", "app_debug"})
public abstract interface AppContainer {
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.nhatkyduonghuyet.data.LogEntryRepository getLogEntryRepository();
}