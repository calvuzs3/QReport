package net.calvuz.qreport.settings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.settings.data.local.TechnicianSettingsDataStore
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import net.calvuz.qreport.settings.domain.repository.TechnicianSettingsRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementazione del repository per le impostazioni del tecnico
 *
 * Gestisce:
 * - Persistenza via DataStore
 * - Validazione dati
 * - Export/Import per backup
 * - Error handling robusto
 */
@Singleton
class TechnicianSettingsRepositoryImpl @Inject constructor(
    private val technicianSettingsDataStore: TechnicianSettingsDataStore
) : TechnicianSettingsRepository {

    /**
     * Ottieni informazioni tecnico correnti
     */
    override fun getTechnicianInfo(): Flow<TechnicianInfo> {
        return technicianSettingsDataStore.getTechnicianInfo()
            .catch { exception ->
                Timber.Forest.e(exception, "Errore lettura technician info")
                emit(TechnicianInfo()) // Emit empty info on error
            }
    }

    /**
     * Aggiorna informazioni tecnico
     */
    override suspend fun updateTechnicianInfo(technicianInfo: TechnicianInfo): Result<Unit> {
        return try {
            // Validate data before saving
            val validationResult = validateTechnicianInfo(technicianInfo)
            if (validationResult.isNotEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Dati non validi: ${validationResult.joinToString(", ")}")
                )
            }

            // Save to DataStore
            technicianSettingsDataStore.updateTechnicianInfo(technicianInfo)

            Timber.Forest.i("Technician info updated successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore aggiornamento technician info")
            Result.failure(e)
        }
    }

    /**
     * Verifica se ci sono dati tecnico salvati
     */
    override fun hasTechnicianData(): Flow<Boolean> {
        return technicianSettingsDataStore.getTechnicianInfo()
            .map { technicianInfo ->
                technicianInfo.name.isNotBlank() ||
                        technicianInfo.company.isNotBlank() ||
                        technicianInfo.certification.isNotBlank() ||
                        technicianInfo.phone.isNotBlank() ||
                        technicianInfo.email.isNotBlank()
            }
            .catch { exception ->
                Timber.Forest.e(exception, "Errore verifica technician data")
                emit(false) // Assume no data on error
            }
    }

    /**
     * Reset alle impostazioni di default
     */
    override suspend fun resetToDefault(): Result<Unit> {
        return try {
            technicianSettingsDataStore.clearTechnicianInfo()

            Timber.Forest.i("Technician settings reset to default")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore reset technician settings")
            Result.failure(e)
        }
    }

    /**
     * Export delle settings per backup
     */
    override suspend fun exportForBackup(): Map<String, String> {
        return try {
            val technicianInfo = technicianSettingsDataStore.getTechnicianInfo().first()

            val backupMap = mutableMapOf<String, String>()

            // Export only non-empty fields to optimize backup size
            if (technicianInfo.name.isNotBlank()) {
                backupMap["technician_name"] = technicianInfo.name
            }

            if (technicianInfo.company.isNotBlank()) {
                backupMap["technician_company"] = technicianInfo.company
            }

            if (technicianInfo.certification.isNotBlank()) {
                backupMap["technician_certification"] = technicianInfo.certification
            }

            if (technicianInfo.phone.isNotBlank()) {
                backupMap["technician_phone"] = technicianInfo.phone
            }

            if (technicianInfo.email.isNotBlank()) {
                backupMap["technician_email"] = technicianInfo.email
            }

            // Add metadata for backup validation
            if (backupMap.isNotEmpty()) {
                backupMap["backup_timestamp"] = System.currentTimeMillis().toString()
                backupMap["backup_version"] = "1.0"
            }

            Timber.Forest.i("Exported ${backupMap.size} technician settings for backup")
            backupMap

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore export technician settings")
            emptyMap()
        }
    }

    /**
     * Import delle settings da backup
     */
    override suspend fun importFromBackup(backupData: Map<String, String>): Result<Unit> {
        return try {
            if (backupData.isEmpty()) {
                Timber.Forest.w("Backup data is empty, skipping import")
                return Result.success(Unit)
            }

            // Validate backup data
            val validationResult = validateBackupData(backupData)
            if (validationResult.isNotEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Backup data non valido: ${validationResult.joinToString(", ")}")
                )
            }

            // Create TechnicianInfo from backup data
            val technicianInfo = TechnicianInfo(
                name = backupData["technician_name"]?.trim() ?: "",
                company = backupData["technician_company"]?.trim() ?: "",
                certification = backupData["technician_certification"]?.trim() ?: "",
                phone = backupData["technician_phone"]?.trim() ?: "",
                email = backupData["technician_email"]?.trim() ?: ""
            )

            // Validate the restored data
            val technicianValidation = validateTechnicianInfo(technicianInfo)
            if (technicianValidation.isNotEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Dati ripristinati non validi: ${technicianValidation.joinToString(", ")}")
                )
            }

            // Import to DataStore
            technicianSettingsDataStore.updateTechnicianInfo(technicianInfo)

            Timber.Forest.i("Technician settings imported successfully from backup")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore import technician settings")
            Result.failure(e)
        }
    }

    // ===== VALIDATION METHODS =====

    /**
     * Validate TechnicianInfo data
     */
    private fun validateTechnicianInfo(technicianInfo: TechnicianInfo): List<String> {
        Timber.Forest.i("Validating technician info")

        val errors = mutableListOf<String>()

        // Name validation
        if (technicianInfo.name.isNotBlank() && technicianInfo.name.trim().length < 2) {
            errors.add("Il nome deve avere almeno 2 caratteri")
        }

        if (technicianInfo.name.length > 100) {
            errors.add("Il nome non può superare 100 caratteri")
        }

        // Company validation
        if (technicianInfo.company.isNotBlank() && technicianInfo.company.trim().length < 2) {
            errors.add("Il nome dell'azienda deve avere almeno 2 caratteri")
        }

        if (technicianInfo.company.length > 100) {
            errors.add("Il nome dell'azienda non può superare 100 caratteri")
        }

        // Certification validation
        if (technicianInfo.certification.length > 200) {
            errors.add("La certificazione non può superare 200 caratteri")
        }

        // Phone validation
        if (technicianInfo.phone.isNotBlank() && !isValidPhone(technicianInfo.phone)) {
            errors.add("Formato telefono non valido")
        }

        // Email validation
        if (technicianInfo.email.isNotBlank() && !isValidEmail(technicianInfo.email)) {
            errors.add("Formato email non valido")
        }

        return errors
    }

    /**
     * Validate backup data structure
     */
    private fun validateBackupData(backupData: Map<String, String>): List<String> {
        Timber.Forest.i("Validating backup data structure")

        val errors = mutableListOf<String>()

        try {
            // Check for suspicious data patterns
            backupData.forEach { (key, value) ->
                if (key.isBlank()) {
                    errors.add("Chiave backup vuota trovata")
                }

                if (value.length > 1000) {
                    errors.add("Valore backup troppo lungo per la chiave: $key")
                }

                // Basic security check - no suspicious characters
                if (value.contains('\u0000') || value.contains('\uFFFD')) {
                    errors.add("Caratteri non validi trovati in: $key")
                }
            }

            // Validate backup metadata if present
            backupData["backup_timestamp"]?.let { timestamp ->
                val timestampLong = timestamp.toLongOrNull()
                if (timestampLong == null || timestampLong <= 0) {
                    errors.add("Timestamp backup non valido")
                }
            }

        } catch (e: Exception) {
            errors.add("Errore validazione struttura backup: ${e.message}")
        }

        return errors
    }

    /**
     * Validate phone number format
     */
    private fun isValidPhone(phone: String): Boolean {
        // Allow international formats, spaces, dashes, parentheses
        val phoneRegex = """^[+]?[0-9\s\-()]{8,}$""".toRegex()
        return phone.matches(phoneRegex)
    }

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        // Standard email regex pattern
        val emailRegex = """^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""".toRegex()
        return email.matches(emailRegex)
    }

    // ===== UTILITY METHODS =====

    /**
     * Get summary of current technician settings for debugging/logging
     */
    suspend fun getTechnicianSettingsSummary(): Map<String, String> {
        return try {
            val technicianInfo = technicianSettingsDataStore.getTechnicianInfo().first()
            val hasData = hasTechnicianData().first()

            mapOf(
                "has_data" to hasData.toString(),
                "name_length" to technicianInfo.name.length.toString(),
                "company_length" to technicianInfo.company.length.toString(),
                "has_certification" to technicianInfo.certification.isNotBlank().toString(),
                "has_phone" to technicianInfo.phone.isNotBlank().toString(),
                "has_email" to technicianInfo.email.isNotBlank().toString()
            )

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore ottenendo summary technician settings")
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}