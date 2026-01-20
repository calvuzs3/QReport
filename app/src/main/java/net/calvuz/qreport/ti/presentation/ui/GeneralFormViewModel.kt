package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.model.WorkLocationType
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.String

/**
 * ViewModel for TechnicalIntervention form screen
 * Handles both creation (interventionId = null) and editing (interventionId != null)
 */
@HiltViewModel
class GeneralFormViewModel @Inject constructor(
    private val getInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase,
    private val updateTechnicalInterventionUseCase: UpdateTechnicalInterventionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GeneralFormState())
    val state: StateFlow<GeneralFormState> = _state.asStateFlow()

    private var currentInterventionId: String? = null
    private var currentIntervention: TechnicalIntervention? = null
    private var originalData: GeneralOriginalData? = null


    // ===== LOADING EXISTING INTERVENTION =====

    fun loadInterventionGeneral(interventionId: String) {
        if (currentInterventionId == interventionId) return // Already loaded

        currentInterventionId = interventionId


        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getInterventionByIdUseCase(interventionId)) {
                is QrResult.Success -> {
                    currentIntervention = result.data
                    val intervention = result.data
                    populateFormFromIntervention(intervention)
                }

                is QrResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.error.asUiText()
                    )
                }
            }
        }
    }

    private fun populateFormFromIntervention(intervention: TechnicalIntervention) {

        val techniciansItems = mutableListOf<String>()
        intervention.technicians?.forEach { technician ->
            techniciansItems.add(technician)
        }

        // Store original data for dirty checking
        originalData = GeneralOriginalData(
            // Customer section
            customerName = intervention.customerData.customerName,
            customerContact = intervention.customerData.customerContact,
            ticketNumber = intervention.customerData.ticketNumber,
            customerOrderNumber = intervention.customerData.customerOrderNumber,
            notes = intervention.customerData.notes,
            // Robot section
            serialNumber = intervention.robotData.serialNumber,
            hoursOfDuty = intervention.robotData.hoursOfDuty.toString(),  // Convert Int to String
            // Work location section
            workLocation = intervention.workLocation.type,
            customLocation = intervention.workLocation.customLocation ?: "",
            // Technicians section
            technicians = techniciansItems
        )

        _state.update { currentState ->
            currentState.copy(
                // Customer section
                customerName = intervention.customerData.customerName,
                customerContact = intervention.customerData.customerContact,
                ticketNumber = intervention.customerData.ticketNumber,
                customerOrderNumber = intervention.customerData.customerOrderNumber,
                notes = intervention.customerData.notes,

                // Robot section
                serialNumber = intervention.robotData.serialNumber,
                hoursOfDuty = intervention.robotData.hoursOfDuty.toString(),  // Convert Int to String

                // Work location section
                workLocation = intervention.workLocation.type,
                customLocation = intervention.workLocation.customLocation ?: "",

                // Technicians section
                technicians = intervention.technicians.joinToString(", "),

                // State
                isLoading = false,
                isDirty = false, // Reset dirty flag after loading
                errorMessage = null
            )
        }

        Timber.d("GeneralFormViewModel: Form populated from intervention - customer=${intervention.customerData.customerName}, robot=${intervention.robotData.serialNumber}")
    }

    // ===== CUSTOMER SECTION UPDATES =====

    fun updateCustomerName(value: String) {
        _state.update {
            val newState = it.copy(customerName = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
        // Removed: autoSaveAfterDelay() - Save only on tab change
    }

    fun updateCustomerContact(value: String) {
        _state.update {
            val newState = it.copy(customerContact = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
        // Removed: autoSaveAfterDelay() - Save only on tab change
    }

    fun updateTicketNumber(value: String) {
        _state.update {
            val newState = it.copy(ticketNumber = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
        // Removed: autoSaveAfterDelay() - Save only on tab change
    }

    fun updateCustomerOrderNumber(value: String) {
        _state.update {
            val newState = it.copy(customerOrderNumber = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
        // Removed: autoSaveAfterDelay() - Save only on tab change
    }

    fun updateNotes(value: String) {
        _state.update {
            val newState = it.copy(notes = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
        // Removed: autoSaveAfterDelay() - Save only on tab change
    }

    // ===== ROBOT SECTION UPDATES =====

    fun updateSerialNumber(value: String) {
        _state.update {
            val newState = it.copy(serialNumber = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty, errorMessage = null)
        }
        // Removed: autoSaveAfterDelay() - Save only on tab change
    }

    fun updateHoursOfDuty(value: String) {
        _state.update {
            val newState = it.copy(hoursOfDuty = value)  // Keep as String
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
        // Removed: autoSaveAfterDelay() - Save only on tab change
    }

    // ===== WORK LOCATION UPDATES =====

    fun updateWorkLocation(type: WorkLocationType) {
        _state.update {
            val newState = it.copy(workLocation = type)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateCustomLocation(value: String) {
        _state.update {
            val newState = it.copy(customLocation = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    // ===== TECHNICIANS UPDATES =====

    fun updateTechnicians(value: String) {
        val techniciansList = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val errorMessage = if (techniciansList.size > 6) {
            QrError.CreateInterventionError.TooManyTechnicians()
        } else null

        _state.update {
            val newState = it.copy(technicians = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    // ===== MAIN ACTIONS =====


    /**
     * Check if current state differs from original data
     */
    private fun checkIfDirty(currentState: GeneralFormState): Boolean {
        val original = originalData ?: return false
        val isDirty =
            currentState.customerName != original.customerName ||
                    currentState.customerContact != original.customerContact ||
                    currentState.ticketNumber != original.ticketNumber ||
                    currentState.customerOrderNumber != original.customerOrderNumber ||
                    currentState.notes != original.notes ||
                    currentState.serialNumber != original.serialNumber ||
                    currentState.hoursOfDuty != original.hoursOfDuty ||
                    currentState.workLocation != original.workLocation ||
                    currentState.customLocation != original.customLocation ||
                    currentState.technicians != original.technicians.joinToString(", ")
        return isDirty
    }

    /**
     * Internal save method that can be called synchronously
     */
    private suspend fun saveCurrentStateInternal(
        currentState: GeneralFormState,
        intervention: TechnicalIntervention
    ): QrResult<TechnicalIntervention, QrError> {
        return try {
            // Parse technicians
            val techniciansList = currentState.technicians
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(6) // Safety limit

            // Parse hours of duty from String
            val hoursOfDutyInt = currentState.hoursOfDuty.toIntOrNull() ?: 0

            // Build updated customer data
            val updatedCustomerData = intervention.customerData.copy(
                customerName = currentState.customerName,
                customerContact = currentState.customerContact,
                ticketNumber = currentState.ticketNumber,
                customerOrderNumber = currentState.customerOrderNumber,
                notes = currentState.notes
            )

            // Build updated robot data
            val updatedRobotData = intervention.robotData.copy(
                serialNumber = currentState.serialNumber,
                hoursOfDuty = hoursOfDutyInt
            )

            // Build updated work location
            val updatedWorkLocation = net.calvuz.qreport.ti.domain.model.WorkLocation(
                type = currentState.workLocation,
                customLocation = currentState.customLocation.takeIf { it.isNotBlank() } ?: ""
            )

            Timber.d("GeneralFormViewModel: Updating intervention with all fields - " +
                    "customer=${updatedCustomerData.customerName}, " +
                    "robot=${updatedRobotData.serialNumber}, " +
                    "technicians=${techniciansList.size}, " +
                    "workLocation=${updatedWorkLocation.type}")

            // Create complete updated intervention
            val updatedIntervention = intervention.copy(
                customerData = updatedCustomerData,
                robotData = updatedRobotData,
                workLocation = updatedWorkLocation,
                technicians = techniciansList
            )

            when (val result = updateTechnicalInterventionUseCase(updatedIntervention)) {
                is QrResult.Success -> {
                    Timber.d("GeneralFormViewModel: Save successful")
                    currentIntervention = updatedIntervention
                    QrResult.Success(updatedIntervention)
                }

                is QrResult.Error -> {
                    Timber.e("GeneralFormViewModel: Save failed - ${result.error}")
                    QrResult.Error(QrError.InterventionError.GeneralError.UpdateError(result.error))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GeneralFormViewModel: Exception during save")
            QrResult.Error(QrError.InterventionError.GeneralError.SaveError(e.message))
        }
    }

    /**
     * Auto-save when leaving tab (called by parent EditInterventionScreen)
     * Returns success/failure for tab change decision
     */
    suspend fun autoSaveOnTabChange(): QrResult<Unit, QrError.InterventionError> {
        val currentState = _state.value
        val intervention = currentIntervention

        if (!currentState.isDirty) {
            Timber.d("autoSaveOnTabChange: No changes to save")
            return QrResult.Success(Unit)
        }

        Timber.d("autoSaveOnTabChange: Starting auto-save, isDirty=${currentState.isDirty}")
        Timber.d("autoSaveOnTabChange: Current data - customer='${currentState.customerName}', robot='${currentState.serialNumber}', technicians='${currentState.technicians}'")

        if (intervention == null) {
            Timber.e("autoSaveOnTabChange: No intervention loaded")
            val error = QrError.InterventionError.NoInterventionLoaded()
            _state.update { it.copy(errorMessage = UiText.DynStr("No intervention loaded")) }
            return QrResult.Error(error)
        }

        return try {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            val result = saveCurrentStateInternal(currentState, intervention)

            when (result) {
                is QrResult.Success -> {
                    Timber.d("autoSaveOnTabChange: Save successful")

                    // Update original data to current values
                    originalData = GeneralOriginalData(
                        customerName = currentState.customerName,
                        customerContact = currentState.customerContact,
                        ticketNumber = currentState.ticketNumber,
                        customerOrderNumber = currentState.customerOrderNumber,
                        notes = currentState.notes,
                        serialNumber = currentState.serialNumber,
                        hoursOfDuty = currentState.hoursOfDuty,
                        workLocation = currentState.workLocation,
                        customLocation = currentState.customLocation,
                        technicians = currentState.technicians.split(",").map { it.trim() }
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
                    val error = QrError.InterventionError.UpdateError()
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = UiText.DynStr("Errore nel salvataggio: ${result.error}")
                        )
                    }
                    QrResult.Error(error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "autoSaveOnTabChange: Exception during save")
            val error = QrError.InterventionError.UpdateError()
            _state.update {
                it.copy(
                    isSaving = false,
                    errorMessage = UiText.DynStr("Errore nel salvataggio: ${e.message}")
                )
            }
            QrResult.Error(error)
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

    fun clearSuccess() {
        _state.update() { it.copy(isSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI State for TechnicalIntervention form (create/edit)
 */
data class GeneralFormState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAutoSaved: Boolean = false,
    val isDirty: Boolean = false,

    // ===== CUSTOMER SECTION =====
    val customerName: String = "",
    val customerContact: String = "",
    val ticketNumber: String = "",
    val customerOrderNumber: String = "",
    val notes: String = "",

    // ===== ROBOT SECTION =====
    val serialNumber: String = "",
    val hoursOfDuty: String = "",  // Changed to String for UI consistency

    // ===== WORK LOCATION SECTION =====
    val workLocation: WorkLocationType = WorkLocationType.CLIENT_SITE,
    val customLocation: String = "",

    // ===== TECHNICIANS SECTION =====
    val technicians: String = "",

    // ===== FORM MODE =====
    val isEditMode: Boolean = false,
    val existingInterventionId: String? = null,

    // ===== UI STATE =====
    val isSuccess: Boolean = false,
    val errorMessage: UiText? = null,
    val savedInterventionId: String? = null
) {

    /**
     * Form validation - checks all required fields
     */
    val canSave: Boolean
        get() = customerName.isNotBlank() &&
                ticketNumber.isNotBlank() &&
                customerOrderNumber.isNotBlank() &&
                serialNumber.isNotBlank() &&
                (workLocation != WorkLocationType.OTHER || customLocation.isNotBlank()) &&
                techniciansList.size <= 6 &&
                !isLoading

    /**
     * Parsed technicians list for validation
     */
    private val techniciansList: List<String>
        get() = technicians.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Title for the screen based on mode
     */
    val screenTitle: String
        get() = if (isEditMode) "Modifica Intervento" else "Nuovo Intervento"

    /**
     * Action button text based on mode
     */
    val actionButtonText: String
        get() = if (isEditMode) "SALVA" else "CREA"
}

data class GeneralOriginalData(
    // Customer section
    val customerName: String,
    val customerContact: String,
    val ticketNumber: String,
    val customerOrderNumber: String,
    val notes: String,

    // Robot section
    val serialNumber: String,
    val hoursOfDuty: String,  // Changed to String for consistency

    // Work location section
    val workLocation: WorkLocationType,
    val customLocation: String,

    // Technicians section
    val technicians: List<String>,
)