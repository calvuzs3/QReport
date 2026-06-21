package net.calvuz.qreport.checkup.items.presentation.ui

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
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster
import net.calvuz.qreport.checkup.criticality.domain.usecase.ObserveActiveCriticalityLevelsUseCase
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster
import net.calvuz.qreport.checkup.items.domain.usecase.CreateCheckItemTemplateUseCase
import net.calvuz.qreport.checkup.items.domain.usecase.DeactivateCheckItemTemplateUseCase
import net.calvuz.qreport.checkup.items.domain.usecase.ObserveCheckItemTemplatesUseCase
import net.calvuz.qreport.checkup.items.domain.usecase.RestoreCheckItemTemplateUseCase
import net.calvuz.qreport.checkup.items.domain.usecase.UpdateCheckItemTemplateUseCase
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.modules.domain.usecase.ObserveActiveModuleTypesUseCase
import java.util.UUID
import javax.inject.Inject

data class CheckItemTemplatesUiState(
    val templates: List<CheckItemTemplateMaster> = emptyList(),
    val moduleTypes: List<ModuleTypeMaster> = emptyList(),
    val criticalityLevels: List<CriticalityMaster> = emptyList(),
    val isLoading: Boolean = true,
    val editingTemplate: CheckItemTemplateMaster? = null,
    val isCreatingNew: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CheckItemTemplatesViewModel @Inject constructor(
    observeTemplates: ObserveCheckItemTemplatesUseCase,
    observeActiveModuleTypes: ObserveActiveModuleTypesUseCase,
    observeActiveCriticalityLevels: ObserveActiveCriticalityLevelsUseCase,
    private val createTemplate: CreateCheckItemTemplateUseCase,
    private val updateTemplate: UpdateCheckItemTemplateUseCase,
    private val deactivateTemplate: DeactivateCheckItemTemplateUseCase,
    private val restoreTemplate: RestoreCheckItemTemplateUseCase
) : ViewModel() {

    private val editingTemplate = MutableStateFlow<CheckItemTemplateMaster?>(null)
    private val isCreatingNew = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CheckItemTemplatesUiState> = combine(
        observeTemplates(),
        observeActiveModuleTypes(),
        observeActiveCriticalityLevels(),
        editingTemplate,
        isCreatingNew,
        errorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CheckItemTemplatesUiState(
            templates = values[0] as List<CheckItemTemplateMaster>,
            moduleTypes = values[1] as List<ModuleTypeMaster>,
            criticalityLevels = values[2] as List<CriticalityMaster>,
            isLoading = false,
            editingTemplate = values[3] as CheckItemTemplateMaster?,
            isCreatingNew = values[4] as Boolean,
            errorMessage = values[5] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CheckItemTemplatesUiState())

    fun onAddClick() {
        isCreatingNew.value = true
    }

    fun onEditClick(template: CheckItemTemplateMaster) {
        editingTemplate.value = template
    }

    fun onDismissDialog() {
        isCreatingNew.value = false
        editingTemplate.value = null
        errorMessage.value = null
    }

    fun onSave(
        category: String,
        description: String,
        moduleTypeId: String,
        criticalityId: String,
        orderIndex: Int
    ) {
        viewModelScope.launch {
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val current = editingTemplate.value

            val template = if (current != null) {
                current.copy(
                    category = category,
                    description = description,
                    moduleTypeId = moduleTypeId,
                    criticalityId = criticalityId,
                    orderIndex = orderIndex,
                    updatedAt = now
                )
            } else {
                CheckItemTemplateMaster(
                    id = UUID.randomUUID().toString(),
                    moduleTypeId = moduleTypeId,
                    category = category,
                    description = description,
                    criticalityId = criticalityId,
                    orderIndex = orderIndex,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val result = if (current != null) updateTemplate(template) else createTemplate(template)

            when (result) {
                is QrResult.Success -> onDismissDialog()
                is QrResult.Error -> errorMessage.value = result.error.toString()
            }
        }
    }

    fun onDeactivate(id: String) {
        viewModelScope.launch { deactivateTemplate(id) }
    }

    fun onRestore(id: String) {
        viewModelScope.launch { restoreTemplate(id) }
    }
}
