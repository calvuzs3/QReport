package net.calvuz.qreport.data.repository.settings

import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import net.calvuz.qreport.data.core.ValidationResult
import net.calvuz.qreport.domain.model.backup.SettingsBackup
import net.calvuz.qreport.domain.repository.settings.SettingsRepository
import net.calvuz.qreport.domain.repository.settings.TechnicianSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SettingsRepository implementation
 *
 * SettingsBackup structure:
 * - preferences: Map<String, String> -> app preferences generali
 * - userSettings: Map<String, String> -> settings utente inclusi dati tecnico
 * - backupDateTime: Instant
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val technicianSettingsRepository: TechnicianSettingsRepository
) : SettingsRepository {

    override suspend fun exportSettings(): SettingsBackup {
        return try {
            val technicianSettings = technicianSettingsRepository.exportForBackup()

            val preferences = emptyMap<String, String>() // Per future app settings

            // Include technician settings in userSettings section
            val userSettings = buildMap {
                // Technician data with prefix
                technicianSettings.forEach { (key, value) ->
                    put("tech_$key", value)
                }

                // Future user preferences can be added here
                // put("app_theme", "dark")
                // put("auto_sync", "true")
            }

            SettingsBackup(
                preferences = preferences,
                userSettings = userSettings,
                backupDateTime = Clock.System.now()
            )

        } catch (_: Exception) {
            // Return empty backup if error
            SettingsBackup(
                preferences = emptyMap(),
                userSettings = emptyMap(),
                backupDateTime = Clock.System.now()
            )
        }
    }

    override suspend fun importSettings(settingsBackup: SettingsBackup): Result<Unit> {
        return try {
            // Extract technician settings from userSettings
            val technicianBackupData = settingsBackup.userSettings
                .filterKeys { it.startsWith("tech_") }
                .mapKeys { (key, _) -> key.removePrefix("tech_") }

            if (technicianBackupData.isNotEmpty()) {
                // Import technician settings
                val importResult =
                    technicianSettingsRepository.importFromBackup(technicianBackupData)

                if (importResult.isFailure) {
                    return Result.failure(
                        Exception("Errore importando impostazioni tecnico: ${importResult.exceptionOrNull()?.message}")
                    )
                }
            }

            // Future: Import other app preferences from settingsBackup.preferences
            // importAppPreferences(settingsBackup.preferences)

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se ci sono settings del tecnico da esportare
     */
    override suspend fun hasTechnicianSettingsToExport(): Boolean {
        return try {
            technicianSettingsRepository.hasTechnicianData().first()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Export solo delle impostazioni tecnico (per debug/testing)
     */
    override suspend fun exportTechnicianSettingsOnly(): Map<String, String> {
        return try {
            technicianSettingsRepository.exportForBackup()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Import solo delle impostazioni tecnico (per debug/testing)
     */
    override suspend fun importTechnicianSettingsOnly(backupData: Map<String, String>): Result<Unit> {
        return technicianSettingsRepository.importFromBackup(backupData)
    }

    /**
     * Cancella tutte le impostazioni utente (per reset completo)
     */
    override suspend fun clearAllUserSettings(): Result<Unit> {
        return technicianSettingsRepository.resetToDefault()
    }

    /**
     * Valida backup settings prima dell'import
     */
    override suspend fun validateSettingsBackup(settingsBackup: SettingsBackup): ValidationResult {
        val issues = mutableListOf<String>()

        try {
            // Verifica formato timestamp
            if (settingsBackup.backupDateTime.toString().isBlank()) {
                issues.add("Timestamp backup non valido")
            }

            // Verifica presenza dati tecnico se attesi
            val hasTechData = settingsBackup.userSettings.keys.any { it.startsWith("tech_") }
            if (!hasTechData) {
                issues.add("Nessuna impostazione tecnico trovata nel backup")
            }

            // Verifica integrità dati tecnico
            if (hasTechData) {
                val techName = settingsBackup.userSettings["tech_technician_name"]
                val techCompany = settingsBackup.userSettings["tech_technician_company"]

                if (techName.isNullOrBlank() && techCompany.isNullOrBlank()) {
                    issues.add("Dati tecnico nel backup sono vuoti")
                }
            }

            return if (issues.isEmpty()) {
                ValidationResult.Valid(
                    // It could return ValidationResultEmpty
                    isValid = true,
                    message = "Validazione riuscita"
                )
            } else {
                ValidationResult.NotValid(
                    isValid = false,
                    issues = issues
                )
            }

        } catch (e: Exception) {
            return ValidationResult.NotValid(
                isValid = false,
                issues = listOf("Errore validazione backup: ${e.message}"),
            )
        }
    }
}