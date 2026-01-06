package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.island.domain.validator.IslandDataValidator
import javax.inject.Inject

/**
 * Use Case per aggiornamento di un'isola robotizzata esistente
 *
 * Gestisce:
 * - Validazione esistenza isola
 * - Validazione dati aggiornati
 * - Controllo duplicati serial number escludendo l'isola corrente
 * - Validazione coerenza date manutenzione
 * - Aggiornamento timestamp
 * - Calcolo automatico ore operative
 */
class UpdateIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val validateIslandData: IslandDataValidator,
    private val checkIslandExists: CheckIslandExistsUseCase,
    private val checkSerialNumberUniqueness: CheckSerialNumberUniquenessUseCase
) {

    /**
     * Aggiorna un'isola robotizzata esistente
     *
     * @param island Isola con dati aggiornati (deve avere ID esistente)
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(island: Island): Result<Unit> {
        return try {
            // 1. Validazione esistenza isola
            val originalIsland = checkIslandExists(island.id)
                .getOrElse { return Result.failure(it) }

            // 2. Validazione dati aggiornati
            validateIslandData(island).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati serial number (se cambiato)
            if (island.serialNumber != originalIsland.serialNumber) {
                checkSerialNumberUniqueness(island.serialNumber)
                    .onFailure { return Result.failure(it) }
            }

            // 4. Validazione coerenza date manutenzione
            validateMaintenanceDates(island).onFailure { return Result.failure(it) }

            // 5. Validazione che facility_id non sia cambiato (business rule)
            if (island.facilityId != originalIsland.facilityId) {
                return Result.failure(
                    IllegalArgumentException("Non è possibile cambiare la facility di un'isola esistente")
                )
            }

            // 6. Calcolo ore operative se necessario
            val finalIsland = calculateOperatingHoursIfNeeded(originalIsland, island)

            // 7. Aggiornamento con timestamp corrente
            val updatedIsland = finalIsland.copy(updatedAt = Clock.System.now())
            islandRepository.updateIsland(updatedIsland)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Validazione coerenza date manutenzione
     */
    private fun validateMaintenanceDates(island: Island): Result<Unit> {
        val now = Clock.System.now()

        return when {
            // Data installazione non può essere nel futuro
            island.installationDate?.let { it > now } == true ->
                Result.failure(IllegalArgumentException("Data installazione non può essere nel futuro"))

            // Garanzia non può essere precedente all'installazione
            island.warrantyExpiration?.let {
                island.installationDate?.let { install -> it < install }
            } == true ->
                Result.failure(IllegalArgumentException("Scadenza garanzia non può essere precedente all'installazione"))

            // Ultima manutenzione non può essere precedente all'installazione
            island.lastMaintenanceDate?.let {
                island.installationDate?.let { install -> it < install }
            } == true ->
                Result.failure(IllegalArgumentException("Ultima manutenzione non può essere precedente all'installazione"))

            // Ultima manutenzione non può essere nel futuro
            island.lastMaintenanceDate?.let { it > now } == true ->
                Result.failure(IllegalArgumentException("Ultima manutenzione non può essere nel futuro"))

            // Prossima manutenzione deve essere successiva all'ultima
            island.nextScheduledMaintenance?.let { next ->
                island.lastMaintenanceDate?.let { last -> next <= last }
            } == true ->
                Result.failure(IllegalArgumentException("Prossima manutenzione deve essere successiva all'ultima"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Calcola ore operative incrementali se necessario
     */
    private fun calculateOperatingHoursIfNeeded(
        originalIsland: Island,
        updatedIsland: Island
    ): Island {
        // Se le ore operative sono diminuite, potrebbe essere un reset dopo manutenzione
        if (updatedIsland.operatingHours < originalIsland.operatingHours) {
            // Consenti solo se c'è stata una nuova manutenzione
            val hasNewMaintenance = updatedIsland.lastMaintenanceDate != null &&
                    originalIsland.lastMaintenanceDate != updatedIsland.lastMaintenanceDate

            if (!hasNewMaintenance) {
                // Mantieni le ore originali se non è un reset legittimo
                return updatedIsland.copy(operatingHours = originalIsland.operatingHours)
            }
        }

        return updatedIsland
    }
}