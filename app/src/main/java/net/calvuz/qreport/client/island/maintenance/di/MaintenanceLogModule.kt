package net.calvuz.qreport.client.island.maintenance.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.island.maintenance.data.local.dao.MaintenanceLogDao
import net.calvuz.qreport.client.island.maintenance.data.local.repository.MaintenanceLogRepositoryImpl
import net.calvuz.qreport.client.island.maintenance.domain.repository.MaintenanceLogRepository
import javax.inject.Singleton
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity

/**
 * Hilt module for the MaintenanceLog feature.
 *
 * Note: [MaintenanceLogDao] is provided by the existing [QReportDatabase] Hilt module —
 * no additional @Provides needed here.
 *
 * Remember to register [MaintenanceLogEntity] and [MaintenanceLogDao] in
 * [QReportDatabase] before running the app:
 *   - Add MaintenanceLogEntity to the @Database entities list
 *   - Add abstract fun maintenanceLogDao(): MaintenanceLogDao
 *   - Increment the Room database version and provide a Migration
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MaintenanceLogModule {

    @Binds
    @Singleton
    abstract fun bindMaintenanceLogRepository(
        impl: MaintenanceLogRepositoryImpl
    ): MaintenanceLogRepository

    companion object {
        @Provides
        @Singleton
        fun provideMaintenanceLogDao(
            database: QReportDatabase
        ): MaintenanceLogDao = database.maintenanceLogDao()
    }
}