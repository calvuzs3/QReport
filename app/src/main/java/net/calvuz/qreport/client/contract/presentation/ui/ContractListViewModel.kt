package net.calvuz.qreport.client.contract.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.contract.data.local.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.usecase.DeleteContractUseCase
import net.calvuz.qreport.client.contract.domain.usecase.GetContractsByClientUseCase
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionActionHandler
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository

/** ContractListScreen UiState */
data class ContractListUiState(
    val clientId: String = "",
    val contracts: List<ContractWithStats> = emptyList(),
    val filteredContracts: List<ContractWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeleting: String? = null,
    val successMessage: UiText? = null,
    val error: UiText? = null,
    val searchQuery: String = "",
    val selectedFilter: ContractFilter = ContractFilter.ACTIVE,
    val selectedSortOrder: ContractSortOrder = ContractSortOrder.EXPIRE_RECENT,

    // Card Variant
    val cardVariant: ListViewMode = ListViewMode.FULL
)

/** ContractListScreen Filter */
enum class ContractFilter { ACTIVE, INACTIVE, ALL }

/** ContractListScreen SortOrder */
enum class ContractSortOrder { EXPIRE_RECENT, EXPIRE_OLDEST, NAME }

@HiltViewModel
class ContractListViewModel @Inject constructor(
    private val getContractsByClientUseCase: GetContractsByClientUseCase,
    private val deleteContractUseCase: DeleteContractUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState: StateFlow<ContractListUiState> = _uiState.asStateFlow()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    companion object {
        private const val _KEY = AppSettingsDataStore.LIST_KEY_CONTRACTS
    }

    fun init() {
        Timber.d("Contact list view model initialized")
        // The Screen call the initialize
        //initializeForClient(_uiState.value.clientId)
    }

    fun initializeForClient(clientId: String) {
        if (clientId == _uiState.value.clientId) return

        _uiState.value = _uiState.value.copy(clientId = clientId)
        loadContracts(clientId)
    }

    fun loadContracts(clientId: String) {
        if (clientId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = UiText.StringResource(R.string.err_contracts_list_invalid_client_id)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                clientId = clientId
            )

            try {
                Timber.d("Loading contacts for client: $clientId")

                getContractsByClientUseCase.observeContractsByClient(clientId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in contacts flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResources(R.string.err_contracts_list_load_contacts, exception.message ?: "")
                            )
                        }
                    }
                    .collect { contacts ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping contacts processing - job cancelled")
                            return@collect
                        }

                        // Enrich with statistics
                        val contractsithStats = enrichWithStatistics(contacts)

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                contractsithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            )

                            _uiState.value = currentState.copy(
                                contracts = contractsithStats,
                                filteredContracts = filteredAndSorted,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Loaded ${contacts.size} contracts successfully")
                        }
                    }
            } catch (_: CancellationException) {
                Timber.d("Contacts loading cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Exception loading contracts")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResources(R.string.err_contracts_list_unexpected, e.message ?: "")
                )
            }
        }
    }

    fun refresh() {
        val clientId = _uiState.value.clientId
        if (clientId.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                Timber.d("Refreshing contracts for client: $clientId")
                delay(500)

                when (val result = getContractsByClientUseCase(clientId)) {

                    is QrResult.Success -> {
                        val contracts = result.data

                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping refresh processing - job cancelled")
                            return@launch
                        }

                        val contractsWithStats = enrichWithStatistics(contracts)

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                contractsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            )

                            _uiState.value = currentState.copy(
                                contracts = contractsWithStats,
                                filteredContracts = filteredAndSorted,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Contracts refresh completed successfully")
                        }
                    }

                    is QrResult.Error -> {
                        if (currentCoroutineContext().isActive) {
                            Timber.e("Failed to refresh contracts")
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = UiText.StringResources(R.string.err_contracts_list_refresh_failed, result.error)
                            )
                        }
                        Timber.d("Error refreshing contracts: ${result.error}")
                    }
                }
            } catch (_: CancellationException) {
                Timber.d("Refresh cancelled")
                if (currentCoroutineContext().isActive) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to refresh contracts")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = UiText.StringResources(R.string.err_contracts_list_refresh_unexpected, e.message ?: "")
                    )
                }
            }
        }
    }

    fun delete(contractId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = contractId)

            try {
                when (deleteContractUseCase(contractId)) {
                    is QrResult.Success -> {
                        Timber.d("Contact deleted successfully: $contractId")
                        refresh() // Show changes
                    }

                    is QrResult.Error -> {
                        Timber.e("Failed to delete contract $contractId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = null,
                            error = UiText.StringResources(R.string.err_contracts_list_delete_failed, contractId)
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting contract")
                _uiState.value = _uiState.value.copy(
                    isDeleting = null,
                    error = UiText.StringResources(R.string.err_contracts_list_unexpected, e.message ?: "")
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = null)
            }
        }
    }

    /**
     * Handle contract renewal - creates a new contract based on the existing one
     * This could be expanded to navigate to a renewal screen or show a renewal dialog
     */
    fun renew(contractId: String) {
        viewModelScope.launch {
            try {
                val contract = _uiState.value.contracts
                    .find { it.contract.id == contractId }
                    ?.contract

                if (contract != null) {
                    Timber.d("Renewing contract: $contractId")
                    // TODO: Implement contract renewal logic
                    // This could involve:
                    // 1. Navigate to contract creation with pre-filled data
                    // 2. Show a renewal dialog
                    // 3. Create a new contract with extended dates

                    // For now, log the action
                    Timber.d("Contract renewal requested for: ${contract.name}")
                } else {
                    Timber.e("Contract not found for renewal: $contractId")
                    _uiState.value = _uiState.value.copy(
                        error = UiText.StringResource(R.string.err_contracts_list_contract_not_found_for_renewal)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception renewing contract")
                _uiState.value = _uiState.value.copy(
                    error = UiText.StringResources(R.string.err_contracts_list_renewal_failed, e.message ?: "")
                )
            }
        }
    }

    /**
     * Get contract by ID - useful for action handlers
     */
    fun getContract(contractId: String): Contract? {
        return _uiState.value.contracts
            .find { it.contract.id == contractId }
            ?.contract
    }

    /**
     * Check if contract is eligible for renewal
     */
    fun isContractEligibleForRenewal(contractId: String): Boolean {
        val contractWithStats = _uiState.value.contracts
            .find { it.contract.id == contractId }

        return contractWithStats?.let {
            // Contract is eligible for renewal if:
            // 1. It's expired OR
            // 2. It expires within 30 days
            val isExpired = it.stats.isExpired
            val expiresWithin30Days = it.contract.endDate
                .minus(30.days)
                .let { thirtyDaysBeforeExpiry ->
                    kotlinx.datetime.Clock.System.now() >= thirtyDaysBeforeExpiry
                }

            isExpired || expiresWithin30Days
        } == true
    }

    fun setActive( contracts: Set<Contract>, active: Boolean) {
        viewModelScope.launch {
            // TODO: Implement active/inactive logic
        }
    }

    // ===== Search and filter logic =====

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value

        if (query.length >= 3) {
            updateSearchQuery(query)
        } else {
            val filteredAndSorted = applyFiltersAndSort(
                currentState.contracts,
                query,
                currentState.selectedFilter,
                currentState.selectedSortOrder
            )

            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredContracts = filteredAndSorted
            )
        }
    }

    fun updateFilter(filter: ContractFilter) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.contracts,
            currentState.searchQuery,
            filter,
            currentState.selectedSortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredContracts = filteredAndSorted
        )
    }

    fun updateSortOrder(sortOrder: ContractSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.contracts,
            currentState.searchQuery,
            currentState.selectedFilter,
            sortOrder
        )

        _uiState.value = currentState.copy(
            selectedSortOrder = sortOrder,
            filteredContracts = filteredAndSorted
        )
    }

    /**
     * Cycle through card display variants: FULL -> COMPACT -> MINIMAL -> FULL.
     * The preference is persisted via [AppSettingsRepository].
     */
    fun cycleCardVariant() {
        val current = _uiState.value.cardVariant
        val next = when (current) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }

        // Update UI immediately
        _uiState.value = _uiState.value.copy(cardVariant = next)

        // Persist in background
        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(
                    _KEY,
                    next
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist card variant preference")
            }
        }
    }

    // ===== Error handling =====

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Observe the persisted card variant preference and apply it to UI state.
     */
    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(_KEY)
                .catch { e ->
                    Timber.e(e, "Error observing card variant preference")
                }
                .collect { viewMode ->
                    _uiState.value = _uiState.value.copy(
                        cardVariant = viewMode
                    )
                }
        }
    }

    private fun enrichWithStatistics(contracts: List<Contract>): List<ContractWithStats> {
        return contracts.map { contract ->
            val stats = try {
                ContractsStatistics(
                    isExpired = !contract.isValid()
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception getting stats for contact ${contract.id}")
                ContractsStatistics(
                    isExpired = false
                )
            }

            ContractWithStats(contract = contract, stats = stats)
        }
    }

    private fun applyFiltersAndSort(
        contracts: List<ContractWithStats>,
        searchQuery: String,
        filter: ContractFilter,
        sortOrder: ContractSortOrder
    ): List<ContractWithStats> {
        var filtered = contracts

        // Apply status filter
        filtered = when (filter) {
            ContractFilter.ALL -> filtered
            ContractFilter.ACTIVE -> filtered.filter { it.contract.isValid() }
            ContractFilter.INACTIVE -> filtered.filter { !it.contract.isValid() }
        }

        // Apply local search query (per query corte)
        if (searchQuery.isNotBlank() && searchQuery.length <= 2) {
            filtered = filtered.filter { contractWithStats ->
                val contract = contractWithStats.contract
                val nameMatch = contract.name?.contains(searchQuery, ignoreCase = true) ?: false
                val descriptionMatch =
                    contract.description?.contains(searchQuery, ignoreCase = true) ?: false
                val startDateMatch =
                    contract.startDate.toString().contains(searchQuery, ignoreCase = true)
                val endDateMatch =
                    contract.endDate.toString().contains(searchQuery, ignoreCase = true)

                // Il contratto passa il filtro se almeno una delle condizioni è vera
                nameMatch || descriptionMatch || startDateMatch || endDateMatch
            }
        }

        // Apply sorting
        filtered = when (sortOrder) {
            ContractSortOrder.NAME -> filtered.sortedBy { it.contract.name }
            ContractSortOrder.EXPIRE_RECENT -> filtered.sortedBy { it.contract.endDate }
            ContractSortOrder.EXPIRE_OLDEST -> filtered.sortedByDescending { it.contract.endDate }
        }

        return filtered
    }
}

// ============================================================
// DATA CLASSES
// ============================================================

data class ContractWithStats(
    val contract: Contract,
    val stats: ContractsStatistics
)

data class ContractsStatistics(
    val isExpired: Boolean
)


/**
 * Technical Intervention specific action handler
 */
class ContractActionHandler(
    private val onEdit: (Set<Contract>) -> Unit,
    private val onDelete: (Set<Contract>) -> Unit,
    private val onRenew: (Set<Contract>) -> Unit,
    private val onSetActive: (Set<Contract>) -> Unit,
    private val onSetInactive: (Set<Contract>) -> Unit,
    private val onArchive: (Set<Contract>) -> Unit,
//    private val onExport: (Set<Contract>) -> Unit,
    private val onSelectAll: () -> Unit
) : SimpleSelectionActionHandler<Contract> {

    override fun onActionClick(action: SelectionAction, selectedItems: Set<Contract>) {
        when (action) {
            SelectionAction.Edit -> onEdit(selectedItems)
            SelectionAction.Delete -> onDelete(selectedItems)
            SelectionAction.Renew -> onRenew(selectedItems)
            SelectionAction.SetActive -> onSetActive(selectedItems)
            SelectionAction.SetInactive -> onSetInactive(selectedItems)
            SelectionAction.Archive -> {onArchive(selectedItems)}
//            SelectionAction.Export -> onExport(selectedItems)
            SelectionAction.SelectAll -> onSelectAll()


//            SelectionAction.MarkCompleted -> {
//                // Handle mark completed - set status to COMPLETED
//                // You would implement this in ViewModel
//            }

            is SelectionAction.Custom -> {
                // Handle any custom actions
                when (action.actionId) {
                    "renew" -> { /* handle duplicate */
                    }
                }
            }

            else -> {}
        }
    }

    override fun isActionEnabled(
        action: SelectionAction,
        selectedItems: Set<Contract>
    ): Boolean {
        return when (action) {
            SelectionAction.Edit -> selectedItems.size == 1 // Edit only for single selection
            SelectionAction.Delete -> selectedItems.isNotEmpty() && selectedItems.all {
                // Can delete only ..
                true
            }

            SelectionAction.SetActive -> selectedItems.isNotEmpty()
            SelectionAction.SetInactive -> selectedItems.isNotEmpty()
            SelectionAction.Archive -> selectedItems.isNotEmpty()
            SelectionAction.SelectAll -> true
            is SelectionAction.Custom -> true // Custom logic per action
            else -> false
        }
    }

    override fun getDeleteConfirmationMessage(selectedItems: Set<Contract>): String {
        return when (selectedItems.size) {
            1 -> "Delete contract ${selectedItems.first().startDate}-${selectedItems.first().endDate}?"
            else -> "Delete ${selectedItems.size} contracts?"
        }
    }
}