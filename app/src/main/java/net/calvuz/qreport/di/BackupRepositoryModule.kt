package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.repository.backup.PhotoArchiveRepositoryImpl
import net.calvuz.qreport.data.repository.backup.SettingsBackupRepositoryImpl
import net.calvuz.qreport.domain.repository.backup.PhotoArchiveRepository
import net.calvuz.qreport.domain.repository.backup.SettingsBackupRepository
import javax.inject.Singleton

/**
 * Module aggiuntivo per le repository implementate in Fase 5.3
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupRepositoryModule {

    /**
     * PhotoArchiveRepository - Fase 5.3 ✅
     */
    @Binds
    @Singleton
    abstract fun bindPhotoArchiveRepository(
        photoArchiveRepositoryImpl: PhotoArchiveRepositoryImpl
    ): PhotoArchiveRepository

    /**
     * SettingsBackupRepository - Fase 5.3 ✅
     */
    @Binds
    @Singleton
    abstract fun bindSettingsBackupRepository(
        settingsBackupRepositoryImpl: SettingsBackupRepositoryImpl
    ): SettingsBackupRepository
}