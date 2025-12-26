package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.local.file.FileManagerImpl
import net.calvuz.qreport.data.repository.backup.BackupRepositoryImpl
import net.calvuz.qreport.data.repository.backup.DatabaseExportRepositoryImpl
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.repository.backup.BackupRepository
import net.calvuz.qreport.domain.repository.backup.DatabaseExportRepository
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
     * Bind FileManager interface alla sua implementazione
     */
    @Binds
    @Singleton
    abstract fun bindBackupFileManager(
        fileManagerImpl: FileManagerImpl
    ): FileManager

    /**
     * Bind DatabaseExportRepository interface alla sua implementazione
     */
    @Binds
    @Singleton
    abstract fun bindDatabaseExportRepository(
        databaseExportRepositoryImpl: DatabaseExportRepositoryImpl
    ): DatabaseExportRepository

    /**
     * Bind BackupRepository interface alla sua implementazione principale
     */
    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository
}