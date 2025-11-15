package net.calvuz.qreport.domain.usecase.client.facilityisland

import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import net.calvuz.qreport.util.DateTimeUtils.toItalianDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Use Case per eliminazione di un'isola robotizzata
 *
 * Gestisce:
 * - Validazione esistenza isola
 * - Controllo stato operativo prima dell'eliminazione
 * - Eliminazione sicura (soft delete)
 * - Validazione impatti su facility
 *
 * Business Rules:
 * - Isole con manutenzione in corso non possono essere eliminate
 * - Warning se l'isola è sotto garanzia
 * - Controllo che non sia l'ultima isola operativa della facility
 */
class DeleteFacilityIslandUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Elimina un'isola robotizzata
     *
     * @param islandId ID dell'isola da eliminare
     * @param force Se true, forza l'eliminazione anche in caso di warning
     * @return Result con Unit se successo, errore con dettagli se fallimento
     */
    suspend operator fun invoke(islandId: String, force: Boolean = false): Result<Unit> {
        return try {
            // 1. Validazione input
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID isola non può essere vuoto"))
            }

            // 2. Verificare che l'isola esista
            val island = checkIslandExists(islandId)
                .getOrElse { return Result.failure(it) }

            // 3. Controlli business pre-eliminazione
            if (!force) {
                checkPreDeletionConditions(island).onFailure { return Result.failure(it) }
            }

            // 4. Eliminazione isola (soft delete)
            facilityIslandRepository.deleteIsland(islandId)

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
                when {
                    island == null ->
                        throw NoSuchElementException("Isola con ID '$islandId' non trovata")
                    !island.isActive ->
                        throw IllegalStateException("Isola con ID '$islandId' già eliminata")
                    else -> island
                }
            }
    }

    /**
     * Controlli business pre-eliminazione
     */
    private suspend fun checkPreDeletionConditions(
        island: FacilityIsland
    ): Result<Unit> {
        return try {
            val currentTime = Clock.System.now() // System.currentTimeMillis()
            val warnings = mutableListOf<String>()
            val errors = mutableListOf<String>()

            // 1. Controllo garanzia attiva
            island.warrantyExpiration?.let { warranty ->
                if (warranty > currentTime) {
                    warnings.add("L'isola è ancora sotto garanzia (scade il ${warranty.toItalianDate()})")
                }
            }

            // 2. Controllo manutenzione programmata
            island.nextScheduledMaintenance?.let { nextMaintenance ->
                val daysDifference = (nextMaintenance - currentTime) / (24 * 60 * 60 * 1000)
                if (daysDifference.inWholeDays <= 7 && daysDifference.inWholeDays >= 0) {
                    warnings.add("Manutenzione programmata tra ${daysDifference.inWholeDays} giorni")
                }
                if (nextMaintenance <= currentTime) {
                    errors.add("L'isola richiede manutenzione immediata. Completare la manutenzione prima di eliminare.")
                }
            }

            // 3. Controllo ore operative elevate (possibile valore storico)
            if (island.operatingHours > 10000) {
                warnings.add("L'isola ha ${island.operatingHours} ore operative significative")
            }

            // 4. Controllo cicli elevati
            if (island.cycleCount > 1000000) {
                warnings.add("L'isola ha ${island.cycleCount} cicli di lavorazione significativi")
            }

            // 5. Controllo se è l'ultima isola della facility
            val siblingIslands = facilityIslandRepository.getIslandsByFacility(island.facilityId)
                .getOrElse { emptyList() }

            val activeIslands = siblingIslands.filter { it.isActive && it.id != island.id }
            if (activeIslands.isEmpty()) {
                warnings.add("Questa è l'ultima isola attiva della facility")
            }

            // Se ci sono errori bloccanti, fallisci
            if (errors.isNotEmpty()) {
                throw IllegalStateException("Impossibile eliminare isola: ${errors.joinToString("; ")}")
            }

            // Se ci sono warning, segnala ma procedi se non è forzato
            if (warnings.isNotEmpty()) {
                throw IllegalStateException(
                    "Warning eliminazione isola: ${warnings.joinToString("; ")}. " +
                            "Utilizzare force=true per procedere comunque."
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se un'isola può essere eliminata senza forzatura
     *
     * @param islandId ID dell'isola da verificare
     * @return Result con Boolean - true se può essere eliminata, false altrimenti
     */
    suspend fun canDeleteIsland(islandId: String): Result<Boolean> {
        return try {
            val island = checkIslandExists(islandId)
                .getOrElse { return Result.success(false) }

            checkPreDeletionConditions(island)
                .map { true }
                .recover { false }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene informazioni sui warning/blocchi per l'eliminazione
     *
     * @param islandId ID dell'isola
     * @return Result con dettagli dei controlli
     */
    suspend fun getIslandDeletionInfo(islandId: String): Result<IslandDeletionInfo> {
        return try {
            val island = checkIslandExists(islandId)
                .getOrElse { return Result.failure(it) }

            val currentTime = Clock.System.now() // System.currentTimeMillis()
            val warnings = mutableListOf<String>()
            val errors = mutableListOf<String>()
            val info = mutableMapOf<String, Any>()

            // Raccoglie informazioni dettagliate
            island.warrantyExpiration?.let { warranty ->
                info["warrantyStatus"] = if (warranty > currentTime) "ACTIVE" else "EXPIRED"
                info["warrantyExpiration"] = warranty
                if (warranty > currentTime) {
                    warnings.add("Sotto garanzia")
                }
            }

            island.nextScheduledMaintenance?.let { nextMaintenance ->
                info["maintenanceStatus"] = when {
                    nextMaintenance <= currentTime -> "OVERDUE"
                    nextMaintenance - currentTime <= 7.days -> "DUE_SOON"
                    else -> "SCHEDULED"
                }
                info["nextMaintenanceDate"] = nextMaintenance

                val daysDifference = (nextMaintenance - currentTime) / (24 * 60 * 60 * 1000)
                if (nextMaintenance <= currentTime) {
                    errors.add("Manutenzione scaduta")
                } else if (daysDifference <= 7.days) {
                    warnings.add("Manutenzione imminente")
                }
            }

            val siblingIslands = facilityIslandRepository.getIslandsByFacility(island.facilityId)
                .getOrElse { emptyList() }
            val activeIslands = siblingIslands.filter { it.isActive && it.id != island.id }
            info["remainingActiveIslands"] = activeIslands.size

            if (activeIslands.isEmpty()) {
                warnings.add("Ultima isola della facility")
            }

            info["operatingHours"] = island.operatingHours
            info["cycleCount"] = island.cycleCount

            val deletionInfo = IslandDeletionInfo(
                islandId = island.id,
                canDelete = errors.isEmpty(),
                canDeleteWithForce = true,
                warnings = warnings,
                errors = errors,
                additionalInfo = info
            )

            Result.success(deletionInfo)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina tutte le isole di una facility (uso interno per eliminazione facility)
     *
     * @param facilityId ID della facility
     * @return Result con numero di isole eliminate
     */
    suspend fun deleteAllIslandsForFacility(facilityId: String): Result<Int> {
        return try {
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility non può essere vuoto"))
            }

            val islands = facilityIslandRepository.getIslandsByFacility(facilityId)
                .getOrElse { return Result.failure(it) }

            var deletedCount = 0

            islands.forEach { island ->
                facilityIslandRepository.deleteIsland(island.id)
                    .onSuccess { deletedCount++ }
                    .onFailure { return Result.failure(it) }
            }

            Result.success(deletedCount)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Conta le isole rimanenti dopo una potenziale eliminazione
     *
     * @param islandId ID dell'isola da eliminare
     * @return Result con numero di isole che rimarrebbero nella facility
     */
    suspend fun countRemainingIslandsAfterDeletion(islandId: String): Result<Int> {
        return try {
            val island = checkIslandExists(islandId)
                .getOrElse { return Result.failure(it) }

            val currentCount = facilityIslandRepository.getIslandsCountByFacility(island.facilityId)
                .getOrElse { return Result.failure(it) }

            Result.success(currentCount - 1)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formatta una data timestamp in stringa leggibile
     */
    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
        return formatter.format(date)
    }
}

/**
 * Informazioni dettagliate per l'eliminazione di un'isola
 */
data class IslandDeletionInfo(
    val islandId: String,
    val canDelete: Boolean,
    val canDeleteWithForce: Boolean,
    val warnings: List<String>,
    val errors: List<String>,
    val additionalInfo: Map<String, Any>
)