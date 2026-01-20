package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.*
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for WorkDayFormScreen
 * Manages single WorkDay data with travel, work hours and expenses
 */
@HiltViewModel
class WorkDayFormViewModel @Inject constructor(
    private val getTechnicalInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase,
    private val updateTechnicalInterventionUseCase: UpdateTechnicalInterventionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WorkDayFormState())
    val state: StateFlow<WorkDayFormState> = _state.asStateFlow()

    private var currentInterventionId: String? = null
    private var currentIntervention: TechnicalIntervention? = null
    private var originalData: WorkDayOriginalData? = null

    /**
     * Load work day data from intervention
     */
    fun loadWorkDayData(interventionId: String) {
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
     * Populate form fields from intervention work days
     * For now, use first work day or create default
     */
    private fun populateFormFromIntervention(intervention: TechnicalIntervention) {
        val workDay = intervention.workDays.firstOrNull() ?: createDefaultWorkDay()

        // Store original data for dirty checking
        originalData = WorkDayOriginalData(
            date = formatDateForDisplay(workDay.date),
            remoteAssistance = workDay.remoteAssistance,
            technicianCount = workDay.technicianCount.toString(),
            technicianInitials = workDay.technicianInitials,
            outboundTravelStart = workDay.outboundTravelStart,
            outboundTravelEnd = workDay.outboundTravelEnd,
            returnTravelStart = workDay.returnTravelStart,
            returnTravelEnd = workDay.returnTravelEnd,
            morningStart = workDay.morningStart,
            morningEnd = workDay.morningEnd,
            afternoonStart = workDay.afternoonStart,
            afternoonEnd = workDay.afternoonEnd,
            morningPocketMoney = workDay.morningPocketMoney,
            afternoonPocketMoney = workDay.afternoonPocketMoney,
            totalKilometers = if (workDay.totalKilometers > 0.0) workDay.totalKilometers.toString() else "",
            flight = workDay.flight,
            rentCar = workDay.rentCar,
            transferToAirport = workDay.transferToAirport,
            lodging = workDay.lodging
        )

        _state.update {
            it.copy(
                isLoading = false,
                // Basic info
                date = formatDateForDisplay(workDay.date),
                remoteAssistance = workDay.remoteAssistance,
                technicianCount = workDay.technicianCount.toString(),
                technicianInitials = workDay.technicianInitials,

                // Travel hours
                outboundTravelStart = workDay.outboundTravelStart,
                outboundTravelEnd = workDay.outboundTravelEnd,
                returnTravelStart = workDay.returnTravelStart,
                returnTravelEnd = workDay.returnTravelEnd,

                // Work hours
                morningStart = workDay.morningStart,
                morningEnd = workDay.morningEnd,
                afternoonStart = workDay.afternoonStart,
                afternoonEnd = workDay.afternoonEnd,

                // Expenses
                morningPocketMoney = workDay.morningPocketMoney,
                afternoonPocketMoney = workDay.afternoonPocketMoney,
                totalKilometers = if (workDay.totalKilometers > 0.0) workDay.totalKilometers.toString() else "",
                flight = workDay.flight,
                rentCar = workDay.rentCar,
                transferToAirport = workDay.transferToAirport,
                lodging = workDay.lodging,

                isDirty = false, // Reset dirty flag after loading
                errorMessage = null
            )
        }
    }

    /**
     * Create default work day for today
     */
    private fun createDefaultWorkDay(): WorkDay {
        return WorkDay(
            date = Clock.System.now(),
            remoteAssistance = false,
            technicianCount = 1
        )
    }

    // ===== UPDATE METHODS =====

    fun updateDate(date: String) {
        _state.update {
            val newState = it.copy(date = date)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateRemoteAssistance(remoteAssistance: Boolean) {
        _state.update {
            val newState = it.copy(
                remoteAssistance = remoteAssistance,
                // Clear travel fields if enabling remote assistance
                outboundTravelStart = if (remoteAssistance) "" else it.outboundTravelStart,
                outboundTravelEnd = if (remoteAssistance) "" else it.outboundTravelEnd,
                returnTravelStart = if (remoteAssistance) "" else it.returnTravelStart,
                returnTravelEnd = if (remoteAssistance) "" else it.returnTravelEnd,
                totalKilometers = if (remoteAssistance) "" else it.totalKilometers,
                flight = if (remoteAssistance) false else it.flight,
                rentCar = if (remoteAssistance) false else it.rentCar,
                transferToAirport = if (remoteAssistance) false else it.transferToAirport,
                lodging = if (remoteAssistance) false else it.lodging
            )
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateTechnicianCount(count: String) {
        val filtered = count.filter { it.isDigit() }
        _state.update {
            val newState = it.copy(technicianCount = filtered)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateTechnicianInitials(initials: String) {
        _state.update {
            val newState = it.copy(technicianInitials = initials)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    // Travel hours
    fun updateOutboundTravelStart(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(outboundTravelStart = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateOutboundTravelEnd(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(outboundTravelEnd = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateReturnTravelStart(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(returnTravelStart = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateReturnTravelEnd(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(returnTravelEnd = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    // Work hours
    fun updateMorningStart(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(morningStart = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateMorningEnd(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(morningEnd = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateAfternoonStart(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(afternoonStart = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateAfternoonEnd(time: String) {
        val formatted = formatTimeInput(time)
        _state.update {
            val newState = it.copy(afternoonEnd = formatted)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    // Expenses
    fun updateMorningPocketMoney(value: Boolean) {
        _state.update {
            val newState = it.copy(morningPocketMoney = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateAfternoonPocketMoney(value: Boolean) {
        _state.update {
            val newState = it.copy(afternoonPocketMoney = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateTotalKilometers(kilometers: String) {
        val filtered = kilometers.filter { it.isDigit() || it == '.' }
        _state.update {
            val newState = it.copy(totalKilometers = filtered)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateFlight(value: Boolean) {
        _state.update {
            val newState = it.copy(flight = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateRentCar(value: Boolean) {
        _state.update {
            val newState = it.copy(rentCar = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateTransferToAirport(value: Boolean) {
        _state.update {
            val newState = it.copy(transferToAirport = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    fun updateLodging(value: Boolean) {
        _state.update {
            val newState = it.copy(lodging = value)
            val isDirty = checkIfDirty(newState)
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Check if current state differs from original data
     */
    private fun checkIfDirty(currentState: WorkDayFormState): Boolean {
        val original = originalData ?: return false

        val isDirty = currentState.date != original.date ||
                currentState.remoteAssistance != original.remoteAssistance ||
                currentState.technicianCount != original.technicianCount ||
                currentState.technicianInitials != original.technicianInitials ||
                currentState.outboundTravelStart != original.outboundTravelStart ||
                currentState.outboundTravelEnd != original.outboundTravelEnd ||
                currentState.returnTravelStart != original.returnTravelStart ||
                currentState.returnTravelEnd != original.returnTravelEnd ||
                currentState.morningStart != original.morningStart ||
                currentState.morningEnd != original.morningEnd ||
                currentState.afternoonStart != original.afternoonStart ||
                currentState.afternoonEnd != original.afternoonEnd ||
                currentState.morningPocketMoney != original.morningPocketMoney ||
                currentState.afternoonPocketMoney != original.afternoonPocketMoney ||
                currentState.totalKilometers != original.totalKilometers ||
                currentState.flight != original.flight ||
                currentState.rentCar != original.rentCar ||
                currentState.transferToAirport != original.transferToAirport ||
                currentState.lodging != original.lodging

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
            return QrResult.Error(QrError.InterventionError.WorkDayError.SaveError(error))
        }

        return try {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            val result = saveCurrentStateInternal(currentState, intervention)

            when (result) {
                is QrResult.Success -> {
                    Timber.d("autoSaveOnTabChange: Save successful")

                    // Update original data to current values
                    originalData = WorkDayOriginalData(
                        date = currentState.date,
                        remoteAssistance = currentState.remoteAssistance,
                        technicianCount = currentState.technicianCount,
                        technicianInitials = currentState.technicianInitials,
                        outboundTravelStart = currentState.outboundTravelStart,
                        outboundTravelEnd = currentState.outboundTravelEnd,
                        returnTravelStart = currentState.returnTravelStart,
                        returnTravelEnd = currentState.returnTravelEnd,
                        morningStart = currentState.morningStart,
                        morningEnd = currentState.morningEnd,
                        afternoonStart = currentState.afternoonStart,
                        afternoonEnd = currentState.afternoonEnd,
                        morningPocketMoney = currentState.morningPocketMoney,
                        afternoonPocketMoney = currentState.afternoonPocketMoney,
                        totalKilometers = currentState.totalKilometers,
                        flight = currentState.flight,
                        rentCar = currentState.rentCar,
                        transferToAirport = currentState.transferToAirport,
                        lodging = currentState.lodging
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
            QrResult.Error(QrError.InterventionError.WorkDayError.SaveError())
        }
    }

    /**
     * Check if this tab has unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        return _state.value.isDirty
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
        currentState: WorkDayFormState,
        intervention: TechnicalIntervention
    ): QrResult<TechnicalIntervention, QrError> {
        return try {
            // Convert form state to WorkDay domain model
            val workDay = WorkDay(
                date = parseDateFromDisplay(currentState.date) ?: Clock.System.now(),
                remoteAssistance = currentState.remoteAssistance,
                technicianCount = currentState.technicianCount.toIntOrNull() ?: 1,
                technicianInitials = currentState.technicianInitials,

                // Travel hours
                outboundTravelStart = currentState.outboundTravelStart,
                outboundTravelEnd = currentState.outboundTravelEnd,
                returnTravelStart = currentState.returnTravelStart,
                returnTravelEnd = currentState.returnTravelEnd,

                // Work hours
                morningStart = currentState.morningStart,
                morningEnd = currentState.morningEnd,
                afternoonStart = currentState.afternoonStart,
                afternoonEnd = currentState.afternoonEnd,

                // Expenses
                morningPocketMoney = currentState.morningPocketMoney,
                afternoonPocketMoney = currentState.afternoonPocketMoney,
                totalKilometers = currentState.totalKilometers.toDoubleOrNull() ?: 0.0,
                flight = currentState.flight,
                rentCar = currentState.rentCar,
                transferToAirport = currentState.transferToAirport,
                lodging = currentState.lodging
            )

            // Update intervention with new work day (replace first or add new)
            val updatedWorkDays = intervention.workDays.toMutableList()
            if (updatedWorkDays.isEmpty()) {
                updatedWorkDays.add(workDay)
            } else {
                updatedWorkDays[0] = workDay // Replace first work day
            }

            val updatedIntervention = intervention.copy(
                workDays = updatedWorkDays
            )

            when (val result = updateTechnicalInterventionUseCase(updatedIntervention)) {
                is QrResult.Success -> {
                    currentIntervention = updatedIntervention
                    QrResult.Success(updatedIntervention)
                }

                is QrResult.Error -> {
                    QrResult.Error(QrError.InterventionError.WorkDayError.UpdateError(result.error))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving work day state")
            QrResult.Error(QrError.InterventionError.WorkDayError.SaveError())
        }
    }

    /**
     * Clear auto-save flag
     */
    fun clearAutoSaveFlag() {
        _state.update { it.copy(isAutoSaved = false) }
    }

    /**
     * Format time input to HH:mm
     */
    private fun formatTimeInput(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when (digits.length) {
            0, 1, 2 -> digits
            3 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
            4 -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
            else -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
        }
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
            null
        }
    }
}

/**
 * State for WorkDayFormScreen
 */
data class WorkDayFormState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAutoSaved: Boolean = false,
    val isDirty: Boolean = false,

    // Basic info
    val date: String = "",
    val remoteAssistance: Boolean = false,
    val technicianCount: String = "1",
    val technicianInitials: String = "",

    // Travel hours
    val outboundTravelStart: String = "",
    val outboundTravelEnd: String = "",
    val returnTravelStart: String = "",
    val returnTravelEnd: String = "",

    // Work hours
    val morningStart: String = "",
    val morningEnd: String = "",
    val afternoonStart: String = "",
    val afternoonEnd: String = "",

    // Expenses
    val morningPocketMoney: Boolean = false,
    val afternoonPocketMoney: Boolean = false,
    val totalKilometers: String = "",
    val flight: Boolean = false,
    val rentCar: Boolean = false,
    val transferToAirport: Boolean = false,
    val lodging: Boolean = false,

    val errorMessage: String? = null
)

/**
 * Data class to store original values for dirty checking
 */
data class WorkDayOriginalData(
    val date: String,
    val remoteAssistance: Boolean,
    val technicianCount: String,
    val technicianInitials: String,
    val outboundTravelStart: String,
    val outboundTravelEnd: String,
    val returnTravelStart: String,
    val returnTravelEnd: String,
    val morningStart: String,
    val morningEnd: String,
    val afternoonStart: String,
    val afternoonEnd: String,
    val morningPocketMoney: Boolean,
    val afternoonPocketMoney: Boolean,
    val totalKilometers: String,
    val flight: Boolean,
    val rentCar: Boolean,
    val transferToAirport: Boolean,
    val lodging: Boolean
)