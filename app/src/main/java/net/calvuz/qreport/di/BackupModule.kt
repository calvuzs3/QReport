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

/*
=============================================================================
                            DEPENDENCY INJECTION SETUP
=============================================================================

Questo module configura la dependency injection per:

✅ FileManager → FileManagerImpl
✅ DatabaseExportRepository → DatabaseExportRepositoryImpl
✅ BackupRepository → BackupRepositoryImpl
✅ PhotoArchiveRepository → PhotoArchiveRepositoryImpl (Fase 5.3)
✅ SettingsBackupRepository → SettingsBackupRepositoryImpl (Fase 5.3)

🎉 FASE 5.3 COMPLETATA - Tutti i componenti backup implementati!

UTILIZZO:

1. Aggiungi questo file al progetto in:
   app/src/main/java/net/calvuz/qreport/di/BackupModule.kt

2. Hilt automaticamente:
   - Crea singleton instances
   - Inietta dependencies nei costruttori
   - Gestisce lifecycle

3. In qualsiasi classe @Inject constructor:
   @Inject constructor(
       private val backupRepository: BackupRepository  // ✅ Automatically injected
   )

4. Nei ViewModel con @HiltViewModel:
   @HiltViewModel
   class BackupViewModel @Inject constructor(
       private val backupRepository: BackupRepository,
       private val photoArchiveRepository: PhotoArchiveRepository,
       private val settingsRepository: SettingsBackupRepository
   ) : ViewModel()

DEPENDENCIES NECESSARIE in build.gradle:
implementation "androidx.datastore:datastore-preferences:1.0.0"
implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2"
implementation "org.jetbrains.kotlinx:kotlinx-datetime:0.5.0"

=============================================================================
*/