package net.calvuz.qreport.domain.repository.settings

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.settings.TechnicianInfo

/**
 * Repository per le impostazioni del tecnico
 */
interface TechnicianSettingsRepository {

    /**
     * Ottieni informazioni tecnico correnti
     */
    fun getTechnicianInfo(): Flow<TechnicianInfo>

    /**
     * Aggiorna informazioni tecnico
     */
    suspend fun updateTechnicianInfo(technicianInfo: TechnicianInfo): Result<Unit>

    /**
     * Verifica se ci sono dati tecnico salvati
     */
    fun hasTechnicianData(): Flow<Boolean>

    /**
     * Reset alle impostazioni di default
     */
    suspend fun resetToDefault(): Result<Unit>

    /**
     * Export delle settings per backup
     */
    suspend fun exportForBackup(): Map<String, String>

    /**
     * Import delle settings da backup
     */
    suspend fun importFromBackup(backupData: Map<String, String>): Result<Unit>
}