package net.calvuz.qreport.client.unit.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.usecase.ObserveIslandsUseCase
import net.calvuz.qreport.client.island.presentation.ui.components.DeleteMechanicalUnitUseCase
import net.calvuz.qreport.client.island.presentation.ui.components.IslandOption
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.model.UnitType
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import net.calvuz.qreport.client.unit.domain.usecase.ObserveMechanicalUnitsUseCase
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitFilter
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitPkg
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitSortOrder
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class MechanicalUnitListUiState(
    val allUnits: List<MechanicalUnit> = emptyList(),
    val filteredUnits: List<MechanicalUnit> = emptyList(),
    val islandId: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingUnit: String? = null,
    val searchQuery: String = "",
    val selectedFilter: MechanicalUnitFilter = MechanicalUnitPkg.selectedFilter,
    val sortOrder: MechanicalUnitSortOrder = MechanicalUnitPkg.selectedSortOrder,
    val error: UiText? = null,                  // UiText instead of raw String
    val cardVariant: ListViewMode = ListViewMode.FULL,
    val availableIslands: List<IslandOption> = listOf(IslandOption.ALL),
    val selectedIsland: IslandOption = IslandOption.ALL
)

@HiltViewModel
class MechanicalUnitListViewModel @Inject constructor(
    private val repository: MechanicalUnitRepository,
    private val observeIslandsUseCase: ObserveIslandsUseCase,
    private val observeMechanicalUnitsUseCase: ObserveMechanicalUnitsUseCase,
    private val deleteMechanicalUnitUseCase: DeleteMechanicalUnitUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MechanicalUnitListUiState())
    val uiState: StateFlow<MechanicalUnitListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentIslandId: String = ""

    companion object {
        private const val KEY = AppSettingsDataStore.LIST_KEY_MECHANICAL_UNITS
    }

    init {
        Timber.d("MechanicalUnitListViewModel init")
        observeCardVariant()
        loadIslandsForDropdown()
    }

    // =========================================================================
    // PUBLIC
    // =========================================================================

    fun initialize() = loadUnits()

    fun initializeForIsland(islandId: String) {
        if (islandId == currentIslandId) return
        currentIslandId = islandId
        _uiState.update { state ->
            state.copy(
                islandId = islandId,
                selectedIsland = state.availableIslands.find { it.id == islandId } ?: IslandOption.ALL
            )
        }
        loadUnits()
    }

    fun loadUnits() {
        loadJob?.cancel()
        val islandId = currentIslandId.takeIf { it.isNotEmpty() }
        Timber.d("Observing units islandId=${islandId ?: "all"}")

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !it.isRefreshing, error = null) }
            try {
                observeMechanicalUnitsUseCase(islandId = islandId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception)
                        if (currentCoroutineContext().isActive) {
                            _uiState.update {
                                it.copy(isLoading = false, isRefreshing = false,
                                    error = UiText.StringResource(R.string.err_unit_load))
                            }
                        }
                    }
                    .collect { units ->
                        if (!currentCoroutineContext().isActive) return@collect
                        val current = _uiState.value
                        _uiState.value = current.copy(
                            allUnits = units,
                            filteredUnits = applyFiltersAndSort(units, current.searchQuery, current.selectedFilter, current.sortOrder),
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                        Timber.d("Loaded ${units.size} units from Flow")
                    }
            } catch (_: CancellationException) {
                Timber.d("Units observation cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e)
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false,
                            error = UiText.StringResource(R.string.err_unit_load))
                    }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        loadUnits()
    }

    fun deleteUnit(unitId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingUnit = unitId) }
            when (val delete = deleteMechanicalUnitUseCase(unitId)) {
                is QrResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isDeletingUnit = null,
                            error = delete.error.asUiText()
                        )
                        return@launch
                    }
                }
                is QrResult.Success -> {
                    return@launch
                }
            }

            _uiState.update { it.copy(isDeletingUnit = null) }
        }
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value
        if (query.length >= 3) {
            performSearch(query)
        } else {
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    filteredUnits = applyFiltersAndSort(current.allUnits, query, current.selectedFilter, current.sortOrder)
                )
            }
        }
    }

    fun updateFilter(filter: MechanicalUnitFilter) {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                filteredUnits = applyFiltersAndSort(current.allUnits, current.searchQuery, filter, current.sortOrder)
            )
        }
    }

    fun updateSortOrder(sortOrder: MechanicalUnitSortOrder) {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                sortOrder = sortOrder,
                filteredUnits = applyFiltersAndSort(current.allUnits, current.searchQuery, current.selectedFilter, sortOrder)
            )
        }
    }

    fun updateSelectedIsland(island: IslandOption) {
        if (island == _uiState.value.selectedIsland) return
        currentIslandId = island.id
        _uiState.update { it.copy(selectedIsland = island, islandId = island.id) }
        loadUnits()
    }

    fun cycleCardVariant() {
        val next = when (_uiState.value.cardVariant) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }
        _uiState.update { it.copy(cardVariant = next) }
        viewModelScope.launch {
            try { appSettingsRepository.setListViewMode(KEY, next) }
            catch (e: Exception) { Timber.e(e) }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(KEY)
                .catch { e -> Timber.e(e) }
                .collect { viewMode -> _uiState.update { it.copy(cardVariant = viewMode) } }
        }
    }

    private fun performSearch(query: String) {
        val current = _uiState.value
        // unitType.displayName removed — search only on domain fields
        val filtered = current.allUnits.filter { unit ->
            unit.name.contains(query, ignoreCase = true) ||
                    unit.serialNumber?.contains(query, ignoreCase = true) == true ||
                    unit.model?.contains(query, ignoreCase = true) == true
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredUnits = applyFiltersAndSort(filtered, query, current.selectedFilter, current.sortOrder)
            )
        }
    }

    private fun applyFiltersAndSort(
        units: List<MechanicalUnit>,
        searchQuery: String,
        filter: MechanicalUnitFilter,
        sortOrder: MechanicalUnitSortOrder
    ): List<MechanicalUnit> {
        var result = units

        // Short query local filter (≤2 chars)
        if (searchQuery.isNotBlank() && searchQuery.length < 3) {
            result = result.filter { unit ->
                unit.name.contains(searchQuery, ignoreCase = true) ||
                        unit.serialNumber?.contains(searchQuery, ignoreCase = true) == true
                // unitType.displayName removed
            }
        }

        result = when (filter) {
            MechanicalUnitFilter.ALL -> result
            MechanicalUnitFilter.ACTIVE -> result.filter { it.isActive }
            MechanicalUnitFilter.INACTIVE -> result.filter { !it.isActive }
            MechanicalUnitFilter.ROBOT -> result.filter { it.unitType == UnitType.ROBOT }
        }

        return when (sortOrder) {
            MechanicalUnitSortOrder.CREATED_RECENT -> result.sortedByDescending { it.createdAt }
            MechanicalUnitSortOrder.NAME -> result.sortedBy { it.name.lowercase() }
            MechanicalUnitSortOrder.BY_TYPE -> result.sortedWith(
                compareBy<MechanicalUnit> { it.unitType.name }.thenBy { it.name.lowercase() }
            )
            MechanicalUnitSortOrder.BY_SERIAL -> result.sortedBy { it.serialNumber }
        }
    }

    private fun loadIslandsForDropdown() {
        viewModelScope.launch {
            try {
                observeIslandsUseCase()
                    .catch { e -> Timber.e(e) }
                    .collect { islands ->
                        val options = listOf(IslandOption.ALL) + islands.map { island ->
                            IslandOption(id = island.id, name = island.displayName)
                        }
                        _uiState.update { state ->
                            val synced = options.find { it.id == state.islandId } ?: IslandOption.ALL
                            state.copy(availableIslands = options, selectedIsland = synced)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}

sealed class MechanicalUnitListEvent {
    data class SearchQueryChanged(val query: String) : MechanicalUnitListEvent()
    data class FilterChanged(val filter: MechanicalUnitFilter) : MechanicalUnitListEvent()
    data class SortOrderChanged(val sortOrder: MechanicalUnitSortOrder) : MechanicalUnitListEvent()
    data class DeleteUnit(val unitId: String) : MechanicalUnitListEvent()
    object Refresh : MechanicalUnitListEvent()
    object DismissError : MechanicalUnitListEvent()
}