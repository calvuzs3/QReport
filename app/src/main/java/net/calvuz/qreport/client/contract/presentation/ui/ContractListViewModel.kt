@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.client.contract.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.client.domain.usecase.ObserveClientsUseCase
import net.calvuz.qreport.client.contract.data.local.mapper.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.usecase.DeleteContractUseCase
import net.calvuz.qreport.client.contract.domain.usecase.ObserveContractsUseCase
import net.calvuz.qreport.client.contract.presentation.model.ContractFilter
import net.calvuz.qreport.client.contract.presentation.model.ContractSortOrder
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionActionHandler
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.presentation.model.ContractPkg
import net.calvuz.qreport.client.facility.presentation.model.ClientOption
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

// =============================================================================
// UI STATE
// =============================================================================

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
    val selectedFilter: ContractFilter = ContractPkg.selectedFilter,
    val selectedSortOrder: ContractSortOrder = ContractPkg.selectedSortOrder,
    // Bulk
    val isBulkDeleting: Boolean = false,
    val bulkDeleteProgress: Int = 0,
    val bulkDeleteTotal: Int = 0,
    // Client selector
    val availableClients: List<ClientOption> = listOf(ClientOption.ALL),
    val selectedClient: ClientOption = ClientOption.ALL,
    // Card variant
    val cardVariant: ListViewMode = ListViewMode.FULL
)

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class ContractListViewModel @Inject constructor(
    private val observeContractsUseCase: ObserveContractsUseCase,
    private val deleteContractUseCase: DeleteContractUseCase,
    private val observeClientsUseCase: ObserveClientsUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState: StateFlow<ContractListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    companion object {
        private const val KEY = AppSettingsDataStore.LIST_KEY_CONTRACTS
    }

    init {
        observeCardVariant()
        loadClientsForDropdown()
    }

    // =========================================================================
    // PUBLIC
    // =========================================================================

    /** Load all contracts regardless of client — called when no clientId is provided. */
    fun initialize() {
        _uiState.value = _uiState.value.copy(clientId = "", selectedClient = ClientOption.ALL)
        loadContracts(clientId = null)
    }

    fun initializeForClient(clientId: String) {
        if (clientId == _uiState.value.clientId) return
        _uiState.value = _uiState.value.copy(
            clientId = clientId,
            selectedClient = _uiState.value.availableClients.find { it.id == clientId }
                ?: ClientOption.ALL
        )
        loadContracts(clientId = clientId)
    }

    fun updateSelectedClient(client: ClientOption) {
        if (client == _uiState.value.selectedClient) return
        _uiState.value = _uiState.value.copy(selectedClient = client, clientId = client.id)
        loadContracts(clientId = client.id.takeIf { it.isNotBlank() })
    }

    fun refresh() {
        val clientId = _uiState.value.clientId.takeIf { it.isNotBlank() }
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadContracts(clientId = clientId)
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            searchQuery = query,
            filteredContracts = applyFiltersAndSort(
                current.contracts, query, current.selectedFilter, current.selectedSortOrder
            )
        )
    }

    fun updateFilter(filter: ContractFilter) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedFilter = filter,
            filteredContracts = applyFiltersAndSort(
                current.contracts, current.searchQuery, filter, current.selectedSortOrder
            )
        )
    }

    fun updateSortOrder(sortOrder: ContractSortOrder) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedSortOrder = sortOrder,
            filteredContracts = applyFiltersAndSort(
                current.contracts, current.searchQuery, current.selectedFilter, sortOrder
            )
        )
    }
    
    @Suppress("unused")
    fun deleteContract(contractId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = contractId)
            when (val result = deleteContractUseCase(contractId)) {
                is QrResult.Success -> Timber.d("Successfully deleted contract $contractId")
                is QrResult.Error -> {
                    Timber.e("Failed to delete contract $contractId: ${result.error}")
                    _uiState.value = _uiState.value.copy(
                        error = UiText.StringResource(R.string.err_contracts_list_delete_failed)
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isDeleting = null)
        }
    }

    fun renew(contractId: String) {
        Timber.d("TODO: renew contract $contractId")
    }

    fun setActive(contracts: Set<Contract>, active: Boolean) {
        Timber.d("TODO: setActive=$active for ${contracts.size} contracts")
    }

    fun cycleCardVariant() {
        val next = when (_uiState.value.cardVariant) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }
        _uiState.value = _uiState.value.copy(cardVariant = next)
        viewModelScope.launch {
            try { appSettingsRepository.setListViewMode(KEY, next) }
            catch (e: Exception) { Timber.e(e, "Failed to persist card variant") }
        }
    }

    fun dismissError() = run { _uiState.value = _uiState.value.copy(error = null) }
    fun dismissSuccess() = run { _uiState.value = _uiState.value.copy(successMessage = null) }

    // =========================================================================
    // PRIVATE — FLOW OBSERVATION
    // =========================================================================

    private fun loadContracts(clientId: String?) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !_uiState.value.isRefreshing, error = null
            )
            try {
                observeContractsUseCase(clientId)
                    .catch { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Timber.e(e, "Error in contracts flow")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, isRefreshing = false,
                            error = UiText.StringResource(R.string.err_contracts_list_load_contracts)
                        )
                    }
                    .collect { contracts ->
                        val enriched = enrichWithStatistics(contracts)
                        val current = _uiState.value
                        _uiState.value = current.copy(
                            contracts = enriched,
                            filteredContracts = applyFiltersAndSort(
                                enriched, current.searchQuery,
                                current.selectedFilter, current.selectedSortOrder
                            ),
                            isLoading = false, isRefreshing = false, error = null
                        )
                        Timber.d("Contracts flow: ${contracts.size} records (clientId=${clientId ?: "all"})")
                    }
            } catch (_: CancellationException) {
                Timber.d("Contracts observation cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error observing contracts")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResource(R.string.err_contracts_list_load_contracts)
                )
            }
        }
    }

    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(KEY)
                .catch { e -> Timber.e(e, "Error observing card variant") }
                .collect { viewMode -> _uiState.value = _uiState.value.copy(cardVariant = viewMode) }
        }
    }

    private fun loadClientsForDropdown() {
        viewModelScope.launch {
            try {
                observeClientsUseCase()
                    .catch { e -> Timber.e(e, "Error loading clients for dropdown") }
                    .collect { clients ->
                        val options = listOf(ClientOption.ALL) + clients.map { c ->
                            ClientOption(id = c.id, companyName = c.companyName)
                        }
                        _uiState.value = _uiState.value.let { state ->
                            val synced = options.find { it.id == state.clientId } ?: ClientOption.ALL
                            state.copy(availableClients = options, selectedClient = synced)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start client observation")
            }
        }
    }

    private fun enrichWithStatistics(contracts: List<Contract>): List<ContractWithStats> =
        contracts.map { contract ->
            ContractWithStats(
                contract = contract,
                stats = ContractsStatistics(isExpired = !contract.isValid())
            )
        }

    private fun applyFiltersAndSort(
        contracts: List<ContractWithStats>,
        searchQuery: String,
        filter: ContractFilter,
        sortOrder: ContractSortOrder
    ): List<ContractWithStats> {
        var result = contracts

        result = when (filter) {
            ContractFilter.ALL -> result
            ContractFilter.ACTIVE -> result.filter { it.contract.isActive }
            ContractFilter.INACTIVE -> result.filter { !it.contract.isActive }
            ContractFilter.OUTDATED -> result.filter { !it.contract.isValid() }
        }

        if (searchQuery.isNotBlank()) {
            result = result.filter { cws ->
                cws.contract.name?.contains(searchQuery, ignoreCase = true) == true ||
                        cws.contract.description?.contains(searchQuery, ignoreCase = true) == true ||
                        cws.contract.startDate.toString().contains(searchQuery, ignoreCase = true) ||
                        cws.contract.endDate.toString().contains(searchQuery, ignoreCase = true)
            }
        }

        return when (sortOrder) {
            ContractSortOrder.NAME -> result.sortedBy { it.contract.name }
            ContractSortOrder.EXPIRE_RECENT -> result.sortedBy { it.contract.endDate }
            ContractSortOrder.EXPIRE_OLDEST -> result.sortedByDescending { it.contract.endDate }
        }
    }

    // ===== NEW: Bulk Delete Implementation =====

    fun bulkDeleteContracts(contractIds: List<String>) {
        if (contractIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBulkDeleting = true, bulkDeleteProgress = 0, bulkDeleteTotal = contractIds.size
                )
            }

            try {
                Timber.d("Starting bulk delete for ${contractIds.size} contracts")

                var successCount = 0
                val failedIds = mutableListOf<String>()

                contractIds.forEachIndexed { index, contactId ->
                    try {
                        when (val result = deleteContractUseCase(contactId)) {
                            is QrResult.Success -> {
                                successCount++
                                Timber.d("Successfully deleted contract $contactId")
                            }

                            is QrResult.Error -> {
                                failedIds.add(contactId)
                                Timber.e("Failed to delete contact $contactId - ${result.error}")
                            }
                        }
                    } catch (e: Exception) {
                        failedIds.add(contactId)
                        Timber.e(e, "Exception deleting contact: $contactId")
                    }

                    // Update progress
                    _uiState.update {
                        it.copy(
                            bulkDeleteProgress = index + 1
                        )
                    }
                }

                // Show result message
                val errorMessage = when {
                    failedIds.isEmpty() -> {
                        // All deleted successfully
                        null
                    }

                    successCount == 0 -> {
                        // All failed
                        UiText.StringResources(
                            R.string.err_contract_list_bulk_delete_all_failed, failedIds.size
                        )
                    }

                    else -> {
                        // Partial success
                        UiText.StringResources(
                            R.string.err_contract_list_bulk_delete_partial_failed,
                            successCount,
                            failedIds.size
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        error = errorMessage
                    )
                }

                Timber.d("Bulk delete completed: $successCount success, ${failedIds.size} failed")

            } catch (e: Exception) {
                Timber.e(e, "Exception during bulk delete")
                _uiState.update {
                    it.copy(
                        error = UiText.StringResources(
                            R.string.err_contract_list_bulk_delete_unexpected, e.message ?: "")
                    )
                }
            }.also {
                _uiState.update {
                    it.copy(
                        isBulkDeleting = false,
                        bulkDeleteProgress = 0,
                        bulkDeleteTotal = 0
                    )
                }
            }
        }
    }
}

// =============================================================================
// DATA CLASSES
// =============================================================================

data class ContractWithStats(val contract: Contract, val stats: ContractsStatistics)
data class ContractsStatistics(val isExpired: Boolean)

// =============================================================================
// ACTION HANDLER
// =============================================================================

class ContractActionHandler(
    private val onEdit: (Set<Contract>) -> Unit,
    private val onDelete: (Set<Contract>) -> Unit,
    private val onRenew: (Set<Contract>) -> Unit,
    private val onSetActive: (Set<Contract>) -> Unit,
    private val onSetInactive: (Set<Contract>) -> Unit,
    private val onArchive: (Set<Contract>) -> Unit,
    private val onSelectAll: () -> Unit,
    val onPerformDelete: (Set<Contract>) -> Unit
) : SimpleSelectionActionHandler<Contract> {

    override fun onActionClick(action: SelectionAction, selectedItems: Set<Contract>) {
        when (action) {
            SelectionAction.Edit -> onEdit(selectedItems)
            SelectionAction.Delete -> onDelete(selectedItems)
            SelectionAction.Renew -> onRenew(selectedItems)
            SelectionAction.SetActive -> onSetActive(selectedItems)
            SelectionAction.SetInactive -> onSetInactive(selectedItems)
            SelectionAction.Archive -> onArchive(selectedItems)
            SelectionAction.SelectAll -> onSelectAll()
            is SelectionAction.Custom -> Unit
            else -> Unit
        }
    }

    override fun isActionEnabled(action: SelectionAction, selectedItems: Set<Contract>): Boolean =
        when (action) {
            SelectionAction.Edit -> selectedItems.size == 1
            SelectionAction.Delete,
            SelectionAction.SetActive,
            SelectionAction.SetInactive,
            SelectionAction.Archive -> selectedItems.isNotEmpty()
            SelectionAction.SelectAll -> true
            is SelectionAction.Custom -> true
            else -> false
        }

    override fun getDeleteConfirmationMessage(selectedItems: Set<Contract>): UiText =
        when (selectedItems.size) {
            1 -> UiText.StringResources(R.string.contracts_list_delete_confirmation_dates,
                selectedItems.first().startDate, selectedItems.first().endDate)
            else -> UiText.StringResources(R.string.contracts_list_delete_confirmation, selectedItems.size)
        }
}