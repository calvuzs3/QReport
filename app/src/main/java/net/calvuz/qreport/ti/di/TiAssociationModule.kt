package net.calvuz.qreport.ti.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.ti.data.local.dao.TiAssociationDao
import net.calvuz.qreport.ti.data.local.dao.TiMaintenanceLogAssociationDao
import net.calvuz.qreport.ti.data.local.repository.TiAssociationRepositoryImpl
import net.calvuz.qreport.ti.data.local.repository.TiMaintenanceLogAssociationRepositoryImpl
import net.calvuz.qreport.ti.domain.repository.TiAssociationRepository
import net.calvuz.qreport.ti.domain.repository.TiMaintenanceLogAssociationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TiAssociationModule {

    @Binds
    @Singleton
    abstract fun bindTiAssociationRepository(
        impl: TiAssociationRepositoryImpl
    ): TiAssociationRepository

    @Binds
    @Singleton
    abstract fun bindTiMaintenanceLogAssociationRepository(
        impl: TiMaintenanceLogAssociationRepositoryImpl
    ): TiMaintenanceLogAssociationRepository

    companion object {
        @Provides
        @Singleton
        fun provideTiAssociationDao(database: QReportDatabase): TiAssociationDao =
            database.tiAssociationDao()

        @Provides
        @Singleton
        fun provideTiMaintenanceLogAssociationDao(database: QReportDatabase): TiMaintenanceLogAssociationDao =
            database.tiMaintenanceLogAssociationDao()
    }
}
