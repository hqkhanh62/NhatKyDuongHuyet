package com.example.nhatkyduonghuyet.widget

import android.content.Context
import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import org.json.JSONObject

data class WidgetSnapshot(
    val localDate: String,
    val averageMmol: Double?,
    val measurementCount: Int,
    val latestMmol: Double?,
    val capturedAt: Long,
    val state: WidgetState = WidgetState.FRESH
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("localDate", localDate)
            put("averageMmol", averageMmol ?: 0.0)
            put("hasAverage", averageMmol != null)
            put("measurementCount", measurementCount)
            put("latestMmol", latestMmol ?: 0.0)
            put("hasLatest", latestMmol != null)
            put("capturedAt", capturedAt)
            put("state", state.name)
        }.toString()
    }

    companion object {
        fun fromJson(json: String?): WidgetSnapshot? {
            if (json == null) return null
            return try {
                val obj = JSONObject(json)
                WidgetSnapshot(
                    localDate = obj.getString("localDate"),
                    averageMmol = if (obj.getBoolean("hasAverage")) obj.getDouble("averageMmol") else null,
                    measurementCount = obj.getInt("measurementCount"),
                    latestMmol = if (obj.getBoolean("hasLatest")) obj.getDouble("latestMmol") else null,
                    capturedAt = obj.getLong("capturedAt"),
                    state = WidgetState.valueOf(obj.getString("state"))
                )
            } catch (e: Exception) {
                null
            }
        }

        fun empty(date: String) = WidgetSnapshot(
            localDate = date,
            averageMmol = null,
            measurementCount = 0,
            latestMmol = null,
            capturedAt = System.currentTimeMillis(),
            state = WidgetState.EMPTY
        )

        fun error(date: String) = WidgetSnapshot(
            localDate = date,
            averageMmol = null,
            measurementCount = 0,
            latestMmol = null,
            capturedAt = System.currentTimeMillis(),
            state = WidgetState.ERROR
        )
    }
}

enum class WidgetState { EMPTY, FRESH, STALE, ERROR }

object WidgetSnapshotStore {
    private const val PREFS_NAME = "glucose_widget_prefs"
    private const val KEY_SNAPSHOT = "latest_snapshot"

    fun save(context: Context, snapshot: WidgetSnapshot) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SNAPSHOT, snapshot.toJson())
            .apply()
    }

    fun get(context: Context): WidgetSnapshot? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SNAPSHOT, null)
        return WidgetSnapshot.fromJson(json)
    }
}
