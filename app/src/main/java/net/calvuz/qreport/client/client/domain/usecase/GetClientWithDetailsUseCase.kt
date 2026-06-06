package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.presentation.model.ClientStatistics
import net.calvuz.qreport.client.client.presentation.model.ClientWithDetails
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsByClientUseCase
import net.calvuz.qreport.client.contract.domain.usecase.GetContractsByClientUseCase
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilitiesByClientUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandsByFacilityUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Assembles a [ClientWithDetails] from multiple sources.
 *
 * Only the base client load is fatal; child collections (facilities, contacts,
 * contracts, islands) degrade gracefully — a failure returns an empty list
 * and logs a warning rather than failing the whole use case.
 */
class GetClientWithDetailsUseCase @Inject constructor(
    private val getClientById: GetClientByIdUseCase,
    private val getFacilitiesByClient: GetFacilitiesByClientUseCase,
    private val getContactsByClient: GetContactsByClientUseCase,
    private val getIslandsByFacility: GetIslandsByFacilityUseCase,
    private val getContractsByClient: GetContractsByClientUseCase,
) {
    suspend operator fun invoke(clientId: String): QrResult<ClientWithDetails, QrError.ClientError> {

        Timber.d("Getting client details for $clientId")

        if (clientId.isBlank()) {
            Timber.w("ClientId is blank")
            return QrResult.Error(QrError.ClientError.NotFound())
        }

        // 1. Load base client — fatal if missing
        val client = when (val result = getClientById(clientId)) {
            is QrResult.Error -> return QrResult.Error(result.error)
            is QrResult.Success -> result.data
        }

        // 2. Load facilities — degrade gracefully
        val facilities = when (val result = getFacilitiesByClient(clientId)) {
            is QrResult.Success ->                 result.data
            is QrResult.Error ->                 emptyList()
        }

        // 3. Load contacts — degrade gracefully
        val contacts = when (val result = getContactsByClient(clientId)) {
            is QrResult.Success -> result.data
            is QrResult.Error ->                emptyList()
        }

        // 4. Load contracts — degrade gracefully
        val contracts = when (val result = getContractsByClient(clientId)) {
            is QrResult.Success -> result.data
            is QrResult.Error ->                emptyList()
        }

        // 5. Load islands per facility — degrade gracefully
        val facilitiesWithIslands = facilities.map { facility ->
            val islands = when (val result = getIslandsByFacility(facility.id)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> emptyList()
            }
            FacilityWithIslands(facility = facility, islands = islands)
        }

        // 6. Aggregate statistics
        val stats = ClientStatistics(
            facilitiesCount = facilities.size,
            islandsCount = facilitiesWithIslands.sumOf { it.islands.size },
            contactsCount = contacts.size,
            contractsCount = contracts.size,
            totalCheckUps = 0,      // TODO: integrate CheckUpRepository
            completedCheckUps = 0,  // TODO: integrate CheckUpRepository
            lastCheckUpDate = null  // TODO: integrate CheckUpRepository
        )

        Timber.i("Loaded client details {facilities=${stats.facilitiesCount}, contacts=${stats.contactsCount}, contracts=${stats.contractsCount}, islands=${stats.islandsCount}}")

        return QrResult.Success(
            ClientWithDetails(
                client = client,
                facilities = facilitiesWithIslands,
                contacts = contacts,
                contracts = contracts,
                statistics = stats,
                primaryContact = contacts.find { it.isPrimary },
                primaryFacility = facilitiesWithIslands.find { it.facility.isPrimary }?.facility,
                totalCheckUps = 0,      // TODO: integrate CheckUpRepository
                lastCheckUpDate = null  // TODO: integrate CheckUpRepository
            )
        )
    }
}