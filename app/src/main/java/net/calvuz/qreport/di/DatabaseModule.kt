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
import net.calvuz.qreport.data.local.dao.CheckUpDao
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
            .fallbackToDestructiveMigration() // Solo per sviluppo, rimuovere in produzione
            .build()
    }

    /**
     * DAO per gestione CheckUp
     */
    @Provides
    fun provideCheckUpDao(
        database: QReportDatabase
    ): CheckUpDao = database.checkUpDao()

    /**
     * DAO per gestione CheckItem
     */
    @Provides
    fun provideCheckItemDao(
        database: QReportDatabase
    ): CheckItemDao = database.checkItemDao()

    /**
     * DAO per gestione Photo
     */
    @Provides
    fun providePhotoDao(
        database: QReportDatabase
    ): PhotoDao = database.photoDao()

    /**
     * DAO per gestione SparePart
     */
    @Provides
    fun provideSparePartDao(
        database: QReportDatabase
    ): SparePartDao = database.sparePartDao()
}