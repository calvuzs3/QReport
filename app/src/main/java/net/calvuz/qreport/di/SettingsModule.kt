package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.repository.backup.SettingsBackupRepositoryImpl
import net.calvuz.qreport.data.repository.settings.SettingsRepositoryImpl
import net.calvuz.qreport.data.repository.settings.TechnicianSettingsRepositoryImpl
import net.calvuz.qreport.domain.repository.backup.SettingsBackupRepository
import net.calvuz.qreport.domain.repository.settings.SettingsRepository
import net.calvuz.qreport.domain.repository.settings.TechnicianSettingsRepository
import javax.inject.Singleton

/**
 * Modulo Hilt per Phase 6: Technician Settings & Enhanced Backup Integration
 *
 * Provides:
 * - TechnicianSettingsRepository with backup support
 * - Extended SettingsRepository with technician data integration
 * - All required dependencies for pre-population and settings management
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    /**
     * Bind TechnicianSettingsRepository
     */
    @Binds
    @Singleton
    abstract fun bindTechnicianSettingsRepository(
        technicianSettingsRepositoryImpl: TechnicianSettingsRepositoryImpl
    ): TechnicianSettingsRepository

    /**
     * Bind Extended SettingsRepository with technician integration
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    /**
     * Bind Extended SettingsRepository with technician integration
     */
    @Binds
    @Singleton
    abstract fun bindSettingsBackupRepository(
        settingsBackupRepositoryImpl: SettingsBackupRepositoryImpl
    ): SettingsBackupRepository
}