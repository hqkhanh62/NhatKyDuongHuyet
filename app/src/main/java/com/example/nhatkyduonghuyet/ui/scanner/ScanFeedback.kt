package com.example.nhatkyduonghuyet.ui.scanner

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic feedback for the Pro scanner.
 *
 * - Normal reading: single short buzz.
 * - Danger reading (hypo / very-high): SOS vibration pattern so the user
 *   feels the warning even without looking at the screen.
 */
object ScanFeedback {

    fun vibrateForResult(context: Context, isDanger: Boolean) {
        val vibrator = vibratorOf(context) ?: return
        try {
            if (isDanger) {
                vibrateSos(vibrator)
            } else {
                vibrateOnce(vibrator, 200L)
            }
        } catch (_: Exception) {
            // Haptics are best-effort on every device.
        }
    }

    fun vibrateSaved(context: Context) {
        val vibrator = vibratorOf(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0L, 80L, 80L, 160L), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 80L, 80L, 160L), -1)
            }
        } catch (_: Exception) {
            // Best-effort.
        }
    }

    private fun vibratorOf(context: Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    } catch (_: Exception) {
        null
    }

    private fun vibrateOnce(vibrator: Vibrator, millis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }

    /** SOS in Morse: dot-dot-dot / dash-dash-dash / dot-dot-dot. */
    private fun vibrateSos(vibrator: Vibrator) {
        val dot = 150L
        val dash = 450L
        val gap = 100L
        val letterGap = 300L
        val pattern = longArrayOf(
            0, dot, gap, dot, gap, dot, letterGap,
            dash, gap, dash, gap, dash, letterGap,
            dot, gap, dot, gap, dot
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
