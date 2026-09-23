package com.example.nhatkyduonghuyet.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations that upgrade the database **without dropping user data**.
 *
 * Historically the app was built with `fallbackToDestructiveMigration()`, so
 * every schema bump silently wiped the blood-glucose diary and the medication
 * history. The old schemas were never exported (`exportSchema = false`), so we
 * cannot rely on hand-written "v2 -> v3" DDL: we do not know exactly what v2
 * looked like on any given user's phone.
 *
 * Instead each migration is *idempotent and defensive*: it inspects the live
 * database and only creates what is missing. One code path is therefore correct
 * for every legacy version, and re-running it is harmless.
 *
 * The SQL itself is kept as pure data ([createStatements] /
 * [missingColumnStatements]) so it can be executed against a real SQLite engine
 * in a JVM unit test. Instrumented tests do not run in CI, so this is the only
 * way the migrations are actually verified on every push.
 */
object DatabaseMigrations {

    /** Latest schema version, kept in sync with [AppDatabase]. */
    const val LATEST_VERSION = 4

    /** Current shape of `log_entries`, column name -> SQLite declaration. */
    val LOG_ENTRY_COLUMNS: Map<String, String> = linkedMapOf(
        "date" to "TEXT NOT NULL DEFAULT ''",
        "session" to "TEXT NOT NULL DEFAULT ''",
        "medType" to "TEXT",
        "dose" to "TEXT",
        "time" to "TEXT",
        "value" to "INTEGER NOT NULL DEFAULT 0",
        "bgBefore" to "REAL",
        "bgAfter" to "REAL",
        "bpSys" to "INTEGER",
        "bpDia" to "INTEGER",
        "heartRate" to "INTEGER",
        "note" to "TEXT"
    )

    private const val CREATE_LOG_ENTRIES = "CREATE TABLE IF NOT EXISTS `log_entries` (" +
        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
        "`date` TEXT NOT NULL, " +
        "`session` TEXT NOT NULL, " +
        "`medType` TEXT, " +
        "`dose` TEXT, " +
        "`time` TEXT, " +
        "`value` INTEGER NOT NULL DEFAULT 0, " +
        "`bgBefore` REAL, " +
        "`bgAfter` REAL, " +
        "`bpSys` INTEGER, " +
        "`bpDia` INTEGER, " +
        "`heartRate` INTEGER, " +
        "`note` TEXT)"

    private const val CREATE_MEDICATIONS = "CREATE TABLE IF NOT EXISTS `medications` (" +
        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
        "`name` TEXT NOT NULL, " +
        "`dosage` TEXT NOT NULL, " +
        "`instruction` TEXT NOT NULL, " +
        "`timing` TEXT NOT NULL)"

    private const val CREATE_MEDICATION_LOGS = "CREATE TABLE IF NOT EXISTS `medication_logs` (" +
        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
        "`medicationId` INTEGER NOT NULL, " +
        "`medicationNameSnapshot` TEXT NOT NULL, " +
        "`dosageSnapshot` TEXT NOT NULL, " +
        "`timestamp` INTEGER NOT NULL, " +
        "`date` TEXT NOT NULL, " +
        "`session` TEXT NOT NULL, " +
        "`amountTaken` REAL NOT NULL DEFAULT 1.0)"

    private const val CREATE_MEDICATION_LOGS_INDEX =
        "CREATE UNIQUE INDEX IF NOT EXISTS " +
            "`index_medication_logs_medicationId_date_session` " +
            "ON `medication_logs` (`medicationId`, `date`, `session`)"

    /** Tables/indices to ensure exist. Every statement uses IF NOT EXISTS. */
    fun createStatements(): List<String> = listOf(
        CREATE_LOG_ENTRIES,
        CREATE_MEDICATIONS,
        CREATE_MEDICATION_LOGS,
        CREATE_MEDICATION_LOGS_INDEX
    )

    /**
     * `ALTER TABLE ... ADD COLUMN` for each expected column the live table is
     * missing. Pure function of the observed column set, so it is unit-testable.
     */
    fun missingColumnStatements(
        table: String,
        existingColumns: Set<String>,
        expected: Map<String, String> = LOG_ENTRY_COLUMNS
    ): List<String> {
        // An empty set means the table does not exist yet; it will be created
        // with the full shape, so nothing to alter.
        if (existingColumns.isEmpty()) return emptyList()
        return expected.mapNotNull { (column, declaration) ->
            if (column in existingColumns) null
            else "ALTER TABLE `$table` ADD COLUMN `$column` $declaration"
        }
    }

    /**
     * Brings any older database up to the version-4 shape while keeping rows.
     * Safe from v1, v2 or v3 because every statement is conditional.
     */
    private fun upgradeToLatest(db: SupportSQLiteDatabase) {
        createStatements().forEach(db::execSQL)
        missingColumnStatements("log_entries", existingColumns(db, "log_entries"))
            .forEach(db::execSQL)
    }

    private fun existingColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex < 0) return emptySet()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        return columns
    }

    private fun migration(from: Int, to: Int) = object : Migration(from, to) {
        override fun migrate(db: SupportSQLiteDatabase) = upgradeToLatest(db)
    }

    /**
     * Every upgrade path into the current version, including direct jumps so a
     * user who skipped several releases still keeps their data instead of
     * falling through to a destructive rebuild.
     */
    val ALL: Array<Migration> = buildList {
        for (from in 1 until LATEST_VERSION) {
            for (to in (from + 1)..LATEST_VERSION) {
                add(migration(from, to))
            }
        }
    }.toTypedArray()
}
