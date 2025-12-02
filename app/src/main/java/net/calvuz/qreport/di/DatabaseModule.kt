package net.calvuz.qreport.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.local.QReportDatabase
import net.calvuz.qreport.data.local.QReportDatabase.Companion.MIGRATION_CHECKUP_ISLAND_ASSOCIATION_2_3
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

//@Module
//@InstallIn(SingletonComponent::class)
//object DatabaseModule {
//
//    @Provides
//    @Singleton
//    fun provideQReportDatabase(
//        @ApplicationContext context: Context
//    ): QReportDatabase {
//        return Room.databaseBuilder(
//            context.applicationContext,
//            QReportDatabase::class.java,
//            QReportDatabase.DATABASE_NAME
//        )
//            .fallbackToDestructiveMigration() // Per la versione iniziale
//            .build()
//    }
//
//    @Provides
//    fun provideCheckUpDao(database: QReportDatabase): CheckUpDao {
//        return database.checkUpDao()
//    }
//
//    @Provides
//    fun provideCheckItemDao(database: QReportDatabase): CheckItemDao {
//        return database.checkItemDao()
//    }
//
//    @Provides
//    fun providePhotoDao(database: QReportDatabase): PhotoDao {
//        return database.photoDao()
//    }
//
//    @Provides
//    fun provideSparePartDao(database: QReportDatabase): SparePartDao {
//        return database.sparePartDao()
//    }
//
//    @Provides
//    fun provideCheckItemTemplateDao(database: QReportDatabase): CheckItemTemplateDao {
//        return database.checkItemTemplateDao()
//    }
//}
/**
 * Database Dependency Injection Module
 *
 * Modulo Hilt per fornire istanze singleton del database Room
 * e dei relativi DAO per l'intera applicazione.
 *
 * Setup:
 * - Database Room con configurazione ottimizzata
 * - DAO injection per tutti i layer
 * - Gestione transazioni e error handling
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Fornisce l'istanza singleton del database Room
     */
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
//            .fallbackToDestructiveMigration() // Solo per sviluppo, rimuovere in produzione
            .addMigrations(
                // Altre migration esistenti...
//                MIGRATION_CHECKUP_ISLAND_ASSOCIATION_2_3  // ← AGGIUNGI QUESTA
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