package com.example.nhatkyduonghuyet.domain.scanner

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.GlucoseRiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vòng "cảnh báo rủi ro + tính lại HbA1c" chạy ngay khi một chỉ số được quét. */
class ScanInsightEngineTest {

    private fun entry(
        date: String,
        session: String = "Sáng",
        before: Double? = null,
        after: Double? = null,
        time: String? = "07:00"
    ) = LogEntry(
        date = date,
        session = session,
        time = time,
        bgBefore = before,
        bgAfter = after
    )

    @Test
    fun `flags severe hypoglycaemia as critical`() {
        val insight = ScanInsightEngine.analyze(2.5f, GlucoseSession.MORNING, ReadingSlot.BEFORE_MEAL, emptyList(), "2026-09-15")
        assertEquals(GlucoseRiskLevel.LOW, insight.risk)
        assertTrue(insight.isCritical)
        assertTrue(insight.alerts.any { it.message.contains("Hạ đường huyết nặng") })
    }

    @Test
    fun `flags moderate low as critical too`() {
        val insight = ScanInsightEngine.analyze(3.6f, GlucoseSession.NOON, ReadingSlot.BEFORE_MEAL, emptyList(), "2026-09-15")
        assertEquals(GlucoseRiskLevel.LOW, insight.risk)
        assertTrue(insight.alerts.any { it.level == AlertLevel.CRITICAL })
    }

    @Test
    fun `very high reading raises the danger alert`() {
        val insight = ScanInsightEngine.analyze(17.2f, GlucoseSession.EVENING, ReadingSlot.AFTER_MEAL, emptyList(), "2026-09-15")
        assertEquals(GlucoseRiskLevel.VERY_HIGH, insight.risk)
        assertTrue(insight.alerts.any { it.message.contains("Rất cao") })
    }

    @Test
    fun `post meal target is stricter than the daytime target`() {
        val afterMeal = ScanInsightEngine.analyze(9.0f, GlucoseSession.NOON, ReadingSlot.AFTER_MEAL, emptyList(), "2026-09-15")
        val beforeMeal = ScanInsightEngine.analyze(9.0f, GlucoseSession.NOON, ReadingSlot.BEFORE_MEAL, emptyList(), "2026-09-15")
        assertEquals(GlucoseRiskLevel.HIGH, afterMeal.risk)
        assertEquals(GlucoseRiskLevel.NORMAL, beforeMeal.risk)
        assertTrue(afterMeal.alerts.any { it.message.contains("sau ăn 2 giờ") })
    }

    @Test
    fun `fasting reading in the impaired range gets an advisory note`() {
        val insight = ScanInsightEngine.analyze(6.4f, GlucoseSession.MORNING, ReadingSlot.BEFORE_MEAL, emptyList(), "2026-09-15")
        assertTrue(insight.alerts.any { it.message.contains("ranh giới rối loạn dung nạp glucose") })
    }

    @Test
    fun `a reading far above the recent average is reported as unusual`() {
        val history = listOf(
            entry("2026-09-13", before = 5.4),
            entry("2026-09-14", before = 5.6),
            entry("2026-09-15", before = 5.5)
        )
        val insight = ScanInsightEngine.analyze(8.9f, GlucoseSession.NOON, ReadingSlot.BEFORE_MEAL, history, "2026-09-16")
        assertTrue(insight.alerts.any { it.message.contains("Bất thường") })
    }

    @Test
    fun `recomputes the estimated hba1c including the scanned value`() {
        val history = listOf(
            entry("2026-09-13", before = 6.0, after = 7.0),
            entry("2026-09-14", before = 6.2, after = 7.4)
        )
        val insight = ScanInsightEngine.analyze(15.0f, GlucoseSession.NOON, ReadingSlot.AFTER_MEAL, history, "2026-09-15")
        assertTrue(insight.hba1c.isAvailable)
        assertTrue(
            "chỉ số cao phải kéo HbA1c lên",
            insight.hba1c.percent > insight.previousHba1c.percent
        )
        assertTrue(insight.hba1cDelta > 0.0)
        assertEquals("+", insight.hba1cDeltaText?.take(1))
    }

    @Test
    fun `surfaces the ai forecast warning when the model expects a spike`() {
        val insight = ScanInsightEngine.analyze(
            value = 8.0f,
            session = GlucoseSession.NOON,
            slot = ReadingSlot.BEFORE_MEAL,
            entries = emptyList(),
            date = "2026-09-15",
            forecastNext = 12.5f
        )
        assertEquals(12.5f, insight.forecastNext)
        assertTrue(insight.alerts.any { it.message.contains("AI dự báo 6 giờ tới") })
    }

    @Test
    fun `a healthy reading is not alarming`() {
        val insight = ScanInsightEngine.analyze(5.2f, GlucoseSession.NOON, ReadingSlot.BEFORE_MEAL, emptyList(), "2026-09-15")
        assertEquals(GlucoseRiskLevel.NORMAL, insight.risk)
        assertFalse(insight.isCritical)
        assertFalse(insight.hasWarning)
        assertTrue(insight.alerts.isNotEmpty())
    }

    @Test
    fun `reports time in range and sample count`() {
        val history = listOf(
            entry("2026-09-13", before = 5.0, after = 9.0),
            entry("2026-09-14", before = 12.0, after = 3.0)
        )
        val insight = ScanInsightEngine.analyze(6.0f, GlucoseSession.NOON, ReadingSlot.BEFORE_MEAL, history, "2026-09-15")
        // 5.0, 9.0, 12.0, 3.0 (lịch sử) + 6.0 -> 3/5 nằm trong 3.9..10.0
        assertEquals(60, insight.timeInRangePercent)
        assertEquals(5, insight.sampleCount)
    }
}
