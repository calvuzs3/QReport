package net.calvuz.qreport.client.contract.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.client.contract.data.local.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.usecase.DeleteContractUseCase
import net.calvuz.qreport.client.contract.domain.usecase.GetContractsByClientUseCase
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber
import javax.inject.Inject


data class ContractListUiState(
    val clientId: String = "",
    val contracts: List<ContractWithStats> = emptyList(),
    val filteredContracts: List<ContractWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeleting: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: ContractFilter = ContractFilter.ACTIVE,
    val selectedSortOrder: ContractSortOrder = ContractSortOrder.EXPIRE_RECENT,
)


enum class ContractFilter {
    ALL,
    ACTIVE,
    INACTIVE
}

enum class ContractSortOrder {
    EXPIRE_RECENT,
    EXPIRE_OLDEST,
    NAME
}

@HiltViewModel
class ContractListViewModel @Inject constructor(
    private val getContractsByClientUseCase: GetContractsByClientUseCase,
    private val deleteContractUseCase: DeleteContractUseCase
): ViewModel() {

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
                error = "ID cliente non valido"
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
                                error = "Errore caricamento Contatti: ${exception.message}"
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

                            Timber.d("Loaded ${contacts.size} contacts successfully")
                        }
                    }
            } catch (_: CancellationException) {
                Timber.d("Contacts loading cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Exception loading contacts")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
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
                            Timber.e( "Failed to refresh contracts")
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = "Errore refresh: ${result.error}"
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
                        error = "Errore refresh: ${e.message}"
                    )
                }
            }
        }
    }

    fun delete(contractId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = contractId)

            try {
                when (val result = deleteContractUseCase(contractId)) {
                    is QrResult.Success -> {
                        Timber.d("Contact deleted successfully: $contractId")
                        // Ricarica la lista per mostrare i cambiamenti
                        refresh()
                    }
                    is QrResult.Error -> {
                        Timber.e( "Failed to delete contract $contractId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = null,
                            error = "Errore eliminazione Contratto: ${contractId}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting contract")
                _uiState.value = _uiState.value.copy(
                    isDeleting = null,
                    error = "Errore imprevisto: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = null)
            }
        }
    }

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

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    suspend fun enrichWithStatistics(contracts: List<Contract>): List<ContractWithStats> {
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
                val descriptionMatch = contract.description?.contains(searchQuery, ignoreCase = true) ?: false
                val startDateMatch = contract.startDate.toString().contains(searchQuery, ignoreCase = true)
                val endDateMatch = contract.endDate.toString().contains(searchQuery, ignoreCase = true)

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
//
//    private fun performSearch(query: String) {
//        val currentState = _uiState.value
//        val filtered = currentState.contracts.filter { contractWithStats ->
//
//            if (contractWithStats.contract.name != null)
//                contractWithStats.contract.name.contains(query, ignoreCase = true)
//            else false
//
//            if (contractWithStats.contract.description != null)
//                contractWithStats.contract.description.contains(query, ignoreCase = true)
//            else false
//
//            contractWithStats.contract.startDate.toString().contains(query, ignoreCase = true)
//            contractWithStats.contract.endDate.toString().contains(query, ignoreCase = true)
//
//        }
//
//        val filteredAndSorted = applyFiltersAndSort(
//            filtered,
//            query,
//            currentState.selectedFilter,
//            currentState.sortOrder
//        )
//
//        _uiState.value = currentState.copy(
//            searchQuery = query,
//            filteredContracts = filteredAndSorted
//        )
//    }
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
