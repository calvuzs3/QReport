package net.calvuz.qreport.app.database.di

import android.content.Context
import androidx.room.Room
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
import net.calvuz.qreport.client.contract.data.local.ContractDao
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

//    val MIGRATION_1_2 = object : Migration(1,2){
//        override fun migrate(db: SupportSQLiteDatabase) {
//            val TABLE_NAME = "contracts"
//
//            // 1. TABLE
//            db.execSQL("""
//                    CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (
//                    `id` TEXT NOT NULL,
//                    `client_id` TEXT NOT NULL,
//                    `name` TEXT,
//                    `description` TEXT,
//                    `start_date` INTEGER NOT NULL,
//                    `end_date` INTEGER NOT NULL,
//                    `has_priority` INTEGER NOT NULL,
//                     `has_remote_assistance` INTEGER NOT NULL,
//                     `has_maintenance` INTEGER NOT NULL,
//                     `created_at` INTEGER NOT NULL,
//                     `updated_at` INTEGER NOT NULL,
//                     PRIMARY KEY(`id`),
//                     FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`)
//                        ON UPDATE NO ACTION ON DELETE CASCADE )
//                """.trimIndent())
//
//            // 2. INDEXES
//            db.execSQL("CREATE INDEX IF NOT EXISTS `index_contracts_client_id` ON `${TABLE_NAME}` (`client_id`)")
//            db.execSQL("CREATE INDEX IF NOT EXISTS `index_contracts_has_maintenance` ON `${TABLE_NAME}` (`has_maintenance`)")
//            db.execSQL("CREATE INDEX IF NOT EXISTS `index_contracts_name` ON `${TABLE_NAME}` (`name`)")
//
//            Timber.d("Database migration from version 3 to 4 completed successfully")
//        }
//    }

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
            //.fallbackToDestructiveMigration()  // Only in development
            .addMigrations(
                //MIGRATION_1_2
                // Migrations go here...
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
    fun provideTechnicianInterventionDao(
        database: QReportDatabase
    ): TechnicalInterventionDao = database.technicalInterventionDao()

}
