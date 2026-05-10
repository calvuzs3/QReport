package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.ClientStatistics
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
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

            // Verifica che tutte le chiamate siano riuscite
            val activeClients = activeClientsResult.getOrElse { return Result.failure(it) }
            val totalClients = totalClientsResult.getOrElse { return Result.failure(it) }

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
            )

            Result.success(statistics)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}