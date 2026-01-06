package net.calvuz.qreport.checkup.presentation.model

import net.calvuz.qreport.checkup.domain.model.CheckUpIslandAssociation
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.island.domain.model.Island

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
    val availableIslands: List<Island> = emptyList(),
    val isLoadingIslands: Boolean = false
)