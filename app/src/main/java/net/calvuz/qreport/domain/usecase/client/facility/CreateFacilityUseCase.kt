package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityType
import net.calvuz.qreport.domain.model.client.Address
import net.calvuz.qreport.domain.repository.FacilityRepository
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.usecase.client.client.CheckClientExistsUseCase
import net.calvuz.qreport.domain.validator.FacilityDataValidator
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
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkFacilityNameUniqueness: CheckFacilityNameUniquenessUseCase,
    private val validateInput: FacilityDataValidator,
    private val handlePrimaryFacilityChange: HandlePrimaryFacilityChangeUseCase
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
}