package net.calvuz.qreport.settings.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import net.calvuz.qreport.settings.domain.repository.TechnicianSettingsRepository
import javax.inject.Inject

/**
 * Use case per gestire l'integrazione delle impostazioni tecnico con i CheckUp
 *
 * Fornisce:
 * 1. Pre-popolamento automatico dei dati tecnico nei CheckUp
 * 2. Verifica disponibilità dati salvati
 * 3. Caricamento preferenze utente
 */
class TechnicianSettingsUseCase @Inject constructor(
    private val technicianSettingsRepository: TechnicianSettingsRepository
) {

    /**
     * Ottiene le informazioni del tecnico per pre-popolamento CheckUp
     */
    fun getTechnicianInfoForCheckUp(): Flow<TechnicianInfo> {
        return technicianSettingsRepository.getTechnicianInfo()
    }

    /**
     * Ottiene le informazioni del tecnico in modo sincrono per uso immediato
     */
    suspend fun getTechnicianInfoImmediate(): TechnicianInfo {
        return try {
            technicianSettingsRepository.getTechnicianInfo().first()
        } catch (_: Exception) {
            TechnicianInfo() // Default vuoto se errore
        }
    }

    /**
     * Verifica se ci sono dati del tecnico salvati
     */
    suspend fun hasSavedTechnicianData(): Boolean {
        return try {
            technicianSettingsRepository.hasTechnicianData().first()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Aggiorna informazioni tecnico (da settings screen)
     */
    suspend fun updateTechnicianInfo(technicianInfo: TechnicianInfo): Result<Unit> {
        return technicianSettingsRepository.updateTechnicianInfo(technicianInfo)
    }

    /**
     * Crea TechnicianInfo con fallback intelligente
     * Se non ci sono dati salvati, restituisce un oggetto vuoto
     * Se ci sono dati parziali, li completa con valori ragionevoli
     */
    suspend fun getTechnicianInfoWithFallback(): TechnicianInfo {
        val savedInfo = getTechnicianInfoImmediate()

        return if (savedInfo.name.isBlank() && savedInfo.company.isBlank()) {
            // Nessun dato salvato, restituisci vuoto per permettere inserimento manuale
            TechnicianInfo()
        } else {
            // Ci sono dati salvati, usali per pre-popolamento
            savedInfo
        }
    }

    /**
     * Suggerisce se mostrare il pulsante "Carica da Profilo"
     */
    suspend fun shouldShowLoadFromProfileButton(): Boolean {
        return hasSavedTechnicianData()
    }

    /**
     * Verifica se i dati del tecnico sono sufficienti per un CheckUp professionale
     */
    suspend fun isTechnicianDataComplete(): Boolean {
        val technicianInfo = getTechnicianInfoImmediate()
        return technicianInfo.name.isNotBlank() &&
                technicianInfo.company.isNotBlank() &&
                (technicianInfo.phone.isNotBlank() || technicianInfo.email.isNotBlank())
    }

    suspend fun resetToDefault() {
        technicianSettingsRepository.resetToDefault()
    }

    /**
     * Export per backup - delega al repository
     */
    suspend fun exportForBackup(): Map<String, String> {
        return technicianSettingsRepository.exportForBackup()
    }

    /**
     * Import da backup - delega al repository
     */
    suspend fun importFromBackup(backupData: Map<String, String>): Result<Unit> {
        return technicianSettingsRepository.importFromBackup(backupData)
    }
}