package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recreate checkup_spare_parts with correct Room schema.
        // v11 migration used DEFAULT values and wrong index name which
        // may fail Room's schema validation on existing devices.
        db.execSQL("DROP TABLE IF EXISTS `checkup_spare_parts`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `checkup_spare_parts` (`id` TEXT NOT NULL, `checkup_id` TEXT NOT NULL, `article_uuid` TEXT NOT NULL, `name` TEXT NOT NULL, `code_oem` TEXT NOT NULL, `code_erp` TEXT NOT NULL, `code_bm` TEXT NOT NULL, `unit` TEXT NOT NULL, `quantity` REAL, `notes` TEXT NOT NULL, `added_at` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_spare_parts_checkup_id` ON `checkup_spare_parts` (`checkup_id`)")
    }
}
