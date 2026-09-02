package com.example.nhatkyduonghuyet.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetWorkerEntryPoint {
        fun logRepository(): LogRepository
    }

    override suspend fun doWork(): Result = try {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WidgetWorkerEntryPoint::class.java
        )
        val repository = entryPoint.logRepository()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())

        val logs = repository.getLogsByDate(today).first()
        val glucoseValues = logs.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
            .filter { it > 0 }

        val snapshot = if (glucoseValues.isEmpty()) {
            WidgetSnapshot.empty(today)
        } else {
            WidgetSnapshot(
                localDate = today,
                averageMmol = glucoseValues.average(),
                measurementCount = glucoseValues.size,
                latestMmol = glucoseValues.last(),
                capturedAt = System.currentTimeMillis(),
                state = WidgetState.FRESH
            )
        }

        WidgetSnapshotStore.save(applicationContext, snapshot)
        
        // Trigger widget update
        WidgetUpdater.updateAllWidgets(applicationContext)

        Result.success()
    } catch (e: Exception) {
        Log.e("WidgetRefreshWorker", "Error updating widget", e)
        Result.retry()
    }
}
