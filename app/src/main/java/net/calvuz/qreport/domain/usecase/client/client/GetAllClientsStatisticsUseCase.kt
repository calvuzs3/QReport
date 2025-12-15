package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.ClientStatistics
import net.calvuz.qreport.domain.repository.ClientRepository
import javax.inject.Inject

/**
 * Use Case per statistiche e dashboard clienti
 *
 * Gestisce:
 * - Conteggi clienti (attivi, totali)
 * - Statistiche per settore
 * - Conteggi dipendenze (facilities, contatti, isole)
 * - Metriche per dashboard executive
 */
class GetAllClientsStatisticsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {

    /**
     * Ottiene statistiche complete per dashboard
     *
     * @return Result con oggetto statistiche complete
     */
    suspend operator fun invoke(): Result<ClientStatistics> {
        return try {
            val activeClientsResult = clientRepository.getActiveClientsCount()
            val totalClientsResult = clientRepository.getTotalClientsCount()
            val industriesResult = clientRepository.getAllIndustries()

            // Verifica che tutte le chiamate siano riuscite
            val activeClients = activeClientsResult.getOrElse { return Result.failure(it) }
            val totalClients = totalClientsResult.getOrElse { return Result.failure(it) }
            val industries = industriesResult.getOrElse { return Result.failure(it) }

            // Calcola statistiche aggregate
            val inactiveClients = totalClients - activeClients
            val activationRate = if (totalClients > 0) {
                (activeClients.toDouble() / totalClients.toDouble() * 100).toInt()
            } else 0

            val statistics = ClientStatistics(
                activeClients = activeClients,
                totalClients = totalClients,
                inactiveClients = inactiveClients,
                activationRate = activationRate,
                totalIndustries = industries.size,
                industries = industries
            )

            Result.success(statistics)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

//    /**
//     * Ottiene statistiche dettagliate per un singolo cliente
//     *
//     * @param clientId ID del cliente
//     * @return Result con statistiche specifiche del cliente
//     */
//    suspend fun getClientDetailStats(clientId: String): Result<ClientDetailStats> {
//        return try {
//            // Validazione input
//            if (clientId.isBlank()) {
//                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
//            }
//
//            // Verifica esistenza cliente
//            val client = clientRepository.getClientById(clientId)
//                .getOrElse { return Result.failure(it) }
//                ?: return Result.failure(NoSuchElementException("Cliente non trovato"))
//
//            // Raccogli statistiche
//            val facilitiesCount = clientRepository.getFacilitiesCount(clientId)
//                .getOrElse { return Result.failure(it) }
//
//            val contactsCount = clientRepository.getContactsCount(clientId)
//                .getOrElse { return Result.failure(it) }
//
//            val islandsCount = clientRepository.getIslandsCount(clientId)
//                .getOrElse { return Result.failure(it) }
//
//            val stats = ClientDetailStats(
//                clientId = client.id,
//                companyName = client.companyName,
//                isActive = client.isActive,
//                facilitiesCount = facilitiesCount,
//                contactsCount = contactsCount,
//                islandsCount = islandsCount,
//                createdAt = client.createdAt.epochSeconds,
//                updatedAt = client.updatedAt.epochSeconds,
//                industry = client.industry
//            )
//
//            Result.success(stats)
//
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

//    /**
//     * Ottiene statistiche per settore/industria
//     *
//     * @return Result con mappa settore -> numero clienti
//     */
//    suspend fun getIndustryStatistics(): Result<Map<String, Int>> {
//        return try {
//            val industries = clientRepository.getAllIndustries()
//                .getOrElse { return Result.failure(it) }
//
//            val industryStats = mutableMapOf<String, Int>()
//
//            // Per ogni settore, conta i clienti
//            industries.forEach { industry ->
//                val clientsInIndustry = clientRepository.getClientsByIndustry(industry)
//                    .getOrElse { return Result.failure(it) }
//                industryStats[industry] = clientsInIndustry.size
//            }
//
//            // Aggiungi clienti senza settore specificato
//            val allClients = clientRepository.getActiveClients()
//                .getOrElse { return Result.failure(it) }
//
//            val clientsWithoutIndustry = allClients.count { it.industry.isNullOrBlank() }
//            if (clientsWithoutIndustry > 0) {
//                industryStats["Non specificato"] = clientsWithoutIndustry
//            }
//
//            Result.success(industryStats.toMap())
//
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

//    /**
//     * Ottiene top 5 settori per numero clienti
//     *
//     * @return Result con lista settori ordinati per popolarità
//     */
//    suspend fun getTopIndustries(limit: Int = 5): Result<List<IndustryStats>> {
//        return try {
//            getIndustryStatistics()
//                .map { industryMap ->
//                    industryMap.map { (industry, count) ->
//                        IndustryStats(industry, count)
//                    }
//                        .sortedByDescending { it.clientCount }
//                        .take(limit)
//                }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

//    /**
//     * Verifica salute generale sistema clienti
//     *
//     * @return Result con indicatori di salute
//     */
//    suspend fun getSystemHealthStats(): Result<SystemHealthStats> {
//        return try {
//            val baseStats = invoke().getOrElse { return Result.failure(it) }
//
//            // Conta clienti con dipendenze complete (facilities + contatti + isole)
//            val clientsWithFacilities = clientRepository.getClientsWithFacilities()
//                .getOrElse { return Result.failure(it) }.size
//
//            val clientsWithContacts = clientRepository.getClientsWithContacts()
//                .getOrElse { return Result.failure(it) }.size
//
//            val clientsWithIslands = clientRepository.getClientsWithIslands()
//                .getOrElse { return Result.failure(it) }.size
//
//            // Calcola percentuali completezza
//            val totalActive = baseStats.activeClients
//            val facilitiesCompleteness = if (totalActive > 0) {
//                (clientsWithFacilities.toDouble() / totalActive * 100).toInt()
//            } else 0
//
//            val contactsCompleteness = if (totalActive > 0) {
//                (clientsWithContacts.toDouble() / totalActive * 100).toInt()
//            } else 0
//
//            val islandsCompleteness = if (totalActive > 0) {
//                (clientsWithIslands.toDouble() / totalActive * 100).toInt()
//            } else 0
//
//            val healthStats = SystemHealthStats(
//                totalActiveClients = totalActive,
//                clientsWithFacilities = clientsWithFacilities,
//                clientsWithContacts = clientsWithContacts,
//                clientsWithIslands = clientsWithIslands,
//                facilitiesCompleteness = facilitiesCompleteness,
//                contactsCompleteness = contactsCompleteness,
//                islandsCompleteness = islandsCompleteness,
//                overallHealthScore = (facilitiesCompleteness + contactsCompleteness + islandsCompleteness) / 3
//            )
//
//            Result.success(healthStats)
//
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}

///**
// * Statistiche generali clienti
// */
//data class ClientStatistics(
//    val activeClients: Int,
//    val totalClients: Int,
//    val inactiveClients: Int,
//    val activationRate: Int, // Percentuale
//    val totalIndustries: Int,
//    val industries: List<String>
//)

///**
// * Statistiche dettagliate singolo cliente
// */
//data class ClientDetailStats(
//    val clientId: String,
//    val companyName: String,
//    val isActive: Boolean,
//    val facilitiesCount: Int,
//    val contactsCount: Int,
//    val islandsCount: Int,
//    val createdAt: Long,
//    val updatedAt: Long,
//    val industry: String?
//)
//
///**
// * Statistiche per settore
// */
//data class IndustryStats(
//    val industry: String,
//    val clientCount: Int
//)
//
///**
// * Statistiche salute sistema
// */
//data class SystemHealthStats(
//    val totalActiveClients: Int,
//    val clientsWithFacilities: Int,
//    val clientsWithContacts: Int,
//    val clientsWithIslands: Int,
//    val facilitiesCompleteness: Int, // Percentuale
//    val contactsCompleteness: Int, // Percentuale
//    val islandsCompleteness: Int, // Percentuale
//    val overallHealthScore: Int // Media percentuali
//)