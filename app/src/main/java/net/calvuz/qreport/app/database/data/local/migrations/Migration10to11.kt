package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `checkup_spare_parts` (`id` TEXT NOT NULL, `checkup_id` TEXT NOT NULL, `article_uuid` TEXT NOT NULL, `name` TEXT NOT NULL, `code_oem` TEXT NOT NULL, `code_erp` TEXT NOT NULL, `code_bm` TEXT NOT NULL, `unit` TEXT NOT NULL, `quantity` REAL, `notes` TEXT NOT NULL, `added_at` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkup_spare_parts_checkup_id` ON `checkup_spare_parts` (`checkup_id`)")
    }
}
