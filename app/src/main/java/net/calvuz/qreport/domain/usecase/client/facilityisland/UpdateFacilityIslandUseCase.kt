package net.calvuz.qreport.domain.usecase.client.facilityisland

import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import kotlinx.datetime.Clock
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
class UpdateFacilityIslandUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Aggiorna un'isola robotizzata esistente
     *
     * @param island Isola con dati aggiornati (deve avere ID esistente)
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(island: FacilityIsland): Result<Unit> {
        return try {
            // 1. Validazione esistenza isola
            val originalIsland = checkIslandExists(island.id)
                .getOrElse { return Result.failure(it) }

            // 2. Validazione dati aggiornati
            validateIslandData(island).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati serial number (se cambiato)
            if (island.serialNumber != originalIsland.serialNumber) {
                checkSerialNumberUniqueness(island.id, island.serialNumber)
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
            facilityIslandRepository.updateIsland(updatedIsland)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che l'isola esista e la restituisce
     */
    private suspend fun checkIslandExists(islandId: String): Result<FacilityIsland> {
        return facilityIslandRepository.getIslandById(islandId)
            .mapCatching { island ->
                island ?: throw NoSuchElementException("Isola con ID '$islandId' non trovata")
            }
    }

    /**
     * Validazione dati isola
     */
    private fun validateIslandData(island: FacilityIsland): Result<Unit> {
        return when {
            island.id.isBlank() ->
                Result.failure(IllegalArgumentException("ID isola è obbligatorio"))

            island.facilityId.isBlank() ->
                Result.failure(IllegalArgumentException("ID facility è obbligatorio"))

            island.serialNumber.isBlank() ->
                Result.failure(IllegalArgumentException("Serial number è obbligatorio"))

            island.serialNumber.length < 3 ->
                Result.failure(IllegalArgumentException("Serial number deve essere di almeno 3 caratteri"))

            island.serialNumber.length > 50 ->
                Result.failure(IllegalArgumentException("Serial number troppo lungo (max 50 caratteri)"))

            !isValidSerialNumber(island.serialNumber) ->
                Result.failure(IllegalArgumentException("Formato serial number non valido (solo lettere, numeri, trattini)"))

            (island.model?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Modello troppo lungo (max 100 caratteri)"))

            (island.customName?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Nome personalizzato troppo lungo (max 100 caratteri)"))

            (island.location?.length ?: 0) > 200 ->
                Result.failure(IllegalArgumentException("Ubicazione troppo lunga (max 200 caratteri)"))

            island.operatingHours < 0 ->
                Result.failure(IllegalArgumentException("Ore operative non possono essere negative"))

            island.cycleCount < 0 ->
                Result.failure(IllegalArgumentException("Conteggio cicli non può essere negativo"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Controllo univocità serial number escludendo l'isola corrente
     */
    private suspend fun checkSerialNumberUniqueness(islandId: String, serialNumber: String): Result<Unit> {
        return facilityIslandRepository.isSerialNumberTaken(serialNumber, islandId)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Serial number '$serialNumber' già utilizzato da un'altra isola")
                }
            }
    }

    /**
     * Validazione coerenza date manutenzione
     */
    private fun validateMaintenanceDates(island: FacilityIsland): Result<Unit> {
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
        originalIsland: FacilityIsland,
        updatedIsland: FacilityIsland
    ): FacilityIsland {
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

    /**
     * Validazione formato serial number
     */
    private fun isValidSerialNumber(serialNumber: String): Boolean {
        return serialNumber.matches("[A-Za-z0-9\\-_]+".toRegex())
    }

    /**
     * Aggiorna solo le ore operative e conteggio cicli di un'isola
     *
     * @param islandId ID dell'isola
     * @param additionalHours Ore da aggiungere
     * @param additionalCycles Cicli da aggiungere
     * @return Result con Unit se successo
     */
    suspend fun updateOperationalData(
        islandId: String,
        additionalHours: Int,
        additionalCycles: Long = 0
    ): Result<Unit> {
        return try {
            if (additionalHours < 0 || additionalCycles < 0) {
                return Result.failure(
                    IllegalArgumentException("Ore e cicli aggiuntivi non possono essere negativi")
                )
            }

            // Aggiorna tramite repository (più efficiente)
            facilityIslandRepository.updateOperatingHours(islandId, additionalHours)
                .onFailure { return Result.failure(it) }

            if (additionalCycles > 0) {
                facilityIslandRepository.updateCycleCount(islandId, additionalCycles)
                    .onFailure { return Result.failure(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna solo campi specifici di un'isola
     *
     * @param islandId ID dell'isola da aggiornare
     * @param updates Mappa campo -> nuovo valore
     * @return Result con Unit se successo
     */
    suspend fun updateIslandFields(
        islandId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            val originalIsland = checkIslandExists(islandId)
                .getOrElse { return Result.failure(it) }

            var updatedIsland = originalIsland

            updates.forEach { (field, value) ->
                updatedIsland = when (field) {
                    "serialNumber" -> updatedIsland.copy(serialNumber = value as String)
                    "model" -> updatedIsland.copy(model = value as? String)
                    "customName" -> updatedIsland.copy(customName = value as? String)
                    "location" -> updatedIsland.copy(location = value as? String)
                    "notes" -> updatedIsland.copy(notes = value as? String)
                    "operatingHours" -> updatedIsland.copy(operatingHours = value as Int)
                    "cycleCount" -> updatedIsland.copy(cycleCount = value as Long)
                    "isActive" -> updatedIsland.copy(isActive = value as Boolean)
                    else -> throw IllegalArgumentException("Campo '$field' non supportato per aggiornamento")
                }
            }

            invoke(updatedIsland)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Attiva o disattiva un'isola
     *
     * @param islandId ID dell'isola
     * @param isActive Nuovo stato attivo/inattivo
     * @return Result con Unit se successo
     */
    suspend fun updateIslandStatus(islandId: String, isActive: Boolean): Result<Unit> {
        return try {
            val island = checkIslandExists(islandId)
                .getOrElse { return Result.failure(it) }

            if (island.isActive == isActive) {
                return Result.success(Unit) // Nessun cambio necessario
            }

            val updatedIsland = island.copy(
                isActive = isActive,
                updatedAt = Clock.System.now()
            )

            facilityIslandRepository.updateIsland(updatedIsland)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}