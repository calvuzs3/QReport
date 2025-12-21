package net.calvuz.qreport.data.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import net.calvuz.qreport.data.backup.model.BackupSerializationException
import net.calvuz.qreport.domain.model.backup.*
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FASE 5.2 - JSON SERIALIZATION MANAGER
 *
 * Gestisce la serializzazione/deserializzazione JSON dei backup QReport
 * usando kotlinx.serialization con support per Instant e gestione errori.
 */

@Singleton
class BackupJsonSerializer @Inject constructor() {

    /**
     * Configurazione JSON con custom serializers
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
     * Serializza BackupData in JSON string
     */
    fun serializeBackup(backupData: BackupData): Result<String> {
        return try {
            val jsonString = json.encodeToString<BackupData>( backupData)
            Timber.d("Backup serializzato: ${jsonString.length} caratteri")
            Result.success(jsonString)

        } catch (e: Exception) {
            Timber.e(e, "Errore serializzazione backup")
            Result.failure(BackupSerializationException("Serializzazione fallita: ${e.message}", e))
        }
    }

    /**
     * Deserializza JSON string in BackupData
     */
    fun deserializeBackup(jsonString: String): Result<BackupData> {
        return try {
            val backupData = json.decodeFromString<BackupData>( jsonString)
            Timber.d("Backup deserializzato: ${backupData.database.getTotalRecordCount()} record")
            Result.success(backupData)

        } catch (e: Exception) {
            Timber.e(e, "Errore deserializzazione backup")
            Result.failure(
                BackupSerializationException(
                    "Deserializzazione fallita: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Salva BackupData in file JSON
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
            Timber.d("Backup salvato in ${file.path} (${fileSize / 1024} KB)")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Errore salvataggio backup file")
            Result.failure(
                BackupSerializationException(
                    "Salvataggio file fallito: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Carica BackupData da file JSON
     */
    fun loadBackupFromFile(file: File): Result<BackupData> {
        return try {
            if (!file.exists()) {
                return Result.failure(BackupSerializationException("File backup non trovato: ${file.path}"))
            }

            val jsonString = file.readText()
            Timber.d("Caricamento backup da ${file.path} (${jsonString.length} caratteri)")

            deserializeBackup(jsonString)

        } catch (e: Exception) {
            Timber.e(e, "Errore caricamento backup file")
            Result.failure(
                BackupSerializationException(
                    "Caricamento file fallito: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Valida formato JSON backup senza deserializzazione completa
     */
    fun validateBackupJson(jsonString: String): BackupValidationResult {
        return try {
            // Parse JSON per verificare struttura base
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject

            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Verifica campi obbligatori
            val requiredFields = listOf("metadata", "database", "settings", "photoManifest")
            for (field in requiredFields) {
                if (field !in jsonObject) {
                    errors.add("Campo obbligatorio mancante: $field")
                }
            }

            // Verifica metadata se presente
            jsonObject["metadata"]?.jsonObject?.let { metadata ->
                if ("id" !in metadata) errors.add("metadata.id mancante")
                if ("timestamp" !in metadata) errors.add("metadata.timestamp mancante")
                if ("appVersion" !in metadata) errors.add("metadata.appVersion mancante")
            }

            // Verifica database se presente
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

            BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore validazione JSON backup")
            BackupValidationResult.invalid(listOf("JSON non valido: ${e.message}"))
        }
    }

    /**
     * Calcola checksum SHA256 di backup JSON
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

            Timber.d("Checksum calcolato: $checksum")
            Result.success(checksum)

        } catch (e: Exception) {
            Timber.e(e, "Errore calcolo checksum")
            Result.failure(
                BackupSerializationException(
                    "Calcolo checksum fallito: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Verifica integrità backup tramite checksum
     */
    fun verifyBackupIntegrity(backupData: BackupData, expectedChecksum: String): Boolean {
        return try {
            val calculatedChecksumResult = calculateBackupChecksum(backupData)
            if (calculatedChecksumResult.isFailure) {
                Timber.w("Impossibile calcolare checksum per verifica")
                return false
            }

            val calculatedChecksum = calculatedChecksumResult.getOrThrow()
            val isValid = calculatedChecksum == expectedChecksum

            if (isValid) {
                Timber.d("✓ Checksum backup valido")
            } else {
                Timber.w("✗ Checksum backup non valido - Atteso: $expectedChecksum, Calcolato: $calculatedChecksum")
            }

            isValid

        } catch (e: Exception) {
            Timber.e(e, "Errore verifica integrità backup")
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

/*
=============================================================================
                            UTILIZZO ESEMPIO
=============================================================================

// Serializzazione
val jsonSerializer = BackupJsonSerializer()
val backupData = createBackupData()

val jsonResult = jsonSerializer.serializeBackup(backupData)
jsonResult.fold(
    onSuccess = { json ->
        val file = File("/path/to/backup.json")
        jsonSerializer.saveBackupToFile(backupData, file)
    },
    onFailure = { error ->
        Timber.e("Serialization failed: ${error.message}")
    }
)

// Deserializzazione
val file = File("/path/to/backup.json")
val loadResult = jsonSerializer.loadBackupFromFile(file)
loadResult.fold(
    onSuccess = { backupData ->
        // Process backup data
    },
    onFailure = { error ->
        Timber.e("Load failed: ${error.message}")
    }
)

=============================================================================
*/