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

/** ContractListScreen UiState */
data class ContractListUiState(
    val clientId: String = "",
    val contracts: List<ContractWithStats> = emptyList(),
    val filteredContracts: List<ContractWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeleting: String? = null,
    val error: UiText? = null,
    val searchQuery: String = "",
    val selectedFilter: ContractFilter = ContractFilter.ACTIVE,
    val selectedSortOrder: ContractSortOrder = ContractSortOrder.EXPIRE_RECENT,
)

/** ContractListScreen Filter */
enum class ContractFilter { ACTIVE, INACTIVE, ALL }

/** ContractListScreen SortOrder */
enum class ContractSortOrder { EXPIRE_RECENT, EXPIRE_OLDEST, NAME }

@HiltViewModel
class ContractListViewModel @Inject constructor(
    private val getContractsByClientUseCase: GetContractsByClientUseCase,
    private val deleteContractUseCase: DeleteContractUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState: StateFlow<ContractListUiState> = _uiState.asStateFlow()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

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

    // ===== Error handling =====

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

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