package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use Case per recuperare tutti i clienti
 *
 * Gestisce:
 * - Recupero clienti attivi o tutti
 * - Ordinamento
 * - Flow reattivo per UI
 */
class GetAllClientsUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {

    /**
     * Recupera tutti i clienti attivi
     *
     * @return Result con lista clienti ordinata per ragione sociale
     */
    suspend fun getActiveClients(): Result<List<Client>> {
        return try {
            clientRepository.getActiveClients()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera tutti i clienti (attivi e inattivi)
     *
     * @return Result con lista clienti ordinata per ragione sociale
     */
    suspend fun getAllClients(): Result<List<Client>> {
        return try {
            clientRepository.getAllClients()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva tutti i clienti attivi (Flow reattivo)
     *
     * @return Flow con lista clienti che si aggiorna automaticamente
     */
    fun observeActiveClients(): Flow<List<Client>> {
        return clientRepository.getAllClientsFlow()
            .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
    }

    /**
     * Recupera clienti per industria/settore
     *
     * @param industry Settore da filtrare
     * @return Result con lista clienti del settore specificato
     */
    suspend fun getClientsByIndustry(industry: String): Result<List<Client>> {
        return try {
            if (industry.isBlank()) {
                return Result.failure(IllegalArgumentException("Settore non può essere vuoto"))
            }

            clientRepository.getClientsByIndustry(industry)
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera clienti con facilities associate
     *
     * @return Result con lista clienti che hanno stabilimenti
     */
    suspend fun getClientsWithFacilities(): Result<List<Client>> {
        return try {
            clientRepository.getClientsWithFacilities()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera clienti con contatti associati
     *
     * @return Result con lista clienti che hanno contatti
     */
    suspend fun getClientsWithContacts(): Result<List<Client>> {
        return try {
            clientRepository.getClientsWithContacts()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera clienti con isole associate
     *
     * @return Result con lista clienti che hanno isole robotizzate
     */
    suspend fun getClientsWithIslands(): Result<List<Client>> {
        return try {
            clientRepository.getClientsWithIslands()
                .map { clients -> clients.sortedBy { it.companyName.lowercase() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}