package com.example.nhatkyduonghuyet;

import android.content.Context;
import com.example.nhatkyduonghuyet.data.LogEntryRepository;
import com.example.nhatkyduonghuyet.data.local.AppDatabase;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068VX\u0096\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u000b"}, d2 = {"Lcom/example/nhatkyduonghuyet/AppDataContainer;", "Lcom/example/nhatkyduonghuyet/AppContainer;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "logEntryRepository", "Lcom/example/nhatkyduonghuyet/data/LogEntryRepository;", "getLogEntryRepository", "()Lcom/example/nhatkyduonghuyet/data/LogEntryRepository;", "logEntryRepository$delegate", "Lkotlin/Lazy;", "app_debug"})
public final class AppDataContainer implements com.example.nhatkyduonghuyet.AppContainer {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy logEntryRepository$delegate = null;
    
    public AppDataContainer(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.example.nhatkyduonghuyet.data.LogEntryRepository getLogEntryRepository() {
        return null;
    }
}