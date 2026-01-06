package net.calvuz.qreport.backup.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.backup.data.repository.BackupFileRepositoryImpl
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import net.calvuz.qreport.backup.domain.repository.DatabaseExportRepository
import net.calvuz.qreport.backup.domain.repository.PhotoArchiveRepository
import net.calvuz.qreport.backup.data.repository.BackupRepositoryImpl
import net.calvuz.qreport.backup.data.repository.DatabaseExportRepositoryImpl
import net.calvuz.qreport.backup.data.repository.PhotoArchiveRepositoryImpl
import net.calvuz.qreport.backup.data.repository.SettingsBackupRepositoryImpl
import net.calvuz.qreport.backup.data.repository.ShareBackupRepositoryImpl
import net.calvuz.qreport.backup.domain.repository.BackupFileRepository
import net.calvuz.qreport.backup.domain.repository.SettingsBackupRepository
import net.calvuz.qreport.backup.domain.repository.ShareBackupRepository
import javax.inject.Singleton

/**
 * HILT MODULE PER BACKUP SYSTEM
 *
 * Configura dependency injection per tutti i componenti del sistema backup.
 * Bind interfaces alle loro implementazioni concrete.
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    /**
     *  Backup repository
     */
    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository

    /** Backup file repository */
    @Binds
    @Singleton
    abstract fun bindBackupFileRepository(
        backupFileRepositoryImpl: BackupFileRepositoryImpl
    ): BackupFileRepository


    /**
     * Database export repository
     */
    @Binds
    @Singleton
    abstract fun bindDatabaseExportRepository(
        databaseExportRepositoryImpl: DatabaseExportRepositoryImpl
    ): DatabaseExportRepository

    /**
     * Photo archive repository
     */
    @Binds
    @Singleton
    abstract fun bindPhotoArchiveRepository(
        photoArchiveRepositoryImpl: PhotoArchiveRepositoryImpl
    ): PhotoArchiveRepository

    /**
     * Settings backup repository
     */
    @Binds
    @Singleton
    abstract fun bindSettingsBackupRepository(
        settingsBackupRepositoryImpl: SettingsBackupRepositoryImpl
    ): SettingsBackupRepository

    /**
     * ShareBackupRepository
     */
    @Binds
    @Singleton
    abstract fun bindShareBackupRepository(
        shareBackupRepositoryImpl: ShareBackupRepositoryImpl
    ): ShareBackupRepository

}