package net.calvuz.qreport.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.settings.data.repository.SettingsRepositoryImpl
import net.calvuz.qreport.settings.data.repository.TechnicianSettingsRepositoryImpl
import net.calvuz.qreport.settings.domain.repository.SettingsRepository
import net.calvuz.qreport.settings.domain.repository.TechnicianSettingsRepository
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

}