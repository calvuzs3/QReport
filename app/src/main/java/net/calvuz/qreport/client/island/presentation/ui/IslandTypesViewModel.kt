package net.calvuz.qreport.client.island.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import net.calvuz.qreport.client.island.domain.usecase.CreateIslandTypeUseCase
import net.calvuz.qreport.client.island.domain.usecase.DeactivateIslandTypeUseCase
import net.calvuz.qreport.client.island.domain.usecase.ObserveIslandTypesUseCase
import net.calvuz.qreport.client.island.domain.usecase.RestoreIslandTypeUseCase
import net.calvuz.qreport.client.island.domain.usecase.UpdateIslandTypeUseCase
import java.util.UUID
import javax.inject.Inject

data class IslandTypesUiState(
    val types: List<IslandTypeMaster> = emptyList(),
    val isLoading: Boolean = true,
    val editingType: IslandTypeMaster? = null,
    val isCreatingNew: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class IslandTypesViewModel @Inject constructor(
    observeIslandTypes: ObserveIslandTypesUseCase,
    private val createIslandType: CreateIslandTypeUseCase,
    private val updateIslandType: UpdateIslandTypeUseCase,
    private val deactivateIslandType: DeactivateIslandTypeUseCase,
    private val restoreIslandType: RestoreIslandTypeUseCase
) : ViewModel() {

    private val editingType = MutableStateFlow<IslandTypeMaster?>(null)
    private val isCreatingNew = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<IslandTypesUiState> = combine(
        observeIslandTypes(),
        editingType,
        isCreatingNew,
        errorMessage
    ) { types, editing, creatingNew, error ->
        IslandTypesUiState(
            types = types,
            isLoading = false,
            editingType = editing,
            isCreatingNew = creatingNew,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IslandTypesUiState())

    fun onAddClick() {
        isCreatingNew.value = true
    }

    fun onEditClick(type: IslandTypeMaster) {
        editingType.value = type
    }

    fun onDismissDialog() {
        isCreatingNew.value = false
        editingType.value = null
        errorMessage.value = null
    }

    fun onSave(
        code: String,
        label: String,
        description: String?,
        iconName: String?,
        maintenanceIntervalDays: Int,
        sortOrder: Int
    ) {
        viewModelScope.launch {
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val current = editingType.value

            val type = if (current != null) {
                current.copy(
                    code = code,
                    label = label,
                    description = description,
                    iconName = iconName,
                    maintenanceIntervalDays = maintenanceIntervalDays,
                    sortOrder = sortOrder,
                    updatedAt = now
                )
            } else {
                IslandTypeMaster(
                    id = UUID.randomUUID().toString(),
                    code = code,
                    label = label,
                    description = description,
                    iconName = iconName,
                    maintenanceIntervalDays = maintenanceIntervalDays,
                    sortOrder = sortOrder,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val result = if (current != null) updateIslandType(type) else createIslandType(type)

            when (result) {
                is QrResult.Success -> onDismissDialog()
                is QrResult.Error -> errorMessage.value = result.error.toString()
            }
        }
    }

    fun onDeactivate(id: String) {
        viewModelScope.launch { deactivateIslandType(id) }
    }

    fun onRestore(id: String) {
        viewModelScope.launch { restoreIslandType(id) }
    }
}
