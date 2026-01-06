package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.facility.domain.usecase.CheckFacilityExistsUseCase
import net.calvuz.qreport.client.island.domain.validator.IslandDataValidator
import kotlin.time.Duration.Companion.days
import javax.inject.Inject

/**
 * Use Case per creazione di una nuova isola robotizzata
 *
 * Gestisce:
 * - Validazione dati isola
 * - Verifica esistenza facility
 * - Controllo duplicati serial number
 * - Validazione date manutenzione e garanzia
 * - Calcolo automatico prossima manutenzione
 *
 * Business Rules:
 * - Serial number deve essere univoco globalmente
 * - Facility deve esistere ed essere attiva
 * - Date manutenzione devono essere coerenti
 * - Garanzia non può essere scaduta alla creazione
 */
class CreateIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val validateIslandData: IslandDataValidator,
    private val checkFacilityExists: CheckFacilityExistsUseCase,
    private val checkSerialNumberUniqueness: CheckSerialNumberUniquenessUseCase
) {

    /**
     * Crea una nuova isola robotizzata
     *
     * @param island Isola da creare
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(island: Island): Result<Unit> {
        return try {
            // 1. Validazione dati base
            validateIslandData(island).onFailure { return Result.failure(it) }

            // 2. Verifica che la facility esista ed è attiva
            checkFacilityExists(island.facilityId).onFailure { return Result.failure(it) }

            // 3. Controllo duplicati serial number
            checkSerialNumberUniqueness(island.serialNumber).onFailure { return Result.failure(it) }

            // 4. Validazione date manutenzione e garanzia
            validateMaintenanceDates(island).onFailure { return Result.failure(it) }

            // 5. Calcolo automatico prossima manutenzione se non specificata
            val finalIsland = calculateNextMaintenanceIfNeeded(island)

            // 6. Creazione nel repository
            islandRepository.createIsland(finalIsland)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione date manutenzione e garanzia
     */
    private fun validateMaintenanceDates(island: Island): Result<Unit> {
        val now = Clock.System.now()

        return when {
            // Validazione data installazione
            island.installationDate?.let { it > now } == true ->
                Result.failure(IllegalArgumentException("Data installazione non può essere nel futuro"))

            // Validazione garanzia
            island.warrantyExpiration?.let {
                island.installationDate?.let { install -> it < install }
            } == true ->
                Result.failure(IllegalArgumentException("Data scadenza garanzia non può essere precedente all'installazione"))

            // Validazione ultima manutenzione
            island.lastMaintenanceDate?.let {
                island.installationDate?.let { install -> it < install }
            } == true ->
                Result.failure(IllegalArgumentException("Data ultima manutenzione non può essere precedente all'installazione"))

            island.lastMaintenanceDate?.let { it > now } == true ->
                Result.failure(IllegalArgumentException("Data ultima manutenzione non può essere nel futuro"))

            // Validazione prossima manutenzione
            island.nextScheduledMaintenance?.let { next ->
                island.lastMaintenanceDate?.let { last -> next <= last }
            } == true ->
                Result.failure(IllegalArgumentException("Prossima manutenzione deve essere successiva all'ultima manutenzione"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Calcola automaticamente la prossima manutenzione se non specificata
     */
    private fun calculateNextMaintenanceIfNeeded(island: Island): Island {
        // Se non è specificata la prossima manutenzione, calcolala automaticamente
        if (island.nextScheduledMaintenance == null) {
            val baseDate = island.lastMaintenanceDate ?: island.installationDate ?: Clock.System.now()
            val maintenanceInterval = getMaintenanceIntervalForType(island.islandType)

            return island.copy(
                nextScheduledMaintenance = baseDate + maintenanceInterval.days
            )
        }

        return island
    }

    /**
     * Ottiene l'intervallo di manutenzione standard per tipo di isola (in giorni)
     */
    private fun getMaintenanceIntervalForType(islandType: IslandType): Int {
        return when (islandType) {
            IslandType.POLY_MOVE -> 90 // 90 giorni
            IslandType.POLY_CAST -> 120 // 120 giorni
            IslandType.POLY_EBT -> 60 // 60 giorni
            IslandType.POLY_TAG_BLE -> 180 // 180 giorni
            IslandType.POLY_TAG_FC -> 180 // 180 giorni
            IslandType.POLY_TAG_V -> 150 // 150 giorni
            IslandType.POLY_SAMPLE -> 30 // 30 giorni (più frequente)
        }
    }
}