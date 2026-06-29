package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add sync tracking fields to checkups
        db.execSQL("ALTER TABLE `checkups` ADD COLUMN `synced_at` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `checkups` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        // Add sync tracking to island associations
        db.execSQL("ALTER TABLE `checkup_island_associations` ADD COLUMN `synced_at` INTEGER DEFAULT NULL")
    }
}
