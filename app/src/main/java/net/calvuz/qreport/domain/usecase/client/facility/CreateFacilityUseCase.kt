package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityType
import net.calvuz.qreport.domain.model.client.Address
import net.calvuz.qreport.domain.repository.FacilityRepository
import net.calvuz.qreport.domain.repository.ClientRepository
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per creazione di un nuovo stabilimento
 *
 * Gestisce:
 * - Validazione esistenza cliente
 * - Validazione dati stabilimento
 * - Controllo duplicati nome per cliente
 * - Gestione facility primaria
 * - Creazione con timestamp correnti
 */
class CreateFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val clientRepository: ClientRepository
) {

    /**
     * Crea un nuovo stabilimento per un cliente
     *
     * @param clientId ID del cliente
     * @param name Nome dello stabilimento
     * @param address Indirizzo completo
     * @param facilityType Tipologia stabilimento
     * @param code Codice interno (opzionale)
     * @param description Descrizione (opzionale)
     * @param isPrimary Se impostare come stabilimento principale
     * @return Result con ID del facility creato se successo, errore altrimenti
     */
    suspend operator fun invoke(
        clientId: String,
        name: String,
        address: Address,
        facilityType: FacilityType = FacilityType.PRODUCTION,
        code: String? = null,
        description: String? = null,
        isPrimary: Boolean = false
    ): Result<String> {
        return try {
            // 1. Validazione input
            validateInput(clientId, name, address).onFailure { return Result.failure(it) }

            // 2. Verifica esistenza cliente
            checkClientExists(clientId).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati nome per cliente
            checkFacilityNameUniqueness(clientId, name)
                .onFailure { return Result.failure(it) }

            // 4. Se è primario, gestisci il cambio di facility primaria
            if (isPrimary) {
                handlePrimaryFacilityChange(clientId)
                    .onFailure { return Result.failure(it) }
            }

            // 5. Creazione facility
            val facility = Facility(
                id = UUID.randomUUID().toString(),
                clientId = clientId,
                name = name.trim(),
                code = code?.trim()?.takeIf { it.isNotBlank() },
                description = description?.trim()?.takeIf { it.isNotBlank() },
                facilityType = facilityType,
                address = address,
                isPrimary = isPrimary,
                isActive = true,
                islands = emptyList(),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // 6. Salvataggio
            facilityRepository.createFacility(facility)
                .onFailure { return Result.failure(it) }

            Result.success(facility.id)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione input base
     */
    private fun validateInput(clientId: String, name: String, address: Address): Result<Unit> {
        return when {
            clientId.isBlank() ->
                Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))

            name.isBlank() ->
                Result.failure(IllegalArgumentException("Nome stabilimento è obbligatorio"))

            name.length < 2 ->
                Result.failure(IllegalArgumentException("Nome stabilimento deve essere di almeno 2 caratteri"))

            name.length > 100 ->
                Result.failure(IllegalArgumentException("Nome stabilimento troppo lungo (max 100 caratteri)"))

            !address.isComplete() ->
                Result.failure(IllegalArgumentException("Indirizzo stabilimento incompleto"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Verifica che il cliente esista ed è attivo
     */
    private suspend fun checkClientExists(clientId: String): Result<Unit> {
        return clientRepository.getClientById(clientId)
            .mapCatching { client ->
                when {
                    client == null ->
                        throw NoSuchElementException("Cliente con ID '$clientId' non trovato")
                    !client.isActive ->
                        throw IllegalStateException("Cliente con ID '$clientId' non attivo")
                }
            }
    }

    /**
     * Controllo univocità nome stabilimento per cliente
     */
    private suspend fun checkFacilityNameUniqueness(clientId: String, name: String): Result<Unit> {
        return facilityRepository.isFacilityNameTakenForClient(clientId, name)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Esiste già uno stabilimento '$name' per questo cliente")
                }
            }
    }

    /**
     * Gestisce il cambio di facility primaria prima della creazione
     */
    private suspend fun handlePrimaryFacilityChange(clientId: String): Result<Unit> {
        return try {
            // Se esiste già una facility primaria, rimuovi il flag
            val existingPrimary = facilityRepository.getPrimaryFacility(clientId)
                .getOrElse { return Result.success(Unit) } // Non c'è facility primaria esistente

            if (existingPrimary != null) {
                // Aggiorna la facility esistente rimuovendo il flag primario
                val updatedPrimary = existingPrimary.copy(
                    isPrimary = false,
                    updatedAt = Clock.System.now()
                )
                facilityRepository.updateFacility(updatedPrimary)
                    .onFailure { return Result.failure(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea stabilimento con dati minimi
     */
    suspend fun createMinimalFacility(
        clientId: String,
        name: String,
        city: String
    ): Result<String> {
        val address = Address(
            city = city,
            country = "Italia"
        )
        return invoke(
            clientId = clientId,
            name = name,
            address = address,
            facilityType = FacilityType.OTHER
        )
    }

    /**
     * Crea facility primario per nuovo cliente
     */
    suspend fun createPrimaryFacility(
        clientId: String,
        name: String,
        address: Address,
        facilityType: FacilityType = FacilityType.PRODUCTION
    ): Result<String> {
        return invoke(
            clientId = clientId,
            name = name,
            address = address,
            facilityType = facilityType,
            isPrimary = true
        )
    }
}