package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.ClientSingleStatistics
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
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
    //private val getCheckupCountUseCase: GetCheckUpCountUseCase,
    private val clientRepository: ClientRepository,
    private val checkUpRepository: CheckUpRepository? = null // Opzionale se non ancora disponibile
) {

    /**
     * Ottiene statistiche per un singolo cliente
     *
     * @param clientId ID del cliente
     * @return Result con statistiche del cliente per UI
     */
    suspend operator fun invoke(clientId: String): Result<ClientSingleStatistics> {
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

            val contractsCount = clientRepository.getContractsCount(clientId)
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

            val stats = ClientSingleStatistics(
                facilitiesCount = facilitiesCount,
                islandsCount = islandsCount,
                contactsCount = contactsCount,
                contractsCount = contractsCount,
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
    suspend fun getBasicStats(clientId: String): Result<ClientSingleStatistics> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            val facilitiesCount = clientRepository.getFacilitiesCount(clientId)
                .getOrElse { return Result.failure(it) }

            val contactsCount = clientRepository.getContactsCount(clientId)
                .getOrElse { return Result.failure(it) }

            val contractsCount = clientRepository.getContractsCount(clientId)
                .getOrElse { return Result.failure(it) }

            val islandsCount = clientRepository.getIslandsCount(clientId)
                .getOrElse { return Result.failure(it) }

            val stats = ClientSingleStatistics(
                facilitiesCount = facilitiesCount,
                islandsCount = islandsCount,
                contactsCount = contactsCount,
                contractsCount = contractsCount,     // Placeholder
                totalCheckUps = 0,      // Placeholder getCheckupCountUseCase()
                completedCheckUps = 0,  // Placeholder
                lastCheckUpDate = null  // Placeholder
            )

            Result.success(stats)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}