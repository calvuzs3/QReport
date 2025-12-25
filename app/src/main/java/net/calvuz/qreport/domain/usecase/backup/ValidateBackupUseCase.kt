package net.calvuz.qreport.domain.usecase.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.util.SizeUtils
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/**
 * FASE 5.4 - VALIDATE BACKUP USE CASE
 *
 * Valida integrità e validità di file backup prima di operazioni critiche.
 * Utilizzato prima di restore per prevenire corruzioni.
 */
class ValidateBackupUseCase @Inject constructor(
) {

    /**
     * Valida backup completo con tutte le verifiche
     *
     * @param backupFilePath Path del file backup da validare
     * @return BackupValidationResult con dettagli validazione
     */
    suspend operator fun invoke(backupFilePath: String): BackupValidationResult =
        withContext(Dispatchers.IO) {

            try {
                Timber.d("Validating backup: $backupFilePath")

                val validationErrors = mutableListOf<String>()
                val validationWarnings = mutableListOf<String>()

                // 1. Validazione esistenza file
                val backupFile = File(backupFilePath)
                if (!backupFile.exists()) {
                    validationErrors.add("File backup non esistente: $backupFilePath")
                    return@withContext BackupValidationResult.invalid(validationErrors)
                }

                if (!backupFile.canRead()) {
                    validationErrors.add("File backup non leggibile")
                    return@withContext BackupValidationResult.invalid(validationErrors)
                }

                if (backupFile.length() == 0L) {
                    validationErrors.add("File backup vuoto")
                    return@withContext BackupValidationResult.invalid(validationErrors)
                }

                // 2. Validazione formato JSON
                val jsonContent = try {
                    backupFile.readText()
                } catch (e: Exception) {
                    validationErrors.add("Impossibile leggere contenuto file: ${e.message}")
                    return@withContext BackupValidationResult.invalid(validationErrors)
                }

                if (jsonContent.isBlank()) {
                    validationErrors.add("Contenuto JSON vuoto")
                    return@withContext BackupValidationResult.invalid(validationErrors)
                }

                // 3. Parsing JSON e validazione struttura
                val backupData = try {
                    Json.decodeFromString<BackupData>(jsonContent)
                } catch (e: Exception) {
                    validationErrors.add("Formato JSON backup non valido: ${e.message}")
                    return@withContext BackupValidationResult.invalid(validationErrors)
                }

                // 4. Validazione metadata
                validateMetadata(backupData.metadata, validationErrors, validationWarnings)

                // 5. Validazione database
                validateDatabase(backupData.database, validationErrors, validationWarnings)

                // 6. Validazione photo manifest
                if (backupData.includesPhotos()) {
                    validatePhotoManifest(
                        backupData.photoManifest,
                        validationErrors,
                        validationWarnings
                    )
                }

                // 7. Validazione checksum se presente
                if (backupData.metadata.checksum.isNotEmpty()) {
                    // TODO apply a correct checksum check
                    //validateChecksum(backupData, validationErrors, validationWarnings)
                    true
                }

                // 8. Validazioni dimensione e performance
                validatePerformanceAspects(backupFile, backupData, validationWarnings)

                // RESULT with errors
                if (validationErrors.isNotEmpty()) {
                    Timber.d(
                        "Backup validation completed: INVALID" +
                                "(${validationErrors.size} errors, ${validationWarnings.size} warnings)"
                    )
                    validationErrors.forEach { Timber.d(" - $it") }

                    BackupValidationResult.invalid(validationErrors)
                } else {

                    // RESULT without errors
                    Timber.d(
                        "Backup validation completed: VALID" +
                                "(${validationWarnings.size} warnings)"
                    )

                    BackupValidationResult.valid(
                        warnings = validationWarnings
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during backup validation")
                BackupValidationResult.invalid(listOf("Validazione fallita: ${e.message}"))
            }
        }

    /**
     * Validazione rapida per controlli base
     */
    suspend fun quickValidate(backupFilePath: String): Boolean = withContext(Dispatchers.IO) {

        try {
            val backupFile = File(backupFilePath)

            // Controlli base rapidi
            if (!backupFile.exists() || !backupFile.canRead() || backupFile.length() == 0L) {
                return@withContext false
            }

            // Controllo che sia JSON valido
            val jsonContent = backupFile.readText()
            Json.decodeFromString<BackupData>(jsonContent)

            true

        } catch (e: Exception) {
            Timber.w(e, "Quick validation failed for: $backupFilePath")
            false
        }
    }

    /**
     * Validazione batch per lista backup
     */
    suspend fun validateMultiple(backupFilePaths: List<String>): Map<String, BackupValidationResult> =
        withContext(Dispatchers.IO) {

            val results = mutableMapOf<String, BackupValidationResult>()

            for (filePath in backupFilePaths) {
                try {
                    results[filePath] = invoke(filePath)
                } catch (e: Exception) {
                    results[filePath] =
                        BackupValidationResult.invalid(listOf("Validazione fallita: ${e.message}"))
                }
            }

            Timber.d("Validated ${backupFilePaths.size} backups: ${results.values.count { it.isValid }} valid")
            results
        }

    // ===== VALIDATION METHODS =====

    /**
     * Valida metadata backup
     */
    private fun validateMetadata(
        metadata: BackupMetadata,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // ID valido
        if (metadata.id.isBlank()) {
            errors.add("Backup ID vuoto")
        }

        // App version
        if (metadata.appVersion.isBlank()) {
            warnings.add("Versione app non specificata")
        }

        // Database version
        if (metadata.databaseVersion <= 0) {
            warnings.add("Versione database non valida: ${metadata.databaseVersion}")
        }

        // Total size ragionevole
        if (metadata.totalSize <= 0) {
            warnings.add("Dimensione totale non valida: ${metadata.totalSize}")
        } else if (metadata.totalSize > 2L * 1024 * 1024 * 1024) {  // > 2GB
            warnings.add("Backup molto grande: ${metadata.totalSize / (1024 * 1024 * 1024)}GB")
        }

        // Device info
        if (metadata.deviceInfo.model.isBlank()) {
            warnings.add("Informazioni device incomplete")
        }
    }

    /**
     * Valida database backup
     */
    private fun validateDatabase(
        database: DatabaseBackup,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        var totalRecords = 0

        // Conta record in tutte le tabelle
        totalRecords += database.checkUps.size
        totalRecords += database.checkItems.size
        totalRecords += database.photos.size
        totalRecords += database.spareParts.size
        totalRecords += database.clients.size
        totalRecords += database.contacts.size
        totalRecords += database.facilities.size
        totalRecords += database.facilityIslands.size
        totalRecords += database.checkUpAssociations.size

        if (totalRecords == 0) {
            warnings.add("Database backup vuoto (0 record)")
        }

        // Validazione relazioni FK (basic)
        val clientIds = database.clients.map { it.id }.toSet()
        val facilityIds = database.facilities.map { it.id }.toSet()
        val checkUpIds = database.checkUps.map { it.id }.toSet()
        val facilityIslandIds = database.facilityIslands.map { it.id }.toSet()

        // Verifica che facilities abbiano client validi
        database.facilities.forEach { facility ->
            if (facility.clientId !in clientIds) {
                errors.add("Facility ${facility.id} ha client ID non valido: ${facility.clientId}")
            }
        }

        // Verifica che facility islands abbiano facility valide
        database.facilityIslands.forEach { island ->
            if (island.facilityId !in facilityIds) {
                errors.add("FacilityIsland ${island.id} ha facility ID non valido: ${island.facilityId}")
            }
        }

        // Verifica checkup associations
        database.checkUpAssociations.forEach { association ->
            if (association.checkupId !in checkUpIds) {
                errors.add("CheckUp association ha checkup ID non valido: ${association.checkupId}")
            }
            if (association.islandId !in facilityIslandIds) {
                errors.add("CheckUp association ha island ID non valido: ${association.islandId}")
            }
        }
    }

    /**
     * Valida photo manifest
     */
    private fun validatePhotoManifest(
        manifest: PhotoManifest,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (manifest.totalPhotos < 0) {
            errors.add("Numero foto non valido: ${manifest.totalPhotos}")
        }

        if (manifest.totalSize < 0) {
            errors.add("Dimensione foto non valida: ${manifest.totalSize}")
        }

        if (manifest.photos.size != manifest.totalPhotos) {
            warnings.add("Mismatch count foto: dichiarate ${manifest.totalPhotos}, trovate ${manifest.photos.size}")
        }

        // Valida hash foto se presenti
        manifest.photos.forEach { photoInfo ->
            if (photoInfo.sha256Hash.length != 64) {  // SHA256 = 64 hex chars
                warnings.add("Hash SHA256 non valido per foto: ${photoInfo.fileName}")
            }
        }
    }

    /**
     * Valida checksum backup
     */
    private fun validateChecksum(
        backupData: BackupData,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        try {
            // Ricalcola checksum e confronta
            val calculatedChecksum = calculateBackupChecksum(backupData)

            if (calculatedChecksum != backupData.metadata.checksum) {
                errors.add("Checksum backup non valido - possibile corruzione")
            }

        } catch (e: Exception) {
            warnings.add("Impossibile validare checksum: ${e.message}")
        }
    }

    /**
     * Validazioni performance e dimensioni
     */
    private fun validatePerformanceAspects(
        backupFile: File,
        backupData: BackupData,
        warnings: MutableList<String>
    ) {
        val fileSizeMB = backupFile.length() / (1024.0 * 1024.0)

        // File molto grandi
        if (fileSizeMB > 500) {
            warnings.add("File backup molto grande (${SizeUtils.getFormattedSize(fileSizeMB.toDouble())}) - potrebbero esserci problemi di performance")
        }

        // Molti record
        val totalRecords = backupData.database.getTotalRecordCount()
        if (totalRecords > 50000) {
            warnings.add("Backup con molti record ($totalRecords) - restore potrebbe essere lento")
        }

        // Molte foto
        if (backupData.photoManifest.totalPhotos > 1000) {
            warnings.add("Backup con molte foto (${backupData.photoManifest.totalPhotos}) - restore potrebbe essere lento")
        }
    }

    /**
     * Calcola checksum SHA256 del backup
     */
    private fun calculateBackupChecksum(backupData: BackupData): String {
        // Crea copia senza checksum per calcolo
        val dataForChecksum = backupData.copy(
            metadata = backupData.metadata.copy(checksum = "")
        )

        val jsonString = Json.encodeToString(BackupData.serializer(), dataForChecksum)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(jsonString.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/*
=============================================================================
                            BACKUP VALIDATION STRATEGY
=============================================================================

VALIDATION LEVELS:

1. 🟩 QUICK VALIDATION (per UI lists)
   - File exists & readable
   - Valid JSON structure
   - Basic parsing

2. 🟨 FULL VALIDATION (pre-restore)
   - Complete structure validation
   - Foreign key integrity
   - Photo manifest validation
   - Checksum verification
   - Performance warnings

3. 🟧 BATCH VALIDATION (maintenance)
   - Multiple backup validation
   - Cleanup invalid backups
   - Storage optimization

VALIDATION CATEGORIES:

✅ CRITICAL ERRORS (prevent restore):
   - File not found/readable
   - Invalid JSON structure
   - Missing required fields
   - FK constraint violations
   - Checksum mismatch

⚠️ WARNINGS (allow restore with caution):
   - Large file sizes
   - Many records (performance impact)
   - Missing optional metadata
   - Version mismatches

USAGE EXAMPLE:

val validation = validateBackupUseCase("/path/to/backup.json")
if (validation.isValid) {
    // Proceed with restore
    if (validation.hasWarnings()) {
        // Show warnings to user
    }
} else {
    // Show errors, prevent restore
}

=============================================================================
*/