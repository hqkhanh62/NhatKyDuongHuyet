package com.example.nhatkyduonghuyet.scan

import java.util.Locale

/**
 * HbA1c estimation shown instantly after a scan (Real-time AI Loop).
 *
 * Uses the same clinical approximation as the Dashboard:
 * `HbA1c (%) = (avgGlucose + 2.59) / 1.59`.
 * The single-scan value is provisional — the Dashboard recomputes the
 * weighted HbA1c from the full history right after auto-save.
 */
object Hba1cEstimator {

    fun fromAverageGlucose(avgMmol: Double): Double =
        if (avgMmol > 0.0 && avgMmol.isFinite()) (avgMmol + 2.59) / 1.59 else 0.0

    fun fromSingleReading(valueMmol: Float): Double =
        fromAverageGlucose(valueMmol.toDouble())

    fun format(hba1c: Double): String =
        String.format(Locale.US, "%.1f%%", hba1c)
}
