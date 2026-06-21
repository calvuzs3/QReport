package net.calvuz.qreport.app.database.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.items.data.local.seed.CheckUpMasterDataSeed
import net.calvuz.qreport.checkup.modules.domain.model.ModuleType

/**
 * Normalizes modules/criticality/checklist-templates from hardcoded enums/objects
 * (`ModuleType`, `CriticalityLevel`, `CheckItemModules`) into local master data
 * tables — same Expand approach already used for `island_types` (Migration2to3):
 * the enums are kept as fallback, not removed.
 *
 * `module_types.id`/`criticality_levels.id` are set equal to their `code` (the
 * enum `.name`) on purpose: it lets every other insert in this migration
 * reference them directly without a lookup, and keeps the 1:1 backfill of
 * existing `check_items` rows a trivial column copy.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `module_types` (
                `id` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `description` TEXT,
                `icon_name` TEXT,
                `sort_order` INTEGER NOT NULL,
                `is_active` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_module_types_code` ON `module_types` (`code`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_module_types_is_active` ON `module_types` (`is_active`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_module_types_sort_order` ON `module_types` (`sort_order`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `criticality_levels` (
                `id` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `priority` INTEGER NOT NULL,
                `color_hex` TEXT NOT NULL,
                `icon_emoji` TEXT,
                `sort_order` INTEGER NOT NULL,
                `is_active` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_criticality_levels_code` ON `criticality_levels` (`code`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_criticality_levels_is_active` ON `criticality_levels` (`is_active`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_criticality_levels_sort_order` ON `criticality_levels` (`sort_order`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `check_item_templates` (
                `id` TEXT NOT NULL,
                `module_type_id` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `criticality_id` TEXT NOT NULL,
                `order_index` INTEGER NOT NULL,
                `is_active` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_item_templates_module_type_id` ON `check_item_templates` (`module_type_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_item_templates_criticality_id` ON `check_item_templates` (`criticality_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_item_templates_is_active` ON `check_item_templates` (`is_active`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_item_templates_order_index` ON `check_item_templates` (`order_index`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `check_item_template_island_types` (
                `template_id` TEXT NOT NULL,
                `island_type_id` TEXT NOT NULL,
                PRIMARY KEY(`template_id`, `island_type_id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_item_template_island_types_island_type_id` ON `check_item_template_island_types` (`island_type_id`)")

        // Expand check_items with nullable FK columns, legacy string columns kept as fallback
        db.execSQL("ALTER TABLE `check_items` ADD COLUMN `module_type_id` TEXT")
        db.execSQL("ALTER TABLE `check_items` ADD COLUMN `criticality_id` TEXT")

        // Seed module_types from the ModuleType enum (id == code == enum name)
        ModuleType.entries.forEachIndexed { index, type ->
            db.execSQL(
                """
                INSERT INTO `module_types`
                (`id`, `code`, `label`, `description`, `icon_name`, `sort_order`, `is_active`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, NULL, ?, 1, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(type.name, type.name, type.displayName, type.description, index, now, now)
            )
        }

        // Seed criticality_levels from the CriticalityLevel enum (id == code == enum name)
        CriticalityLevel.entries.forEach { level ->
            db.execSQL(
                """
                INSERT INTO `criticality_levels`
                (`id`, `code`, `label`, `priority`, `color_hex`, `icon_emoji`, `sort_order`, `is_active`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(level.name, level.name, level.displayName, level.priority, level.color, level.icon, level.priority, now, now)
            )
        }

        // Seed check_item_templates + island-type crossref from the historical CheckItemModules data
        CheckUpMasterDataSeed.TEMPLATES.forEach { seed ->
            db.execSQL(
                """
                INSERT INTO `check_item_templates`
                (`id`, `module_type_id`, `category`, `description`, `criticality_id`, `order_index`, `is_active`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(seed.id, seed.moduleTypeCode, seed.category, seed.description, seed.criticalityCode, seed.orderIndex, now, now)
            )
            seed.islandTypeLabels.forEach { label ->
                // No-op if island_types is still empty on a fresh install (pre-sync) —
                // BackfillCheckItemTemplateIslandTypesUseCase retries this after first sync.
                db.execSQL(
                    """
                    INSERT INTO `check_item_template_island_types` (`template_id`, `island_type_id`)
                    SELECT ?, `id` FROM `island_types` WHERE `label` = ?
                    """.trimIndent(),
                    arrayOf(seed.id, label)
                )
            }
        }

        // Backfill existing check_items rows: module_type/criticality columns already
        // store the enum `.name`, which is exactly the new masters' id — trivial 1:1 copy.
        db.execSQL("UPDATE `check_items` SET `module_type_id` = `module_type` WHERE `module_type_id` IS NULL")
        db.execSQL("UPDATE `check_items` SET `criticality_id` = `criticality` WHERE `criticality_id` IS NULL")
    }
}
