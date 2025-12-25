package net.calvuz.qreport.data.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import net.calvuz.qreport.data.backup.model.BackupSerializationException
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.util.SizeUtils.getFormattedSize
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON backup serialization/deserialization
 * - kotlinx.serialization
 * - Instant support
 * - error handling
 */

@Singleton
class BackupJsonSerializer @Inject constructor() {

    /**
     * JSON configuration with custom serializer
     */
    private val json = Json {
        // Configuration
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true

        // Custom serializers module
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer)
        }
    }

    /**
     * Serialize BackupData into JSON string
     */
    fun serializeBackup(backupData: BackupData): Result<String> {
        return try {
            val jsonString = json.encodeToString<BackupData>( backupData)
            Timber.d("Serialized backup: ${jsonString.length} chars")
            Result.success(jsonString)

        } catch (e: Exception) {
            Timber.e(e, "JSON backup serialization failed")
            Result.failure(BackupSerializationException("JSON backup serialization failed: ${e.message}", e))
        }
    }

    /**
     * Deserialize JSON string into BackupData
     */
    fun deserializeBackup(jsonString: String): Result<BackupData> {
        return try {
            val backupData = json.decodeFromString<BackupData>( jsonString)
            Timber.d("Deserializad backup: ${backupData.database.getTotalRecordCount()} records")
            Result.success(backupData)

        } catch (e: Exception) {
            Timber.e(e, "JSON backup deserialization failed")
            Result.failure(
                BackupSerializationException(
                    "JSON backup deserialization failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Save BackupData in JSON file
     */
    fun saveBackupToFile(backupData: BackupData, file: File): Result<Unit> {
        return try {
            val jsonResult = serializeBackup(backupData)
            if (jsonResult.isFailure) {
                return Result.failure(jsonResult.exceptionOrNull()!!)
            }

            file.parentFile?.mkdirs()
            file.writeText(jsonResult.getOrThrow())

            val fileSize = file.length()
            Timber.d("Backup saved in ${file.path} (${fileSize.getFormattedSize()})")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Backup saving failed")
            Result.failure(
                BackupSerializationException(
                    "Backup saving failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Load BackupData from JSON file
     */
    fun loadBackupFromFile(file: File): Result<BackupData> {
        return try {
            if (!file.exists()) {
                return Result.failure(BackupSerializationException("Backup file not found: ${file.path}"))
            }

            val jsonString = file.readText()
            Timber.d("Backup loading from ${file.path} (${jsonString.length} chars)")

            deserializeBackup(jsonString)

        } catch (e: Exception) {
            Timber.e(e, "Backup file loading failed")
            Result.failure(
                BackupSerializationException(
                    "Loading file failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Validate JSON backup format without complete deserialization
     */
    fun validateBackupJson(jsonString: String): BackupValidationResult {
        return try {
            // Parse JSON for base structure check
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject

            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Check mandatory fields
            val requiredFields = listOf("metadata", "database", "settings", "photoManifest")
            for (field in requiredFields) {
                if (field !in jsonObject) {
                    errors.add("Missing field: $field")
                }
            }

            // Check metadata if present
            jsonObject["metadata"]?.jsonObject?.let { metadata ->
                if ("id" !in metadata) errors.add("missing metadata.id ")
                if ("timestamp" !in metadata) errors.add("missing metadata.timestamp ")
                if ("appVersion" !in metadata) errors.add("missing metadata.appVersion ")
            }

            // Check database if present
            jsonObject["database"]?.jsonObject?.let { database ->
                val expectedTables = listOf(
                    "checkUps", "checkItems", "photos", "spareParts",
                    "clients", "contacts", "facilities", "facilityIslands",
                    "checkUpAssociations"
                )

                for (table in expectedTables) {
                    if (table !in database) {
                        warnings.add("Tabella database mancante: $table")
                    }
                }
            }

            val result = BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )
            Timber.d("JSON BackupData validation: $result")
            result

        } catch (e: Exception) {
            Timber.e(e, "JSON BackupData validation failed")
            BackupValidationResult.invalid(listOf("JSON non valido: ${e.message}"))
        }
    }

    /**
     * Calculate JSON backup SHA256 checksum
     */
    fun calculateBackupChecksum(backupData: BackupData): Result<String> {
        return try {
            val jsonResult = serializeBackup(backupData)
            if (jsonResult.isFailure) {
                return Result.failure(jsonResult.exceptionOrNull()!!)
            }

            val jsonBytes = jsonResult.getOrThrow().toByteArray()
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(jsonBytes)
            val checksum = hashBytes.joinToString("") { "%02x".format(it) }

            Timber.d("BackupData checksum: $checksum")
            Result.success(checksum)

        } catch (e: Exception) {
            Timber.e(e, "BackupData checksum failed")
            Result.failure(
                BackupSerializationException(
                    "Calcolo checksum fallito: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Backup integrity verification through checksum
     */
    fun verifyBackupIntegrity(backupData: BackupData): Boolean {
        return try {
            // Get the expected value
            val expectedChecksum = backupData.metadata.checksum
            // Recreate first checksum conditions
            val tmpMetadata = backupData.metadata.copy(checksum = "", totalSize = 0L)
            val tmpBackupData = backupData.copy(metadata = tmpMetadata)

            // Verify
            val calculatedChecksumResult = calculateBackupChecksum(tmpBackupData)
            if (calculatedChecksumResult.isFailure) {
                Timber.w("Checksum calculation impossible")
                return false
            }

            val calculatedChecksum = calculatedChecksumResult.getOrThrow()
            val isValid = calculatedChecksum == expectedChecksum

            if (isValid) {
                Timber.v("✓ Checksum backup is valid")
            } else {
                Timber.w("✗ Checksum backup is not valid\n- expected:  $expectedChecksum\ncalculated: $calculatedChecksum")
            }

            isValid

        } catch (e: Exception) {
            Timber.e(e, "Backup integrity verification failed")
            false
        }
    }
}

/**
 * Custom Serializer per kotlinx.datetime.Instant
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}