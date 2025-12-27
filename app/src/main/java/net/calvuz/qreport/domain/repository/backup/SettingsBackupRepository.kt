package net.calvuz.qreport.domain.repository.backup

import net.calvuz.qreport.domain.model.backup.SettingsBackup

interface SettingsBackupRepository {

    suspend fun exportSettings(): SettingsBackup
    suspend fun importSettings(settingsBackup: SettingsBackup): Result<Unit>

}