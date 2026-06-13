package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.model.WorkDay
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for WorkDays tab content.
 * Manages the list of work days and navigation between list and detail views.
 */
@HiltViewModel
class WorkDaysTabViewModel @Inject constructor(
    private val getTechnicalInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase,
    private val updateTechnicalInterventionUseCase: UpdateTechnicalInterventionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WorkDaysTabState())
    val state: StateFlow<WorkDaysTabState> = _state.asStateFlow()

    private var currentInterventionId: String? = null
    private var currentIntervention: TechnicalIntervention? = null

    /**
     * Load work days from intervention
     */
    fun loadWorkDays(interventionId: String) {
        if (currentInterventionId == interventionId && _state.value.workDays.isNotEmpty()) {
            Timber.d("WorkDays already loaded for intervention: $interventionId")
            return
        }

        // Log
        Timber.v("Loading work days for intervention: $interventionId")

        currentInterventionId = interventionId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTechnicalInterventionByIdUseCase(interventionId)) {
                is QrResult.Success -> {
//                    if (result.data != null) {
                        currentIntervention = result.data
                        _state.update {
                            it.copy(
                                isLoading = false,
                                workDays = result.data.workDays,
                                errorMessage = null
                            )
                        }

                        // Log
                        Timber.v("Loaded ${result.data.workDays.size} work days")

//                    } else {
//                        _state.update {
//                            it.copy(
//                                isLoading = false,
//                                errorMessage = "Intervento non trovato"
//                            )
//                        }
//                    }
                }
                is QrResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.asUiText()
                        )
                    }
                }
            }
        }
    }

    /**
     * Refresh work days from database (call after detail form saves)
     */
    fun refreshWorkDays() {
        currentInterventionId?.let { id ->
            viewModelScope.launch {
                when (val result = getTechnicalInterventionByIdUseCase(id)) {
                    is QrResult.Success -> {
//                        if (result.data != null) {
                            currentIntervention = result.data
                            _state.update {
                                it.copy(workDays = result.data.workDays)
                            }
//                        }
                    }
                    is QrResult.Error -> {
                        Timber.e("Failed to refresh work days: ${result.error}")
                    }
                }
            }
        }
    }

    /**
     * Navigate to detail view for existing work day
     */
    fun editWorkDay(index: Int) {
        if (index < 0 || index >= _state.value.workDays.size) {
            Timber.w("Invalid work day index: $index")
            return
        }
        _state.update {
            it.copy(
                viewMode = WorkDaysViewMode.Detail(workDayIndex = index)
            )
        }
    }

    /**
     * Navigate to detail view for new work day
     */
    fun addNewWorkDay() {
        _state.update {
            it.copy(
                viewMode = WorkDaysViewMode.Detail(workDayIndex = null)
            )
        }
    }

    /**
     * Navigate back to list view
     */
    fun navigateBackToList(skipDataRefresh: Boolean = false) {
        if (!skipDataRefresh) {
            refreshWorkDays()
        }
        _state.update {
            it.copy(viewMode = WorkDaysViewMode.List)
        }
    }

    /**
     * Delete work day at index
     */
    fun deleteWorkDay(index: Int) {
        val intervention = currentIntervention ?: return
        val workDays = _state.value.workDays.toMutableList()

        if (index < 0 || index >= workDays.size) {
            Timber.w("Invalid work day index for deletion: $index")
            return
        }

        workDays.removeAt(index)

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            // Re-fetch the latest data to avoid overwriting changes saved from other tabs
            val baseIntervention = when (val freshResult = getTechnicalInterventionByIdUseCase(intervention.id)) {
                is QrResult.Success -> freshResult.data
                is QrResult.Error -> intervention
            }

            val updatedIntervention = baseIntervention.copy(workDays = workDays)

            when (val result = updateTechnicalInterventionUseCase(updatedIntervention)) {
                is QrResult.Success -> {
                    currentIntervention = updatedIntervention
                    _state.update {
                        it.copy(
                            isSaving = false,
                            workDays = workDays
                        )
                    }
                    Timber.d("Work day deleted successfully")
                }
                is QrResult.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.error.asUiText()
                        )
                    }
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

/**
 * View mode for WorkDays tab
 */
sealed class WorkDaysViewMode {
    data object List : WorkDaysViewMode()
    data class Detail(val workDayIndex: Int?) : WorkDaysViewMode() // null = new, 0..n = edit
}

/**
 * State for WorkDays tab
 */
data class WorkDaysTabState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val workDays: List<WorkDay> = emptyList(),
    val viewMode: WorkDaysViewMode = WorkDaysViewMode.List,
    val errorMessage: UiText? = null
)

/**
 * UI model for work day list item (simplified display data)
 */
data class WorkDayListItem(
    val index: Int,
    val date: Instant,
    val dateFormatted: String,
    val isRemoteAssistance: Boolean,
    val technicianCount: Int,
    val technicianInitials: String,
    val totalKilometers: Double,
    val hasMorningWork: Boolean,
    val hasAfternoonWork: Boolean
)

/**
 * Extension to convert WorkDay to list item
 */
fun WorkDay.toListItem(index: Int): WorkDayListItem {
    return WorkDayListItem(
        index = index,
        date = date,
        dateFormatted = (date.toItalianDate()),
        isRemoteAssistance = remoteAssistance,
        technicianCount = technicianCount,
        technicianInitials = technicianInitials,
        totalKilometers = totalKilometers,
        hasMorningWork = morningStart.isNotBlank() || morningEnd.isNotBlank(),
        hasAfternoonWork = afternoonStart.isNotBlank() || afternoonEnd.isNotBlank()
    )
}