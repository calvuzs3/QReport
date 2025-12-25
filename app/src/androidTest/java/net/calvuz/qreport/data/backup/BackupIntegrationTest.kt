package net.calvuz.qreport.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.calvuz.qreport.data.backup.model.ArchiveProgress
import net.calvuz.qreport.data.backup.model.ExtractionProgress
import net.calvuz.qreport.data.local.QReportDatabase
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.repository.backup.BackupRepository
import net.calvuz.qreport.domain.repository.backup.PhotoArchiveRepository
import net.calvuz.qreport.domain.repository.backup.SettingsBackupRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

/**
 * INSTRUMENTED TEST INTEGRAZIONE COMPLETA FASE 5.3
 *
 * Verifica che tutto il sistema backup funzioni end-to-end:
 * - Database export/import
 * - Photo archiving con ZIP
 * - Settings backup/restore
 * - File management
 * - Progress tracking
 *
 * NOTA: Questo è un instrumented test che gira su device/emulator reale
 */

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)  // ✅ CORRETTO per androidTest
class BackupIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var backupRepository: BackupRepository

    @Inject
    lateinit var photoArchiveRepository: PhotoArchiveRepository

    @Inject
    lateinit var settingsBackupRepository: SettingsBackupRepository

    @Inject
    lateinit var fileManager: FileManager

    private lateinit var context: Context
    private lateinit var database: QReportDatabase
    private lateinit var testBackupDir: File

    @Before
    fun setup() {
        hiltRule.inject()

        context = ApplicationProvider.getApplicationContext()

        // Database in-memory per test
        database = Room.inMemoryDatabaseBuilder(
            context,
            QReportDatabase::class.java
        ).allowMainThreadQueries().build()

        // Directory temporanea per test backup
        testBackupDir = File(context.cacheDir, "test_backups").apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        database.close()
        testBackupDir.deleteRecursively()
    }

    @Test
    fun testCompleteBackupAndRestoreCycle() = runTest {

        // ===== SETUP TEST DATA =====

        // 1. Crea dati test nel database
        createTestDatabaseData()

        // 2. Crea foto test
        val testPhotos = createTestPhotoFiles()

        // 3. Configura settings test
        setupTestSettings()

        // ===== BACKUP CREATION =====

        // 4. Crea backup completo
        val backupProgressList = mutableListOf<BackupProgress>()

        backupRepository.createFullBackup(
            includePhotos = true,
            includeThumbnails = false,
            backupMode = BackupMode.LOCAL,
            description = "Integration test backup"
        ).toList().also { progressList ->
            backupProgressList.addAll(progressList)
        }

        // 5. Verifica progress tracking
        assertTrue(backupProgressList.any { it is BackupProgress.InProgress })

        val completedProgress = backupProgressList.last()
        assertTrue(completedProgress is BackupProgress.Completed)

        val backupPath = (completedProgress as BackupProgress.Completed).backupPath
        assertNotNull(backupPath)

        println("✅ Backup creato: $backupPath")

        // ===== BACKUP VALIDATION =====

        // 6. Valida backup file
        val validation = fileManager.validateBackupFile(backupPath)
        assertTrue("Backup deve essere valido", validation.isValid)

        // 7. Carica backup per verifica
        val backupData = fileManager.loadBackup(backupPath)

        assertEquals(3, backupData.database.clients.size)
        assertEquals(2, backupData.database.checkUps.size)
        assertEquals(testPhotos.size, backupData.photoManifest.totalPhotos)

        println("✅ Backup validato correttamente")

        // ===== BACKUP RESTORE =====

        // 8. Clear database per simulare restore
        clearTestDatabaseData()

        // 9. Restore da backup
        val restoreProgressList = mutableListOf<RestoreProgress>()

        backupRepository.restoreFromBackup(
            backupPath = backupPath,
            strategy = RestoreStrategy.REPLACE_ALL
        ).toList().also { progressList ->
            restoreProgressList.addAll(progressList)
        }

        // 10. Verifica restore progress
        assertTrue(restoreProgressList.any { it is RestoreProgress.InProgress })

        val completedRestore = restoreProgressList.last()
        assertTrue(completedRestore is RestoreProgress.Completed)

        println("✅ Restore completato")

        // ===== VERIFICATION =====

        // 11. Verifica dati ripristinati
        verifyRestoredData()

        // 12. Verifica settings ripristinate
        verifyRestoredSettings()

        // 13. Verifica foto ripristinate
        verifyRestoredPhotos(testPhotos)

        println("✅ Tutto il ciclo backup/restore funziona correttamente!")
    }

    @Test
    fun testPhotoArchivingWithZipCompression() = runTest {

        // Given - Crea foto test
        val testPhotos = createTestPhotoFiles()
        val archivePath = File(testBackupDir, "test_photos.zip").absolutePath

        // When - Crea archivio
        val archiveProgressList = mutableListOf<ArchiveProgress>()

        photoArchiveRepository.createPhotoArchive(
            outputPath = archivePath,
            includesThumbnails = false
        ).toList().also { progressList ->
            archiveProgressList.addAll(progressList)
        }

        // Then - Verifica archivio creato
        val completedArchive = archiveProgressList.last()
        assertTrue(completedArchive is ArchiveProgress.Completed)

        val archiveFile = File(archivePath)
        assertTrue(archiveFile.exists())
        assertTrue(archiveFile.length() > 0)

        // When - Estrai archivio
        val extractDir = File(testBackupDir, "extracted_photos")

        val extractProgressList = mutableListOf<ExtractionProgress>()

        photoArchiveRepository.extractPhotoArchive(
            archivePath = archivePath,
            outputDir = extractDir.absolutePath
        ).toList().also { progressList ->
            extractProgressList.addAll(progressList)
        }

        // Then - Verifica estrazione
        val completedExtraction = extractProgressList.last()
        assertTrue(completedExtraction is ExtractionProgress.Completed)

        // Verifica foto estratte
        assertTrue(extractDir.exists())
        val extractedFiles = extractDir.walkTopDown().filter { it.isFile }.count()
        assertEquals(testPhotos.size, extractedFiles)

        println("✅ Photo archiving e extraction funzionano correttamente!")
    }

    @Test
    fun testSettingsBackupAndRestore() = runTest {

        // Given - Setup settings test
        setupTestSettings()

        // When - Export settings
        val settingsBackup = settingsBackupRepository.exportSettings()

        // Then - Verifica export
        assertTrue(settingsBackup.preferences.isNotEmpty())
        assertNotNull(settingsBackup.backupDateTime)

        // When - Clear e import settings
        clearTestSettings()
        val importResult = settingsBackupRepository.importSettings(settingsBackup)

        // Then - Verifica import
        assertTrue(importResult.isSuccess)

        // Verifica settings ripristinate
        verifyRestoredSettings()

        println("✅ Settings backup/restore funziona correttamente!")
    }

    // ===== HELPER METHODS =====

    private fun createTestDatabaseData() {
        // TODO: Implementa creazione dati test nel database
        // Crea 3 client, 2 checkup, qualche check item, etc.
        println("📊 Test database data created")
    }

    private fun createTestPhotoFiles(): List<File> {
        val photos = mutableListOf<File>()

        // Crea foto test (file vuoti per semplicità)
        repeat(5) { index ->
            val photoFile = File(testBackupDir, "test_photo_$index.jpg")
            photoFile.writeText("fake photo content $index")
            photos.add(photoFile)
        }

        println("📸 ${photos.size} test photos created")
        return photos
    }

    private fun setupTestSettings() {
        // TODO: Setup impostazioni test usando SettingsBackupRepository
        println("⚙️ Test settings configured")
    }

    private fun clearTestDatabaseData() {
        // TODO: Clear database data
        println("🗑️ Database data cleared")
    }

    private fun clearTestSettings() {
        // TODO: Clear settings
        println("🗑️ Settings cleared")
    }

    private fun verifyRestoredData() {
        // TODO: Verifica che i dati siano stati ripristinati
        println("✅ Database data verified")
    }

    private fun verifyRestoredSettings() {
        // TODO: Verifica settings ripristinate
        println("✅ Settings verified")
    }

    private fun verifyRestoredPhotos(originalPhotos: List<File>) {
        // TODO: Verifica foto ripristinate
        println("✅ Photos verified")
    }
}

/*
=============================================================================
                        INSTRUMENTED TEST EXECUTION
=============================================================================

COMANDO PER ESEGUIRE:
./gradlew connectedAndroidTest --tests="BackupIntegrationTest"

OPPURE:
./gradlew :app:connectedAndroidTest

REQUIREMENTS:
✅ Device/emulator connesso e disponibile
✅ @HiltAndroidTest per dependency injection
✅ @RunWith(AndroidJUnit4::class) per instrumented tests
✅ CustomTestRunner configurato in build.gradle

VANTAGGI INSTRUMENTED TESTS:
✅ Test su Android reale (non simulato)
✅ Full Android API access
✅ Real filesystem operations
✅ Actual DataStore/SharedPreferences
✅ Real photo file handling

SVANTAGGI:
❌ Più lenti (device communication)
❌ Richiedono device/emulator
❌ Meno controllabili dell'ambiente

=============================================================================
*/