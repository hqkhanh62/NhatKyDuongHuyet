package com.example.nhatkyduonghuyet.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The report is what the user actually sees after a restore, so "imported
 * nothing" must never be reported as success.
 */
class RestoreReportTest {

    @Test
    fun `empty report is not a success`() {
        val report = RestoreReport()
        assertFalse(report.touchedAnything)
        assertTrue(report.describe().contains("Không có dữ liệu"))
    }

    @Test
    fun `skipped-only report is still not a success`() {
        val report = RestoreReport(skipped = 4)
        assertFalse(report.touchedAnything)
        assertTrue(report.describe().contains("4"))
    }

    @Test
    fun `report lists each non-zero category`() {
        val text = RestoreReport(
            diaryAdded = 2,
            diaryUpdated = 1,
            medicationsAdded = 3,
            medicationLogsAdded = 4
        ).describe()
        assertTrue(text.contains("2 mục nhật ký mới"))
        assertTrue(text.contains("1 mục nhật ký cập nhật"))
        assertTrue(text.contains("3 thuốc"))
        assertTrue(text.contains("4 lượt uống"))
    }

    @Test
    fun `report hides zero categories`() {
        val text = RestoreReport(diaryAdded = 2).describe()
        assertTrue(text.contains("2 mục nhật ký mới"))
        assertFalse(text.contains("thuốc"))
        assertFalse(text.contains("lượt uống"))
    }

    @Test
    fun `updates alone count as having done something`() {
        assertTrue(RestoreReport(diaryUpdated = 1).touchedAnything)
    }
}
