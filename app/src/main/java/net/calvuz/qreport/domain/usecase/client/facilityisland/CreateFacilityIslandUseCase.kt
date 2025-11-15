package net.calvuz.qreport.domain.usecase.client.facilityisland

import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import net.calvuz.qreport.domain.repository.FacilityRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
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
class CreateFacilityIslandUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository,
    private val facilityRepository: FacilityRepository
) {

    /**
     * Crea una nuova isola robotizzata
     *
     * @param island Isola da creare
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(island: FacilityIsland): Result<Unit> {
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
            facilityIslandRepository.createIsland(finalIsland)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validazione dati isola
     */
    private fun validateIslandData(island: FacilityIsland): Result<Unit> {
        return when {
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
     * Verifica che la facility esista ed è attiva
     */
    private suspend fun checkFacilityExists(facilityId: String): Result<Unit> {
        return facilityRepository.getFacilityById(facilityId)
            .mapCatching { facility ->
                when {
                    facility == null ->
                        throw NoSuchElementException("Facility con ID '$facilityId' non trovata")
                    !facility.isActive ->
                        throw IllegalStateException("Facility con ID '$facilityId' non attiva")
                }
            }
    }

    /**
     * Controllo univocità serial number
     */
    private suspend fun checkSerialNumberUniqueness(serialNumber: String): Result<Unit> {
        return facilityIslandRepository.isSerialNumberTaken(serialNumber)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Serial number '$serialNumber' già esistente")
                }
            }
    }

    /**
     * Validazione date manutenzione e garanzia
     */
    private fun validateMaintenanceDates(island: FacilityIsland): Result<Unit> {
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
    private fun calculateNextMaintenanceIfNeeded(island: FacilityIsland): FacilityIsland {
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

    /**
     * Validazione formato serial number (alfanumerico + trattini)
     */
    private fun isValidSerialNumber(serialNumber: String): Boolean {
        return serialNumber.matches("[A-Za-z0-9\\-_]+".toRegex())
    }

    /**
     * Crea isola con configurazione standard per tipo
     *
     * @param facilityId ID della facility
     * @param islandType Tipo di isola
     * @param serialNumber Serial number univoco
     * @param customName Nome personalizzato (opzionale)
     * @return Result con Unit se successo
     */
    suspend fun createStandardIsland(
        facilityId: String,
        islandType: IslandType,
        serialNumber: String,
        customName: String? = null
    ): Result<Unit> {
        return try {
            val now = Clock.System.now()
            val standardModel = getStandardModelForType(islandType)
            val warrantyPeriod = 730.days // 2 anni

            val island = FacilityIsland(
                id = UUID.randomUUID().toString(),
                facilityId = facilityId,
                islandType = islandType,
                serialNumber = serialNumber,
                model = standardModel,
                installationDate = now,
                warrantyExpiration = now + warrantyPeriod,
                isActive = true,
                operatingHours = 0,
                cycleCount = 0L,
                lastMaintenanceDate = null,
                nextScheduledMaintenance = null, // Verrà calcolata automaticamente
                customName = customName,
                location = null,
                notes = "Configurazione standard per ${islandType.name}",
                createdAt = now,
                updatedAt = now
            )

            invoke(island)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene il modello standard per tipo di isola
     */
    private fun getStandardModelForType(islandType: IslandType): String {
        return when (islandType) {
            IslandType.POLY_MOVE -> "POLY-MOVE-2024"
            IslandType.POLY_CAST -> "POLY-CAST-2024"
            IslandType.POLY_EBT -> "POLY-EBT-2024"
            IslandType.POLY_TAG_BLE -> "POLY-TAG-BLE-2024"
            IslandType.POLY_TAG_FC -> "POLY-TAG-FC-2024"
            IslandType.POLY_TAG_V -> "POLY-TAG-V-2024"
            IslandType.POLY_SAMPLE -> "POLY-SAMPLE-2024"
        }
    }

    /**
     * Valida che la configurazione dell'isola sia appropriata per il tipo
     */
    private fun validateIslandConfiguration(island: FacilityIsland): Result<Unit> {
        return when (island.islandType) {
            IslandType.POLY_SAMPLE -> {
                // Le isole SAMPLE richiedono manutenzione più frequente
                if (island.nextScheduledMaintenance != null) {
                    val maxInterval = 45.days // Max 45 giorni per SAMPLE
                    val baseDate = island.lastMaintenanceDate ?: island.installationDate ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
                    if ((island.nextScheduledMaintenance - baseDate) > maxInterval) {
                        return Result.failure(
                            IllegalArgumentException("Intervallo manutenzione troppo lungo per isola POLY_SAMPLE (max 45 giorni)")
                        )
                    }
                }
                Result.success(Unit)
            }
            else -> Result.success(Unit)
        }
    }
}