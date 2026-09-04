package com.example.nhatkyduonghuyet.data.backup

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.ZipInputStream

class BackupBundleTest {

    private fun snapshot() = BackupSnapshot(
        logEntries = listOf(
            LogEntry(
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
        ),
        medications = listOf(
            Medication(
                id = 3L,
                name = "Metformin",
                dosage = "500mg",
                instruction = "Sau ăn",
                timing = "MORNING"
            )
        ),
        medicationLogs = listOf(
            MedicationLog(
                id = 5L,
                medicationId = 3L,
                medicationNameSnapshot = "Metformin",
                dosageSnapshot = "500mg",
                timestamp = 1_760_000_000_000L,
                date = "2026-08-01",
                session = "MORNING",
                amountTaken = 1.0f
            )
        )
    )

    @Test
    fun `bundle round-trips every dataset in one file`() {
        val restored = BackupBundle.unpack(BackupBundle.pack(snapshot()))

        assertEquals(1, restored.logEntries.size)
        assertEquals(1, restored.medications.size)
        assertEquals(1, restored.medicationLogs.size)

        // The whole point of the bundle: nothing is left behind.
        assertEquals(snapshot().totalRows, restored.totalRows)
    }

    @Test
    fun `bundle preserves blood pressure and heart rate`() {
        val restored = BackupBundle.unpack(BackupBundle.pack(snapshot()))
        val entry = restored.logEntries.single()
        assertEquals(130, entry.bpSys)
        assertEquals(85, entry.bpDia)
        assertEquals(72, entry.heartRate)
    }

    @Test
    fun `bundle contains one entry per dataset plus a readme`() {
        val names = mutableListOf<String>()
        ZipInputStream(BackupBundle.pack(snapshot()).inputStream()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                names += e.name
                zip.closeEntry()
            }
        }
        BackupPart.entries.forEach { part ->
            assertTrue("thiếu ${part.fileName}", names.contains(part.fileName))
        }
        // A human opening the zip should find instructions.
        assertTrue(names.any { it.endsWith(".txt") })
    }

    @Test
    fun `unpack ignores entries it does not recognise`() {
        // The readme is an unrecognised entry and must not break the restore.
        val restored = BackupBundle.unpack(BackupBundle.pack(snapshot()))
        assertTrue(restored.totalRows > 0)
    }

    @Test
    fun `isBundle detects a zip by its magic bytes`() {
        val packed = BackupBundle.pack(snapshot())
        // Misleading name on purpose: content must win.
        assertTrue(BackupBundle.isBundle("khong_phai_zip.csv", packed))
    }

    @Test
    fun `isBundle rejects plain csv content`() {
        val csv = BackupCsv.encode(BackupPart.DIARY, snapshot())
        assertFalse(BackupBundle.isBundle("nhat_ky.csv", csv.toByteArray()))
    }

    @Test
    fun `isBundle falls back to the file extension`() {
        assertTrue(BackupBundle.isBundle("sao_luu.zip", ByteArray(0)))
    }

    @Test
    fun `empty snapshot still produces a readable bundle`() {
        val restored = BackupBundle.unpack(BackupBundle.pack(BackupSnapshot()))
        assertTrue(restored.isEmpty)
    }

    @Test
    fun `bundle name is a zip with the expected prefix`() {
        assertTrue(BackupBundle.FILE_PREFIX.isNotBlank())
        assertEquals("application/zip", BackupBundle.MIME_TYPE)
    }
}
