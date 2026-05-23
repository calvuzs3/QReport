package net.calvuz.qreport.client.unit.presentation.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import net.calvuz.qreport.client.island.presentation.model.IslandFilter
import net.calvuz.qreport.client.island.presentation.model.IslandSortOrder
import net.calvuz.qreport.client.island.presentation.ui.IslandWithStats
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import net.calvuz.qreport.settings.domain.model.ListViewMode
import javax.inject.Inject

//data class MechanicalUnitListUiState(
//    val islands: List<IslandWithStats> = emptyList(),
//    val facilityId: String = "",
//    val isLoading: Boolean = false,
//    val isRefreshing: Boolean = false,
//    val isDeletingIsland: String? = null,
//    val allIslands: List<Island> = emptyList(),
//    val filteredIslands: List<IslandWithStats> = emptyList(),
//    val searchQuery: String = "",
//    val selectedFilter: IslandFilter = IslandFilter.ACTIVE,
//    val sortOrder: IslandSortOrder = IslandSortOrder.CUSTOM_NAME,
//    val statistics: FacilityOperationalSummary? = null,
//    val searchSuggestions: List<Island> = emptyList(),
//    val error: String? = null,
//    val cardVariant: ListViewMode = ListViewMode.FULL
//)
//
//data class MechanicalUnitWithStats(
//    val mechanicalUnit: MechanicalUnit,
//    val stats: MechanicalUnitStatistics
//)
//
//data class MechanicalUnitStatistics(
//   val emptyField: String = ""
//) {
//    // Empty body
//}

/**
 * ViewModel for [MechanicalUnitListScreen].
 *
 * Receives [islandId] from the navigation back-stack saved state.
 */
@HiltViewModel
class MechanicalUnitListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MechanicalUnitRepository
) : ViewModel() {

    private val islandId: String = checkNotNull(savedStateHandle["islandId"])

    /** Live list of mechanical units for the current island. */
    val units: StateFlow<List<MechanicalUnit>> = repository
        .getForIslandFlow(islandId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() = _error.update { null }

    fun delete(unit: MechanicalUnit) {
        viewModelScope.launch {
            repository.delete(unit.id).onFailure { e ->
                _error.update { e.message }
            }
        }
    }
}

