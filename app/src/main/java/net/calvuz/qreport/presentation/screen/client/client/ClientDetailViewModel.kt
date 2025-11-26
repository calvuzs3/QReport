package net.calvuz.qreport.presentation.screen.client.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.ContactStatistics
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.usecase.client.client.GetClientWithDetailsUseCase
import net.calvuz.qreport.domain.usecase.client.client.ClientWithDetails
import net.calvuz.qreport.domain.usecase.client.client.FacilityWithIslands
import net.calvuz.qreport.domain.usecase.client.client.SingleClientStatistics
import net.calvuz.qreport.domain.usecase.client.contact.GetContactStatisticsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per ClientDetailScreen
 *
 * Gestisce:
 * - Caricamento dettagli cliente completi
 * - Navigation tra tab (Info, Facilities, Contacts, History)
 * - Stato loading/error/success
 * - Actions per modifica e navigazione
 */

data class ClientDetailUiState(
    // Data loading
    val isLoading: Boolean = false,
    val error: String? = null,
    val clientDetails: ClientWithDetails? = null,

    // UI State
    val selectedTab: ClientDetailTab = ClientDetailTab.INFO,
    val contactStatistics: ContactStatistics? = null, // ← AGGIUNGERE QUESTO

    // Quick access data
    val companyName: String = "",
    val industry: String? = null,
    val statusBadge: String = "",
    val statusBadgeColor: String = "6C757D",
    val statisticsSummary: String = "",

    // Tab data
    val facilitiesWithIslands: List<FacilityWithIslands> = emptyList(),
    val activeContacts: List<Contact> = emptyList(),
    val allIslands: List<FacilityIsland> = emptyList(),
    val statistics: SingleClientStatistics? = null
) {

    val hasData: Boolean
        get() = clientDetails != null

    val isEmpty: Boolean
        get() = !isLoading && !hasData && error == null

    val clientId: String?
        get() = clientDetails?.client?.id

//    val clientName: String?
//        get() = clientDetails?.client?.displayName

    val isFullyOperational: Boolean
        get() = clientDetails?.isFullyOperational() == true

    // Tab counts per badge
    val facilitiesCount: Int
        get() = facilitiesWithIslands.size

    val contacts: List<Contact>
        get() = activeContacts

    val contactsCount: Int
        get() = activeContacts.size

    val islandsCount: Int
        get() = allIslands.size

    val checkUpsCount: Int
        get() = clientDetails?.totalCheckUps ?: 0
}

/**
 * Tab disponibili nella detail screen
 */
enum class ClientDetailTab(val title: String) {
    INFO("Informazioni"),
    FACILITIES("Stabilimenti"),
    CONTACTS("Referenti"),
    HISTORY("Storico")
}

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val getClientWithDetailsUseCase: GetClientWithDetailsUseCase,
    private val getContactStatisticsUseCase: GetContactStatisticsUseCase // ← AGGIUNGERE QUESTO

) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientDetailUiState())
    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ClientDetailViewModel initialized")
    }

    // ============================================================
    // CLIENT LOADING
    // ============================================================

    fun loadClientDetails(clientId: String) {
        if (clientId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "ID cliente non valido"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                getClientWithDetailsUseCase(clientId).fold(
                    onSuccess = { clientDetails ->
                        Timber.d("Client details loaded successfully for: ${clientDetails.client.companyName}")
                        populateUiState(clientDetails)

                        // ✅ AGGIUNGI QUESTA RIGA ALLA FINE
                        loadContactStatistics(clientId)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load client details for ID: $clientId")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Errore caricamento cliente: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception loading client details")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadContactStatistics(clientId: String) {
        getContactStatisticsUseCase(clientId).fold(
            onSuccess = { stats ->
                _uiState.value = _uiState.value.copy(
                    contactStatistics = stats
                )
            },
            onFailure = { error ->
                // Log error but don't fail the whole screen
                Timber.e(error, "Failed to load contact statistics")
            }
        )
    }

    private fun populateUiState(clientDetails: ClientWithDetails) {
        val client = clientDetails.client
        val statusBadge = client.statusBadge

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            clientDetails = clientDetails,

            // Quick access data
            companyName = client.companyName,
            industry = client.industry,
            statusBadge = statusBadge.text,
            statusBadgeColor = statusBadge.color,
            statisticsSummary = clientDetails.statistics.summaryText,

            // Tab data
            facilitiesWithIslands = clientDetails.facilities,
            activeContacts = clientDetails.activeContacts,
            allIslands = clientDetails.activeIslands,
            statistics = clientDetails.statistics
        )
    }

    // ============================================================
    // TAB NAVIGATION
    // ============================================================

    fun selectTab(tab: ClientDetailTab) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.value = _uiState.value.copy(selectedTab = tab)
            Timber.d("Selected tab: ${tab.title}")
        }
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    fun refreshData() {
        val currentClientId = _uiState.value.clientId
        if (currentClientId != null) {
            loadClientDetails(currentClientId)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // CONTACT NAVIGATION ACTIONS
    // ============================================================

    /**
     * Azione per navigare alla lista completa contatti
     */
    fun onViewAllContactsClick(): String? {
        val clientId = _uiState.value.clientId
        Timber.d("Navigate to contacts list for client: $clientId")
        return clientId
    }

    /**
     * Azione per creare nuovo contatto
     */
    fun onCreateContactClick(): String? {
        val clientId = _uiState.value.clientId
        Timber.d("Navigate to create contact for client: $clientId")
        return clientId
    }

    /**
     * Ottieni client ID per navigation
     */
    fun getClientIdForNavigation(): String? = _uiState.value.clientId

    /**
     * Ottieni company name per navigation
     */
    fun getCompanyNameForNavigation(): String = _uiState.value.companyName

//    // Contact management convenience properties in UiState
//    val primaryContact: Contact?
//        get() = _uiState.value.activeContacts.find { it.isPrimary }
//
//    val hasContacts: Boolean
//        get() = _uiState.value.activeContacts.isNotEmpty()

//    val hasPrimaryContact: Boolean
//        get() = primaryContact != null

    // ============================================================
    // UTILITY METHODS per UI convenience
    // ============================================================

//    /**
//     * Get primary contact per info section
//     */
//    fun getPrimaryContact(): Contact? = _uiState.value.clientDetails?.primaryContact

    /**
     * Get primary facility per info section
     */
    fun getPrimaryFacility(): FacilityWithIslands? =
        _uiState.value.facilitiesWithIslands.find { it.facility.isPrimary }

    /**
     * Get headquarters address formatted
     */
    fun getHeadquartersAddress(): String? =
        _uiState.value.clientDetails?.client?.headquarters?.toDisplayString()

    /**
     * Get islands needing maintenance across all facilities
     */
    fun getIslandsNeedingMaintenance(): List<FacilityIsland> =
        _uiState.value.facilitiesWithIslands.flatMap { it.islandsNeedingMaintenance }

    /**
     * Check if client has complete setup
     */
    fun hasCompleteSetup(): Boolean = _uiState.value.isFullyOperational

    /**
     * Get status message for info section
     */
    fun getStatusMessage(): String = _uiState.value.clientDetails?.statusMessage ?: ""

    /**
     * Get facilities count with active filter
     */
    fun getActiveFacilitiesCount(): Int =
        _uiState.value.facilitiesWithIslands.count { it.facility.isActive }

    /**
     * Get contacts count with active filter
     */
    fun getActiveContactsCount(): Int =
        _uiState.value.activeContacts.count { it.isActive }

    /**
     * Get islands count with active filter
     */
    fun getActiveIslandsCount(): Int =
        _uiState.value.allIslands.count { it.isActive }

    // ============================================================
    // FUTURE: Actions per navigazioni verso detail specifici
    // ============================================================

    /**
     * Azione per navigare al dettaglio facility (quando implementato)
     */
    fun onFacilityClick(facilityId: String) {
        Timber.d("TODO: Navigate to facility detail: $facilityId")
        // TODO: Implementare navigazione quando FacilityDetailScreen sarà pronto
    }

    /**
     * Azione per navigare al dettaglio island (quando implementato)
     */
    fun onIslandClick(islandId: String) {
        Timber.d("TODO: Navigate to island detail: $islandId")
        // TODO: Implementare navigazione quando IslandDetailScreen sarà pronto
    }

    /**
     * Azione per creare nuovo CheckUp (quando implementato)
     */
    fun onCreateCheckUpClick() {
        val clientId = _uiState.value.clientId
        Timber.d("TODO: Navigate to create CheckUp for client: $clientId")
        // TODO: Implementare navigazione quando CheckUp creation sarà integrato con client selection
    }
}