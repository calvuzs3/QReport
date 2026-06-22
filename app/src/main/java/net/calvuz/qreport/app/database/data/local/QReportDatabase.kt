package net.calvuz.qreport.app.database.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.calvuz.qreport.app.app.QReportApplication
import net.calvuz.qreport.app.app.data.converter.AddressConverter
import net.calvuz.qreport.app.app.data.converter.DatabaseConverters
import net.calvuz.qreport.app.app.data.converter.PhotoConverter
import net.calvuz.qreport.checkup.items.data.local.dao.CheckItemDao
import net.calvuz.qreport.checkup.items.data.local.dao.CheckItemTemplateDao
import net.calvuz.qreport.checkup.checkup.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.checkup.checkup.data.local.dao.CheckUpDao
import net.calvuz.qreport.checkup.modules.data.local.dao.ModuleTypeDao
import net.calvuz.qreport.checkup.criticality.data.local.dao.CriticalityDao
import net.calvuz.qreport.checkup.status.data.local.dao.CheckUpStatusDao
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import net.calvuz.qreport.client.contact.data.local.dao.ContactDao
import net.calvuz.qreport.client.facility.data.local.dao.FacilityDao
import net.calvuz.qreport.client.island.data.local.dao.IslandDao
import net.calvuz.qreport.client.island.data.local.dao.IslandTypeDao
import net.calvuz.qreport.photo.data.local.dao.PhotoDao
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemEntity
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemTemplateEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpEntity
import net.calvuz.qreport.checkup.modules.data.local.entity.ModuleTypeEntity
import net.calvuz.qreport.checkup.modules.data.local.entity.ModuleTypeIslandTypeCrossRef
import net.calvuz.qreport.checkup.criticality.data.local.entity.CriticalityEntity
import net.calvuz.qreport.checkup.status.data.local.entity.CheckUpStatusEntity
import net.calvuz.qreport.checkup.status.data.local.entity.CheckUpStatusTransitionCrossRef
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpIslandAssociationEntity
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.contact.data.local.entity.ContactEntity
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.photo.data.local.entity.PhotoEntity
import net.calvuz.qreport.client.contract.data.local.dao.ContractDao
import net.calvuz.qreport.client.contract.data.local.entity.ContractEntity
import net.calvuz.qreport.client.document.data.local.dao.DocumentDao
import net.calvuz.qreport.client.document.data.local.entity.DocumentEntity
import net.calvuz.qreport.client.island.maintenance.data.local.dao.MaintenanceLogDao
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity
import net.calvuz.qreport.client.unit.data.local.dao.MechanicalUnitDao
import net.calvuz.qreport.client.unit.data.local.entity.MechanicalUnitEntity
import net.calvuz.qreport.sync.data.local.dao.SyncDao
import net.calvuz.qreport.ti.data.local.dao.TechnicalInterventionDao
import net.calvuz.qreport.ti.data.local.entity.TechnicalInterventionEntity
import net.calvuz.qreport.checkup.checkup.data.local.dao.CheckUpMaintenanceLogAssociationDao
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpMaintenanceLogAssociationEntity
import net.calvuz.qreport.ti.data.local.dao.TiAssociationDao
import net.calvuz.qreport.ti.data.local.dao.TiMaintenanceLogAssociationDao
import net.calvuz.qreport.ti.data.local.entity.TiIslandAssociationEntity
import net.calvuz.qreport.ti.data.local.entity.TiMaintenanceLogAssociationEntity
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_2_3
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_3_4
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_4_5
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_5_6
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_6_7
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_7_8

/**
 * QReport Room Database
 *
 * Database principale dell'applicazione per storage offline.
 * Gestisce tutte le entità e relazioni per i check-up delle isole robotizzate.
 *
 * Features:
 * - Storage 100% offline
 * - Relazioni tra entità con Foreign Keys
 * - Type converters per Instant (kotlinx.datetime)
 * - Migrazioni automatiche per versioni future
 *
 * @version 1 - Database schema iniziale
 */
@Database(
    entities = [
        // checkups
        CheckUpEntity::class,
        CheckItemEntity::class,
        PhotoEntity::class,
        // clients
        ClientEntity::class,
        ContactEntity::class,
        FacilityEntity::class,
        IslandEntity::class,
        MechanicalUnitEntity::class,
        MaintenanceLogEntity::class,
        DocumentEntity::class,
        // checkup-client association
        CheckUpIslandAssociationEntity::class,
        // client-contract association
        ContractEntity::class,
        // IRF
        TechnicalInterventionEntity::class,
        // TI-Island association
        TiIslandAssociationEntity::class,
        // CheckUp-MaintenanceLog association
        CheckUpMaintenanceLogAssociationEntity::class,
        // TI-MaintenanceLog association
        TiMaintenanceLogAssociationEntity::class,
        // Island type definitions (server-authoritative, populated via sync)
        IslandTypeEntity::class,
        // Checklist master data (local-only, no server sync)
        ModuleTypeEntity::class,
        CriticalityEntity::class,
        CheckItemTemplateEntity::class,
        ModuleTypeIslandTypeCrossRef::class,
        CheckUpStatusEntity::class,
        CheckUpStatusTransitionCrossRef::class
    ],
    version = QReportApplication.DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = []
)
@TypeConverters(
    DatabaseConverters::class,
    AddressConverter::class,
    PhotoConverter::class,
)
abstract class QReportDatabase : RoomDatabase() {

    // DAO abstract methods
    abstract fun checkUpDao(): CheckUpDao
    abstract fun checkItemDao(): CheckItemDao
    abstract fun photoDao(): PhotoDao
    abstract fun clientDao(): ClientDao
    abstract fun contactDao(): ContactDao
    abstract fun facilityDao(): FacilityDao
    abstract fun facilityIslandDao(): IslandDao
    abstract fun mechanicalUnitDao(): MechanicalUnitDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun islandDocumentDao(): DocumentDao
    abstract fun checkUpAssociationDao(): CheckUpAssociationDao
    abstract fun contractDao(): ContractDao
    abstract fun technicalInterventionDao(): TechnicalInterventionDao
    abstract fun tiAssociationDao(): TiAssociationDao
    abstract fun tiMaintenanceLogAssociationDao(): TiMaintenanceLogAssociationDao
    abstract fun checkUpMaintenanceLogAssociationDao(): CheckUpMaintenanceLogAssociationDao
    abstract fun syncDao(): SyncDao
    abstract fun islandTypeDao(): IslandTypeDao
    abstract fun moduleTypeDao(): ModuleTypeDao
    abstract fun criticalityDao(): CriticalityDao
    abstract fun checkItemTemplateDao(): CheckItemTemplateDao
    abstract fun checkUpStatusDao(): CheckUpStatusDao

    companion object {

        val MIGRATION_2_3 = net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_2_3
        val MIGRATION_3_4 = net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_3_4
        val MIGRATION_4_5 = net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_4_5
        val MIGRATION_5_6 = net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_5_6
        val MIGRATION_6_7 = net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_6_7
        val MIGRATION_7_8 = net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_7_8

        // On a fresh install Room creates all tables directly at DATABASE_VERSION and
        // calls onCreate — no Migration runs, so that's where base master data must be seeded.
        val CALLBACK = object : Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                net.calvuz.qreport.app.database.data.local.migrations.BaseMasterDataSeeder.seedAll(db)
            }
        }
    }
}