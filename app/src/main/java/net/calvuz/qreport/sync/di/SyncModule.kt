package net.calvuz.qreport.sync.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.sync.data.local.dao.SyncDao
import net.calvuz.qreport.sync.data.repository.SyncRepositoryImpl
import net.calvuz.qreport.sync.domain.repository.SyncRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    /**
     * Bind SyncRepository
     */
    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository

    companion object {
        /**
         * Provide SyncDao
         */
        @Provides
        @Singleton
        fun provideSyncDao(
            database: QReportDatabase
        ): SyncDao = database.syncDao()
    }
}