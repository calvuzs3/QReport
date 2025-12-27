package net.calvuz.qreport.domain.repository.settings

import net.calvuz.qreport.data.core.ValidationResult
import net.calvuz.qreport.domain.model.backup.SettingsBackup

/**
 * Repository per backup impostazioni
 */
interface SettingsRepository {

    suspend fun exportSettings(): SettingsBackup
    suspend fun importSettings(settingsBackup: SettingsBackup): Result<Unit>
    suspend fun validateSettingsBackup(settingsBackup: SettingsBackup): ValidationResult
    suspend fun hasTechnicianSettingsToExport(): Boolean
    suspend fun exportTechnicianSettingsOnly(): Map<String, String>
    suspend fun importTechnicianSettingsOnly(backupData: Map<String, String>): Result<Unit>
    suspend fun clearAllUserSettings(): Result<Unit>
}

