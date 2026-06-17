package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // island_types becomes bidirectionally syncable: add synced_at/is_deleted
        // so create/update/soft-delete from the device can be pushed to the server.
        db.execSQL("ALTER TABLE `island_types` ADD COLUMN `synced_at` INTEGER")
        db.execSQL("ALTER TABLE `island_types` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_island_types_updated_at` ON `island_types` (`updated_at`)"
        )
    }
}
