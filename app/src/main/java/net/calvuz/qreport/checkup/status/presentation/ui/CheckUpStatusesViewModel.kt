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
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.checkup.status.domain.usecase.CreateCheckUpStatusUseCase
import net.calvuz.qreport.checkup.status.domain.usecase.DeactivateCheckUpStatusUseCase
import net.calvuz.qreport.checkup.status.domain.usecase.ObserveCheckUpStatusesUseCase
import net.calvuz.qreport.checkup.status.domain.usecase.RestoreCheckUpStatusUseCase
import net.calvuz.qreport.checkup.status.domain.usecase.UpdateCheckUpStatusMasterUseCase
import java.util.UUID
import javax.inject.Inject

data class CheckUpStatusesUiState(
    val statuses: List<CheckUpStatusMaster> = emptyList(),
    val isLoading: Boolean = true,
    val editingStatus: CheckUpStatusMaster? = null,
    val isCreatingNew: Boolean = false,
    val errorMessage: String? = null
)

/** Mirror of [net.calvuz.qreport.checkup.criticality.presentation.ui.CriticalityLevelsViewModel]. */
@HiltViewModel
class CheckUpStatusesViewModel @Inject constructor(
    observeCheckUpStatuses: ObserveCheckUpStatusesUseCase,
    private val createCheckUpStatus: CreateCheckUpStatusUseCase,
    private val updateCheckUpStatus: UpdateCheckUpStatusMasterUseCase,
    private val deactivateCheckUpStatus: DeactivateCheckUpStatusUseCase,
    private val restoreCheckUpStatus: RestoreCheckUpStatusUseCase
) : ViewModel() {

    private val editingStatus = MutableStateFlow<CheckUpStatusMaster?>(null)
    private val isCreatingNew = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CheckUpStatusesUiState> = combine(
        observeCheckUpStatuses(),
        editingStatus,
        isCreatingNew,
        errorMessage
    ) { statuses, editing, creatingNew, error ->
        CheckUpStatusesUiState(
            statuses = statuses,
            isLoading = false,
            editingStatus = editing,
            isCreatingNew = creatingNew,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CheckUpStatusesUiState())

    fun onAddClick() {
        isCreatingNew.value = true
    }

    fun onEditClick(status: CheckUpStatusMaster) {
        editingStatus.value = status
    }

    fun onDismissDialog() {
        isCreatingNew.value = false
        editingStatus.value = null
        errorMessage.value = null
    }

    fun onSave(
        code: String,
        label: String,
        colorHex: String,
        iconEmoji: String?,
        sortOrder: Int,
        blocksDeletion: Boolean,
        marksCompletion: Boolean
    ) {
        viewModelScope.launch {
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val current = editingStatus.value

            val status = if (current != null) {
                current.copy(
                    code = code,
                    label = label,
                    colorHex = colorHex,
                    iconEmoji = iconEmoji,
                    sortOrder = sortOrder,
                    blocksDeletion = blocksDeletion,
                    marksCompletion = marksCompletion,
                    updatedAt = now
                )
            } else {
                CheckUpStatusMaster(
                    id = UUID.randomUUID().toString(),
                    code = code,
                    label = label,
                    colorHex = colorHex,
                    iconEmoji = iconEmoji,
                    sortOrder = sortOrder,
                    blocksDeletion = blocksDeletion,
                    marksCompletion = marksCompletion,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val result = if (current != null) updateCheckUpStatus(status) else createCheckUpStatus(status)

            when (result) {
                is QrResult.Success -> onDismissDialog()
                is QrResult.Error -> errorMessage.value = result.error.toString()
            }
        }
    }

    fun onDeactivate(id: String) {
        viewModelScope.launch { deactivateCheckUpStatus(id) }
    }

    fun onRestore(id: String) {
        viewModelScope.launch { restoreCheckUpStatus(id) }
    }
}
