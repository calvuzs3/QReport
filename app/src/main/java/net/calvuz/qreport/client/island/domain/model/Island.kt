package net.calvuz.qreport.client.island.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Robotic Island
 */
@Serializable
data class Island(
    val id: String,
    val facilityId: String,

    // ===== COMMISSIONING & NAME =====
    val commissioningNumber: String? = null,    // Commissioning phase Number or Code *
    val customName: String? = null,

    // ===== ISLAND TYPE =====
    val islandType: IslandType,

    // ===== TECHNICAL DETAILS =====
    val serialNumber: String,
    val modelNumber:String? = null,
    //DEPRECATED
    val model: String? = null,
    val installationDate: Instant? = null,
    val warrantyExpiration: Instant? = null,

    // ===== MAINTENANCE STATUS =====
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L,
    val lastMaintenanceDate: Instant? = null,
    val nextScheduledMaintenance: Instant? = null,

    // ===== CONFIGURATION =====
    val location: String? = null,
    val notes: String? = null,

    // ===== META =====
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Nome display dell'isola
     */
    val displayName: String
        get() = customName ?: "${islandType.displayName} (${serialNumber})"

    /**
     * Verifica se isola ha bisogno di manutenzione
     */
    fun needsMaintenance(): Boolean {
        return nextScheduledMaintenance?.let { next ->
            next <= Clock.System.now()
        } ?: false
    }

    /**
     * Verifica se isola è sotto garanzia
     */
    fun isUnderWarranty(): Boolean {
        return warrantyExpiration?.let { expiry ->
            expiry > Clock.System.now()
        } ?: false
    }

    /**
     * Stato operativo descrittivo
     */
    val islandOperationalStatus: IslandOperationalStatus
        get() = when {
            !isActive -> IslandOperationalStatus.INACTIVE
            needsMaintenance() -> IslandOperationalStatus.MAINTENANCE_DUE
            else -> IslandOperationalStatus.OPERATIONAL
        }

    /**
     * Calcola giorni dalla prossima manutenzione (positivo se in anticipo, negativo se in ritardo)
     */
    fun daysToNextMaintenance(): Long? {
        return nextScheduledMaintenance?.let { next ->
            val now = Clock.System.now()
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
