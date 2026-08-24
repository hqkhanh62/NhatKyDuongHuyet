package com.example.nhatkyduonghuyet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.nhatkyduonghuyet.MainActivity
import com.example.nhatkyduonghuyet.R

class GlucoseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int) {
        
        val views = RemoteViews(context.packageName, R.layout.glucose_widget)
        
        // Cài đặt click để mở app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // Ở đây có thể lấy dữ liệu mới nhất từ database nếu muốn, 
        // nhưng đơn giản nhất là chỉ dẫn tới app hoặc hiển thị text mặc định.
        views.setTextViewText(R.id.widget_title, "Đường huyết")
        views.setTextViewText(R.id.widget_value, "-- mmol/L")

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
