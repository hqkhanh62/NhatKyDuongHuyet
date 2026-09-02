package com.example.nhatkyduonghuyet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.WorkManager
import com.example.nhatkyduonghuyet.MainActivity
import com.example.nhatkyduonghuyet.R
import com.example.nhatkyduonghuyet.domain.PrivacyPolicy
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class GlucoseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdater.requestUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork("refresh_glucose_widget")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, GlucoseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.glucose_widget)
            val snapshot = WidgetSnapshotStore.get(context)
            val hideData = PrivacyPolicy.shouldHideWidgetData(context)

            if (snapshot == null) {
                views.setTextViewText(R.id.widget_date, "Đang tải...")
                views.setTextViewText(R.id.widget_avg, "--")
                views.setTextViewText(R.id.widget_count, "")
                WidgetUpdater.requestUpdate(context)
            } else {
                val displayDate = try {
                    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val outputSdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                    inputSdf.parse(snapshot.localDate)?.let { outputSdf.format(it) } ?: snapshot.localDate
                } catch (e: Exception) {
                    snapshot.localDate
                }

                views.setTextViewText(R.id.widget_date, displayDate)
                
                val avgText = if (hideData) {
                    "***"
                } else if (snapshot.averageMmol != null) {
                    "%.1f".format(Locale.US, snapshot.averageMmol)
                } else {
                    "--"
                }
                views.setTextViewText(R.id.widget_avg, avgText)
                
                val countText = when {
                    hideData -> if (snapshot.measurementCount > 0) "Có dữ liệu hôm nay" else "Chưa có số đo"
                    snapshot.measurementCount > 0 -> "${snapshot.measurementCount} lần đo"
                    else -> "Chưa có số đo"
                }
                views.setTextViewText(R.id.widget_count, countText)

                // Stale state indicator
                val isStale = (System.currentTimeMillis() - snapshot.capturedAt) > 3_600_000 // 1 hour
                if (isStale || snapshot.state == WidgetState.STALE) {
                    views.setTextColor(R.id.widget_avg, 0x88FFFFFF.toInt())
                } else {
                    views.setTextColor(R.id.widget_avg, 0xFFFFFFFF.toInt())
                }
            }

            // Deep link intent (WIDGET-04)
            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.nhatkyduonghuyet.OPEN_DAY_DETAIL"
                putExtra("date", snapshot?.localDate)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 
                appWidgetId, // Unique request code per instance (WIDGET-09)
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
