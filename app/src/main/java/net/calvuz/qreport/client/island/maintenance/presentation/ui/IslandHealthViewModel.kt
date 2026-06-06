package net.calvuz.qreport.client.island.maintenance.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.usecase.GetIslandByIdUseCase
import net.calvuz.qreport.client.island.maintenance.domain.model.IslandHealthSummary
import net.calvuz.qreport.client.island.maintenance.domain.usecase.GetIslandHealthSummaryUseCase
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// UI STATE
// =============================================================================

data class IslandHealthUiState(
    val isLoading: Boolean = false,
    val islandId: String = "",
    val islandName: String = "",
    val summary: IslandHealthSummary? = null,
    val error: UiText? = null
) {
    val hasData: Boolean get() = summary != null
}

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class IslandHealthViewModel @Inject constructor(
    private val getIslandHealthSummaryUseCase: GetIslandHealthSummaryUseCase,
    private val getIslandByIdUseCase: GetIslandByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IslandHealthUiState())
    val uiState = _uiState.asStateFlow()

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    fun initialize(islandId: String) {
        if (_uiState.value.islandId == islandId && _uiState.value.hasData) return
        _uiState.update { it.copy(islandId = islandId) }
        resolveIslandName(islandId)
        loadSummary(islandId)
    }

    private fun resolveIslandName(islandId: String) {
        viewModelScope.launch {
            when (val result = getIslandByIdUseCase(islandId)) {
                is QrResult.Success -> {
                    val island = result.data
                    _uiState.update { it.copy(islandName = island.customName ?: island.serialNumber) }
                }
                is QrResult.Error ->
                    Timber.w("Could not resolve island name for health screen: $islandId")
            }
        }
    }

    fun loadSummary(islandId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getIslandHealthSummaryUseCase(islandId)) {
                is QrResult.Success ->
                    _uiState.update { it.copy(isLoading = false, summary = result.data) }
                is QrResult.Error -> {
                    Timber.e("Failed to load island health summary: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.maint_error_load)
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        val id = _uiState.value.islandId
        if (id.isNotBlank()) loadSummary(id)
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}