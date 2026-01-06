package net.calvuz.qreport.client.client.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.model.ClientSingleStatistics
import net.calvuz.qreport.client.client.domain.usecase.GetClientStatisticsUseCase
import net.calvuz.qreport.client.client.domain.usecase.DeleteClientUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetAllActiveClientsUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetAllActiveClientsWithContactsUseCase
import net.calvuz.qreport.client.client.domain.usecase.ObserveAllActiveClientsUseCase
import net.calvuz.qreport.client.client.domain.usecase.SearchClientsUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetAllActiveClientsWithFacilitiesUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetAllActiveClientsWithIslandsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per ClientListScreen - VERSIONE OTTIMIZZATA CON METODI USE CASE REALI
 *
 * Features:
 * - Usa observeActiveClients() per Flow reattivo
 * - Usa getActiveClients() per refresh one-shot
 * - Sfrutta metodi specializzati (getClientsWithFacilities, etc.)
 * - Gestione filtri ottimizzata con backend queries
 * - Performance migliorate
 */

data class ClientListUiState(
    val clients: List<ClientWithStats> = emptyList(),
    val filteredClients: List<ClientWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: ClientFilter = ClientFilter.ALL,
    val selectedSortOrder: ClientSortOrder = ClientSortOrder.CREATED_RECENT
)

enum class ClientFilter {
    ALL, ACTIVE, INACTIVE, WITH_FACILITIES, WITH_CONTACTS, WITH_ISLANDS
}

enum class ClientSortOrder {
    COMPANY_NAME, CREATED_RECENT, CREATED_OLDEST, FACILITIES_COUNT, CHECKUPS_COUNT
}

@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val getAllActiveClientsUseCase: GetAllActiveClientsUseCase,
    private val getAllActiveClientsWithFacilitiesUseCase: GetAllActiveClientsWithFacilitiesUseCase,
    private val getAllActiveClientWithContactsUseCase: GetAllActiveClientsWithContactsUseCase,
    private val getAllActiveClientsWithIslandsUseCase: GetAllActiveClientsWithIslandsUseCase,
    private val observeAllActiveClientsUseCase: ObserveAllActiveClientsUseCase,
    private val getClientStatisticsUseCase: GetClientStatisticsUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
    private val searchClientsUseCase: SearchClientsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientListUiState())
    val uiState: StateFlow<ClientListUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ClientListViewModel initialized with optimized use case calls")
        loadClients()
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Timber.d("Loading clients list with observeActiveClients()")

                observeAllActiveClientsUseCase()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in observeActiveClients flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = "Errore caricamento clienti: ${exception.message}"
                            )
                        }
                    }
                    .collect { clients ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping clients processing - job cancelled")
                            return@collect
                        }

                        // Enrich with REAL statistics
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

                            Timber.d("Loaded ${clients.size} clients successfully")
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
                        error = "Errore caricamento clienti: ${e.message}"
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                Timber.d("Refreshing clients list with getActiveClients()")

                // Use getActiveClients() for one-shot refresh operation
                getAllActiveClientsUseCase()
                    .fold(
                        onSuccess = { clients ->
                            if (!currentCoroutineContext().isActive) {
                                Timber.d("Skipping refresh processing - job cancelled")
                                return@launch
                            }

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
                                    isRefreshing = false,
                                    error = null
                                )

                                Timber.d("Refresh completed successfully")
                            }
                        },
                        onFailure = { error ->
                            if (currentCoroutineContext().isActive) {
                                Timber.e(error, "Failed to refresh clients")
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = "Errore refresh: ${error.message}"
                                )
                            }
                        }
                    )

            } catch (_: CancellationException) {
                Timber.d("Refresh cancelled")
                if (currentCoroutineContext().isActive) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to refresh clients")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Errore refresh: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting client: $clientId")

                deleteClientUseCase(clientId).fold(
                    onSuccess = {
                        Timber.d("Client deleted successfully")
                        // The list will be automatically updated via observeActiveClients() Flow
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            Timber.e(error, "Failed to delete client")
                            _uiState.value = _uiState.value.copy(
                                error = "Errore eliminazione cliente: ${error.message}"
                            )
                        }
                    }
                )

            } catch (_: CancellationException) {
                Timber.d("Delete operation cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Exception deleting client")
                    _uiState.value = _uiState.value.copy(
                        error = "Errore imprevisto: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        // Se c'è una query significativa, usa SearchClientsUseCase
        if (query.isNotBlank() && query.length > 2) {
            performSearch(query)
        } else {
            // Per query vuote o troppo corte, filtra la lista corrente
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
        // Per alcuni filtri, usa metodi specializzati del use case per performance migliori
        when (filter) {
            ClientFilter.WITH_FACILITIES -> loadClientsWithSpecialFilter { getAllActiveClientsWithFacilitiesUseCase() }
            ClientFilter.WITH_CONTACTS -> loadClientsWithSpecialFilter { getAllActiveClientWithContactsUseCase() }
            ClientFilter.WITH_ISLANDS -> loadClientsWithSpecialFilter { getAllActiveClientsWithIslandsUseCase() }
            else -> {
                // Per filtri semplici, usa filtro locale
                val currentState = _uiState.value
                val filteredAndSorted = applyFiltersAndSort(
                    currentState.clients,
                    currentState.searchQuery,
                    filter,
                    currentState.selectedSortOrder
                )

                _uiState.value = currentState.copy(
                    selectedFilter = filter,
                    filteredClients = filteredAndSorted
                )
            }
        }
    }

    private fun loadClientsWithSpecialFilter(filterCall: suspend () -> Result<List<Client>>) {
        viewModelScope.launch {
            try {
                Timber.d("Loading clients with specialized filter")

                filterCall().fold(
                    onSuccess = { clients ->
                        if (currentCoroutineContext().isActive) {
                            val clientsWithStats = enrichWithStatistics(clients)
                            val currentState = _uiState.value

                            _uiState.value = currentState.copy(
                                filteredClients = clientsWithStats,
                                selectedFilter = currentState.selectedFilter
                            )
                        }
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            Timber.e(error, "Failed to load clients with filter")
                            _uiState.value = _uiState.value.copy(
                                error = "Errore applicazione filtro: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Exception applying filter")
                    _uiState.value = _uiState.value.copy(
                        error = "Errore imprevisto: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateSortOrder(clientSortOrder: ClientSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.clients,
            currentState.searchQuery,
            currentState.selectedFilter,
            clientSortOrder
        )

        _uiState.value = currentState.copy(
            selectedSortOrder = clientSortOrder,
            filteredClients = filteredAndSorted
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private suspend fun enrichWithStatistics(clients: List<Client>): List<ClientWithStats> {
        return clients.map { client ->
            val stats = try {
                getClientStatisticsUseCase(client.id).getOrElse { error ->
                    Timber.w("Failed to get stats for client ${client.id}: ${error.message}")
                    createEmptyStats()
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception getting stats for client ${client.id}")
                createEmptyStats()
            }

            ClientWithStats(client = client, stats = stats)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                Timber.d("Performing search for: $query")

                searchClientsUseCase(query).fold(
                    onSuccess = { searchResults ->
                        if (currentCoroutineContext().isActive) {
                            val clientsWithStats = enrichWithStatistics(searchResults)

                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                clientsWithStats,
                                query,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            )

                            _uiState.value = currentState.copy(
                                searchQuery = query,
                                filteredClients = filteredAndSorted
                            )

                            Timber.d("Search completed with ${searchResults.size} results")
                        }
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            Timber.e(error, "Search failed")
                            _uiState.value = _uiState.value.copy(
                                error = "Errore ricerca: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Exception during search")
                    _uiState.value = _uiState.value.copy(
                        error = "Errore ricerca: ${e.message}"
                    )
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
        var filtered = clients

        // Apply status filter (solo per filtri che non usano metodi specializzati)
        filtered = when (filter) {
            ClientFilter.ALL -> filtered
            ClientFilter.ACTIVE -> filtered.filter { it.client.isActive }
            ClientFilter.INACTIVE -> filtered.filter { !it.client.isActive }
            // I filtri WITH_* sono gestiti dai metodi specializzati
            ClientFilter.WITH_FACILITIES,
            ClientFilter.WITH_CONTACTS,
            ClientFilter.WITH_ISLANDS -> filtered
        }

        // Apply local search query (solo per query corte)
        if (searchQuery.isNotBlank() && searchQuery.length <= 2) {
            filtered = filtered.filter { clientWithStats ->
                val client = clientWithStats.client
                client.companyName.contains(searchQuery, ignoreCase = true) ||
                        client.vatNumber?.contains(searchQuery, ignoreCase = true) == true ||
                        client.headquarters?.city?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        // Apply sorting
        filtered = when (clientSortOrder) {
            ClientSortOrder.COMPANY_NAME -> filtered.sortedBy { it.client.companyName }
            ClientSortOrder.CREATED_RECENT -> filtered.sortedByDescending { it.client.createdAt }
            ClientSortOrder.CREATED_OLDEST -> filtered.sortedBy { it.client.createdAt }
            ClientSortOrder.FACILITIES_COUNT -> filtered.sortedByDescending { it.stats.facilitiesCount }
            ClientSortOrder.CHECKUPS_COUNT -> filtered.sortedByDescending { it.stats.totalCheckUps }
        }

        return filtered
    }

    private fun createEmptyStats() = ClientSingleStatistics(
        facilitiesCount = 0,
        islandsCount = 0,
        contactsCount = 0,
        totalCheckUps = 0,
        completedCheckUps = 0,
        lastCheckUpDate = null
    )
}

/**
 * Data class per cliente con statistiche
 */
data class ClientWithStats(
    val client: Client,
    val stats: ClientSingleStatistics
)