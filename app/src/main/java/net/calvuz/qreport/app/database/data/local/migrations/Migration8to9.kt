package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds sync support (synced_at, is_deleted) to the four checkup master tables:
 * module_types, check_item_templates, criticality_levels, checkup_statuses.
 *
 * These tables were previously local-only. After this migration they participate
 * in the existing pull/push sync protocol alongside island_types and client data.
 *
 * ALTER TABLE ADD COLUMN with a default value is always non-destructive in SQLite.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "module_types",
            "check_item_templates",
            "criticality_levels",
            "checkup_statuses"
        ).forEach { table ->
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `synced_at` INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        }
    }
}
