package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Normalizes the checkup workflow status (`CheckUpStatus` enum: DRAFT, IN_PROGRESS,
 * COMPLETED, EXPORTED, ARCHIVED) into a local master data table plus a transitions
 * crossref table — same "Expand" idea as `Migration4to5`, but with no dual-field
 * fallback needed: `checkups.status` already stores exactly the enum's `.name`
 * (e.g. "DRAFT"), which becomes the new master row's `id`/`code` 1:1, so existing
 * data needs zero backfill.
 *
 * Unlike the other normalized master tables (ModuleType/CriticalityLevel/...),
 * this one also carries the workflow rules themselves as data, replacing what used
 * to be hardcoded `when` branches:
 * - `blocks_deletion` replaces the per-status delete guard in `DeleteCheckUpUseCase`.
 * - `marks_completion` replaces the `newStatus == COMPLETED` check in
 *   `UpdateCheckUpStatusUseCase` that stamps `checkups.completed_at`.
 * - `checkup_status_transitions` replaces the hardcoded transition matrix; seeded
 *   here with the exact graph that was hardcoded before this migration.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checkup_statuses` (
                `id` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `color_hex` TEXT NOT NULL,
                `icon_emoji` TEXT,
                `sort_order` INTEGER NOT NULL,
                `is_active` INTEGER NOT NULL,
                `blocks_deletion` INTEGER NOT NULL,
                `marks_completion` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_checkup_statuses_code` ON `checkup_statuses` (`code`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_statuses_is_active` ON `checkup_statuses` (`is_active`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_statuses_sort_order` ON `checkup_statuses` (`sort_order`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checkup_status_transitions` (
                `from_status_id` TEXT NOT NULL,
                `to_status_id` TEXT NOT NULL,
                PRIMARY KEY(`from_status_id`, `to_status_id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_status_transitions_to_status_id` ON `checkup_status_transitions` (`to_status_id`)")

        // Seed checkup_statuses (id == code == the enum `.name` already stored in checkups.status)
        data class Seed(
            val code: String,
            val label: String,
            val colorHex: String,
            val iconEmoji: String,
            val sortOrder: Int,
            val blocksDeletion: Boolean,
            val marksCompletion: Boolean
        )

        val seeds = listOf(
            Seed("DRAFT", "Bozza", "#EEEEEE", "📝", 0, blocksDeletion = false, marksCompletion = false),
            Seed("IN_PROGRESS", "In corso", "#9E9E9E", "⏳", 1, blocksDeletion = false, marksCompletion = false),
            Seed("COMPLETED", "Completato", "#1976D2", "✅", 2, blocksDeletion = true, marksCompletion = true),
            Seed("EXPORTED", "Esportato", "#388E3C", "📤", 3, blocksDeletion = true, marksCompletion = false),
            Seed("ARCHIVED", "Archiviato", "#C49000", "📦", 4, blocksDeletion = true, marksCompletion = false)
        )

        seeds.forEach { seed ->
            db.execSQL(
                """
                INSERT INTO `checkup_statuses`
                (`id`, `code`, `label`, `color_hex`, `icon_emoji`, `sort_order`, `is_active`, `blocks_deletion`, `marks_completion`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    seed.code, seed.code, seed.label, seed.colorHex, seed.iconEmoji, seed.sortOrder,
                    if (seed.blocksDeletion) 1 else 0, if (seed.marksCompletion) 1 else 0, now, now
                )
            )
        }

        // Seed checkup_status_transitions — exact replica of the hardcoded matrix in
        // UpdateCheckUpStatusUseCase before this migration. ARCHIVED gets no rows
        // (was the hardcoded terminal state); from now on it stays terminal only
        // until someone adds a transition from the new management screen.
        val transitions = listOf(
            "DRAFT" to "IN_PROGRESS",
            "DRAFT" to "COMPLETED",
            "IN_PROGRESS" to "DRAFT",
            "IN_PROGRESS" to "COMPLETED",
            "COMPLETED" to "EXPORTED",
            "EXPORTED" to "ARCHIVED"
        )

        transitions.forEach { (from, to) ->
            db.execSQL(
                "INSERT INTO `checkup_status_transitions` (`from_status_id`, `to_status_id`) VALUES (?, ?)",
                arrayOf<Any>(from, to)
            )
        }
    }
}
