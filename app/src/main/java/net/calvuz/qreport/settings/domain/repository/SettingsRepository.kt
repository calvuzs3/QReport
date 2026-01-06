package net.calvuz.qreport.settings.domain.repository

import net.calvuz.qreport.app.result.data.ValidationResult
import net.calvuz.qreport.backup.domain.model.backup.SettingsBackup

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

