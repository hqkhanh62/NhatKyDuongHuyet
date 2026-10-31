package com.example.nhatkyduonghuyet.ml

/** Rate-limits full-height OCR retries without ending a scan after empty frames. */
internal class SmallTextSweepPolicy(
    private val minimumIntervalMillis: Long = 600L
) {
    private var lastSweepAtMillis: Long? = null

    @Synchronized
    fun shouldRun(fields: MeterDisplayFields, nowMillis: Long): Boolean {
        if (fields.glucose != null && fields.time != null && fields.date != null) return false

        val lastSweep = lastSweepAtMillis
        if (lastSweep != null && nowMillis - lastSweep < minimumIntervalMillis) return false

        lastSweepAtMillis = nowMillis
        return true
    }
}
