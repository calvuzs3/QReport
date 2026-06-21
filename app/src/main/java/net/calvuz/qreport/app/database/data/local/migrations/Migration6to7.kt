package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Removes the "ricambi necessari" (spare parts needed) feature, now considered
 * obsolete — a separate integration with the QStore warehouse app is planned to
 * replace it, out of scope for this migration.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `spare_parts`")
    }
}
