package net.calvuz.qreport.client.client.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.usecase.DeleteClientUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetActiveClientsWithContactsUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetActiveClientsWithContractsUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetActiveClientsWithFacilitiesUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetActiveClientsWithIslandsUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetClientStatisticsUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetClientsUseCase
import net.calvuz.qreport.client.client.domain.usecase.ObserveClientsUseCase
import net.calvuz.qreport.client.client.domain.usecase.SearchClientsUseCase
import net.calvuz.qreport.client.client.presentation.model.ClientFilter
import net.calvuz.qreport.client.client.presentation.model.ClientPkg
import net.calvuz.qreport.client.client.presentation.model.ClientSortOrder
import net.calvuz.qreport.client.client.presentation.model.ClientStatistics
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject

data class ClientListUiState(
    val clients: List<ClientWithStats> = emptyList(),
    val filteredClients: List<ClientWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    val searchQuery: String = "",
    val selectedFilter: ClientFilter = ClientPkg.selectedFilter,
    val selectedSortOrder: ClientSortOrder = ClientPkg.selectedSortOrder,
    val cardVariant: ListViewMode = ListViewMode.FULL
)

@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val getClientsUseCase: GetClientsUseCase,
    private val getActiveClientsWithFacilitiesUseCase: GetActiveClientsWithFacilitiesUseCase,
    private val getAllActiveClientWithContactsUseCase: GetActiveClientsWithContactsUseCase,
    private val getAllActiveClientWithContractsUseCase: GetActiveClientsWithContractsUseCase,
    private val getActiveClientsWithIslandsUseCase: GetActiveClientsWithIslandsUseCase,
    private val observeClientsUseCase: ObserveClientsUseCase,
    private val getClientStatisticsUseCase: GetClientStatisticsUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
    private val searchClientsUseCase: SearchClientsUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientListUiState())
    val uiState: StateFlow<ClientListUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ClientListViewModel initialized")
        observeCardVariant()
        loadClients()
    }

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                observeClientsUseCase()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in observeClients flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResource(R.string.err_client_load_clients)
                            )
                        }
                    }
                    .collect { clients ->
                        if (!currentCoroutineContext().isActive) return@collect
                        val clientsWithStats = enrichWithStatistics(clients)
                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                clientsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            )
                            _uiState.value = currentState.copy(
                                clients = clientsWithStats,
                                filteredClients = filteredAndSorted,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )
                            Timber.d("Loaded ${clients.size} clients")
                        }
                    }
            } catch (_: CancellationException) {
                Timber.d("Clients loading cancelled (normal during navigation)")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to load clients")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = UiText.StringResource(R.string.err_client_load_clients)
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            delay(500)

            when (val result = getClientsUseCase()) {
                is QrResult.Success -> {
                    if (!currentCoroutineContext().isActive) return@launch
                    val clientsWithStats = enrichWithStatistics(result.data)
                    if (currentCoroutineContext().isActive) {
                        val currentState = _uiState.value
                        _uiState.value = currentState.copy(
                            clients = clientsWithStats,
                            filteredClients = applyFiltersAndSort(
                                clientsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            ),
                            isRefreshing = false,
                            error = null
                        )
                        Timber.d("Refresh completed successfully")
                    }
                }
                is QrResult.Error -> {
                    if (currentCoroutineContext().isActive) {
                        Timber.e("Failed to refresh clients: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = UiText.StringResource(R.string.err_client_refresh)
                        )
                    }
                }
            }
        }
    }

    fun inactivateClient(clientId: String) {
        viewModelScope.launch {
            Timber.d("Inactivating client: $clientId")

            when (val result = deleteClientUseCase(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Client inactivated successfully")
                    // List updates automatically via observeClientsUseCase Flow
                }
                is QrResult.Error -> {
                    if (currentCoroutineContext().isActive) {
                        Timber.e("Failed to inactivate client: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            error = UiText.StringResource(R.string.err_client_inactivate)
                        )
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        if (query.isNotBlank() && query.length > 2) {
            performSearch(query)
        } else {
            val currentState = _uiState.value
            val filteredAndSorted = applyFiltersAndSort(
                currentState.clients,
                query,
                currentState.selectedFilter,
                currentState.selectedSortOrder
            )
            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredClients = filteredAndSorted
            )
        }
    }

    fun updateFilter(filter: ClientFilter) {
        when (filter) {
            ClientFilter.WITH_FACILITIES -> loadClientsWithSpecialFilter { getActiveClientsWithFacilitiesUseCase() }
            ClientFilter.WITH_ISLANDS -> loadClientsWithSpecialFilter { getActiveClientsWithIslandsUseCase() }
            ClientFilter.WITH_CONTACTS -> loadClientsWithSpecialFilter { getAllActiveClientWithContactsUseCase() }
            ClientFilter.WITH_CONTRACTS -> loadClientsWithSpecialFilter { getAllActiveClientWithContractsUseCase() }
            else -> {
                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    selectedFilter = filter,
                    filteredClients = applyFiltersAndSort(
                        currentState.clients,
                        currentState.searchQuery,
                        filter,
                        currentState.selectedSortOrder
                    )
                )
            }
        }
    }

    fun updateSortOrder(clientSortOrder: ClientSortOrder) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedSortOrder = clientSortOrder,
            filteredClients = applyFiltersAndSort(
                currentState.clients,
                currentState.searchQuery,
                currentState.selectedFilter,
                clientSortOrder
            )
        )
    }

    /**
     * Cycles through card display variants: FULL → COMPACT → MINIMAL → FULL.
     * The preference is persisted via [AppSettingsRepository].
     */
    fun cycleCardVariant() {
        val next = when (_uiState.value.cardVariant) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }
        _uiState.value = _uiState.value.copy(cardVariant = next)

        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(AppSettingsDataStore.LIST_KEY_CLIENTS, next)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist card variant preference")
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // =========================================================================
    // PRIVATE METHODS
    // =========================================================================

    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(AppSettingsDataStore.LIST_KEY_CLIENTS)
                .catch { e -> Timber.e(e, "Error observing card variant preference") }
                .collect { viewMode -> _uiState.value = _uiState.value.copy(cardVariant = viewMode) }
        }
    }

    private suspend fun enrichWithStatistics(clients: List<Client>): List<ClientWithStats> =
        clients.map { client ->
            val stats = when (val result = getClientStatisticsUseCase(client.id)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.w("Failed to get stats for client ${client.id}: ${result.error}")
                    createEmptyStats()
                }
            }
            ClientWithStats(client = client, stats = stats)
        }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            when (val result = searchClientsUseCase(query)) {
                is QrResult.Success -> {
                    if (currentCoroutineContext().isActive) {
                        val clientsWithStats = enrichWithStatistics(result.data)
                        val currentState = _uiState.value
                        _uiState.value = currentState.copy(
                            searchQuery = query,
                            filteredClients = applyFiltersAndSort(
                                clientsWithStats,
                                query,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            )
                        )
                        Timber.d("Search completed with ${result.data.size} results")
                    }
                }
                is QrResult.Error -> {
                    if (currentCoroutineContext().isActive) {
                        Timber.e("Search failed: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            error = UiText.StringResource(R.string.err_client_search)
                        )
                    }
                }
            }
        }
    }

    // Lambda signature updated to QrResult to match migrated use cases
    private fun loadClientsWithSpecialFilter(
        filterCall: suspend () -> QrResult<List<Client>, *>
    ) {
        viewModelScope.launch {
            when (val result = filterCall()) {
                is QrResult.Success -> {
                    if (currentCoroutineContext().isActive) {
                        val clientsWithStats = enrichWithStatistics(result.data)
                        val currentState = _uiState.value
                        _uiState.value = currentState.copy(
                            filteredClients = clientsWithStats,
                            selectedFilter = currentState.selectedFilter
                        )
                    }
                }
                is QrResult.Error -> {
                    if (currentCoroutineContext().isActive) {
                        Timber.e("Failed to load clients with filter: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            error = UiText.StringResource(R.string.err_client_filter)
                        )
                    }
                }
            }
        }
    }

    private fun applyFiltersAndSort(
        clients: List<ClientWithStats>,
        searchQuery: String,
        filter: ClientFilter,
        clientSortOrder: ClientSortOrder
    ): List<ClientWithStats> {
        var filtered = when (filter) {
            ClientFilter.ALL -> clients
            ClientFilter.ACTIVE -> clients.filter { it.client.isActive }
            ClientFilter.INACTIVE -> clients.filter { !it.client.isActive }
            // WITH_* filters handled upstream by specialized use cases
            ClientFilter.WITH_FACILITIES,
            ClientFilter.WITH_CONTACTS,
            ClientFilter.WITH_CONTRACTS,
            ClientFilter.WITH_ISLANDS -> clients
        }

        // Local filter for short queries (≤2 chars); longer queries go through SearchClientsUseCase
        if (searchQuery.isNotBlank() && searchQuery.length <= 2) {
            filtered = filtered.filter { clientWithStats ->
                val client = clientWithStats.client
                client.companyName.contains(searchQuery, ignoreCase = true) ||
                        client.headquarters?.city?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        return when (clientSortOrder) {
            ClientSortOrder.COMPANY_NAME -> filtered.sortedBy { it.client.companyName }
            ClientSortOrder.CREATED_RECENT -> filtered.sortedByDescending { it.client.createdAt }
            ClientSortOrder.CREATED_OLDEST -> filtered.sortedBy { it.client.createdAt }
            ClientSortOrder.FACILITIES_COUNT -> filtered.sortedByDescending { it.stats.facilitiesCount }
            ClientSortOrder.CHECKUPS_COUNT -> filtered.sortedByDescending { it.stats.totalCheckUps }
        }
    }

    private fun createEmptyStats() = ClientStatistics(
        facilitiesCount = 0,
        islandsCount = 0,
        contactsCount = 0,
        contractsCount = 0,
        totalCheckUps = 0,
        completedCheckUps = 0,
        lastCheckUpDate = null
    )
}

/** Client domain model paired with its computed display statistics. */
data class ClientWithStats(
    val client: Client,
    val stats: ClientStatistics
)