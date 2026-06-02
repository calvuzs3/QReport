package net.calvuz.qreport.client.client.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.DeleteClientUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetClientWithDetailsUseCase
import net.calvuz.qreport.client.client.presentation.model.ClientStatistics
import net.calvuz.qreport.client.client.presentation.model.ClientWithDetails
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactStatistics
import net.calvuz.qreport.client.contact.domain.usecase.GetContactStatisticsUseCase
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import net.calvuz.qreport.client.contract.domain.usecase.GetContractStatisticsUseCase
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.island.domain.model.Island
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// TABS
// =============================================================================

/**
 * Tabs available in the detail screen.
 *
 * Tab labels are NOT stored here as raw strings — they are resolved at
 * runtime via stringResource() in the composable using [labelResId].
 */
enum class ClientDetailTab(val labelResId: Int) {
    FACILITIES(R.string.client_detail_tab_facilities),
    CONTACTS(R.string.client_detail_tab_contacts),
    CONTRACTS(R.string.client_detail_tab_contracts),
    INFO(R.string.client_detail_tab_info),
}

// =============================================================================
// UI STATE
// =============================================================================

data class ClientDetailUiState(

    // ===== DATA =====
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val clientDetails: ClientWithDetails? = null,

    // ===== DELETE =====
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: UiText? = null,
    val showDeleteConfirmation: Boolean = false,

    // ===== UI =====
    val selectedTab: ClientDetailTab = ClientDetailTab.FACILITIES,
    val companyName: String = "",

    // ===== TAB DATA =====
    val facilitiesWithIslands: List<FacilityWithIslands> = emptyList(),
    val activeContacts: List<Contact> = emptyList(),
    val activeContracts: List<Contract> = emptyList(),
    val allIslands: List<Island> = emptyList(),
    val statistics: ClientStatistics? = null,
    val contactStatistics: ContactStatistics? = null,
    val contractStatistics: ContractStatistics? = null

) {
    val hasData: Boolean get() = clientDetails != null
    val isEmpty: Boolean get() = !isLoading && !hasData && error == null
    val clientId: String? get() = clientDetails?.client?.id
    val isFullyOperational: Boolean get() = clientDetails?.isFullyOperational() == true
    val facilitiesCount: Int get() = facilitiesWithIslands.size
    val contacts: List<Contact> get() = activeContacts
    val contactsCount: Int get() = activeContacts.size
    val contracts: List<Contract> get() = activeContracts
    val contractsCount: Int get() = activeContracts.size
    val islandsCount: Int get() = allIslands.size
    val checkUpsCount: Int get() = clientDetails?.totalCheckUps ?: 0
}

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val getClientWithDetailsUseCase: GetClientWithDetailsUseCase,
    private val getContactStatisticsUseCase: GetContactStatisticsUseCase,
    private val getContractStatisticsUseCase: GetContractStatisticsUseCase,
    private val deleteClientUseCase: DeleteClientUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientDetailUiState())
    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ClientDetailViewModel initialized")
    }

    // =========================================================================
    // LOADING
    // =========================================================================

    fun loadClientDetails(clientId: String) {
        if (clientId.isBlank()) {
            Timber.e("ClientId blank")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = UiText.StringResource(R.string.client_detail_error_invalid_id)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getClientWithDetailsUseCase(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Client details loaded: ${result.data.client.companyName}")
                    populateUiState(result.data)
                    loadContactStatistics(clientId)
                    loadContractStatistics(clientId)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to load client details for: $clientId — ${result.error}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.client_detail_error_load)
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadContactStatistics(clientId: String) {
        when (val result = getContactStatisticsUseCase(clientId)) {
            is QrResult.Success -> _uiState.update { it.copy(contactStatistics = result.data) }
            is QrResult.Error -> Timber.w("Failed to load contact statistics for: $clientId")
        }
    }

    private suspend fun loadContractStatistics(clientId: String) {
        when (val result = getContractStatisticsUseCase(clientId)) {
            is QrResult.Success -> _uiState.update { it.copy(contractStatistics = result.data) }
            is QrResult.Error -> Timber.w("Failed to load contract statistics for: $clientId")
        }
    }

    private fun populateUiState(clientDetails: ClientWithDetails) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                clientDetails = clientDetails,
                companyName = clientDetails.client.companyName,
                facilitiesWithIslands = clientDetails.facilities,
                activeContacts = clientDetails.contacts,
                activeContracts = clientDetails.contracts,
                allIslands = clientDetails.activeIslands,
                statistics = clientDetails.statistics
            )
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteClient() {
        val clientId = _uiState.value.clientId ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isDeleting = true, deleteError = null, showDeleteConfirmation = false)
            }

            when (val result = deleteClientUseCase(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Client deleted: $clientId")
                    _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) }
                }
                is QrResult.Error -> {
                    Timber.e("Failed to delete client: $clientId — ${result.error}")
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteError = result.error.asUiText()
                        )
                    }
                }
            }
        }
    }

    fun resetDeleteState() {
        _uiState.update { it.copy(deleteSuccess = false, deleteError = null) }
    }

    // =========================================================================
    // TAB NAVIGATION
    // =========================================================================

    fun selectTab(tab: ClientDetailTab) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.update { it.copy(selectedTab = tab) }
            Timber.d("Selected tab: ${tab.name}")
        }
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    fun refreshData() {
        _uiState.value.clientId?.let { loadClientDetails(it) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onViewAllContactsClick(): String? = _uiState.value.clientId
    fun onCreateContactClick(): String? = _uiState.value.clientId
    fun getClientIdForNavigation(): String? = _uiState.value.clientId
    fun getCompanyNameForNavigation(): String = _uiState.value.companyName

    // =========================================================================
    // UTILITY
    // =========================================================================

    fun getPrimaryFacility(): FacilityWithIslands? =
        _uiState.value.facilitiesWithIslands.find { it.facility.isPrimary }

    fun getHeadquartersAddress(): String? =
        _uiState.value.clientDetails?.client?.headquarters?.toDisplayString()

    fun getIslandsNeedingMaintenance(): List<Island> =
        _uiState.value.facilitiesWithIslands.flatMap { it.islandsNeedingMaintenance }

    fun hasCompleteSetup(): Boolean = _uiState.value.isFullyOperational
    fun getActiveFacilitiesCount(): Int = _uiState.value.facilitiesWithIslands.count { it.facility.isActive }
    fun getActiveContactsCount(): Int = _uiState.value.activeContacts.count { it.isActive }
    fun getActiveIslandsCount(): Int = _uiState.value.allIslands.count { it.isActive }

    fun onFacilityClick(facilityId: String) {
        Timber.d("TODO: Navigate to facility detail: $facilityId")
    }

    fun onIslandClick(islandId: String) {
        Timber.d("TODO: Navigate to island detail: $islandId")
    }

    fun onCreateCheckUpClick() {
        Timber.d("TODO: Navigate to create CheckUp for client: ${_uiState.value.clientId}")
    }
}