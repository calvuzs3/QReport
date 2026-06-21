package net.calvuz.qreport.checkup.modules.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.modules.domain.usecase.ObserveActiveModuleTypesUseCase
import net.calvuz.qreport.checkup.modules.domain.usecase.ObserveModuleIslandTypeLinksUseCase
import net.calvuz.qreport.checkup.modules.domain.usecase.UpdateModuleIslandTypeLinksUseCase
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import net.calvuz.qreport.client.island.domain.usecase.ObserveActiveIslandTypesUseCase
import javax.inject.Inject

data class ModuleIslandAssociationUiState(
    val islandTypes: List<IslandTypeMaster> = emptyList(),
    val moduleTypes: List<ModuleTypeMaster> = emptyList(),
    val linksByIslandType: Map<String, List<String>> = emptyMap(),
    val isLoading: Boolean = true,
    val editingIslandType: IslandTypeMaster? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ModuleIslandAssociationViewModel @Inject constructor(
    observeActiveIslandTypes: ObserveActiveIslandTypesUseCase,
    observeActiveModuleTypes: ObserveActiveModuleTypesUseCase,
    observeModuleIslandTypeLinks: ObserveModuleIslandTypeLinksUseCase,
    private val updateModuleIslandTypeLinks: UpdateModuleIslandTypeLinksUseCase
) : ViewModel() {

    private val editingIslandType = MutableStateFlow<IslandTypeMaster?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModuleIslandAssociationUiState> = combine(
        observeActiveIslandTypes(),
        observeActiveModuleTypes(),
        observeModuleIslandTypeLinks(),
        editingIslandType,
        errorMessage
    ) { islandTypes, moduleTypes, links, editing, error ->
        ModuleIslandAssociationUiState(
            islandTypes = islandTypes,
            moduleTypes = moduleTypes,
            linksByIslandType = links,
            isLoading = false,
            editingIslandType = editing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModuleIslandAssociationUiState())

    fun onEditClick(islandType: IslandTypeMaster) {
        editingIslandType.value = islandType
    }

    fun onDismissDialog() {
        editingIslandType.value = null
        errorMessage.value = null
    }

    fun onSave(moduleTypeIds: List<String>) {
        val islandType = editingIslandType.value ?: return
        viewModelScope.launch {
            when (val result = updateModuleIslandTypeLinks(islandType.id, moduleTypeIds)) {
                is QrResult.Success -> onDismissDialog()
                is QrResult.Error -> errorMessage.value = result.error.toString()
            }
        }
    }
}
