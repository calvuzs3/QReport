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
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.modules.domain.usecase.CreateModuleTypeUseCase
import net.calvuz.qreport.checkup.modules.domain.usecase.DeactivateModuleTypeUseCase
import net.calvuz.qreport.checkup.modules.domain.usecase.ObserveModuleTypesUseCase
import net.calvuz.qreport.checkup.modules.domain.usecase.RestoreModuleTypeUseCase
import net.calvuz.qreport.checkup.modules.domain.usecase.UpdateModuleTypeUseCase
import java.util.UUID
import javax.inject.Inject

data class ModuleTypesUiState(
    val types: List<ModuleTypeMaster> = emptyList(),
    val isLoading: Boolean = true,
    val editingType: ModuleTypeMaster? = null,
    val isCreatingNew: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ModuleTypesViewModel @Inject constructor(
    observeModuleTypes: ObserveModuleTypesUseCase,
    private val createModuleType: CreateModuleTypeUseCase,
    private val updateModuleType: UpdateModuleTypeUseCase,
    private val deactivateModuleType: DeactivateModuleTypeUseCase,
    private val restoreModuleType: RestoreModuleTypeUseCase
) : ViewModel() {

    private val editingType = MutableStateFlow<ModuleTypeMaster?>(null)
    private val isCreatingNew = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModuleTypesUiState> = combine(
        observeModuleTypes(),
        editingType,
        isCreatingNew,
        errorMessage
    ) { types, editing, creatingNew, error ->
        ModuleTypesUiState(
            types = types,
            isLoading = false,
            editingType = editing,
            isCreatingNew = creatingNew,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModuleTypesUiState())

    fun onAddClick() {
        isCreatingNew.value = true
    }

    fun onEditClick(type: ModuleTypeMaster) {
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
                    sortOrder = sortOrder,
                    updatedAt = now
                )
            } else {
                ModuleTypeMaster(
                    id = UUID.randomUUID().toString(),
                    code = code,
                    label = label,
                    description = description,
                    sortOrder = sortOrder,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val result = if (current != null) updateModuleType(type) else createModuleType(type)

            when (result) {
                is QrResult.Success -> onDismissDialog()
                is QrResult.Error -> errorMessage.value = result.error.toString()
            }
        }
    }

    fun onDeactivate(id: String) {
        viewModelScope.launch { deactivateModuleType(id) }
    }

    fun onRestore(id: String) {
        viewModelScope.launch { restoreModuleType(id) }
    }
}
