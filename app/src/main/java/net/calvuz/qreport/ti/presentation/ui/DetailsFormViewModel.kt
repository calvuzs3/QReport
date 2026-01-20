package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.*
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for InterventionDetailsFormScreen
 * Manages intervention description, materials, external report and completion
 */
@HiltViewModel
class DetailsFormViewModel @Inject constructor(
    private val getTechnicalInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase,
    private val updateTechnicalInterventionUseCase: UpdateTechnicalInterventionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(InterventionDetailsFormState())
    val state: StateFlow<InterventionDetailsFormState> = _state.asStateFlow()

    private var currentInterventionId: String? = null
    private var currentIntervention: TechnicalIntervention? = null
    private var originalData: InterventionDetailsOriginalData? = null

    /**
     * Load intervention details
     */
    fun loadInterventionDetails(interventionId: String) {
        if (currentInterventionId == interventionId) return // Already loaded

        currentInterventionId = interventionId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTechnicalInterventionByIdUseCase(interventionId)) {
                is QrResult.Success -> {
                    if (result.data != null) {
                        currentIntervention = result.data
                        populateFormFromIntervention(result.data)
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Intervento non trovato"
                            )
                        }
                    }
                }

                is QrResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Errore nel caricamento: ${result.error}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Populate form fields from intervention data
     */
    private fun populateFormFromIntervention(intervention: TechnicalIntervention) {
        val materialItems = mutableListOf<MaterialItemState>()

        // Convert domain MaterialItem to UI MaterialItemState
        intervention.materials?.items?.forEachIndexed { index, item ->
            if (index < 6) { // Max 6 items
                materialItems.add(
                    MaterialItemState(
                        quantity = item.quantity.toString(),
                        description = item.description
                    )
                )
            }
        }

        // Pad with empty items to always have 6
        while (materialItems.size < 6) {
            materialItems.add(MaterialItemState())
        }

        // Store original data for dirty checking
        originalData = InterventionDetailsOriginalData(
            interventionDescription = intervention.interventionDescription,
            ddtNumber = intervention.materials?.ddtNumber ?: "",
            ddtDate = intervention.materials?.ddtDate?.let { date ->
                formatDateForDisplay(date)
            } ?: "",
            materialItems = materialItems,
            externalReportNumber = intervention.externalReport?.reportNumber ?: "",
            isComplete = intervention.isComplete
        )

        _state.update {
            it.copy(
                isLoading = false,
                interventionDescription = intervention.interventionDescription,
                ddtNumber = intervention.materials?.ddtNumber ?: "",
                ddtDate = intervention.materials?.ddtDate?.let { date ->
                    formatDateForDisplay(date)
                } ?: "",
                materialItems = materialItems,
                externalReportNumber = intervention.externalReport?.reportNumber ?: "",
                isComplete = intervention.isComplete,
                isDirty = false, // Reset dirty flag after loading
                errorMessage = null
            )
        }
    }

    /**
     * Update intervention description
     */
    fun updateInterventionDescription(description: String) {
        _state.update {
            val newState = it.copy(interventionDescription = description)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Update DDT number
     */
    fun updateDdtNumber(ddtNumber: String) {
        _state.update {
            val newState = it.copy(ddtNumber = ddtNumber)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Update DDT date
     */
    fun updateDdtDate(ddtDate: String) {
        _state.update {
            val newState = it.copy(ddtDate = ddtDate)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Update material item at specific index
     */
    fun updateMaterialItem(index: Int, quantity: String, description: String) {
        if (index < 0 || index >= 6) return

        val currentItems = _state.value.materialItems.toMutableList()

        // Ensure list has enough items
        while (currentItems.size <= index) {
            currentItems.add(MaterialItemState())
        }

        currentItems[index] = MaterialItemState(
            quantity = quantity,
            description = description
        )

        _state.update {
            val newState = it.copy(materialItems = currentItems)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Update external report number
     */
    fun updateExternalReportNumber(reportNumber: String) {
        _state.update {
            val newState = it.copy(externalReportNumber = reportNumber)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Update completion status
     */
    fun updateCompletionStatus(isComplete: Boolean) {
        _state.update {
            val newState = it.copy(isComplete = isComplete)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Save current form state to domain model
     */
    private fun saveCurrentState() {
        val currentState = _state.value
        val intervention = currentIntervention ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val result = saveCurrentStateInternal(currentState, intervention)

            when (result) {
                is QrResult.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            isAutoSaved = true
                        )
                    }
                }

                is QrResult.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Errore nel salvataggio: ${result.error}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Internal save method that can be called synchronously
     */
    private suspend fun saveCurrentStateInternal(
        currentState: InterventionDetailsFormState,
        intervention: TechnicalIntervention
    ): QrResult<TechnicalIntervention, QrError> {
        return try {
            // Convert MaterialItemState back to domain MaterialItem
            val materialItems = currentState.materialItems
                .filter { it.quantity.isNotBlank() || it.description.isNotBlank() }
                .map { item ->
                    MaterialItem(
                        quantity = item.quantity.toDoubleOrNull() ?: 0.0,
                        description = item.description
                    )
                }

            val materialsUsed =
                if (currentState.ddtNumber.isNotBlank() || materialItems.isNotEmpty()) {
                    MaterialsUsed(
                        ddtNumber = currentState.ddtNumber,
                        ddtDate = parseDateFromDisplay(currentState.ddtDate),
                        items = materialItems
                    )
                } else null

            val externalReport = if (currentState.externalReportNumber.isNotBlank()) {
                ExternalReport(reportNumber = currentState.externalReportNumber)
            } else null

            val updatedIntervention = intervention.copy(
                interventionDescription = currentState.interventionDescription,
                materials = materialsUsed,
                externalReport = externalReport,
                isComplete = currentState.isComplete
            )

            when (val result = updateTechnicalInterventionUseCase(updatedIntervention)) {
                is QrResult.Success -> {
                    currentIntervention = updatedIntervention
                    QrResult.Success(updatedIntervention)
                }

                is QrResult.Error -> {
                    QrResult.Error(QrError.InterventionError.DetailError.UpdateError(result.error))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving form state")
            QrResult.Error(QrError.InterventionError.DetailError.SaveError(e.message))
        }
    }

    /**
     * Check if current state differs from original data
     */
    private fun checkIfDirty(currentState: InterventionDetailsFormState): Boolean {
        val original = originalData ?: return false

        val isDirty = currentState.interventionDescription != original.interventionDescription ||
                currentState.ddtNumber != original.ddtNumber ||
                currentState.ddtDate != original.ddtDate ||
                currentState.materialItems != original.materialItems ||
                currentState.externalReportNumber != original.externalReportNumber ||
                currentState.isComplete != original.isComplete

        return isDirty
    }

    /**
     * Auto-save when leaving tab (called by parent EditInterventionScreen)
     * Returns success/failure for tab change decision
     */
    suspend fun autoSaveOnTabChange(): QrResult<Unit, QrError> {
        val currentState = _state.value
        val intervention = currentIntervention

        if (!currentState.isDirty) {
            Timber.d("autoSaveOnTabChange: No changes to save")
            return QrResult.Success(Unit)
        }

        Timber.d("autoSaveOnTabChange: Starting auto-save, isDirty=${currentState.isDirty}")

        if (intervention == null) {
            Timber.e("autoSaveOnTabChange: No intervention loaded")
            val error = "No intervention loaded"
            _state.update { it.copy(errorMessage = error) }
            return QrResult.Error(QrError.InterventionError.DetailError.SaveError())
        }

        return try {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            val result = saveCurrentStateInternal(currentState, intervention)

            when (result) {
                is QrResult.Success -> {
                    Timber.d("autoSaveOnTabChange: Save successful")

                    // Update original data to current values
                    originalData = InterventionDetailsOriginalData(
                        interventionDescription = currentState.interventionDescription,
                        ddtNumber = currentState.ddtNumber,
                        ddtDate = currentState.ddtDate,
                        materialItems = currentState.materialItems,
                        externalReportNumber = currentState.externalReportNumber,
                        isComplete = currentState.isComplete
                    )

                    _state.update {
                        it.copy(
                            isSaving = false,
                            isDirty = false, // Clear dirty flag after successful save
                            isAutoSaved = true
                        )
                    }

                    QrResult.Success(Unit)
                }

                is QrResult.Error -> {
                    Timber.e("autoSaveOnTabChange: Save failed - ${result.error}")
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Errore nel salvataggio: ${result.error}"
                        )
                    }
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "autoSaveOnTabChange: Exception during save")
            val error = "Errore nel salvataggio: ${e.message}"
            _state.update {
                it.copy(
                    isSaving = false,
                    errorMessage = error
                )
            }
            QrResult.Error(QrError.InterventionError.DetailError.SaveError())
        }
    }

    /**
     * Check if this tab has unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        return _state.value.isDirty
    }

    /**
     * Clear auto-save flag
     */
    fun clearAutoSaveFlag() {
        _state.update { it.copy(isAutoSaved = false) }
    }

    /**
     * Format Instant date for display (dd/MM/yyyy)
     */
    private fun formatDateForDisplay(instant: Instant): String {
        return try {
            val localDate = instant.toString().substring(0, 10) // Extract date part
            val parts = localDate.split("-")
            if (parts.size == 3) {
                "${parts[2]}/${parts[1]}/${parts[0]}" // dd/MM/yyyy
            } else {
                ""
            }
        } catch (e: Exception) {
            Timber.e(e, "Error formatting date")
            ""
        }
    }

    /**
     * Parse display date (dd/MM/yyyy) to Instant
     */
    private fun parseDateFromDisplay(dateString: String): Instant? {
        return try {
            if (dateString.isBlank()) return null

            val parts = dateString.split("/")
            if (parts.size != 3) return null

            val day = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val year = parts[2].toIntOrNull() ?: return null

            // Create ISO date string and parse
            val isoDate = String.format("%04d-%02d-%02d", year, month, day)
            Instant.parse("${isoDate}T00:00:00Z")
        } catch (e: Exception) {
            Timber.e(e, "Error parsing date")
            null
        }
    }
}

/**
 * State for InterventionDetailsFormScreen
 */
data class InterventionDetailsFormState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAutoSaved: Boolean = false,
    val isDirty: Boolean = false,
    val interventionDescription: String = "",
    val ddtNumber: String = "",
    val ddtDate: String = "",
    val materialItems: List<MaterialItemState> = List(6) { MaterialItemState() },
    val externalReportNumber: String = "",
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Data class to store original values for dirty checking
 */
data class InterventionDetailsOriginalData(
    val interventionDescription: String,
    val ddtNumber: String,
    val ddtDate: String,
    val materialItems: List<MaterialItemState>,
    val externalReportNumber: String,
    val isComplete: Boolean
)