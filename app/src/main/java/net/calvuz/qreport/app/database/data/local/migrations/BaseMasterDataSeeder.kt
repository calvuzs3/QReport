package net.calvuz.qreport.app.database.data.local.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds the master-data tables that the app always needs a working default for
 * (checkup statuses/transitions, criticality levels), regardless of how the
 * `criticality_levels` / `checkup_statuses` / `checkup_status_transitions` tables
 * came to exist:
 * - On a fresh install, Room creates all tables at the latest version directly and
 *   calls `RoomDatabase.Callback.onCreate` (no migration runs), so seeding happens there.
 * - On an upgrade from <8, `MIGRATION_7_8` creates these tables and calls this seeder too.
 *
 * Other master tables (ModuleType, CheckItemTemplate, IslandType associations) are
 * intentionally left empty — those are configured by the user via "Impostazioni Checkup".
 */
object BaseMasterDataSeeder {

    fun seedCriticalityLevels(db: SupportSQLiteDatabase, now: Long) {
        data class Seed(
            val code: String,
            val label: String,
            val priority: Int,
            val colorHex: String,
            val iconEmoji: String
        )

        val seeds = listOf(
            Seed("NA", "N/A", 0, "#9E9E9E", "➖"),
            Seed("ROUTINE", "Routine", 1, "#2196F3", "🟢"),
            Seed("IMPORTANT", "Importante", 2, "#FF9800", "🟡"),
            Seed("CRITICAL", "Critico", 3, "#F44336", "🔴")
        )

        seeds.forEach { seed ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO `criticality_levels`
                (`id`, `code`, `label`, `priority`, `color_hex`, `icon_emoji`, `sort_order`, `is_active`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(seed.code, seed.code, seed.label, seed.priority, seed.colorHex, seed.iconEmoji, seed.priority, now, now)
            )
        }
    }

    fun seedCheckUpStatuses(db: SupportSQLiteDatabase, now: Long) {
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
                INSERT OR IGNORE INTO `checkup_statuses`
                (`id`, `code`, `label`, `color_hex`, `icon_emoji`, `sort_order`, `is_active`, `blocks_deletion`, `marks_completion`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    seed.code, seed.code, seed.label, seed.colorHex, seed.iconEmoji, seed.sortOrder,
                    if (seed.blocksDeletion) 1 else 0, if (seed.marksCompletion) 1 else 0, now, now
                )
            )
        }
    }

    fun seedCheckUpStatusTransitions(db: SupportSQLiteDatabase) {
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
                "INSERT OR IGNORE INTO `checkup_status_transitions` (`from_status_id`, `to_status_id`) VALUES (?, ?)",
                arrayOf<Any>(from, to)
            )
        }
    }

    fun seedAll(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        seedCriticalityLevels(db, now)
        seedCheckUpStatuses(db, now)
        seedCheckUpStatusTransitions(db)
    }
}
