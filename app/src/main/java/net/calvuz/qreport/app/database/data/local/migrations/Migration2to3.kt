package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create island_types table (server-authoritative, populated via sync pull)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `island_types` (
                `id` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `description` TEXT,
                `icon_name` TEXT,
                `maintenance_interval_days` INTEGER NOT NULL DEFAULT 180,
                `sort_order` INTEGER NOT NULL DEFAULT 0,
                `is_active` INTEGER NOT NULL DEFAULT 1,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `idx_island_types_code` ON `island_types` (`code`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_island_types_is_active` ON `island_types` (`is_active`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_island_types_sort_order` ON `island_types` (`sort_order`)"
        )

        // Add nullable island_type_id to facility_islands (Expand — old column kept)
        db.execSQL(
            "ALTER TABLE `facility_islands` ADD COLUMN `island_type_id` TEXT"
        )

        // Add nullable island_type_id to checkups (Expand — old column kept)
        db.execSQL(
            "ALTER TABLE `checkups` ADD COLUMN `island_type_id` TEXT"
        )
    }
}
