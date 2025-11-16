package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Inject

/**
 * Use Case per statistiche di un singolo cliente
 *
 * Questo use case è specifico per ottenere le statistiche
 * di un cliente individuale da mostrare nelle liste e card.
 *
 * È diverso da GetAllClientsStatisticsUseCase che gestisce
 * le statistiche aggregate per dashboard.
 */
class GetClientStatisticsUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val checkUpRepository: CheckUpRepository? = null // Opzionale se non ancora disponibile
) {

    /**
     * Ottiene statistiche per un singolo cliente
     *
     * @param clientId ID del cliente
     * @return Result con statistiche del cliente per UI
     */
    suspend operator fun invoke(clientId: String): Result<SingleClientStatistics> {
        return try {
            // Validazione input
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // Verifica esistenza cliente
            clientRepository.getClientById(clientId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Cliente non trovato"))

            // Raccogli statistiche base
            val facilitiesCount = clientRepository.getFacilitiesCount(clientId)
                .getOrElse { return Result.failure(it) }

            val contactsCount = clientRepository.getContactsCount(clientId)
                .getOrElse { return Result.failure(it) }

            val islandsCount = clientRepository.getIslandsCount(clientId)
                .getOrElse { return Result.failure(it) }

            // Statistiche CheckUp (opzionali se repository non disponibile)
            val (totalCheckUps, completedCheckUps, lastCheckUpDate) = if (checkUpRepository != null) {
                try {
                    // TODO
//                    val checkUps = checkUpRepository.getCheckUpsByClient(clientId)
//                        .getOrElse { emptyList() }
//
//                    val completed = checkUps.count { it.status.isCompleted() }
//                    val lastDate = checkUps.maxByOrNull { it.updatedAt }?.updatedAt
//
//                    Triple(checkUps.size, completed, lastDate)
                    Triple(0, 0, null)
                } catch (e: Exception) {
                    // Se fallisce, usa valori di default
                    Triple(0, 0, null)
                }
            } else {
                // Repository non disponibile, usa placeholder
                Triple(0, 0, null)
            }

            val stats = SingleClientStatistics(
                facilitiesCount = facilitiesCount,
                islandsCount = islandsCount,
                contactsCount = contactsCount,
                totalCheckUps = totalCheckUps,
                completedCheckUps = completedCheckUps,
                lastCheckUpDate = lastCheckUpDate
            )

            Result.success(stats)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene statistiche rapide senza CheckUp (versione veloce)
     *
     * Usa questa versione se le statistiche CheckUp non sono critiche
     * e vuoi performance migliori.
     */
    suspend fun getBasicStats(clientId: String): Result<SingleClientStatistics> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            val facilitiesCount = clientRepository.getFacilitiesCount(clientId)
                .getOrElse { return Result.failure(it) }

            val contactsCount = clientRepository.getContactsCount(clientId)
                .getOrElse { return Result.failure(it) }

            val islandsCount = clientRepository.getIslandsCount(clientId)
                .getOrElse { return Result.failure(it) }

            val stats = SingleClientStatistics(
                facilitiesCount = facilitiesCount,
                islandsCount = islandsCount,
                contactsCount = contactsCount,
                totalCheckUps = 0,      // Placeholder
                completedCheckUps = 0,  // Placeholder
                lastCheckUpDate = null  // Placeholder
            )

            Result.success(stats)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Statistiche per singolo cliente (per UI liste e card)
 */
data class SingleClientStatistics(
    val facilitiesCount: Int,
    val islandsCount: Int,
    val contactsCount: Int,
    val totalCheckUps: Int,
    val completedCheckUps: Int,
    val lastCheckUpDate: kotlinx.datetime.Instant?
) {
    /**
     * Percentuale CheckUp completati
     */
    val completionRate: Int
        get() = if (totalCheckUps > 0) {
            (completedCheckUps.toDouble() / totalCheckUps * 100).toInt()
        } else 0

    /**
     * Indica se il cliente ha dati completi
     */
    val isComplete: Boolean
        get() = facilitiesCount > 0 && contactsCount > 0

    /**
     * Score salute cliente (0-100)
     */
    val healthScore: Int
        get() {
            var score = 0
            if (facilitiesCount > 0) score += 30
            if (contactsCount > 0) score += 30
            if (islandsCount > 0) score += 20
            if (totalCheckUps > 0) score += 20
            return score
        }

    /**
     * Descrizione stato per UI
     */
    val statusDescription: String
        get() = when {
            !isComplete -> "Setup incompleto"
            totalCheckUps == 0 -> "Nessun check-up"
            completionRate < 50 -> "Check-up in corso"
            else -> "Operativo"
        }
}

/**
 * Extension per verificare se CheckUpStatus è completato
 */
private fun Any.isCompleted(): Boolean {
     return this == CheckUpStatus.COMPLETED || this == CheckUpStatus.EXPORTED
}