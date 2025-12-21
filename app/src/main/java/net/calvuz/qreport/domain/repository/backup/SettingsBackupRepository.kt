package net.calvuz.qreport.domain.repository.backup

import net.calvuz.qreport.domain.model.backup.SettingsBackup

/**
 * Repository per backup impostazioni
 */
interface SettingsBackupRepository {

    /**
     * Esporta impostazioni
     */
    suspend fun exportSettings(): SettingsBackup

    /**
     * Importa impostazioni
     */
    suspend fun importSettings(settingsBackup: SettingsBackup): Result<Unit>
}