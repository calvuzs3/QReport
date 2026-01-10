package net.calvuz.qreport.app.database.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import net.calvuz.qreport.app.app.data.converter.AddressConverter
import net.calvuz.qreport.app.app.data.converter.DatabaseConverters
import net.calvuz.qreport.checkup.data.local.dao.CheckItemDao
import net.calvuz.qreport.checkup.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.checkup.data.local.dao.CheckUpDao
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import net.calvuz.qreport.client.contact.data.local.dao.ContactDao
import net.calvuz.qreport.client.facility.data.local.dao.FacilityDao
import net.calvuz.qreport.client.island.data.local.dao.IslandDao
import net.calvuz.qreport.photo.data.local.dao.PhotoDao
import net.calvuz.qreport.checkup.data.local.dao.SparePartDao
import net.calvuz.qreport.checkup.data.local.entity.CheckItemEntity
import net.calvuz.qreport.checkup.data.local.entity.CheckUpEntity
import net.calvuz.qreport.checkup.data.local.entity.CheckUpIslandAssociationEntity
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.contact.data.local.entity.ContactEntity
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity
import net.calvuz.qreport.photo.data.local.entity.PhotoEntity
import net.calvuz.qreport.checkup.data.local.entity.SparePartEntity
import net.calvuz.qreport.client.contract.data.local.ContractDao
import net.calvuz.qreport.client.contract.data.local.ContractEntity

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
 * @version 3 - Database schema iniziale
 */
@Database(
    entities = [
        // checkups
        CheckUpEntity::class,
        CheckItemEntity::class,
        PhotoEntity::class,
        SparePartEntity::class,
        // clients
        ClientEntity::class,
        ContactEntity::class,
        FacilityEntity::class,
        IslandEntity::class,
        // checkup-client association
        CheckUpIslandAssociationEntity::class,
        // client-contract association
        ContractEntity::class
    ],
    version = QReportDatabase.Companion.DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        // Future migrations will be added here
        //AutoMigration(from = 1, to = 2),  // ← Room: "Ci penso io!"
        //AutoMigration(from = 2, to = 3),  // ← Room genera SQL automaticamente
    ]
)
@TypeConverters(
    DatabaseConverters::class,
    AddressConverter::class
)
abstract class QReportDatabase : RoomDatabase() {

    // DAO abstract methods
    abstract fun checkUpDao(): CheckUpDao
    abstract fun checkItemDao(): CheckItemDao
    abstract fun photoDao(): PhotoDao
    abstract fun sparePartDao(): SparePartDao
    abstract fun clientDao(): ClientDao
    abstract fun contactDao(): ContactDao
    abstract fun facilityDao(): FacilityDao
    abstract fun facilityIslandDao(): IslandDao
    abstract fun checkUpAssociationDao(): CheckUpAssociationDao
    abstract fun contractDao(): ContractDao


    companion object {
        const val DATABASE_NAME = "qreport_database"
        const val DATABASE_VERSION = 1

        /**
         * 3. Callback per inizializzazione database
         * Popola dati di base se necessario
         */
        val CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Qui si potrebbero inserire dati di default se necessari
                // Ad esempio template checklist predefiniti
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Operazioni da eseguire ad ogni apertura del database
                // Ad esempio pulizia di dati temporanei vecchi
            }
        }


        // Migrations, in DatabaseModule:
        //        val MIGRATION_2_3 = Migration(2, 3) { database ->
        //          // ← Write SQL manually
        //          database.execSQL("CREATE TABLE ...")
        //        }
        // and add:
        //        .addMigrations()
    }
}