package net.calvuz.qreport.app.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.checkup.data.local.dao.CheckItemDao
import net.calvuz.qreport.checkup.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.checkup.data.local.dao.CheckUpDao
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import net.calvuz.qreport.client.contact.data.local.dao.ContactDao
import net.calvuz.qreport.client.facility.data.local.dao.FacilityDao
import net.calvuz.qreport.client.island.data.local.dao.IslandDao
import net.calvuz.qreport.photo.data.local.dao.PhotoDao
import net.calvuz.qreport.checkup.data.local.dao.SparePartDao
import net.calvuz.qreport.client.contract.data.local.dao.ContractDao
import net.calvuz.qreport.client.island.maintenance.data.local.dao.MaintenanceLogDao
import net.calvuz.qreport.client.unit.data.local.dao.MechanicalUnitDao
import net.calvuz.qreport.sync.data.local.dao.SyncDao
import net.calvuz.qreport.ti.data.local.dao.TechnicalInterventionDao
import javax.inject.Singleton

/**
 * Database Dependency Injection Module
 *
 * Singleton instance of the Room Database Hilt Module
 * and its relative DAOs
 *
 * Setup:
 * - Database Room
 * - DAO injection
 * - Transactions and Error handling
 *
 * "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` TEXT NOT NULL, `client_id` TEXT NOT NULL, `name` TEXT, `description` TEXT, `start_date` INTEGER NOT NULL, `end_date` INTEGER NOT NULL, `has_priority` INTEGER NOT NULL, `has_remote_assistance` INTEGER NOT NULL, `has_maintenance` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS maintenance_logs (
                id                      TEXT NOT NULL PRIMARY KEY,
                island_id               TEXT NOT NULL,
                operation_type          TEXT NOT NULL,
                custom_operation_label  TEXT,
                mechanical_unit_id      TEXT,
                component_label         TEXT,
                description             TEXT NOT NULL,
                technician_name         TEXT NOT NULL,
                technician_company      TEXT,
                operating_hours_at_event INTEGER,
                cycle_count_at_event    INTEGER,
                outcome                 TEXT NOT NULL,
                duration_minutes        INTEGER,
                notes                   TEXT,
                performed_at            INTEGER NOT NULL,
                created_at              INTEGER NOT NULL,
                updated_at              INTEGER NOT NULL,
                synced_at               INTEGER,
                is_active               INTEGER NOT NULL DEFAULT 1,
                is_deleted              INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (island_id) REFERENCES facility_islands(id)
                    ON DELETE CASCADE
            )
        """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_logs_island_id ON maintenance_logs (island_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_logs_mechanical_unit_id ON maintenance_logs (mechanical_unit_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_logs_operation_type ON maintenance_logs (operation_type)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_logs_outcome ON maintenance_logs (outcome)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_logs_performed_at ON maintenance_logs (performed_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_logs_is_deleted ON maintenance_logs (is_deleted)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {

            // clients — ha già updated_at, solo i 2 nuovi campi
            database.execSQL("ALTER TABLE clients ADD COLUMN synced_at INTEGER")
            database.execSQL("ALTER TABLE clients ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_clients_is_deleted ON clients(is_deleted)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_clients_updated_at ON clients(updated_at)")

            // contacts
            database.execSQL("ALTER TABLE contacts ADD COLUMN synced_at INTEGER")
            database.execSQL("ALTER TABLE contacts ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_contacts_is_deleted ON contacts(is_deleted)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_contacts_updated_at ON contacts(updated_at)")

            // contracts
            database.execSQL("ALTER TABLE contracts ADD COLUMN synced_at INTEGER")
            database.execSQL("ALTER TABLE contracts ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_contracts_is_deleted ON contracts(is_deleted)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_contracts_updated_at ON contracts(updated_at)")
            // facilities
            database.execSQL("ALTER TABLE facilities ADD COLUMN synced_at INTEGER")
            database.execSQL("ALTER TABLE facilities ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_facilities_is_deleted ON facilities(is_deleted)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_facilities_updated_at ON facilities(updated_at)")

            // facility_islands
            database.execSQL("ALTER TABLE facility_islands ADD COLUMN synced_at INTEGER")
            database.execSQL("ALTER TABLE facility_islands ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_facility_islands_is_deleted ON facility_islands(is_deleted)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_facility_islands_updated_at ON facility_islands(updated_at)")

            // mechanical_units
            database.execSQL("ALTER TABLE mechanical_units ADD COLUMN synced_at INTEGER")
            database.execSQL("ALTER TABLE mechanical_units ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_mechanical_units_is_deleted ON mechanical_units(is_deleted)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_mechanical_units_updated_at ON mechanical_units(updated_at)")

        }
    }

    /* Singleton instance of the rRoom Database */
    @Provides
    @Singleton
    fun provideQReportDatabase(
        @ApplicationContext context: Context
    ): QReportDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = QReportDatabase::class.java,
            name = QReportDatabase.Companion.DATABASE_NAME
        )
            .addCallback(QReportDatabase.Companion.CALLBACK)
//            .fallbackToDestructiveMigration()  // Only in development
            .addMigrations(
                // Migrations go here...
                MIGRATION_3_4,
                MIGRATION_4_5
            )
            .build()
    }

    @Provides
    fun provideCheckUpDao(
        database: QReportDatabase
    ): CheckUpDao = database.checkUpDao()

    @Provides
    fun provideCheckItemDao(
        database: QReportDatabase
    ): CheckItemDao = database.checkItemDao()

    @Provides
    fun providePhotoDao(
        database: QReportDatabase
    ): PhotoDao = database.photoDao()

    @Provides
    fun provideSparePartDao(
        database: QReportDatabase
    ): SparePartDao = database.sparePartDao()

    @Provides
    fun provideClientDao(
        database: QReportDatabase
    ): ClientDao = database.clientDao()

    @Provides
    fun provideContactDao(
        database: QReportDatabase
    ): ContactDao = database.contactDao()

    @Provides
    fun provideFacilityDao(
        database: QReportDatabase
    ): FacilityDao = database.facilityDao()

    @Provides
    fun provideFacilityIslandDao(
        database: QReportDatabase
    ): IslandDao = database.facilityIslandDao()

    @Provides
    fun provideCheckUpAssociationDao(
        database: QReportDatabase
    ): CheckUpAssociationDao = database.checkUpAssociationDao()

    @Provides
    fun provideContractsDao(
        database: QReportDatabase
    ): ContractDao = database.contractDao()

    @Provides
    fun provideMechanicalUnitDao(
        database: QReportDatabase
    ): MechanicalUnitDao = database.mechanicalUnitDao()

    @Provides
    fun provideMaintenanceLogDao(
        database: QReportDatabase
    ): MaintenanceLogDao = database.MaintenanceLogDao()

    @Provides
    fun provideTechnicianInterventionDao(
        database: QReportDatabase
    ): TechnicalInterventionDao = database.technicalInterventionDao()

    @Provides
    fun provideSyncDao(
        database: QReportDatabase
    ): SyncDao = database.syncDao()
}
