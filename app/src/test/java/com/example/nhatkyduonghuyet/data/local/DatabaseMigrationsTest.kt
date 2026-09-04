package com.example.nhatkyduonghuyet.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the migration SQL without needing an emulator.
 *
 * These tests are the safety net that lets us drop
 * `fallbackToDestructiveMigration()`: they prove the statements are additive
 * (no DROP / DELETE) and that only genuinely missing columns are added.
 */
class DatabaseMigrationsTest {

    @Test
    fun `covers every upgrade path into the latest version`() {
        val pairs = DatabaseMigrations.ALL.map { it.startVersion to it.endVersion }.toSet()
        // 1->2, 1->3, 1->4, 2->3, 2->4, 3->4
        val expected = buildSet {
            for (from in 1 until DatabaseMigrations.LATEST_VERSION) {
                for (to in (from + 1)..DatabaseMigrations.LATEST_VERSION) {
                    add(from to to)
                }
            }
        }
        assertEquals(expected, pairs)
        // A user skipping releases (v1 straight to v4) must be covered.
        assertTrue(pairs.contains(1 to DatabaseMigrations.LATEST_VERSION))
    }

    @Test
    fun `migrations never destroy data`() {
        val sql = DatabaseMigrations.createStatements().joinToString(" ").uppercase()
        assertTrue("Migration khong duoc DROP TABLE", !sql.contains("DROP TABLE"))
        assertTrue("Migration khong duoc DELETE", !sql.contains("DELETE FROM"))
        assertTrue("Migration khong duoc TRUNCATE", !sql.contains("TRUNCATE"))
    }

    @Test
    fun `every create statement is idempotent`() {
        DatabaseMigrations.createStatements().forEach { stmt ->
            assertTrue("Thieu IF NOT EXISTS: $stmt", stmt.contains("IF NOT EXISTS"))
        }
    }

    @Test
    fun `all three tables and the unique index are created`() {
        val sql = DatabaseMigrations.createStatements().joinToString(" ")
        assertTrue(sql.contains("`log_entries`"))
        assertTrue(sql.contains("`medications`"))
        assertTrue(sql.contains("`medication_logs`"))
        assertTrue(sql.contains("index_medication_logs_medicationId_date_session"))
        assertTrue(sql.contains("CREATE UNIQUE INDEX"))
    }

    @Test
    fun `no columns are added when the table is already current`() {
        val current = DatabaseMigrations.LOG_ENTRY_COLUMNS.keys + "id"
        val statements = DatabaseMigrations.missingColumnStatements("log_entries", current)
        assertTrue(statements.isEmpty())
    }

    @Test
    fun `only the missing columns are added on a legacy table`() {
        // An old build that only had the core diary fields.
        val legacy = setOf("id", "date", "session", "value")
        val statements = DatabaseMigrations.missingColumnStatements("log_entries", legacy)

        val added = statements.map { it.substringAfter("ADD COLUMN `").substringBefore('`') }
        assertTrue("bgBefore phai duoc them", added.contains("bgBefore"))
        assertTrue("heartRate phai duoc them", added.contains("heartRate"))
        assertTrue("note phai duoc them", added.contains("note"))
        // Columns that already exist must not be touched.
        assertTrue(!added.contains("date"))
        assertTrue(!added.contains("session"))
        assertTrue(!added.contains("value"))
        assertEquals(DatabaseMigrations.LOG_ENTRY_COLUMNS.size - 3, statements.size)
    }

    @Test
    fun `non null columns are added with a default so existing rows stay valid`() {
        val legacy = setOf("id", "date")
        val statements = DatabaseMigrations.missingColumnStatements("log_entries", legacy)
        statements.filter { it.contains("NOT NULL") }.forEach {
            // SQLite refuses ADD COLUMN NOT NULL without a default when rows exist.
            assertTrue("Thieu DEFAULT cho cot NOT NULL: $it", it.contains("DEFAULT"))
        }
    }

    @Test
    fun `missing table yields no alter statements`() {
        assertTrue(
            DatabaseMigrations.missingColumnStatements("log_entries", emptySet()).isEmpty()
        )
    }
}
