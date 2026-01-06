package net.calvuz.qreport.settings.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


// Context extension per TechnicianSettings DataStore
private val Context.technicianSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "technician_settings"
)
@Singleton
class TechnicianSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val TECHNICIAN_NAME = stringPreferencesKey("technician_name")
        val TECHNICIAN_COMPANY = stringPreferencesKey("technician_company")
        val TECHNICIAN_CERTIFICATION = stringPreferencesKey("technician_certification")
        val TECHNICIAN_PHONE = stringPreferencesKey("technician_phone")
        val TECHNICIAN_EMAIL = stringPreferencesKey("technician_email")
        val LAST_UPDATED = longPreferencesKey("last_updated")
    }

    /**
     * Flow delle informazioni tecnico correnti
     */
    fun getTechnicianInfo(): Flow<TechnicianInfo> =
        context.technicianSettingsDataStore.data
            .catch { exception ->
                Timber.Forest.e(exception, "Errore lettura technician settings, usando valori default")
                emit(emptyPreferences())
            }
            .map { preferences ->
                mapPreferencesToTechnicianInfo(preferences)
            }

    /**
     * Aggiorna le informazioni del tecnico
     */
    suspend fun updateTechnicianInfo(technicianInfo: TechnicianInfo) {
        try {
            context.technicianSettingsDataStore.edit { preferences ->
                mapTechnicianInfoToPreferences(technicianInfo, preferences)
                preferences[PreferencesKeys.LAST_UPDATED] = System.currentTimeMillis()
            }
            Timber.Forest.d("Technician info updated in DataStore")
        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore aggiornamento technician info in DataStore")
            throw e
        }
    }

    /**
     * Cancella tutte le informazioni del tecnico
     */
    suspend fun clearTechnicianInfo() {
        try {
            context.technicianSettingsDataStore.edit { preferences ->
                // Rimuovi solo le chiavi del tecnico, mantieni eventualmente altri settings
                preferences.remove(PreferencesKeys.TECHNICIAN_NAME)
                preferences.remove(PreferencesKeys.TECHNICIAN_COMPANY)
                preferences.remove(PreferencesKeys.TECHNICIAN_CERTIFICATION)
                preferences.remove(PreferencesKeys.TECHNICIAN_PHONE)
                preferences.remove(PreferencesKeys.TECHNICIAN_EMAIL)
                preferences.remove(PreferencesKeys.LAST_UPDATED)
            }
            Timber.Forest.d("Technician info cleared from DataStore")
        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore clearing technician info from DataStore")
            throw e
        }
    }

    /**
     * Verifica se ci sono dati del tecnico salvati
     */
    fun hasTechnicianData(): Flow<Boolean> =
        context.technicianSettingsDataStore.data
            .catch { exception ->
                Timber.Forest.e(exception, "Errore verifica technician data")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[PreferencesKeys.TECHNICIAN_NAME]?.isNotBlank() == true ||
                        preferences[PreferencesKeys.TECHNICIAN_COMPANY]?.isNotBlank() == true ||
                        preferences[PreferencesKeys.TECHNICIAN_CERTIFICATION]?.isNotBlank() == true ||
                        preferences[PreferencesKeys.TECHNICIAN_PHONE]?.isNotBlank() == true ||
                        preferences[PreferencesKeys.TECHNICIAN_EMAIL]?.isNotBlank() == true
            }

    /**
     * Ottieni timestamp ultimo aggiornamento
     */
    fun getLastUpdatedTimestamp(): Flow<Long?> =
        context.technicianSettingsDataStore.data
            .catch { exception ->
                Timber.Forest.e(exception, "Errore lettura last updated timestamp")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[PreferencesKeys.LAST_UPDATED]
            }

    // ===== EXPORT/IMPORT METHODS FOR BACKUP =====

    /**
     * Export delle settings come Map per backup
     */
    suspend fun exportForBackup(): Map<String, String> {
        return try {
            val preferences = context.technicianSettingsDataStore.data.first()
            val backupMap = mutableMapOf<String, String>()

            // Export solo le chiavi non vuote
            preferences[PreferencesKeys.TECHNICIAN_NAME]?.takeIf { it.isNotBlank() }?.let {
                backupMap["technician_name"] = it
            }

            preferences[PreferencesKeys.TECHNICIAN_COMPANY]?.takeIf { it.isNotBlank() }?.let {
                backupMap["technician_company"] = it
            }

            preferences[PreferencesKeys.TECHNICIAN_CERTIFICATION]?.takeIf { it.isNotBlank() }?.let {
                backupMap["technician_certification"] = it
            }

            preferences[PreferencesKeys.TECHNICIAN_PHONE]?.takeIf { it.isNotBlank() }?.let {
                backupMap["technician_phone"] = it
            }

            preferences[PreferencesKeys.TECHNICIAN_EMAIL]?.takeIf { it.isNotBlank() }?.let {
                backupMap["technician_email"] = it
            }

            preferences[PreferencesKeys.LAST_UPDATED]?.let {
                backupMap["last_updated"] = it.toString()
            }

            backupMap

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore export technician settings for backup")
            emptyMap()
        }
    }

    /**
     * Import delle settings da backup
     */
    suspend fun importFromBackup(backupData: Map<String, String>) {
        try {
            if (backupData.isEmpty()) {
                Timber.Forest.w("Backup data is empty for technician settings")
                return
            }

            context.technicianSettingsDataStore.edit { preferences ->
                // Clear existing data first
                preferences.remove(PreferencesKeys.TECHNICIAN_NAME)
                preferences.remove(PreferencesKeys.TECHNICIAN_COMPANY)
                preferences.remove(PreferencesKeys.TECHNICIAN_CERTIFICATION)
                preferences.remove(PreferencesKeys.TECHNICIAN_PHONE)
                preferences.remove(PreferencesKeys.TECHNICIAN_EMAIL)
                preferences.remove(PreferencesKeys.LAST_UPDATED)

                // Import new data
                backupData["technician_name"]?.takeIf { it.isNotBlank() }?.let {
                    preferences[PreferencesKeys.TECHNICIAN_NAME] = it.trim()
                }

                backupData["technician_company"]?.takeIf { it.isNotBlank() }?.let {
                    preferences[PreferencesKeys.TECHNICIAN_COMPANY] = it.trim()
                }

                backupData["technician_certification"]?.takeIf { it.isNotBlank() }?.let {
                    preferences[PreferencesKeys.TECHNICIAN_CERTIFICATION] = it.trim()
                }

                backupData["technician_phone"]?.takeIf { it.isNotBlank() }?.let {
                    preferences[PreferencesKeys.TECHNICIAN_PHONE] = it.trim()
                }

                backupData["technician_email"]?.takeIf { it.isNotBlank() }?.let {
                    preferences[PreferencesKeys.TECHNICIAN_EMAIL] = it.trim()
                }

                // Restore or set current timestamp
                val lastUpdated = backupData["last_updated"]?.toLongOrNull()
                    ?: System.currentTimeMillis()
                preferences[PreferencesKeys.LAST_UPDATED] = lastUpdated
            }

            Timber.Forest.d("Technician settings imported successfully from backup")

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore import technician settings from backup")
            throw e
        }
    }

    // ===== MAPPING METHODS =====

    /**
     * Mappa Preferences a TechnicianInfo
     */
    private fun mapPreferencesToTechnicianInfo(preferences: Preferences): TechnicianInfo {
        return TechnicianInfo(
            name = preferences[PreferencesKeys.TECHNICIAN_NAME]?.trim() ?: "",
            company = preferences[PreferencesKeys.TECHNICIAN_COMPANY]?.trim() ?: "",
            certification = preferences[PreferencesKeys.TECHNICIAN_CERTIFICATION]?.trim() ?: "",
            phone = preferences[PreferencesKeys.TECHNICIAN_PHONE]?.trim() ?: "",
            email = preferences[PreferencesKeys.TECHNICIAN_EMAIL]?.trim() ?: ""
        )
    }

    /**
     * Mappa TechnicianInfo a Preferences
     */
    private fun mapTechnicianInfoToPreferences(
        technicianInfo: TechnicianInfo,
        preferences: MutablePreferences
    ) {
        // Salva solo i campi non vuoti per ottimizzare storage
        if (technicianInfo.name.isNotBlank()) {
            preferences[PreferencesKeys.TECHNICIAN_NAME] = technicianInfo.name.trim()
        } else {
            preferences.remove(PreferencesKeys.TECHNICIAN_NAME)
        }

        if (technicianInfo.company.isNotBlank()) {
            preferences[PreferencesKeys.TECHNICIAN_COMPANY] = technicianInfo.company.trim()
        } else {
            preferences.remove(PreferencesKeys.TECHNICIAN_COMPANY)
        }

        if (technicianInfo.certification.isNotBlank()) {
            preferences[PreferencesKeys.TECHNICIAN_CERTIFICATION] = technicianInfo.certification.trim()
        } else {
            preferences.remove(PreferencesKeys.TECHNICIAN_CERTIFICATION)
        }

        if (technicianInfo.phone.isNotBlank()) {
            preferences[PreferencesKeys.TECHNICIAN_PHONE] = technicianInfo.phone.trim()
        } else {
            preferences.remove(PreferencesKeys.TECHNICIAN_PHONE)
        }

        if (technicianInfo.email.isNotBlank()) {
            preferences[PreferencesKeys.TECHNICIAN_EMAIL] = technicianInfo.email.trim()
        } else {
            preferences.remove(PreferencesKeys.TECHNICIAN_EMAIL)
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Ottieni summary dello stato corrente (per debugging)
     */
    suspend fun getTechnicianSettingsSummary(): Map<String, Any> {
        return try {
            val preferences = context.technicianSettingsDataStore.data.first()

            mapOf(
                "name_length" to (preferences[PreferencesKeys.TECHNICIAN_NAME]?.length ?: 0),
                "company_length" to (preferences[PreferencesKeys.TECHNICIAN_COMPANY]?.length ?: 0),
                "has_certification" to (preferences[PreferencesKeys.TECHNICIAN_CERTIFICATION]?.isNotBlank() == true),
                "has_phone" to (preferences[PreferencesKeys.TECHNICIAN_PHONE]?.isNotBlank() == true),
                "has_email" to (preferences[PreferencesKeys.TECHNICIAN_EMAIL]?.isNotBlank() == true),
                "last_updated" to (preferences[PreferencesKeys.LAST_UPDATED] ?: 0L),
                "total_preferences" to preferences.asMap().size
            )

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore ottenendo technician settings summary")
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}