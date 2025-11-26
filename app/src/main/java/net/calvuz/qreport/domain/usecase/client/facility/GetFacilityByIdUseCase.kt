package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case per recuperare una singola facility by ID
 *
 * Ottimizzato per:
 * - Recupero veloce facility singola (senza isole)
 * - Validazione esistenza e permessi
 * - Flow reattivo per UI
 * - Helper methods per navigation
 */
class GetFacilityByIdUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {

    /**
     * Recupera una facility by ID
     *
     * @param facilityId ID della facility
     * @return Result con facility se trovata
     */
    suspend operator fun invoke(facilityId: String): Result<Facility?> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            facilityRepository.getFacilityById(facilityId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva una facility by ID (Flow reattivo)
     *
     * @param facilityId ID della facility
     * @return Flow con facility che si aggiorna automaticamente
     */
    fun observeFacility(facilityId: String): Flow<Facility?> {
        return facilityRepository.getFacilityByIdFlow(facilityId)
    }

    /**
     * ✅ HELPER per NAVIGATION: Recupera solo il nome della facility
     * Ottimizzato per le navigation routes che hanno bisogno solo del nome
     *
     * @param facilityId ID della facility
     * @return Result con nome display della facility
     */
    suspend fun getFacilityName(facilityId: String): Result<String> {
        return try {
            if (facilityId.isBlank()) {
                return Result.success("Stabilimento") // Fallback safety
            }

            invoke(facilityId).map { facility ->
                facility?.displayName ?: "Stabilimento"
            }
        } catch (_: Exception) {
            // Fallback in case of any error
            Result.success("Stabilimento")
        }
    }

    /**
     * ✅ HELPER per UI: Recupera info essenziali per display
     * Utile per header, breadcrumbs, navigation titles
     *
     * @param facilityId ID della facility
     * @return Result con info display della facility
     */
    suspend fun getFacilityDisplayInfo(facilityId: String): Result<FacilityDisplayInfo> {
        return try {
            if (facilityId.isBlank()) {
                return Result.success(FacilityDisplayInfo.default())
            }

            invoke(facilityId).map { facility ->
                if (facility != null) {
                    FacilityDisplayInfo(
                        id = facility.id,
                        name = facility.displayName,
                        type = facility.facilityType.displayName,
                        city = facility.address.city ?: "",
                        isPrimary = facility.isPrimary,
                        isActive = facility.isActive
                    )
                } else {
                    FacilityDisplayInfo.default()
                }
            }
        } catch (_: Exception) {
            Result.success(FacilityDisplayInfo.default())
        }
    }

    /**
     * Verifica se una facility esiste ed è attiva
     *
     * @param facilityId ID della facility
     * @return Result con true se esiste ed è attiva
     */
    suspend fun existsAndActive(facilityId: String): Result<Boolean> {
        return invoke(facilityId).map { facility ->
            facility != null && facility.isActive
        }
    }

    /**
     * Recupera il client ID di una facility
     * Utile per navigation che richiede risalire al client
     *
     * @param facilityId ID della facility
     * @return Result con client ID se facility trovata
     */
    suspend fun getClientId(facilityId: String): Result<String?> {
        return invoke(facilityId).map { facility ->
            facility?.clientId
        }
    }

    /**
     * Verifica se la facility ha isole associate
     * Ottimizzato per evitare di caricare tutte le isole
     *
     * @param facilityId ID della facility
     * @return Result con true se ha isole
     */
    suspend fun hasIslands(facilityId: String): Result<Boolean> {
        return try {
            // Usa il count check dal repository che è più veloce
            facilityRepository.getIslandsCount(facilityId).map { count ->
                count > 0
            }
        } catch (_: Exception) {
            Result.success(false) // Default safe
        }
    }
}

/**
 * Info essenziali di una facility per display UI
 * Ottimizzato per navigation e header components
 */
data class FacilityDisplayInfo(
    val id: String,
    val name: String,
    val type: String,
    val city: String,
    val isPrimary: Boolean,
    val isActive: Boolean
) {
    companion object {
        fun default() = FacilityDisplayInfo(
            id = "",
            name = "Stabilimento",
            type = "",
            city = "",
            isPrimary = false,
            isActive = true
        )
    }

    val statusBadge: String
        get() = when {
            isPrimary -> "Primario"
            isActive -> "Attivo"
            else -> "Inattivo"
        }

    val subtitle: String
        get() = buildString {
            if (type.isNotBlank()) append(type)
            if (city.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(city)
            }
        }
}