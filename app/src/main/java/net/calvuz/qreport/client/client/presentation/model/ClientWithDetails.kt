package net.calvuz.qreport.client.client.presentation.model

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.island.domain.model.Island

/**
 * Data class che aggrega tutti i dati del cliente per la detail view
 */
data class ClientWithDetails(
    val client: Client,
    val facilities: List<FacilityWithIslands>,
    val contacts: List<Contact>,
    val contracts: List<Contract>,
    val statistics: ClientStatistics,

    // Convenience fields per UI
    val primaryContact: Contact? = null,
    val primaryFacility: Facility? = null,
    val totalCheckUps: Int = 0,
    val lastCheckUpDate: Instant? = null
) {

    /**
     * FacilityError attive
     */
    val activeFacilityError: List<FacilityWithIslands>
        get() = facilities.filter { it.facility.isActive }

    /**
     * ContactsError attivi
     */
    val activeContactsError: List<Contact>
        get() = contacts.filter { it.isActive }

    /**
     * ContractsError
     */
    val activeContractsError: List<Contract>
        get() = contracts.filter { it.isActive }

    /**
     * Islands attive totali
     */
    val activeIslands: List<Island>
        get() = facilities.flatMap { facilityWithIslands ->
            facilityWithIslands.islands.filter { it.isActive }
        }

    /**
     * Verifica se ha dati completi per operatività
     */
    fun isFullyOperational(): Boolean =
        client.isActive &&
                activeFacilityError.isNotEmpty() &&
                activeContactsError.isNotEmpty() &&
                activeContractsError.isNotEmpty() &&
                activeIslands.isNotEmpty()

    /**
     * Messaggio di stato per UI
     */
    val statusMessage: String
        get() = when {
            !client.isActive -> "Cliente inattivo"
            activeFacilityError.isEmpty() -> "Nessun stabilimento configurato"
            activeContactsError.isEmpty() -> "Nessun referente attivo"
            activeContractsError.isEmpty() -> "Nessun contratto attivo"
            activeIslands.isEmpty() -> "Nessuna isola robotizzata cattivo"
            else -> "Cliente operativo"
        }
}