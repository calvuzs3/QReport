package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Replaces the per-template island-type filter (`check_item_template_island_types`,
 * added in Migration4to5) with a coarser per-module filter: a checkup's checklist
 * is now seeded from the modules associated with its island type, not from each
 * template's own island-type tags — see [net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster].
 *
 * The backfill carries forward every existing template-level association up to
 * its module, so checkups keep surfacing the same items right after this upgrade
 * instead of going empty until someone re-configures the new association screen.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `module_type_island_types` (
                `island_type_id` TEXT NOT NULL,
                `module_type_id` TEXT NOT NULL,
                PRIMARY KEY(`island_type_id`, `module_type_id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_module_type_island_types_module_type_id` ON `module_type_island_types` (`module_type_id`)")

        db.execSQL(
            """
            INSERT OR IGNORE INTO `module_type_island_types` (`island_type_id`, `module_type_id`)
            SELECT DISTINCT x.island_type_id, t.module_type_id
            FROM check_item_template_island_types x
            INNER JOIN check_item_templates t ON t.id = x.template_id
            """.trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `check_item_template_island_types`")
    }
}
