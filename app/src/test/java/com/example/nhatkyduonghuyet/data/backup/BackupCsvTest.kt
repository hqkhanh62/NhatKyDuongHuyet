package com.example.nhatkyduonghuyet.data.backup

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCsvTest {

    private fun diaryEntry() = LogEntry(
        id = 1L,
        date = "2026-08-01",
        session = "Sáng",
        medType = "Insulin",
        dose = "10",
        time = "07:30",
        value = 0,
        bgBefore = 6.1,
        bgAfter = 7.2,
        bpSys = 130,
        bpDia = 85,
        heartRate = 72,
        note = "Sau ăn sáng"
    )

    private fun medication() = Medication(
        id = 3L,
        name = "Metformin",
        dosage = "500mg",
        instruction = "Sau ăn",
        timing = "MORNING"
    )

    private fun medicationLog() = MedicationLog(
        id = 5L,
        medicationId = 3L,
        medicationNameSnapshot = "Metformin",
        dosageSnapshot = "500mg",
        timestamp = 1_760_000_000_000L,
        date = "2026-08-01",
        session = "MORNING",
        amountTaken = 1.0f
    )

    @Test
    fun `every part round-trips through encode and decode`() {
        val snapshot = BackupSnapshot(
            logEntries = listOf(diaryEntry()),
            medications = listOf(medication()),
            medicationLogs = listOf(medicationLog())
        )

        val diary = BackupCsv.decode(
            BackupPart.DIARY,
            BackupCsv.encode(BackupPart.DIARY, snapshot)
        ).logEntries
        assertEquals(1, diary.size)
        // The regression that motivated this design: BP/HR must survive.
        assertEquals(130, diary[0].bpSys)
        assertEquals(85, diary[0].bpDia)
        assertEquals(72, diary[0].heartRate)

        val meds = BackupCsv.decode(
            BackupPart.PRESCRIPTION,
            BackupCsv.encode(BackupPart.PRESCRIPTION, snapshot)
        ).medications
        assertEquals(1, meds.size)
        assertEquals("Metformin", meds[0].name)

        val logs = BackupCsv.decode(
            BackupPart.MEDICATION_HISTORY,
            BackupCsv.encode(BackupPart.MEDICATION_HISTORY, snapshot)
        ).medicationLogs
        assertEquals(1, logs.size)
        assertEquals("MORNING", logs[0].session)
    }

    @Test
    fun `decode only fills its own slice`() {
        val snapshot = BackupSnapshot(medications = listOf(medication()))
        val decoded = BackupCsv.decode(
            BackupPart.PRESCRIPTION,
            BackupCsv.encode(BackupPart.PRESCRIPTION, snapshot)
        )
        assertTrue(decoded.logEntries.isEmpty())
        assertTrue(decoded.medicationLogs.isEmpty())
    }

    @Test
    fun `detectPart recognises each file from its header alone`() {
        val snapshot = BackupSnapshot(
            logEntries = listOf(diaryEntry()),
            medications = listOf(medication()),
            medicationLogs = listOf(medicationLog())
        )
        BackupPart.entries.forEach { part ->
            val content = BackupCsv.encode(part, snapshot)
            assertEquals(
                "header detection failed for $part",
                part,
                // Deliberately pass a misleading name: header must win.
                BackupCsv.detectPart("khong_biet_la_gi.csv", content)
            )
        }
    }

    @Test
    fun `detectPart falls back to the file name`() {
        assertEquals(
            BackupPart.DIARY,
            BackupCsv.detectPart("nhat_ky_duong_huyet_20260801.csv", "rac\n")
        )
    }

    @Test
    fun `detectPart returns null for an unrelated file`() {
        assertNull(BackupCsv.detectPart("hoa_don.csv", "a,b,c\n1,2,3\n"))
    }

    @Test
    fun `empty snapshot still encodes a header for every part`() {
        BackupPart.entries.forEach { part ->
            val content = BackupCsv.encode(part, BackupSnapshot())
            assertTrue("$part produced nothing", content.isNotBlank())
            assertNotNull(BackupCsv.detectPart(part.fileName, content))
        }
    }

    @Test
    fun `snapshot reports emptiness and size`() {
        assertTrue(BackupSnapshot().isEmpty)
        val snapshot = BackupSnapshot(
            logEntries = listOf(diaryEntry()),
            medications = listOf(medication()),
            medicationLogs = listOf(medicationLog())
        )
        assertTrue(!snapshot.isEmpty)
        assertEquals(3, snapshot.totalRows)
    }

    @Test
    fun `part file names are unique and csv`() {
        val names = BackupPart.entries.map { it.fileName }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { it.endsWith(".csv") })
    }
}
