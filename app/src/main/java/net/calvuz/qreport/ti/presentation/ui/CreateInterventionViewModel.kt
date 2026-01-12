package net.calvuz.qreport.ti.presentation.ui

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
import net.calvuz.qreport.ti.domain.model.WorkLocation
import net.calvuz.qreport.ti.domain.model.WorkLocationType
import javax.inject.Inject

/**
 * ViewModel for TechnicalIntervention creation screen
 * Focus on Customer + Robot Data validation and state management
 */
@HiltViewModel
class CreateInterventionViewModel @Inject constructor(
    private val createInterventionUseCase: CreateTechnicalInterventionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateInterventionState())
    val state: StateFlow<CreateInterventionState> = _state.asStateFlow()

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

    // ===== ACTIONS =====

    fun createIntervention() {
        val currentState = _state.value

        // Validate form
        if (!currentState.canCreate) {
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

            // Call use case
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
                        createdInterventionId = result.data
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

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}

/**
 * UI State for TechnicalIntervention creation
 */
data class CreateInterventionState(
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

    // ===== UI STATE =====
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: UiText? = null,
    val createdInterventionId: String? = null
) {

    /**
     * Form validation - checks all required fields
     */
    val canCreate: Boolean
        get() = customerName.isNotBlank() &&
                ticketNumber.isNotBlank() &&
                customerOrderNumber.isNotBlank() &&
                serialNumber.isNotBlank() &&
                hoursOfDuty.isNotBlank() &&
                hoursOfDuty.toIntOrNull() != null &&
                (workLocation != WorkLocationType.OTHER || customLocation.isNotBlank()) &&
                techniciansList.size <= 6 &&
                !isLoading

    /**
     * Parsed technicians list for validation
     */
    private val techniciansList: List<String>
        get() = technicians.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
