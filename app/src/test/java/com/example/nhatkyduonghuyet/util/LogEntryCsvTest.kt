package com.example.nhatkyduonghuyet.util

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app keeps `allowBackup="false"`, so this CSV is the only way a user can
 * carry their diary across a reinstall. These tests guard that it is lossless.
 */
class LogEntryCsvTest {

    private val entries = listOf(
        LogEntry(
            id = 1, date = "2026-09-04", session = "Sáng",
            medType = "Insulin Mixtard", dose = "6 đv", time = "07:15",
            bgBefore = 6.1, bgAfter = 8.4, note = "Ăn cháo",
            bpSys = 130, bpDia = 85, heartRate = 72
        ),
        LogEntry(
            id = 2, date = "2026-09-04", session = "Trưa",
            medType = null, dose = null, time = null,
            bgBefore = null, bgAfter = null,
            note = "Chóng mặt, buồn nôn", bpSys = null, bpDia = null, heartRate = null
        )
    )

    @Test
    fun `blood pressure and heart rate survive a round trip`() {
        // Regression: the old exporter silently dropped these three fields.
        val parsed = LogEntryCsv.parse(LogEntryCsv.build(entries))
        assertEquals(2, parsed.size)
        assertEquals(130, parsed[0].bpSys)
        assertEquals(85, parsed[0].bpDia)
        assertEquals(72, parsed[0].heartRate)
    }

    @Test
    fun `all diary fields survive a round trip`() {
        val parsed = LogEntryCsv.parse(LogEntryCsv.build(entries))
        val a = parsed[0]
        assertEquals("2026-09-04", a.date)
        assertEquals("Sáng", a.session)
        assertEquals("Insulin Mixtard", a.medType)
        assertEquals("6 đv", a.dose)
        assertEquals("07:15", a.time)
        assertEquals(6.1, a.bgBefore!!, 1e-6)
        assertEquals(8.4, a.bgAfter!!, 1e-6)
        assertEquals("Ăn cháo", a.note)
    }

    @Test
    fun `nulls stay null instead of becoming empty strings or zero`() {
        val b = LogEntryCsv.parse(LogEntryCsv.build(entries))[1]
        assertNull(b.medType)
        assertNull(b.dose)
        assertNull(b.time)
        assertNull(b.bgBefore)
        assertNull(b.bgAfter)
        assertNull(b.bpSys)
        assertNull(b.bpDia)
        assertNull(b.heartRate)
        assertNotNull(b.note)
    }

    @Test
    fun `commas in a note do not shift the columns`() {
        val parsed = LogEntryCsv.parse(LogEntryCsv.build(entries))
        assertEquals("Chóng mặt, buồn nôn", parsed[1].note)
        assertEquals("Trưa", parsed[1].session)
    }

    @Test
    fun `file has a utf8 bom so excel shows vietnamese correctly`() {
        assertTrue(LogEntryCsv.build(entries).startsWith(LogEntryCsv.UTF8_BOM))
    }

    @Test
    fun `files from older versions without bp columns still import`() {
        // 8-column layout written by the previous release.
        val legacy = "Ngay,Buoi,Loai insulin/thuoc,Lieu (dv/vien),Gio tiem/uong," +
            "Duong huyet truoc (mmol/L),Duong huyet sau 2 gio (mmol/L),Trieu chung/Ghi chu\n" +
            "2026-08-01,Sáng,Jardiance,1/2 v,07:00,5.8,7.2,Bình thường\n"
        val parsed = LogEntryCsv.parse(legacy)
        assertEquals(1, parsed.size)
        assertEquals("Jardiance", parsed[0].medType)
        assertEquals(5.8, parsed[0].bgBefore!!, 1e-6)
        // Missing columns must be null, not 0.
        assertNull(parsed[0].bpSys)
        assertNull(parsed[0].heartRate)
    }

    @Test
    fun `newline inside a note never splits the record`() {
        val withNewline = listOf(
            entries[0].copy(note = "Dòng 1\nDòng 2")
        )
        val csv = LogEntryCsv.build(withNewline).removePrefix(LogEntryCsv.UTF8_BOM)
        assertEquals(2, csv.trim().lines().size) // header + 1 record
        assertEquals(1, LogEntryCsv.parse(csv).size)
    }

    @Test
    fun `malformed rows are skipped not fatal`() {
        val csv = "Ngay,Buoi,a,b,c,d,e,f\n" +
            "2026-09-01,Sáng,,,,5.5,,\n" +
            "rac\n" +
            ",,,,,,,\n" +
            "2026-09-02,Trưa,,,,6.5,,\n"
        val parsed = LogEntryCsv.parse(csv)
        assertEquals(2, parsed.size)
        assertEquals(listOf("2026-09-01", "2026-09-02"), parsed.map { it.date })
    }

    @Test
    fun `comma decimal from vietnamese locale is accepted`() {
        val csv = "Ngay,Buoi,a,b,c,d,e,f\n2026-09-01,Sáng,,,,\"6,1\",,\n"
        assertEquals(6.1, LogEntryCsv.parse(csv)[0].bgBefore!!, 1e-6)
    }

    @Test
    fun `empty list produces a header only file`() {
        val csv = LogEntryCsv.build(emptyList()).removePrefix(LogEntryCsv.UTF8_BOM)
        assertEquals(1, csv.trim().lines().size)
        assertTrue(LogEntryCsv.parse(csv).isEmpty())
    }

    @Test
    fun `header exposes the three recovered columns`() {
        assertTrue(LogEntryCsv.HEADERS.contains("Huyet ap tam thu"))
        assertTrue(LogEntryCsv.HEADERS.contains("Huyet ap tam truong"))
        assertTrue(LogEntryCsv.HEADERS.contains("Nhip tim"))
    }
}
