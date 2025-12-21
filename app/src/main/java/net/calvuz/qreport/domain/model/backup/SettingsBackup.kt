package net.calvuz.qreport.domain.model.backup

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * SettingsBackup - Backup delle impostazioni app
 */
@Serializable
data class SettingsBackup(
    val preferences: Map<String, String>,
    val userSettings: Map<String, String>,
    @Contextual val backupDateTime: Instant
) {
    companion object {
        fun empty(): SettingsBackup {
            return SettingsBackup(
                preferences = emptyMap(),
                userSettings = emptyMap(),
                backupDateTime = Clock.System.now()
            )
        }
    }
}