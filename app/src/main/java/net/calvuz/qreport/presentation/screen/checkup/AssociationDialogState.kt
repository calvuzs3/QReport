package net.calvuz.qreport.presentation.screen.checkup

import net.calvuz.qreport.domain.model.checkup.CheckUpIslandAssociation
import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityIsland

/**
 * ✅ NUOVO DATA CLASS per stato dialog associazione
 */
data class AssociationDialogState(
    val showDialog: Boolean = false,
    val currentAssociations: List<CheckUpIslandAssociation> = emptyList(),

    // Clienti
    val availableClients: List<Client> = emptyList(),
    val isLoadingClients: Boolean = false,

    // Facilities
    val selectedClientId: String? = null,
    val availableFacilities: List<Facility> = emptyList(),
    val isLoadingFacilities: Boolean = false,

    // Islands
    val selectedFacilityId: String? = null,
    val availableIslands: List<FacilityIsland> = emptyList(),
    val isLoadingIslands: Boolean = false
)

