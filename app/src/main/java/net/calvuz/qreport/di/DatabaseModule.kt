package net.calvuz.qreport.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.local.QReportDatabase
import net.calvuz.qreport.data.local.dao.CheckItemDao
import net.calvuz.qreport.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.data.local.dao.CheckUpDao
import net.calvuz.qreport.data.local.dao.ClientDao
import net.calvuz.qreport.data.local.dao.ContactDao
import net.calvuz.qreport.data.local.dao.FacilityDao
import net.calvuz.qreport.data.local.dao.FacilityIslandDao
import net.calvuz.qreport.data.local.dao.PhotoDao
import net.calvuz.qreport.data.local.dao.SparePartDao
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
            name = QReportDatabase.Companion.DATABASE_NAME
        )
            .addCallback(QReportDatabase.Companion.CALLBACK)
            //.fallbackToDestructiveMigration()  // Only in development
            .addMigrations(
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
    ): FacilityIslandDao = database.facilityIslandDao()

    @Provides
    fun provideCheckUpAssociationDao(
        database: QReportDatabase
    ): CheckUpAssociationDao = database.checkUpAssociationDao()

}