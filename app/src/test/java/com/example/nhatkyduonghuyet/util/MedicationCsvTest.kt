package com.example.nhatkyduonghuyet.util

import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationCsvTest {

    private val meds = listOf(
        Medication(id = 1, name = "Jardiance", dosage = "25 mg", instruction = "Sáng 1/2 v", timing = "07:00"),
        // Comma + quote + newline must survive a round-trip.
        Medication(id = 2, name = "Insulin Mixtard, FlexPen", dosage = "100 IU/mL", instruction = "Trưa 6 đv;\nchiều 8 đv", timing = ""),
        Medication(id = 3, name = "Thuốc \"đặc biệt\"", dosage = "", instruction = "", timing = "")
    )

    private val logs = listOf(
        MedicationLog(
            id = 1, medicationId = 1, medicationNameSnapshot = "Jardiance",
            dosageSnapshot = "25 mg", timestamp = 1_700_000_000_000L,
            date = "2026-09-04", session = "MORNING", amountTaken = 0.5f
        ),
        MedicationLog(
            id = 2, medicationId = 2, medicationNameSnapshot = "Insulin Mixtard, FlexPen",
            dosageSnapshot = "100 IU/mL", timestamp = 1_700_003_600_000L,
            date = "2026-09-04", session = "NOON", amountTaken = 1f
        )
    )

    @Test
    fun `prescription csv has bom and header`() {
        val csv = MedicationCsv.buildPrescriptionCsv(meds)
        assertTrue(csv.startsWith(MedicationCsv.UTF8_BOM))
        val header = csv.removePrefix(MedicationCsv.UTF8_BOM).lineSequence().first()
        assertEquals(MedicationCsv.PRESCRIPTION_HEADERS, MedicationCsv.parseLine(header))
    }

    @Test
    fun `prescription round trip preserves fields`() {
        val csv = MedicationCsv.buildPrescriptionCsv(meds)
        val parsed = MedicationCsv.parsePrescriptionCsv(csv)
        assertEquals(meds.size, parsed.size)
        meds.forEachIndexed { i, original ->
            assertEquals(original.name, parsed[i].name)
            assertEquals(original.dosage, parsed[i].dosage)
            // Newlines are flattened to ';' so the file stays line-based.
            assertEquals(
                original.instruction.replace("\n", ";"),
                parsed[i].instruction
            )
        }
    }

    @Test
    fun `parsed medications get a fresh id so room can autogenerate`() {
        val parsed = MedicationCsv.parsePrescriptionCsv(MedicationCsv.buildPrescriptionCsv(meds))
        assertTrue(parsed.all { it.id == 0L })
    }

    @Test
    fun `history round trip preserves value fields`() {
        val csv = MedicationCsv.buildHistoryCsv(logs)
        val parsed = MedicationCsv.parseHistoryCsv(csv)
        assertEquals(2, parsed.size)
        assertEquals("2026-09-04", parsed[0].date)
        assertEquals("MORNING", parsed[0].session)
        assertEquals("Jardiance", parsed[0].medicationNameSnapshot)
        assertEquals(0.5f, parsed[0].amountTaken, 1e-4f)
        assertEquals(1_700_000_000_000L, parsed[0].timestamp)
        assertEquals(1L, parsed[0].medicationId)

        assertEquals("NOON", parsed[1].session)
        assertEquals("Insulin Mixtard, FlexPen", parsed[1].medicationNameSnapshot)
        assertEquals(1f, parsed[1].amountTaken, 1e-4f)
    }

    @Test
    fun `session labels are vietnamese in the exported file`() {
        val csv = MedicationCsv.buildHistoryCsv(logs)
        assertTrue(csv.contains("Sang"))
        assertTrue(csv.contains("Trua"))
        assertEquals("Truoc khi ngu", MedicationCsv.sessionLabel("BEDTIME"))
        // Unknown keys pass through instead of being dropped.
        assertEquals("SNACK", MedicationCsv.sessionLabel("SNACK"))
    }

    @Test
    fun `newlines are flattened so a record never spans two lines`() {
        val csv = MedicationCsv.buildPrescriptionCsv(meds).removePrefix(MedicationCsv.UTF8_BOM)
        // 1 header + 3 medications, no stray line from the embedded newline.
        assertEquals(4, csv.trim().lines().size)
    }

    @Test
    fun `escaping quotes commas and newlines`() {
        assertEquals("abc", MedicationCsv.escape("abc"))
        assertEquals("\"a,b\"", MedicationCsv.escape("a,b"))
        assertEquals("\"a\"\"b\"", MedicationCsv.escape("a\"b"))
        assertEquals("a;b", MedicationCsv.escape("a\nb"))
        assertEquals(listOf("a,b"), MedicationCsv.parseLine("\"a,b\""))
        assertEquals(listOf("a\"b"), MedicationCsv.parseLine("\"a\"\"b\""))
    }

    @Test
    fun `empty tables still produce a header only file`() {
        val csv = MedicationCsv.buildPrescriptionCsv(emptyList())
        assertEquals(1, csv.removePrefix(MedicationCsv.UTF8_BOM).trim().lines().size)
        assertTrue(MedicationCsv.parsePrescriptionCsv(csv).isEmpty())
    }

    @Test
    fun `malformed rows are skipped not fatal`() {
        val csv = MedicationCsv.UTF8_BOM +
            "ID,Ten thuoc,Ham luong,Lieu dung,Thoi diem\n" +
            "1,Jardiance,25 mg,Sang 1/2 v,\n" +
            "rac\n" +
            ",,\n" +
            "2,Trajenta,5 mg,Sang 1/2 v,\n"
        val parsed = MedicationCsv.parsePrescriptionCsv(csv)
        assertEquals(listOf("Jardiance", "Trajenta"), parsed.map { it.name })
    }

    @Test
    fun `file without header is still parsed`() {
        val csv = "1,Jardiance,25 mg,Sang 1/2 v,\n"
        assertEquals(1, MedicationCsv.parsePrescriptionCsv(csv).size)
    }

    @Test
    fun `history import accepts vietnamese session labels`() {
        val csv = "Ngay,Buoi,Gio uong,Ten thuoc,Ham luong,So luong,MedicationID,Timestamp\n" +
            "2026-09-04,Chieu,17:30,Lipistad,10 mg,1,4,1700000000000\n"
        val parsed = MedicationCsv.parseHistoryCsv(csv)
        assertEquals(1, parsed.size)
        assertEquals("AFTERNOON", parsed[0].session)
        assertEquals("Lipistad", parsed[0].medicationNameSnapshot)
    }
}
