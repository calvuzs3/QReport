package net.calvuz.qreport.checkup.criticality.presentation.ui

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
import net.calvuz.qreport.checkup.criticality.domain.usecase.CreateCriticalityLevelUseCase
import net.calvuz.qreport.checkup.criticality.domain.usecase.DeactivateCriticalityLevelUseCase
import net.calvuz.qreport.checkup.criticality.domain.usecase.ObserveCriticalityLevelsUseCase
import net.calvuz.qreport.checkup.criticality.domain.usecase.RestoreCriticalityLevelUseCase
import net.calvuz.qreport.checkup.criticality.domain.usecase.UpdateCriticalityLevelUseCase
import java.util.UUID
import javax.inject.Inject

data class CriticalityLevelsUiState(
    val levels: List<CriticalityMaster> = emptyList(),
    val isLoading: Boolean = true,
    val editingLevel: CriticalityMaster? = null,
    val isCreatingNew: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CriticalityLevelsViewModel @Inject constructor(
    observeCriticalityLevels: ObserveCriticalityLevelsUseCase,
    private val createCriticalityLevel: CreateCriticalityLevelUseCase,
    private val updateCriticalityLevel: UpdateCriticalityLevelUseCase,
    private val deactivateCriticalityLevel: DeactivateCriticalityLevelUseCase,
    private val restoreCriticalityLevel: RestoreCriticalityLevelUseCase
) : ViewModel() {

    private val editingLevel = MutableStateFlow<CriticalityMaster?>(null)
    private val isCreatingNew = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CriticalityLevelsUiState> = combine(
        observeCriticalityLevels(),
        editingLevel,
        isCreatingNew,
        errorMessage
    ) { levels, editing, creatingNew, error ->
        CriticalityLevelsUiState(
            levels = levels,
            isLoading = false,
            editingLevel = editing,
            isCreatingNew = creatingNew,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CriticalityLevelsUiState())

    fun onAddClick() {
        isCreatingNew.value = true
    }

    fun onEditClick(level: CriticalityMaster) {
        editingLevel.value = level
    }

    fun onDismissDialog() {
        isCreatingNew.value = false
        editingLevel.value = null
        errorMessage.value = null
    }

    fun onSave(
        code: String,
        label: String,
        priority: Int,
        colorHex: String,
        iconEmoji: String?,
        sortOrder: Int
    ) {
        viewModelScope.launch {
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val current = editingLevel.value

            val level = if (current != null) {
                current.copy(
                    code = code,
                    label = label,
                    priority = priority,
                    colorHex = colorHex,
                    iconEmoji = iconEmoji,
                    sortOrder = sortOrder,
                    updatedAt = now
                )
            } else {
                CriticalityMaster(
                    id = UUID.randomUUID().toString(),
                    code = code,
                    label = label,
                    priority = priority,
                    colorHex = colorHex,
                    iconEmoji = iconEmoji,
                    sortOrder = sortOrder,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val result = if (current != null) updateCriticalityLevel(level) else createCriticalityLevel(level)

            when (result) {
                is QrResult.Success -> onDismissDialog()
                is QrResult.Error -> errorMessage.value = result.error.toString()
            }
        }
    }

    fun onDeactivate(id: String) {
        viewModelScope.launch { deactivateCriticalityLevel(id) }
    }

    fun onRestore(id: String) {
        viewModelScope.launch { restoreCriticalityLevel(id) }
    }
}
