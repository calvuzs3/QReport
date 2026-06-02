package net.calvuz.qreport.client.client.presentation.model

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.island.domain.model.Island

/**
 * Aggregated client data for the detail view.
 *
 * Note: [statusMessage] has been removed — localised status strings belong in
 * the UI layer (Composable or ViewModel via UiText), not in a model.
 *
 * Renamed [activeFacilityError], [activeContactsError], [activeContractsError]
 * to [activeFacilities], [activeContacts], [activeContracts] — the "Error"
 * suffix was a leftover from an unfinished refactoring.
 */
data class ClientWithDetails(
    val client: Client,
    val facilities: List<FacilityWithIslands>,
    val contacts: List<Contact>,
    val contracts: List<Contract>,
    val statistics: ClientStatistics,

    // Convenience fields for UI
    val primaryContact: Contact? = null,
    val primaryFacility: Facility? = null,
    val totalCheckUps: Int = 0,
    val lastCheckUpDate: Instant? = null
) {
    val activeFacilities: List<FacilityWithIslands>
        get() = facilities.filter { it.facility.isActive }

    val activeContacts: List<Contact>
        get() = contacts.filter { it.isActive }

    val activeContracts: List<Contract>
        get() = contracts.filter { it.isActive }

    val activeIslands: List<Island>
        get() = facilities.flatMap { it.islands.filter { island -> island.isActive } }

    /**
     * Returns true when the client is active and has at least one active
     * facility, contact, contract, and island.
     */
    fun isFullyOperational(): Boolean =
        client.isActive &&
                activeFacilities.isNotEmpty() &&
                activeContacts.isNotEmpty() &&
                activeContracts.isNotEmpty() &&
                activeIslands.isNotEmpty()
}