package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * Use Case per aggiornamento di uno stabilimento esistente
 *
 * Gestisce:
 * - Validazione esistenza facility
 * - Validazione dati aggiornati
 * - Controllo duplicati nome escludendo facility corrente
 * - Gestione cambio facility primaria
 * - Aggiornamento timestamp
 */
class UpdateFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {

    /**
     * Aggiorna uno stabilimento esistente
     *
     * @param facility Facility con dati aggiornati (deve avere ID esistente)
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(facility: Facility): Result<Unit> {
        return try {
            // 1. Validazione esistenza facility
            val originalFacility = checkFacilityExists(facility.id)
                .getOrElse { return Result.failure(it) }

            // 2. Validazione dati aggiornati
            validateFacilityData(facility).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati nome (se cambiato)
            if (facility.name != originalFacility.name) {
                checkFacilityNameUniqueness(facility.clientId, facility.name, facility.id)
                    .onFailure { return Result.failure(it) }
            }

            // 4. Validazione che clientId non sia cambiato (business rule)
            if (facility.clientId != originalFacility.clientId) {
                return Result.failure(
                    IllegalArgumentException("Non è possibile cambiare il cliente di uno stabilimento esistente")
                )
            }

            // 5. Gestione cambio facility primaria
            if (facility.isPrimary != originalFacility.isPrimary) {
                handlePrimaryFacilityChange(facility, originalFacility)
                    .onFailure { return Result.failure(it) }
            }

            // 6. Aggiornamento con timestamp corrente
            val updatedFacility = facility.copy(updatedAt = Clock.System.now())
            facilityRepository.updateFacility(updatedFacility)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che la facility esista e la restituisce
     */
    private suspend fun checkFacilityExists(facilityId: String): Result<Facility> {
        return facilityRepository.getFacilityById(facilityId)
            .mapCatching { facility ->
                facility ?: throw NoSuchElementException("Stabilimento con ID '$facilityId' non trovato")
            }
    }

    /**
     * Validazione dati facility
     */
    private fun validateFacilityData(facility: Facility): Result<Unit> {
        return when {
            facility.id.isBlank() ->
                Result.failure(IllegalArgumentException("ID facility è obbligatorio"))

            facility.clientId.isBlank() ->
                Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))

            facility.name.isBlank() ->
                Result.failure(IllegalArgumentException("Nome stabilimento è obbligatorio"))

            facility.name.length < 2 ->
                Result.failure(IllegalArgumentException("Nome stabilimento deve essere di almeno 2 caratteri"))

            facility.name.length > 100 ->
                Result.failure(IllegalArgumentException("Nome stabilimento troppo lungo (max 100 caratteri)"))

            (facility.code?.length ?: 0) > 50 ->
                Result.failure(IllegalArgumentException("Codice interno troppo lungo (max 50 caratteri)"))

            (facility.description?.length ?: 0) > 500 ->
                Result.failure(IllegalArgumentException("Descrizione troppo lunga (max 500 caratteri)"))

            !facility.address.isComplete() ->
                Result.failure(IllegalArgumentException("Indirizzo stabilimento incompleto"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Controllo univocità nome facility escludendo facility corrente
     */
    private suspend fun checkFacilityNameUniqueness(
        clientId: String,
        name: String,
        excludeId: String
    ): Result<Unit> {
        return facilityRepository.isFacilityNameTakenForClient(clientId, name, excludeId)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Nome stabilimento '$name' già utilizzato da un altro stabilimento")
                }
            }
    }

    /**
     * Gestisce i cambiamenti dello stato primario
     */
    private suspend fun handlePrimaryFacilityChange(
        newFacility: Facility,
        originalFacility: Facility
    ): Result<Unit> {
        return when {
            // Diventa primario
            newFacility.isPrimary && !originalFacility.isPrimary -> {
                facilityRepository.setPrimaryFacility(newFacility.clientId, newFacility.id)
            }

            // Non è più primario - verifica che ci sia almeno un altro stabilimento
            !newFacility.isPrimary && originalFacility.isPrimary -> {
                validateCanRemovePrimary(newFacility.clientId, newFacility.id)
            }

            else -> Result.success(Unit)
        }
    }

    /**
     * Verifica che si possa rimuovere il flag primario
     */
    private suspend fun validateCanRemovePrimary(clientId: String, facilityId: String): Result<Unit> {
        return facilityRepository.getFacilitiesByClient(clientId)
            .mapCatching { facilities ->
                val otherActiveFacilities = facilities.filter {
                    it.id != facilityId && it.isActive
                }

                if (otherActiveFacilities.isEmpty()) {
                    throw IllegalStateException(
                        "Non è possibile rimuovere il flag primario: non ci sono altri stabilimenti attivi"
                    )
                }

                // Automaticamente imposta il primo come primario
                val newPrimary = otherActiveFacilities.first()
                facilityRepository.setPrimaryFacility(clientId, newPrimary.id)
                    .getOrThrow()
            }
    }

    /**
     * Aggiorna solo campi specifici di una facility
     *
     * @param facilityId ID della facility da aggiornare
     * @param updates Mappa campo -> nuovo valore
     * @return Result con Unit se successo
     */
    suspend fun updateFacilityFields(
        facilityId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            val originalFacility = checkFacilityExists(facilityId)
                .getOrElse { return Result.failure(it) }

            var updatedFacility = originalFacility

            updates.forEach { (field, value) ->
                updatedFacility = when (field) {
                    "name" -> updatedFacility.copy(name = value as String)
                    "code" -> updatedFacility.copy(code = value as? String)
                    "description" -> updatedFacility.copy(description = value as? String)
                    "isPrimary" -> updatedFacility.copy(isPrimary = value as Boolean)
                    "isActive" -> updatedFacility.copy(isActive = value as Boolean)
                    else -> throw IllegalArgumentException("Campo '$field' non supportato per aggiornamento")
                }
            }

            invoke(updatedFacility)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Attiva o disattiva una facility
     *
     * @param facilityId ID della facility
     * @param isActive Nuovo stato attivo/inattivo
     * @return Result con Unit se successo
     */
    suspend fun updateFacilityStatus(facilityId: String, isActive: Boolean): Result<Unit> {
        return try {
            val facility = checkFacilityExists(facilityId)
                .getOrElse { return Result.failure(it) }

            if (facility.isActive == isActive) {
                return Result.success(Unit) // Nessun cambio necessario
            }

            // Se si disattiva la facility primaria, deve esserci un'altra facility attiva
            if (!isActive && facility.isPrimary) {
                validateCanDeactivatePrimary(facility.clientId, facilityId)
                    .onFailure { return Result.failure(it) }
            }

            val updatedFacility = facility.copy(
                isActive = isActive,
                // Se si disattiva, rimuovi anche il flag primario
                isPrimary = if (!isActive) false else facility.isPrimary,
                updatedAt = Clock.System.now()
            )

            invoke(updatedFacility)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che si possa disattivare una facility primaria
     */
    private suspend fun validateCanDeactivatePrimary(clientId: String, facilityId: String): Result<Unit> {
        return facilityRepository.getFacilitiesByClient(clientId)
            .mapCatching { facilities ->
                val otherActiveFacilities = facilities.filter {
                    it.id != facilityId && it.isActive
                }

                if (otherActiveFacilities.isEmpty()) {
                    throw IllegalStateException(
                        "Non è possibile disattivare l'ultima facility attiva per questo cliente"
                    )
                }

                // Automaticamente imposta la prima come primaria
                val newPrimary = otherActiveFacilities.first()
                facilityRepository.setPrimaryFacility(clientId, newPrimary.id)
                    .getOrThrow()
            }
    }
}