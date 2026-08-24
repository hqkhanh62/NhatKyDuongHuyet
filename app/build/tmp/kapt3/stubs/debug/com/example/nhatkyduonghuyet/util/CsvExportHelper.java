package com.example.nhatkyduonghuyet.util;

import android.content.Context;
import android.net.Uri;
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\u0005H\u0002J$\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0004J\u001c\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00042\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rJ\u0016\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0012\u001a\u00020\u0005H\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/example/nhatkyduonghuyet/util/CsvExportHelper;", "", "()V", "CSV_HEADERS", "", "", "escapeCsv", "value", "exportLogEntriesToCsv", "", "context", "Landroid/content/Context;", "uri", "Landroid/net/Uri;", "logEntries", "Lcom/example/nhatkyduonghuyet/data/local/entity/LogEntry;", "importCsv", "parseCsvLine", "line", "app_debug"})
public final class CsvExportHelper {
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> CSV_HEADERS = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.nhatkyduonghuyet.util.CsvExportHelper INSTANCE = null;
    
    private CsvExportHelper() {
        super();
    }
    
    /**
     * Escape gia tri CSV: boc trong dau ngoac kep neu chua dau phay hoac xuong dong
     */
    private final java.lang.String escapeCsv(java.lang.String value) {
        return null;
    }
    
    public final boolean exportLogEntriesToCsv(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.net.Uri uri, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> logEntries) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.nhatkyduonghuyet.data.local.entity.LogEntry> importCsv(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
        return null;
    }
    
    /**
     * Parse dong CSV co ho tro gia tri boc trong dau ngoac kep
     */
    private final java.util.List<java.lang.String> parseCsvLine(java.lang.String line) {
        return null;
    }
}