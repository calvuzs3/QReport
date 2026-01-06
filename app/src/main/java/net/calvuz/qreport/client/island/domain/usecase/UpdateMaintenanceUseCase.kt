package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Use Case specifico per gestione manutenzione isole robotizzate
 *
 * Gestisce:
 * - Registrazione manutenzione completata
 * - Calcolo automatico prossima manutenzione
 * - Reset ore operative dopo manutenzione
 * - Aggiornamento stato operativo
 * - Tracciamento cronologia manutenzioni
 *
 * Business Rules:
 * - Manutenzione completata resetta il contatore ore
 * - Prossima manutenzione calcolata in base al tipo isola
 * - Date manutenzione devono essere coerenti
 * - Solo isole attive possono ricevere manutenzione
 */
class UpdateMaintenanceUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val checkIslandExists: CheckIslandExistsUseCase
) {

    /**
     * Registra una manutenzione completata per un'isola
     *
     * @param islandId ID dell'isola
     * @param maintenanceDate Data completamento manutenzione (default: ora corrente)
     * @param resetOperatingHours Se resettare le ore operative (default: true)
     * @param notes Note aggiuntive sulla manutenzione
     * @return Result con Unit se successo, errore se fallimento
     */
    suspend operator fun invoke(
        islandId: String,
        maintenanceDate: Instant = Clock.System.now(),
        resetOperatingHours: Boolean = true,
        notes: String? = null
    ): Result<Unit> {
        return try {
            // 1. Validazione input
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID isola non può essere vuoto"))
            }

            validateMaintenanceDate(maintenanceDate).onFailure { return Result.failure(it) }

            // 2. Verifica esistenza e stato isola
            val island = checkIslandExists(islandId)
                .getOrElse { return Result.failure(it) }

            // 3. Validazione che la manutenzione sia coerente
            validateMaintenanceLogic(
                island,
                maintenanceDate
            ).onFailure { return Result.failure(it) }

            // 4. Calcola prossima manutenzione
            val nextMaintenanceDate = calculateNextMaintenance(island.islandType, maintenanceDate)

            // 5. Aggiorna dati manutenzione
            islandRepository.updateMaintenanceDate(islandId, maintenanceDate)
                .onFailure { return Result.failure(it) }

            // 6. Aggiorna prossima manutenzione programmata
            islandRepository.updateIsland(
                island.copy(
                    lastMaintenanceDate = maintenanceDate,
                    nextScheduledMaintenance = nextMaintenanceDate,
                    operatingHours = if (resetOperatingHours) 0 else island.operatingHours,
                    notes = if (notes != null) {
                        val timestamp = maintenanceDate.toItalianDate()
                        val existingNotes =
                            island.notes?.takeIf { it.isNotBlank() }?.plus("\n") ?: ""
                        "${existingNotes}Manutenzione $timestamp: $notes"
                    } else island.notes,
                    updatedAt = Clock.System.now()
                )
            ).onFailure { return Result.failure(it) }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna solo la data della prossima manutenzione programmata
     *
     * @param islandId ID dell'isola
     * @param nextMaintenanceDate Data prossima manutenzione
     * @return Result con Unit se successo
     */
    suspend fun updateNextScheduledMaintenance(
        islandId: String,
        nextMaintenanceDate: Instant?
    ): Result<Unit> {
        return try {
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID isola non può essere vuoto"))
            }

            nextMaintenanceDate?.let { date ->
                validateMaintenanceDate(date).onFailure { return Result.failure(it) }

                if (date <= Clock.System.now()) {
                    return Result.failure(
                        IllegalArgumentException("Prossima manutenzione deve essere nel futuro")
                    )
                }
            }

            val island = checkIslandExists(islandId)
                .getOrElse { return Result.failure(it) }

            islandRepository.updateIsland(
                island.copy(
                    nextScheduledMaintenance = nextMaintenanceDate,
                    updatedAt = Clock.System.now()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione data manutenzione
     */
    private fun validateMaintenanceDate(maintenanceDate: Instant): Result<Unit> {
        val now = Clock.System.now()
        val maxFutureDate = now + (24.days) // Massimo 24 giorno nel futuro

        return when {
            maintenanceDate > maxFutureDate ->
                Result.failure(IllegalArgumentException("Data manutenzione non può essere troppo nel futuro"))

            maintenanceDate < (now - (365.days)) ->
                Result.failure(IllegalArgumentException("Data manutenzione non può essere più di 1 anno nel passato"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Validazione logica manutenzione
     */
    private fun validateMaintenanceLogic(
        island: Island,
        maintenanceDate: Instant
    ): Result<Unit> {
        return when {
            // La nuova manutenzione deve essere successiva alla precedente
            island.lastMaintenanceDate?.let { it >= maintenanceDate } == true ->
                Result.failure(
                    IllegalArgumentException("Data manutenzione deve essere successiva all'ultima manutenzione")
                )

            // La manutenzione non può essere precedente all'installazione
            island.installationDate?.let { it > maintenanceDate } == true ->
                Result.failure(
                    IllegalArgumentException("Data manutenzione non può essere precedente all'installazione")
                )

            else -> Result.success(Unit)
        }
    }

    /**
     * Calcola la data della prossima manutenzione in base al tipo di isola
     */
    private fun calculateNextMaintenance(
        islandType: IslandType,
        lastMaintenanceDate: Instant
    ): Instant {
        val interval = when (islandType) {
            IslandType.POLY_MOVE -> 90.days
            IslandType.POLY_CAST -> 120.days
            IslandType.POLY_EBT -> 90.days
            IslandType.POLY_TAG_BLE -> 180.days
            IslandType.POLY_TAG_FC -> 180.days
            IslandType.POLY_TAG_V -> 150.days
            IslandType.POLY_SAMPLE -> 90.days
        }

        return lastMaintenanceDate + interval
    }
}