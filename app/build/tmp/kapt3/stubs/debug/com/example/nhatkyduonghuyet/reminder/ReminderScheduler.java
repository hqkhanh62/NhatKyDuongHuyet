package com.example.nhatkyduonghuyet.reminder;

import android.content.Context;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;
import androidx.work.Data;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bJ\u0016\u0010\t\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u0004J\u000e\u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bJ.\u0010\f\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u000fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/example/nhatkyduonghuyet/reminder/ReminderScheduler;", "", "()V", "REMINDER_WORK_TAG_PREFIX", "", "cancelAllReminders", "", "context", "Landroid/content/Context;", "cancelReminder", "sessionKey", "scheduleAllReminders", "scheduleDailyReminder", "sessionLabel", "hour", "", "minute", "app_debug"})
public final class ReminderScheduler {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String REMINDER_WORK_TAG_PREFIX = "daily_reminder_";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.nhatkyduonghuyet.reminder.ReminderScheduler INSTANCE = null;
    
    private ReminderScheduler() {
        super();
    }
    
    public final void scheduleDailyReminder(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.lang.String sessionKey, @org.jetbrains.annotations.NotNull()
    java.lang.String sessionLabel, int hour, int minute) {
    }
    
    public final void cancelReminder(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.lang.String sessionKey) {
    }
    
    public final void scheduleAllReminders(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void cancelAllReminders(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
}