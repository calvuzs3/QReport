package net.calvuz.qreport.checkup.checkup.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.checkup.checkup.data.local.dao.CheckUpMaintenanceLogAssociationDao
import net.calvuz.qreport.checkup.checkup.data.local.repository.CheckUpMaintenanceLogAssociationRepositoryImpl
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpMaintenanceLogAssociationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckUpAssociationExtModule {

    @Binds
    @Singleton
    abstract fun bindCheckUpMaintenanceLogAssociationRepository(
        impl: CheckUpMaintenanceLogAssociationRepositoryImpl
    ): CheckUpMaintenanceLogAssociationRepository

    companion object {
        @Provides
        @Singleton
        fun provideCheckUpMaintenanceLogAssociationDao(database: QReportDatabase): CheckUpMaintenanceLogAssociationDao =
            database.checkUpMaintenanceLogAssociationDao()
    }
}
