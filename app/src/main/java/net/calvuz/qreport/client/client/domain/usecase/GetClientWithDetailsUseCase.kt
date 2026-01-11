package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.ClientWithDetails
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsByClientUseCase
import net.calvuz.qreport.client.contract.domain.usecase.GetContractsByClientUseCase
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilitiesByClientUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandsByFacilityUseCase
import net.calvuz.qreport.client.client.domain.model.ClientSingleStatistics
import net.calvuz.qreport.app.result.domain.QrResult

import timber.log.Timber
import javax.inject.Inject

/**
 * Get customer with details
 *
 * @return ClientWithDetails
 * - Customer data
 * - FacilitiesWithIslands list
 * - Contacts list
 * - Contracts list
 * - Statistics
 */
class GetClientWithDetailsUseCase @Inject constructor(
    private val getClientByIdUseCase: GetClientByIdUseCase,
    private val getFacilitiesByClientUseCase: GetFacilitiesByClientUseCase,
    private val getContactsByClientUseCase :GetContactsByClientUseCase,
    private val getIslandsByFacilityUseCase: GetIslandsByFacilityUseCase,
    private val getContractsByClientUseCase: GetContractsByClientUseCase,

) {

    /**
     * Get customer with details
     *
     * @param clientId customer ID
     * @return ClientWithDetail instance or Result.failure
     */
    suspend operator fun invoke(clientId: String): Result<ClientWithDetails> {
        return try {
            Timber.i("Loading client details for ID: $clientId")

            // 1. Validazione ID
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // 2. Recupero cliente base
            val client = getClientByIdUseCase(clientId)
                .getOrNull() ?: return Result.failure(
                NoSuchElementException("Cliente con ID '$clientId' non trovato")
            )
            Timber.v("Client found: ${client.companyName}")

            // 3. Recupero facilities associate
            val facilities = getFacilitiesByClientUseCase(clientId)
                .getOrElse {
                    Timber.w("Failed to load facilities for client $clientId: $it")
                    emptyList()
                }
            Timber.v("Found ${facilities.size} facilities")

            // 4a. Recupero contacts associati
            val contacts = when (val result = getContactsByClientUseCase(clientId)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.w("Failed to load contacts for client $clientId")
                    emptyList()
                }
            }
            Timber.v("Found ${contacts.size} contacts")

            // 4b. Recupero contracts * associati
            val contracts = when (val result =getContractsByClientUseCase(clientId)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.w("Failed to load contracts for client $clientId")
                    emptyList()
                }
            }
            Timber.v("Found ${contracts.size} contracts")

            // 5. Recupero islands per ogni facility
            val facilitiesWithIslands = facilities.map { facility ->
                val islands = getIslandsByFacilityUseCase(facility.id)
                    .getOrElse {
                        Timber.w("Failed to load islands for facility ${facility.id}: $it")
                        emptyList()
                    }
                Timber.v("Found ${islands.size} islands for facility ${facility.id}")

                FacilityWithIslands(
                    facility = facility,
                    islands = islands
                )
            }

            // 6. Calcolo statistiche aggregate
            val totalIslands = facilitiesWithIslands.sumOf { it.islands.size }
            val stats = ClientSingleStatistics(
                facilitiesCount = facilities.size,
                islandsCount = totalIslands,
                contactsCount = contacts.size,
                contractsCount = contracts.size,
                totalCheckUps = 0, // TODO: Implementare quando integrato con CheckUp
                completedCheckUps = 0, // TODO: Implementare quando integrato con CheckUp
                lastCheckUpDate = client.updatedAt // TODO: Implementare quando integrato con CheckUp
            )

            // 7. Costruzione risultato finale
            val result = ClientWithDetails(
                client = client,
                facilities = facilitiesWithIslands,
                contacts = contacts,
                contracts = contracts,
                statistics = stats,
                // Campi aggiuntivi per UI convenience
                primaryContact = contacts.find { it.isPrimary },
                primaryFacility = facilitiesWithIslands.find { it.facility.isPrimary }?.facility,
                totalCheckUps = 0, // TODO: Implementare quando integrato
                lastCheckUpDate = null // TODO: Implementare quando integrato
            )
            Timber.i("Loaded client details {facilities=${stats.facilitiesCount}, contacts=${stats.contactsCount}, contracts=${stats.contractsCount}, islands=${stats.islandsCount}}")

            Result.success(result)

        } catch (e: Exception) {
            Timber.e(e, "Error loading client details for ID: $clientId")
            Result.failure(e)
        }
    }
}