package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.model.island.IslandInfo

/**
 * Isola robotizzata associata a uno stabilimento
 * Combina IslandType e IslandInfo esistenti per gestione client-centric
 */
@Serializable
data class FacilityIsland(
    val id: String,
    val facilityId: String,

    // ===== TIPO ISOLA =====
    val islandType: IslandType,           // ✅ Riutilizzo enum esistente

    // ===== DETTAGLI TECNICI =====
    val serialNumber: String,             // Numero seriale unico
    val model: String? = null,
    val installationDate: Instant? = null,
    val warrantyExpiration: Instant? = null,

    // ===== STATO OPERATIVO =====
    val isActive: Boolean = true,
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L,
    val lastMaintenanceDate: Instant? = null,
    val nextScheduledMaintenance: Instant? = null,

    // ===== CONFIGURAZIONE =====
    val customName: String? = null,       // Nome personalizzato dal cliente
    val location: String? = null,         // Posizione nello stabilimento
    val notes: String? = null,

    // ===== METADATI =====
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Nome display dell'isola
     */
    val displayName: String
        get() = customName ?: "${islandType.displayName} (${serialNumber})"

    /**
     * Conversione a IslandInfo per compatibilità CheckUp
     */
    fun toIslandInfo(): IslandInfo = IslandInfo(
        serialNumber = serialNumber,
        model = model ?: "",
        installationDate = installationDate?.toString() ?: "",
        lastMaintenanceDate = lastMaintenanceDate?.toString() ?: "",
        operatingHours = operatingHours,
        cycleCount = cycleCount
    )

    /**
     * Verifica se isola ha bisogno di manutenzione
     */
    fun needsMaintenance(): Boolean {
        return nextScheduledMaintenance?.let { next ->
            next <= kotlinx.datetime.Clock.System.now()
        } ?: false
    }

    /**
     * Verifica se isola è sotto garanzia
     */
    fun isUnderWarranty(): Boolean {
        return warrantyExpiration?.let { expiry ->
            expiry > kotlinx.datetime.Clock.System.now()
        } ?: false
    }

    /**
     * Stato operativo descrittivo
     */
    val facilityIslandOperationalStatus: FacilityIslandOperationalStatus
        get() = when {
            !isActive -> FacilityIslandOperationalStatus.INACTIVE
            needsMaintenance() -> FacilityIslandOperationalStatus.MAINTENANCE_DUE
            else -> FacilityIslandOperationalStatus.OPERATIONAL
        }

    /**
     * Calcola giorni dalla prossima manutenzione (positivo se in anticipo, negativo se in ritardo)
     */
    fun daysToNextMaintenance(): Long? {
        return nextScheduledMaintenance?.let { next ->
            val now = kotlinx.datetime.Clock.System.now()
            val duration = next - now
            duration.inWholeDays
        }
    }

    /**
     * Formattazione per display dello stato manutenzione
     */
    val maintenanceStatusText: String
        get() {
            return when {
                !isActive -> "Inattiva"
                nextScheduledMaintenance == null -> "Nessuna manutenzione programmata"
                else -> {
                    val days = daysToNextMaintenance() ?: return "Data manutenzione non valida"
                    when {
                        days > 30 -> "Manutenzione tra ${days} giorni"
                        days > 0 -> "Manutenzione tra ${days} giorni ⚠️"
                        days == 0L -> "Manutenzione oggi! 🔴"
                        days > -7 -> "Manutenzione in ritardo di ${-days} giorni 🔴"
                        else -> "Manutenzione in grave ritardo (${-days} giorni) 🚨"
                    }
                }
            }
        }
}
