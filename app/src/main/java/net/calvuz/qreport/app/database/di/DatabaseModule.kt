package net.calvuz.qreport.app.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.app.QReportApplication
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.checkup.data.local.dao.CheckItemDao
import net.calvuz.qreport.checkup.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.checkup.data.local.dao.CheckUpDao
import net.calvuz.qreport.photo.data.local.dao.PhotoDao
import net.calvuz.qreport.checkup.data.local.dao.SparePartDao
import net.calvuz.qreport.ti.data.local.dao.TechnicalInterventionDao
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_1_2
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_2_3
import net.calvuz.qreport.app.database.data.local.migrations.MIGRATION_3_4
import net.calvuz.qreport.client.island.data.local.dao.IslandTypeDao
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

    /* Singleton instance of the rRoom Database */
    @Provides
    @Singleton
    fun provideQReportDatabase(
        @ApplicationContext context: Context
    ): QReportDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = QReportDatabase::class.java,
            name = QReportApplication.DATABASE_NAME
        )
            .addCallback(QReportDatabase.CALLBACK)
            //.fallbackToDestructiveMigration()  // Only in development
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
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
    fun provideCheckUpAssociationDao(
        database: QReportDatabase
    ): CheckUpAssociationDao = database.checkUpAssociationDao()

    @Provides
    fun provideTechnicianInterventionDao(
        database: QReportDatabase
    ): TechnicalInterventionDao = database.technicalInterventionDao()

    @Provides
    fun provideIslandTypeDao(
        database: QReportDatabase
    ): IslandTypeDao = database.islandTypeDao()

}
