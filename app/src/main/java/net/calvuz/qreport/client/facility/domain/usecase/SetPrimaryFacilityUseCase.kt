package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * Use Case per impostazione facility primaria
 *
 * Gestisce:
 * - Validazione esistenza facility target
 * - Rimozione flag primario da facility corrente
 * - Impostazione nuovo facility primario
 * - Aggiornamento timestamp
 * - Business rules validation
 */
class SetPrimaryFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {

    /**
     * Imposta una facility come primaria per un cliente
     *
     * @param clientId ID del cliente
     * @param facilityId ID della facility da impostare come primaria
     * @return Result con Unit se successo, errore se fallimento
     */
    suspend operator fun invoke(clientId: String, facilityId: String): Result<Unit> {
        return try {
            // 1. Validazione input
            validateInput(clientId, facilityId).onFailure { return Result.failure(it) }

            // 2. Verifica esistenza e validità facility target
            val targetFacility = validateTargetFacility(clientId, facilityId)
                .getOrElse { return Result.failure(it) }

            // 3. Se è già primaria, non fare nulla
            if (targetFacility.isPrimary) {
                return Result.success(Unit)
            }

            // 4. Rimuovi flag primario dalla facility corrente (se esiste)
            removeCurrentPrimaryFlag(clientId)
                .onFailure { return Result.failure(it) }

            // 5. Imposta nuova facility come primaria
            val updatedFacility = targetFacility.copy(
                isPrimary = true,
                updatedAt = Clock.System.now()
            )

            facilityRepository.updateFacility(updatedFacility)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione input base
     */
    private fun validateInput(clientId: String, facilityId: String): Result<Unit> {
        return when {
            clientId.isBlank() ->
                Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))

            facilityId.isBlank() ->
                Result.failure(IllegalArgumentException("ID facility è obbligatorio"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Verifica che la facility target sia valida per diventare primaria
     */
    private suspend fun validateTargetFacility(clientId: String, facilityId: String): Result<Facility> {
        return facilityRepository.getFacilityById(facilityId)
            .mapCatching { facility ->
                when {
                    facility == null ->
                        throw NoSuchElementException("Facility con ID '$facilityId' non trovata")

                    facility.clientId != clientId ->
                        throw IllegalArgumentException("La facility non appartiene al cliente specificato")

                    !facility.isActive ->
                        throw IllegalStateException("Non è possibile impostare come primaria una facility non attiva")

                    else -> facility
                }
            }
    }

    /**
     * Rimuove il flag primario dalla facility attualmente primaria
     */
    private suspend fun removeCurrentPrimaryFlag(clientId: String): Result<Unit> {
        return try {
            val currentPrimary = facilityRepository.getPrimaryFacility(clientId)
                .getOrElse { return Result.success(Unit) } // Nessuna facility primaria esistente

            if (currentPrimary != null) {
                val updatedCurrent = currentPrimary.copy(
                    isPrimary = false,
                    updatedAt = Clock.System.now()
                )
                facilityRepository.updateFacility(updatedCurrent)
                    .onFailure { return Result.failure(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rimuove il flag primario da tutte le facilities di un cliente
     * Utile per operazioni di manutenzione
     */
    suspend fun clearPrimaryFacility(clientId: String): Result<Unit> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))
            }

            val currentPrimary = facilityRepository.getPrimaryFacility(clientId)
                .getOrElse { return Result.success(Unit) } // Nessuna facility primaria

            if (currentPrimary != null) {
                val updatedFacility = currentPrimary.copy(
                    isPrimary = false,
                    updatedAt = Clock.System.now()
                )
                facilityRepository.updateFacility(updatedFacility)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imposta automaticamente la prima facility attiva come primaria
     * Utile quando non c'è nessuna facility primaria
     */
    suspend fun autoSetPrimaryFacility(clientId: String): Result<String?> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))
            }

            // Verifica che non ci sia già una facility primaria
            val existingPrimary = facilityRepository.getPrimaryFacility(clientId)
                .getOrThrow()

            if (existingPrimary != null) {
                return Result.success(existingPrimary.id) // Già esiste una primaria
            }

            // Trova la prima facility attiva
            val activeFacilities = facilityRepository.getActiveFacilitiesByClient(clientId)
                .getOrThrow()

            if (activeFacilities.isEmpty()) {
                return Result.failure(
                    IllegalStateException("Nessuna facility attiva trovata per il cliente")
                )
            }

            // Imposta la prima come primaria (ordinata per nome)
            val firstFacility = activeFacilities.minByOrNull { it.name }!!
            invoke(clientId, firstFacility.id)
                .onFailure { return Result.failure(it) }

            Result.success(firstFacility.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scambia il ruolo primario tra due facilities
     */
    suspend fun swapPrimaryFacility(
        clientId: String,
        currentPrimaryId: String,
        newPrimaryId: String
    ): Result<Unit> {
        return try {
            // Validazione input
            if (clientId.isBlank() || currentPrimaryId.isBlank() || newPrimaryId.isBlank()) {
                return Result.failure(IllegalArgumentException("Tutti gli ID sono obbligatori"))
            }

            if (currentPrimaryId == newPrimaryId) {
                return Result.success(Unit) // Stessa facility
            }

            // Verifica che currentPrimaryId sia effettivamente primaria
            val currentPrimary = facilityRepository.getFacilityById(currentPrimaryId)
                .getOrThrow() ?: return Result.failure(
                NoSuchElementException("Facility corrente primaria non trovata")
            )

            if (!currentPrimary.isPrimary || currentPrimary.clientId != clientId) {
                return Result.failure(
                    IllegalArgumentException("La facility specificata non è primaria per questo cliente")
                )
            }

            // Imposta la nuova primaria (gestisce automaticamente la rimozione della vecchia)
            invoke(clientId, newPrimaryId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottieni informazioni sullo stato primario delle facilities di un cliente
     */
    suspend fun getPrimaryFacilityStatus(clientId: String): Result<PrimaryFacilityStatus> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))
            }

            val allFacilities = facilityRepository.getFacilitiesByClient(clientId)
                .getOrThrow()

            val activeFacilities = allFacilities.filter { it.isActive }
            val primaryFacility = allFacilities.find { it.isPrimary }

            val status = PrimaryFacilityStatus(
                clientId = clientId,
                primaryFacility = primaryFacility,
                totalFacilities = allFacilities.size,
                activeFacilities = activeFacilities.size,
                hasPrimaryFacility = primaryFacility != null,
                isPrimaryFacilityActive = primaryFacility?.isActive == true,
                canSetPrimary = activeFacilities.isNotEmpty(),
                availableForPrimary = activeFacilities.filter { !it.isPrimary }
            )

            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Status dello stato primario per un cliente
 */
data class PrimaryFacilityStatus(
    val clientId: String,
    val primaryFacility: Facility?,
    val totalFacilities: Int,
    val activeFacilities: Int,
    val hasPrimaryFacility: Boolean,
    val isPrimaryFacilityActive: Boolean,
    val canSetPrimary: Boolean,
    val availableForPrimary: List<Facility>
) {
    val needsPrimaryFacility: Boolean = !hasPrimaryFacility && activeFacilities > 0
    val primaryFacilityName: String? = primaryFacility?.displayName
}