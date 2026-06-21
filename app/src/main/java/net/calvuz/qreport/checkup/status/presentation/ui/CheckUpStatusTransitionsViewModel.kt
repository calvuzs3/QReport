package net.calvuz.qreport.checkup.status.presentation.ui

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
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.checkup.status.domain.usecase.ObserveActiveCheckUpStatusesUseCase
import net.calvuz.qreport.checkup.status.domain.usecase.ObserveCheckUpStatusTransitionsUseCase
import net.calvuz.qreport.checkup.status.domain.usecase.UpdateCheckUpStatusTransitionsUseCase
import javax.inject.Inject

data class CheckUpStatusTransitionsUiState(
    val statuses: List<CheckUpStatusMaster> = emptyList(),
    val transitionsByStatusId: Map<String, List<String>> = emptyMap(),
    val isLoading: Boolean = true,
    val editingFromStatus: CheckUpStatusMaster? = null,
    val errorMessage: String? = null
)

/** Mirror of [net.calvuz.qreport.checkup.modules.presentation.ui.ModuleIslandAssociationViewModel]. */
@HiltViewModel
class CheckUpStatusTransitionsViewModel @Inject constructor(
    observeActiveCheckUpStatuses: ObserveActiveCheckUpStatusesUseCase,
    observeCheckUpStatusTransitions: ObserveCheckUpStatusTransitionsUseCase,
    private val updateCheckUpStatusTransitions: UpdateCheckUpStatusTransitionsUseCase
) : ViewModel() {

    private val editingFromStatus = MutableStateFlow<CheckUpStatusMaster?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CheckUpStatusTransitionsUiState> = combine(
        observeActiveCheckUpStatuses(),
        observeCheckUpStatusTransitions(),
        editingFromStatus,
        errorMessage
    ) { statuses, transitions, editing, error ->
        CheckUpStatusTransitionsUiState(
            statuses = statuses,
            transitionsByStatusId = transitions,
            isLoading = false,
            editingFromStatus = editing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CheckUpStatusTransitionsUiState())

    fun onEditClick(status: CheckUpStatusMaster) {
        editingFromStatus.value = status
    }

    fun onDismissDialog() {
        editingFromStatus.value = null
        errorMessage.value = null
    }

    fun onSave(toStatusIds: List<String>) {
        val fromStatus = editingFromStatus.value ?: return
        viewModelScope.launch {
            when (val result = updateCheckUpStatusTransitions(fromStatus.id, toStatusIds)) {
                is QrResult.Success -> onDismissDialog()
                is QrResult.Error -> errorMessage.value = result.error.toString()
            }
        }
    }
}
