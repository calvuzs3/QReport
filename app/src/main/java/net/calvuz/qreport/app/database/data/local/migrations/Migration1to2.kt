package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // ── ti_island_associations ────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `ti_island_associations` (
                `id` TEXT NOT NULL,
                `intervention_id` TEXT NOT NULL,
                `island_id` TEXT NOT NULL,
                `association_type` TEXT NOT NULL,
                `notes` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `synced_at` INTEGER,
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`intervention_id`) REFERENCES `technical_interventions`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`island_id`) REFERENCES `facility_islands`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ti_island_associations_intervention_id` ON `ti_island_associations`(`intervention_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ti_island_associations_island_id` ON `ti_island_associations`(`island_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ti_island_associations_association_type` ON `ti_island_associations`(`association_type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ti_island_associations_is_deleted` ON `ti_island_associations`(`is_deleted`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ti_island_associations_intervention_id_island_id` ON `ti_island_associations`(`intervention_id`, `island_id`)")

        // ── checkup_maintenance_log_associations ──────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `checkup_maintenance_log_associations` (
                `id` TEXT NOT NULL,
                `checkup_id` TEXT NOT NULL,
                `maintenance_log_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `synced_at` INTEGER,
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`checkup_id`) REFERENCES `checkups`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`maintenance_log_id`) REFERENCES `maintenance_logs`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_maintenance_log_associations_checkup_id` ON `checkup_maintenance_log_associations`(`checkup_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_maintenance_log_associations_maintenance_log_id` ON `checkup_maintenance_log_associations`(`maintenance_log_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_maintenance_log_associations_is_deleted` ON `checkup_maintenance_log_associations`(`is_deleted`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_checkup_maintenance_log_associations_checkup_id_maintenance_log_id` ON `checkup_maintenance_log_associations`(`checkup_id`, `maintenance_log_id`)")

        // ── ti_maintenance_log_associations ───────────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `ti_maintenance_log_associations` (
                `id` TEXT NOT NULL,
                `intervention_id` TEXT NOT NULL,
                `maintenance_log_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `synced_at` INTEGER,
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`intervention_id`) REFERENCES `technical_interventions`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`maintenance_log_id`) REFERENCES `maintenance_logs`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ti_maintenance_log_associations_intervention_id` ON `ti_maintenance_log_associations`(`intervention_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ti_maintenance_log_associations_maintenance_log_id` ON `ti_maintenance_log_associations`(`maintenance_log_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ti_maintenance_log_associations_is_deleted` ON `ti_maintenance_log_associations`(`is_deleted`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ti_maintenance_log_associations_intervention_id_maintenance_log_id` ON `ti_maintenance_log_associations`(`intervention_id`, `maintenance_log_id`)")
    }
}
