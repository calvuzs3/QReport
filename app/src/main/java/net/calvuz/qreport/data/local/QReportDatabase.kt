package net.calvuz.qreport.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.calvuz.qreport.data.local.QReportDatabase.Companion.DATABASE_VERSION
import net.calvuz.qreport.data.local.converters.AddressConverter
import net.calvuz.qreport.data.local.converters.DatabaseConverters
import net.calvuz.qreport.data.local.dao.CheckItemDao
import net.calvuz.qreport.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.data.local.dao.CheckUpDao
import net.calvuz.qreport.data.local.dao.ClientDao
import net.calvuz.qreport.data.local.dao.ContactDao
import net.calvuz.qreport.data.local.dao.FacilityDao
import net.calvuz.qreport.data.local.dao.FacilityIslandDao
import net.calvuz.qreport.data.local.dao.PhotoDao
import net.calvuz.qreport.data.local.dao.SparePartDao
import net.calvuz.qreport.data.local.entity.CheckItemEntity
import net.calvuz.qreport.data.local.entity.CheckUpEntity
import net.calvuz.qreport.data.local.entity.CheckUpIslandAssociationEntity
import net.calvuz.qreport.data.local.entity.ClientEntity
import net.calvuz.qreport.data.local.entity.ContactEntity
import net.calvuz.qreport.data.local.entity.FacilityEntity
import net.calvuz.qreport.data.local.entity.FacilityIslandEntity
import net.calvuz.qreport.data.local.entity.PhotoEntity
import net.calvuz.qreport.data.local.entity.SparePartEntity

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
        CheckUpEntity::class,
        CheckItemEntity::class,
        PhotoEntity::class,
        SparePartEntity::class,
        // new
        ClientEntity::class,
        ContactEntity::class,
        FacilityEntity::class,
        FacilityIslandEntity::class,
        // new
        CheckUpIslandAssociationEntity::class
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        // Future migrations will be added here
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
    abstract fun facilityIslandDao(): FacilityIslandDao
    abstract fun checkUpAssociationDao(): CheckUpAssociationDao


    companion object {
        const val DATABASE_NAME = "qreport_database"
        const val DATABASE_VERSION =3

        /**
         * Callback per inizializzazione database
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


        val MIGRATION_CHECKUP_ISLAND_ASSOCIATION_2_3 = Migration(2, 3) { database ->

            // Crea tabella associazioni CheckUp-Isole
            database.execSQL("""
        CREATE TABLE IF NOT EXISTS checkup_island_associations (
            id TEXT PRIMARY KEY NOT NULL,
            checkup_id TEXT NOT NULL,
            island_id TEXT NOT NULL,
            association_type TEXT NOT NULL DEFAULT 'STANDARD',
            notes TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            FOREIGN KEY (checkup_id) REFERENCES checkups(id) ON DELETE CASCADE,
            FOREIGN KEY (island_id) REFERENCES facility_islands(id) ON DELETE CASCADE,
            UNIQUE(checkup_id, island_id)
        )
    """)

            // Crea indici per performance
            database.execSQL("""
        CREATE INDEX IF NOT EXISTS index_checkup_associations_checkup_id 
        ON checkup_island_associations(checkup_id)
    """)

            database.execSQL("""
        CREATE INDEX IF NOT EXISTS index_checkup_associations_island_id 
        ON checkup_island_associations(island_id)
    """)

            database.execSQL("""
        CREATE INDEX IF NOT EXISTS index_checkup_associations_type 
        ON checkup_island_associations(association_type)
    """)
        }
    }
}