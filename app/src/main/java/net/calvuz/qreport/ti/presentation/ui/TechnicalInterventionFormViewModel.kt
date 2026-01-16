package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.error.presentation.toUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.domain.usecase.intervention.CreateTechnicalInterventionUseCase
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.model.WorkLocation
import net.calvuz.qreport.ti.domain.model.WorkLocationType
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionUseCase
import javax.inject.Inject

/**
 * ViewModel for TechnicalIntervention form screen
 * Handles both creation (interventionId = null) and editing (interventionId != null)
 */
@HiltViewModel
class TechnicalInterventionFormViewModel @Inject constructor(
    private val createInterventionUseCase: CreateTechnicalInterventionUseCase,
    private val getInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase,
    private val updateInterventionUseCase: UpdateTechnicalInterventionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val interventionId: String? = savedStateHandle.get<String>("interventionId")
    val isEditMode: Boolean = interventionId != null

    private val _state = MutableStateFlow(TechnicalInterventionFormState(isEditMode = isEditMode))
    val state: StateFlow<TechnicalInterventionFormState> = _state.asStateFlow()

    init {
        if (isEditMode && interventionId != null) {
            loadExistingIntervention(interventionId)
        }
    }

    // ===== LOADING EXISTING INTERVENTION =====

    private fun loadExistingIntervention(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoadingExisting = true,
                errorMessage = null
            )

            when (val result = getInterventionByIdUseCase(id)) {
                is QrResult.Success -> {
                    val intervention = result.data
                    populateFormFromIntervention(intervention)
                }

                is QrResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingExisting = false,
                        errorMessage = result.error.asUiText()
                    )
                }
            }
        }
    }

    private fun populateFormFromIntervention(intervention: TechnicalIntervention) {
        _state.value = _state.value.copy(
            // Customer section
            customerName = intervention.customerData.customerName,
            customerContact = intervention.customerData.customerContact,
            ticketNumber = intervention.customerData.ticketNumber,
            customerOrderNumber = intervention.customerData.customerOrderNumber,
            notes = intervention.customerData.notes,

            // Robot section
            serialNumber = intervention.robotData.serialNumber,
            hoursOfDuty = intervention.robotData.hoursOfDuty.toString(),

            // Work location section
            workLocation = intervention.workLocation.type,
            customLocation = intervention.workLocation.customLocation,

            // Technicians section
            technicians = intervention.technicians.joinToString(", "),

            // State
            isLoadingExisting = false,
            existingInterventionId = intervention.id,
            errorMessage = null
        )
    }

    // ===== CUSTOMER SECTION UPDATES =====

    fun updateCustomerName(value: String) {
        _state.value = _state.value.copy(
            customerName = value,
            errorMessage = null
        )
    }

    fun updateCustomerContact(value: String) {
        _state.value = _state.value.copy(customerContact = value)
    }

    fun updateTicketNumber(value: String) {
        _state.value = _state.value.copy(
            ticketNumber = value,
            errorMessage = null
        )
    }

    fun updateCustomerOrderNumber(value: String) {
        _state.value = _state.value.copy(
            customerOrderNumber = value,
            errorMessage = null
        )
    }

    fun updateNotes(value: String) {
        _state.value = _state.value.copy(notes = value)
    }

    // ===== ROBOT SECTION UPDATES =====

    fun updateSerialNumber(value: String) {
        _state.value = _state.value.copy(
            serialNumber = value,
            errorMessage = null
        )
    }

    fun updateHoursOfDuty(value: String) {
        _state.value = _state.value.copy(
            hoursOfDuty = value,
            errorMessage = null
        )
    }

    // ===== WORK LOCATION UPDATES =====

    fun updateWorkLocation(type: WorkLocationType) {
        _state.value = _state.value.copy(
            workLocation = type,
            // Clear custom location if switching away from OTHER
            customLocation = if (type != WorkLocationType.OTHER) "" else _state.value.customLocation
        )
    }

    fun updateCustomLocation(value: String) {
        _state.value = _state.value.copy(customLocation = value)
    }

    // ===== TECHNICIANS UPDATES =====

    fun updateTechnicians(value: String) {
        val techniciansList = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val errorMessage = if (techniciansList.size > 6) {
            QrError.CreateInterventionError.TooManyTechnicians()
        } else null

        _state.value = _state.value.copy(
            technicians = value,
            errorMessage = errorMessage?.toUiText()
        )
    }

    // ===== MAIN ACTIONS =====

    fun saveIntervention() {
        if (isEditMode) {
            updateExistingIntervention()
        } else {
            createNewIntervention()
        }
    }

    private fun createNewIntervention() {
        val currentState = _state.value

        // Validate form
        if (!currentState.canSave) {
            _state.value = currentState.copy(
                errorMessage = QrError.CreateInterventionError.CreationFailed().toUiText()
            )
            return
        }

        viewModelScope.launch {
            _state.value = currentState.copy(
                isLoading = true,
                errorMessage = null
            )

            // Parse technicians
            val techniciansList = currentState.technicians
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(6) // Safety limit

            // Parse hours of duty
            val hoursOfDutyInt = currentState.hoursOfDuty.toIntOrNull() ?: 0

            // Create work location
            val workLocation = WorkLocation(
                type = currentState.workLocation,
                customLocation = if (currentState.workLocation == WorkLocationType.OTHER) {
                    currentState.customLocation
                } else ""
            )

            // Call create use case
            val result = createInterventionUseCase.createWithManualData(
                customerName = currentState.customerName,
                serialNumber = currentState.serialNumber,
                hoursOfDuty = hoursOfDutyInt,
                ticketNumber = currentState.ticketNumber,
                customerOrderNumber = currentState.customerOrderNumber,
                customerContact = currentState.customerContact,
                workLocation = workLocation,
                technicians = techniciansList
            )

            when (result) {
                is QrResult.Success -> {
                    _state.value = currentState.copy(
                        isLoading = false,
                        isSuccess = true,
                        savedInterventionId = result.data
                    )
                }

                is QrResult.Error -> {
                    _state.value = currentState.copy(
                        isLoading = false,
                        errorMessage = result.error.asUiText()
                    )
                }
            }
        }
    }

    private fun updateExistingIntervention() {
        val currentState = _state.value
        val existingId = currentState.existingInterventionId ?: return

        // Validate form
        if (!currentState.canSave) {
            _state.value = currentState.copy(
                errorMessage = QrError.CreateInterventionError.CreationFailed().toUiText()
            )
            return
        }

        viewModelScope.launch {
            _state.value = currentState.copy(
                isLoading = true,
                errorMessage = null
            )

            // Parse technicians
            val techniciansList = currentState.technicians
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(6) // Safety limit

            // Parse hours of duty
            val hoursOfDutyInt = currentState.hoursOfDuty.toIntOrNull() ?: 0

            // Create work location
            val workLocation = WorkLocation(
                type = currentState.workLocation,
                customLocation = if (currentState.workLocation == WorkLocationType.OTHER) {
                    currentState.customLocation
                } else ""
            )

            val result = updateInterventionUseCase.updateEditableFields(
                interventionId = existingId,
                hoursOfDuty = hoursOfDutyInt,
                customerContact = currentState.customerContact,
                notes = currentState.notes,
                workLocation = workLocation,
                technicians = techniciansList
            )

            when (result) {
                is QrResult.Success -> {
                    _state.value = currentState.copy(
                        isLoading = false,
                        isSuccess = true,
                        savedInterventionId = result.data.id
                    )
                }
                is QrResult.Error -> {
                    _state.value = currentState.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = result.error.asUiText()
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(isSuccess = false)
    }
}

/**
 * UI State for TechnicalIntervention form (create/edit)
 */
data class TechnicalInterventionFormState(
    // ===== CUSTOMER SECTION =====
    val customerName: String = "",
    val customerContact: String = "",
    val ticketNumber: String = "",
    val customerOrderNumber: String = "",
    val notes: String = "",

    // ===== ROBOT SECTION =====
    val serialNumber: String = "",
    val hoursOfDuty: String = "",

    // ===== WORK LOCATION SECTION =====
    val workLocation: WorkLocationType = WorkLocationType.CLIENT_SITE,
    val customLocation: String = "",

    // ===== TECHNICIANS SECTION =====
    val technicians: String = "",

    // ===== FORM MODE =====
    val isEditMode: Boolean = false,
    val existingInterventionId: String? = null,

    // ===== UI STATE =====
    val isLoading: Boolean = false,
    val isLoadingExisting: Boolean = false,
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
                hoursOfDuty.isNotBlank() &&
                hoursOfDuty.toIntOrNull() != null &&
                (workLocation != WorkLocationType.OTHER || customLocation.isNotBlank()) &&
                techniciansList.size <= 6 &&
                !isLoading &&
                !isLoadingExisting

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