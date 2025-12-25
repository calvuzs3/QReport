package net.calvuz.qreport.data.repository.backup

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.backup.SettingsBackup
import net.calvuz.qreport.domain.repository.backup.SettingsBackupRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.iterator

/**
 * App's Settings backup/restore
 * - SharedPreferences legacy
 * - DataStore preferences
 * - User settings custom
 * - App configuration
 */

// DataStore extension per Context
val Context.settingsDataStore: DataStore<androidx.datastore.preferences.core.Preferences>
        by preferencesDataStore(name = "qreport_settings")

@Singleton
class SettingsBackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsBackupRepository {

    companion object {
        // SharedPreferences keys (legacy)
        private const val SHARED_PREFS_NAME = "qreport_prefs"

        // DataStore keys (moderne)
        val TECHNICIAN_NAME = stringPreferencesKey("technician_name")
        val AUTO_SAVE_CHECKUPS = booleanPreferencesKey("auto_save_checkups")
        val PHOTO_QUALITY = stringPreferencesKey("photo_quality")
        val BACKUP_AUTO_ENABLED = booleanPreferencesKey("backup_auto_enabled")
        val BACKUP_FREQUENCY_DAYS = intPreferencesKey("backup_frequency_days")
        val EXPORT_FORMAT_DEFAULT = stringPreferencesKey("export_format_default")
        val UI_THEME = stringPreferencesKey("ui_theme")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        val APP_VERSION_LAST_USED = stringPreferencesKey("app_version_last_used")
        val FIRST_LAUNCH_DATE = longPreferencesKey("first_launch_date")
        val TOTAL_CHECKUPS_COMPLETED = intPreferencesKey("total_checkups_completed")
    }

    // ===== EXPORT SETTINGS =====

    /**
     * Export app's settings
     */
    override suspend fun exportSettings(): SettingsBackup {
        return try {
            Timber.v("Inizio export settings via repository")

            // 1. Export SharedPreferences legacy
            val sharedPrefs = exportSharedPreferences()
            Timber.v("Exported ${sharedPrefs.size} shared preferences")

            // 2. Export DataStore preferences
            val dataStorePrefs = exportDataStorePreferences()
            Timber.v("Exported ${dataStorePrefs.size} datastore preferences")

            // 3. Combine all settings
            val allPreferences = sharedPrefs + dataStorePrefs

            SettingsBackup(
                preferences = allPreferences,
                userSettings = exportUserSpecificSettings(),
                backupDateTime = Clock.System.now()
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore export settings")
            SettingsBackup.empty()
        }
    }

    /**
     * Export SharedPreferences legacy
     */
    private fun exportSharedPreferences(): Map<String, String> {
        return try {
            val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val allPrefs = prefs.all
            val stringPrefs = mutableMapOf<String, String>()

            for ((key, value) in allPrefs) {
                when (value) {
                    is String -> stringPrefs["legacy_$key"] = value
                    is Boolean -> stringPrefs["legacy_$key"] = value.toString()
                    is Int -> stringPrefs["legacy_$key"] = value.toString()
                    is Long -> stringPrefs["legacy_$key"] = value.toString()
                    is Float -> stringPrefs["legacy_$key"] = value.toString()
                    is Set<*> -> stringPrefs["legacy_$key"] = value.joinToString("|")
                    else -> stringPrefs["legacy_$key"] = value.toString()
                }
            }

            stringPrefs

        } catch (e: Exception) {
            Timber.e(e, "SharedPreferences export failed")
            emptyMap()
        }
    }

    /**
     * Export DataStore preferences
     */
    private suspend fun exportDataStorePreferences(): Map<String, String> {
        return try {
            val preferences = context.settingsDataStore.data.first()
            val dataStorePrefs = mutableMapOf<String, String>()

            preferences[TECHNICIAN_NAME]?.let {
                dataStorePrefs["technician_name"] = it
            }

            preferences[AUTO_SAVE_CHECKUPS]?.let {
                dataStorePrefs["auto_save_checkups"] = it.toString()
            }

            preferences[PHOTO_QUALITY]?.let {
                dataStorePrefs["photo_quality"] = it
            }

            preferences[BACKUP_AUTO_ENABLED]?.let {
                dataStorePrefs["backup_auto_enabled"] = it.toString()
            }

            preferences[BACKUP_FREQUENCY_DAYS]?.let {
                dataStorePrefs["backup_frequency_days"] = it.toString()
            }

            preferences[EXPORT_FORMAT_DEFAULT]?.let {
                dataStorePrefs["export_format_default"] = it
            }

            preferences[UI_THEME]?.let {
                dataStorePrefs["ui_theme"] = it
            }

            preferences[NOTIFICATION_ENABLED]?.let {
                dataStorePrefs["notification_enabled"] = it.toString()
            }

            preferences[ANALYTICS_ENABLED]?.let {
                dataStorePrefs["analytics_enabled"] = it.toString()
            }

            preferences[CRASH_REPORTING_ENABLED]?.let {
                dataStorePrefs["crash_reporting_enabled"] = it.toString()
            }

            preferences[APP_VERSION_LAST_USED]?.let {
                dataStorePrefs["app_version_last_used"] = it
            }

            preferences[FIRST_LAUNCH_DATE]?.let {
                dataStorePrefs["first_launch_date"] = it.toString()
            }

            preferences[TOTAL_CHECKUPS_COMPLETED]?.let {
                dataStorePrefs["total_checkups_completed"] = it.toString()
            }

            dataStorePrefs

        } catch (e: Exception) {
            Timber.e(e, "DataStore preferences export failed")
            emptyMap()
        }
    }

    /**
     * Export settings utente specifiche
     */
    private fun exportUserSpecificSettings(): Map<String, String> {
        return try {
            val userSettings = mutableMapOf<String, String>()

            // Device-specific settings
            userSettings["device_model"] = Build.MODEL
            userSettings["device_manufacturer"] = Build.MANUFACTURER
            userSettings["android_version"] = Build.VERSION.RELEASE
            userSettings["app_package"] = context.packageName

            // Timestamps
            userSettings["settings_exported_at"] = Clock.System.now().toString()

            userSettings

        } catch (e: Exception) {
            Timber.e(e, "User specific settings export failed")
            emptyMap()
        }
    }

    // ===== IMPORT SETTINGS =====

    /**
     * Import app's settings
     */
    override suspend fun importSettings(settingsBackup: SettingsBackup): Result<Unit> {
        return try {
            Timber.v("Inizio import settings")

            // 1. Import SharedPreferences legacy
            val legacyPrefs = settingsBackup.preferences.filterKeys { it.startsWith("legacy_") }
            if (legacyPrefs.isNotEmpty()) {
                importSharedPreferences(legacyPrefs)
                Timber.v("Imported ${legacyPrefs.size} legacy preferences")
            }

            // 2. Import DataStore preferences moderne
            val modernPrefs = settingsBackup.preferences.filterKeys { !it.startsWith("legacy_") }
            if (modernPrefs.isNotEmpty()) {
                importDataStorePreferences(modernPrefs)
                Timber.v("Imported ${modernPrefs.size} modern preferences")
            }

            // 3. Import user settings (opzionale)
            if (settingsBackup.userSettings.isNotEmpty()) {
                importUserSpecificSettings(settingsBackup.userSettings)
                Timber.v("Imported ${settingsBackup.userSettings.size} user settings")
            }

            Timber.v("App settings import completed")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "App settings import failed")
            Result.failure(SettingsImportException("App settings import failed: ${e.message}", e))
        }
    }

    /**
     * Import SharedPreferences legacy
     */
    private fun importSharedPreferences(legacyPrefs: Map<String, String>) {
        try {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            for ((prefixedKey, value) in legacyPrefs) {
                val key = prefixedKey.removePrefix("legacy_")

                // Auto-detect type and apply
                when {
                    value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true) -> {
                        editor.putBoolean(key, value.toBoolean())
                    }
                    value.toLongOrNull() != null -> {
                        val longValue = value.toLong()
                        if (longValue in Int.MIN_VALUE..Int.MAX_VALUE) {
                            editor.putInt(key, longValue.toInt())
                        } else {
                            editor.putLong(key, longValue)
                        }
                    }
                    value.toFloatOrNull() != null -> {
                        editor.putFloat(key, value.toFloat())
                    }
                    value.contains("|") -> {
                        // Set<String> serialized
                        val stringSet = value.split("|").toSet()
                        editor.putStringSet(key, stringSet)
                    }
                    else -> {
                        // Normal String
                        editor.putString(key, value)
                    }
                }
            }

            editor.apply()

        } catch (e: Exception) {
            Timber.e(e, "Import SharedPreferences legacy failed")
        }
    }

    /**
     * Import DataStore preferences
     */
    private suspend fun importDataStorePreferences(modernPrefs: Map<String, String>) {
        try {
            context.settingsDataStore.edit { preferences ->

                modernPrefs["technician_name"]?.let {
                    preferences[TECHNICIAN_NAME] = it
                }

                modernPrefs["auto_save_checkups"]?.toBoolean()?.let {
                    preferences[AUTO_SAVE_CHECKUPS] = it
                }

                modernPrefs["photo_quality"]?.let {
                    preferences[PHOTO_QUALITY] = it
                }

                modernPrefs["backup_auto_enabled"]?.toBoolean()?.let {
                    preferences[BACKUP_AUTO_ENABLED] = it
                }

                modernPrefs["backup_frequency_days"]?.toIntOrNull()?.let {
                    preferences[BACKUP_FREQUENCY_DAYS] = it
                }

                modernPrefs["export_format_default"]?.let {
                    preferences[EXPORT_FORMAT_DEFAULT] = it
                }

                modernPrefs["ui_theme"]?.let {
                    preferences[UI_THEME] = it
                }

                modernPrefs["notification_enabled"]?.toBoolean()?.let {
                    preferences[NOTIFICATION_ENABLED] = it
                }

                modernPrefs["analytics_enabled"]?.toBoolean()?.let {
                    preferences[ANALYTICS_ENABLED] = it
                }

                modernPrefs["crash_reporting_enabled"]?.toBoolean()?.let {
                    preferences[CRASH_REPORTING_ENABLED] = it
                }

                modernPrefs["app_version_last_used"]?.let {
                    preferences[APP_VERSION_LAST_USED] = it
                }

                modernPrefs["first_launch_date"]?.toLongOrNull()?.let {
                    preferences[FIRST_LAUNCH_DATE] = it
                }

                modernPrefs["total_checkups_completed"]?.toIntOrNull()?.let {
                    preferences[TOTAL_CHECKUPS_COMPLETED] = it
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Import DataStore preferences failed")
        }
    }

    /**
     * User settings Import
     */
    private fun importUserSpecificSettings(userSettings: Map<String, String>) {
        try {
            // Log device info differences for debugging
            val currentModel = Build.MODEL
            val backupModel = userSettings["device_model"]

            if (currentModel != backupModel) {
                Timber.w("Device model change: $backupModel → $currentModel")
            }

            val currentAndroid = Build.VERSION.RELEASE
            val backupAndroid = userSettings["android_version"]

            if (currentAndroid != backupAndroid) {
                Timber.w("Android version change: $backupAndroid → $currentAndroid")
            }

            Timber.v("Settings backup created on: ${userSettings["settings_exported_at"]}")

        } catch (e: Exception) {
            Timber.e(e, "User settings Import failed")
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Reset all settings to defaults
     */
    suspend fun resetAllSettings(): Result<Unit> {
        return try {
            // 1. Clear SharedPreferences
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            // 2. Clear DataStore
            context.settingsDataStore.edit { preferences ->
                preferences.clear()
            }

            Timber.v("All settings reset")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "All settings reset failed")
            Result.failure(e)
        }
    }

    /**
     * All settings summary
     */
    suspend fun getSettingsSummary(): Map<String, String> {
        val summary = mutableMapOf<String, String>()

        try {
            // Conta SharedPreferences
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            summary["legacy_preferences_count"] = prefs.all.size.toString()

            // Conta DataStore preferences
            val dataStorePrefs = context.settingsDataStore.data.first()
            summary["modern_preferences_count"] = dataStorePrefs.asMap().size.toString()

            // Settings key
            summary["technician_name"] = dataStorePrefs[TECHNICIAN_NAME] ?: "Non impostato"
            summary["auto_save_enabled"] = dataStorePrefs[AUTO_SAVE_CHECKUPS]?.toString() ?: "false"
            summary["backup_auto_enabled"] = dataStorePrefs[BACKUP_AUTO_ENABLED]?.toString() ?: "false"
            summary["ui_theme"] = dataStorePrefs[UI_THEME] ?: "auto"

        } catch (e: Exception) {
            Timber.e(e, "Settings summary failed")
            summary["error"] = e.message ?: "Unknown error"
        }

        return summary
    }
}

/**
 * Custom exception for import settings
 */
class SettingsImportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/*
=============================================================================
                            SETTINGS BACKUP STRUCTURE
=============================================================================

SettingsBackup JSON Structure:
{
  "preferences": {
    // SharedPreferences legacy (prefixed with "legacy_")
    "legacy_user_preference_1": "value1",
    "legacy_auto_backup_enabled": "true",

    // DataStore preferences moderne
    "technician_name": "Mario Rossi",
    "auto_save_checkups": "true",
    "photo_quality": "high",
    "backup_auto_enabled": "false",
    "backup_frequency_days": "7",
    "export_format_default": "word",
    "ui_theme": "dark",
    "notification_enabled": "true",
    "analytics_enabled": "false",
    "app_version_last_used": "1.2.0",
    "total_checkups_completed": "42"
  },
  "userSettings": {
    "device_model": "Pixel 7",
    "device_manufacturer": "Google",
    "android_version": "14",
    "app_package": "net.calvuz.qreport",
    "settings_exported_at": "2024-12-20T14:30:22Z"
  },
  "backupDateTime": "2024-12-20T14:30:22Z"
}

=============================================================================
*/