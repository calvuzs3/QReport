package net.calvuz.qreport.client.island.maintenance.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOperationType
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOutcome
import net.calvuz.qreport.client.island.domain.usecase.GetIslandByIdUseCase
import net.calvuz.qreport.client.island.maintenance.domain.usecase.CreateMaintenanceLogUseCase
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.usecase.GetMechanicalUnitsByIslandUseCase
import net.calvuz.qreport.settings.domain.usecase.TechnicianSettingsUseCase
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// UI STATE
// =============================================================================

data class MaintenanceLogFormUiState(

    // ===== CONTEXT =====
    val islandId: String = "",
    val islandName: String = "",                // resolved from GetIslandByIdUseCase
    val isLoading: Boolean = false,

    // ===== FORM FIELDS =====
    val performedAt: Instant = Clock.System.now(),
    val operationType: MaintenanceOperationType = MaintenanceOperationType.ROUTINE_INSPECTION,
    val customOperationLabel: String = "",

    // Component target — mutually exclusive in UI, both optional
    val selectedUnitId: String? = null,         // FK — set when user picks from list
    val componentLabel: String = "",            // free text — active when selectedUnitId == null

    val description: String = "",
    val outcome: MaintenanceOutcome = MaintenanceOutcome.COMPLETED,
    val durationMinutes: String = "",           // String for TextField; parsed on save
    val operatingHoursAtEvent: String = "",
    val cycleCountAtEvent: String = "",
    val notes: String = "",

    // Technician — pre-filled from TechnicianSettings, editable
    val technicianName: String = "",
    val technicianCompany: String = "",

    // ===== AVAILABLE UNITS (for dropdown) =====
    val availableUnits: List<MechanicalUnit> = emptyList(),

    // ===== VALIDATION — null = no error =====
    val performedAtError: UiText? = null,
    val customOperationLabelError: UiText? = null,
    val descriptionError: UiText? = null,
    val durationError: UiText? = null,
    val operatingHoursError: UiText? = null,
    val cycleCountError: UiText? = null,
    val technicianNameError: UiText? = null,

    // ===== OPERATION STATE =====
    val saved: Boolean = false,
    val error: UiText? = null,

    // ===== UNSAVED CHANGES GUARD =====
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false
) {
    val isFormValid: Boolean
        get() = description.isNotBlank() &&
                technicianName.isNotBlank() &&
                (operationType != MaintenanceOperationType.OTHER || customOperationLabel.isNotBlank()) &&
                performedAtError == null &&
                customOperationLabelError == null &&
                descriptionError == null &&
                durationError == null &&
                operatingHoursError == null &&
                cycleCountError == null &&
                technicianNameError == null

    /** True when user picked OTHER and needs to fill customOperationLabel. */
    val isCustomLabelRequired: Boolean
        get() = operationType == MaintenanceOperationType.OTHER

    /** True when user has not selected a unit from the list. */
    val isFreeTextComponentActive: Boolean
        get() = selectedUnitId == null
}

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class MaintenanceLogFormViewModel @Inject constructor(
    private val createMaintenanceLogUseCase: CreateMaintenanceLogUseCase,
    private val getUnitsByIslandUseCase: GetMechanicalUnitsByIslandUseCase,
    private val technicianSettingsUseCase: TechnicianSettingsUseCase,
    private val getIslandByIdUseCase: GetIslandByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaintenanceLogFormUiState())
    val uiState = _uiState.asStateFlow()

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    fun initialize(islandId: String) {
        _uiState.update { it.copy(islandId = islandId) }
        resolveIslandName(islandId)
        loadAvailableUnits(islandId)
        prefillTechnician()
    }

    private fun resolveIslandName(islandId: String) {
        viewModelScope.launch {
            when (val result = getIslandByIdUseCase(islandId)) {
                is QrResult.Success -> {
                    val island = result.data
                    val name = island.customName ?: island.serialNumber
                    _uiState.update { it.copy(islandName = name) }
                }
                is QrResult.Error ->
                    Timber.w("Could not resolve island name for $islandId: ${result.error}")
                // Non-blocking — TopAppBar will show an empty subtitle, not a crash
            }
        }
    }

    private fun loadAvailableUnits(islandId: String) {
        viewModelScope.launch {
            when (val result = getUnitsByIslandUseCase(islandId)) {
                is QrResult.Success ->
                    _uiState.update { it.copy(availableUnits = result.data) }
                is QrResult.Error ->
                    Timber.w("Could not load units for island $islandId: ${result.error}")
                // Non-blocking — form works without unit list (free text fallback)
            }
        }
    }

    private fun prefillTechnician() {
        viewModelScope.launch {
            // One-shot read — no need to keep a Flow open for a prefill.
            // getTechnicianInfoImmediate() already returns TechnicianInfo() on error.
            val info = technicianSettingsUseCase.getTechnicianInfoImmediate()

            // Only prefill if saved data is non-empty AND form fields are still untouched.
            // If the technician has no saved profile the form stays blank for manual entry.
            if (info.name.isBlank() && info.company.isBlank()) {
                Timber.d("No saved technician data — form left blank for manual entry")
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    technicianName = state.technicianName.ifBlank { info.name },
                    technicianCompany = state.technicianCompany.ifBlank { info.company }
                )
            }
        }
    }

    // =========================================================================
    // FORM EVENTS
    // =========================================================================

    fun onFormEvent(event: MaintenanceLogFormEvent) {
        when (event) {

            is MaintenanceLogFormEvent.PerformedAtChanged ->
                _uiState.update {
                    it.copy(
                        performedAt = event.instant,
                        performedAtError = validatePerformedAt(event.instant),
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.OperationTypeChanged ->
                _uiState.update {
                    it.copy(
                        operationType = event.type,
                        customOperationLabel = if (event.type != MaintenanceOperationType.OTHER) "" else it.customOperationLabel,
                        customOperationLabelError = null,
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.CustomOperationLabelChanged ->
                _uiState.update {
                    it.copy(
                        customOperationLabel = event.label,
                        customOperationLabelError = validateCustomLabel(it.operationType, event.label),
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.UnitSelected ->
                _uiState.update {
                    it.copy(
                        selectedUnitId = event.unitId,
                        componentLabel = "",    // clear free text when FK is set
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.UnitCleared ->
                _uiState.update {
                    it.copy(selectedUnitId = null, hasUnsavedChanges = true)
                }

            is MaintenanceLogFormEvent.ComponentLabelChanged ->
                _uiState.update {
                    it.copy(
                        componentLabel = event.label,
                        selectedUnitId = null,  // clear FK when user types free text
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.DescriptionChanged ->
                _uiState.update {
                    it.copy(
                        description = event.text,
                        descriptionError = validateDescription(event.text),
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.OutcomeChanged ->
                _uiState.update {
                    it.copy(outcome = event.outcome, hasUnsavedChanges = true)
                }

            is MaintenanceLogFormEvent.DurationChanged ->
                _uiState.update {
                    it.copy(
                        durationMinutes = event.value,
                        durationError = validateOptionalPositiveInt(event.value),
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.OperatingHoursChanged ->
                _uiState.update {
                    it.copy(
                        operatingHoursAtEvent = event.value,
                        operatingHoursError = validateOptionalPositiveInt(event.value),
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.CycleCountChanged ->
                _uiState.update {
                    it.copy(
                        cycleCountAtEvent = event.value,
                        cycleCountError = validateOptionalPositiveLong(event.value),
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.TechnicianNameChanged ->
                _uiState.update {
                    it.copy(
                        technicianName = event.name,
                        technicianNameError = validateTechnicianName(event.name),
                        hasUnsavedChanges = true
                    )
                }

            is MaintenanceLogFormEvent.TechnicianCompanyChanged ->
                _uiState.update {
                    it.copy(technicianCompany = event.company, hasUnsavedChanges = true)
                }

            is MaintenanceLogFormEvent.NotesChanged ->
                _uiState.update {
                    it.copy(notes = event.text, hasUnsavedChanges = true)
                }

            is MaintenanceLogFormEvent.SaveLog -> saveLog()

            // ── Unsaved changes guard ──────────────────────────────────────────
            is MaintenanceLogFormEvent.BackPressed -> {
                if (_uiState.value.hasUnsavedChanges) {
                    _uiState.update { it.copy(showUnsavedChangesDialog = true) }
                } else {
                    _uiState.update { it.copy(saved = false) } // signal navigator to pop
                }
            }
            is MaintenanceLogFormEvent.ConfirmDiscard ->
                _uiState.update {
                    it.copy(showUnsavedChangesDialog = false, hasUnsavedChanges = false, saved = false)
                }
            is MaintenanceLogFormEvent.DismissUnsavedDialog ->
                _uiState.update { it.copy(showUnsavedChangesDialog = false) }
        }
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    private fun saveLog() {
        val state = _uiState.value
        if (!state.isFormValid || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val log = buildLogFromState(state)
            when (val result = createMaintenanceLogUseCase(log)) {
                is QrResult.Success -> {
                    Timber.d("MaintenanceLog saved for island ${state.islandId}")
                    _uiState.update {
                        it.copy(isLoading = false, saved = true, hasUnsavedChanges = false)
                    }
                }
                is QrResult.Error -> {
                    Timber.e("Failed to save MaintenanceLog: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.asUiText()
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // VALIDATION — returns UiText? (null = no error)
    // =========================================================================

    private fun validatePerformedAt(instant: Instant): UiText? = when {
        instant > Clock.System.now() ->
            UiText.StringResource(R.string.maint_error_invalid_performed_at)
        else -> null
    }

    private fun validateCustomLabel(type: MaintenanceOperationType, value: String): UiText? = when {
        type == MaintenanceOperationType.OTHER && value.isBlank() ->
            UiText.StringResource(R.string.maint_error_missing_custom_label)
        else -> null
    }

    private fun validateDescription(value: String): UiText? = when {
        value.isBlank() -> UiText.StringResource(R.string.maint_error_missing_description)
        else -> null
    }

    private fun validateTechnicianName(value: String): UiText? = when {
        value.isBlank() -> UiText.StringResource(R.string.maint_error_missing_technician)
        else -> null
    }

    private fun validateOptionalPositiveInt(value: String): UiText? = when {
        value.isNotBlank() && value.toIntOrNull() == null ->
            UiText.StringResource(R.string.island_form_error_numeric)
        value.toIntOrNull()?.let { it < 0 } == true ->
            UiText.StringResource(R.string.island_form_error_negative)
        else -> null
    }

    private fun validateOptionalPositiveLong(value: String): UiText? = when {
        value.isNotBlank() && value.toLongOrNull() == null ->
            UiText.StringResource(R.string.island_form_error_numeric)
        value.toLongOrNull()?.let { it < 0 } == true ->
            UiText.StringResource(R.string.island_form_error_negative)
        else -> null
    }

    // =========================================================================
    // BUILD DOMAIN OBJECT
    // =========================================================================

    private fun buildLogFromState(state: MaintenanceLogFormUiState): MaintenanceLog {
        val now = Clock.System.now()
        return MaintenanceLog(
            id = "",                            // assigned by CreateMaintenanceLogUseCase
            islandId = state.islandId,
            operationType = state.operationType,
            customOperationLabel = state.customOperationLabel.trim().takeIf { it.isNotBlank() },
            mechanicalUnitId = state.selectedUnitId,
            componentLabel = state.componentLabel.trim().takeIf { it.isNotBlank() },
            description = state.description.trim(),
            technicianName = state.technicianName.trim(),
            technicianCompany = state.technicianCompany.trim().takeIf { it.isNotBlank() },
            operatingHoursAtEvent = state.operatingHoursAtEvent.toIntOrNull(),
            cycleCountAtEvent = state.cycleCountAtEvent.toLongOrNull(),
            outcome = state.outcome,
            durationMinutes = state.durationMinutes.toIntOrNull(),
            notes = state.notes.trim().takeIf { it.isNotBlank() },
            performedAt = state.performedAt,
            createdAt = now,
            updatedAt = now
        )
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // =========================================================================
    // PRIVATE — QrError → UiText (local extension, avoids import of QrErrorExt)
    // =========================================================================

    private fun QrError.MaintenanceLogError.asUiText(): UiText = when (this) {
        is QrError.MaintenanceLogError.MissingDescription ->
            UiText.StringResource(R.string.maint_error_missing_description)
        is QrError.MaintenanceLogError.MissingTechnicianName ->
            UiText.StringResource(R.string.maint_error_missing_technician)
        is QrError.MaintenanceLogError.MissingCustomLabel ->
            UiText.StringResource(R.string.maint_error_missing_custom_label)
        is QrError.MaintenanceLogError.InvalidPerformedAt ->
            UiText.StringResource(R.string.maint_error_invalid_performed_at)
        is QrError.MaintenanceLogError.IslandNotFound ->
            UiText.StringResource(R.string.maint_error_island_not_found)
        is QrError.MaintenanceLogError.UnitNotInIsland ->
            UiText.StringResource(R.string.maint_error_unit_not_in_island)
        is QrError.MaintenanceLogError.CreateError ->
            UiText.StringResource(R.string.maint_error_create)
        is QrError.MaintenanceLogError.LoadError ->
            UiText.StringResource(R.string.maint_error_load)
        is QrError.MaintenanceLogError.UpdateError ->
            UiText.StringResource(R.string.maint_error_update)
    }
}

// =============================================================================
// EVENTS
// =============================================================================

sealed class MaintenanceLogFormEvent {
    data class PerformedAtChanged(val instant: Instant) : MaintenanceLogFormEvent()
    data class OperationTypeChanged(val type: MaintenanceOperationType) : MaintenanceLogFormEvent()
    data class CustomOperationLabelChanged(val label: String) : MaintenanceLogFormEvent()
    data class UnitSelected(val unitId: String) : MaintenanceLogFormEvent()
    object UnitCleared : MaintenanceLogFormEvent()
    data class ComponentLabelChanged(val label: String) : MaintenanceLogFormEvent()
    data class DescriptionChanged(val text: String) : MaintenanceLogFormEvent()
    data class OutcomeChanged(val outcome: MaintenanceOutcome) : MaintenanceLogFormEvent()
    data class DurationChanged(val value: String) : MaintenanceLogFormEvent()
    data class OperatingHoursChanged(val value: String) : MaintenanceLogFormEvent()
    data class CycleCountChanged(val value: String) : MaintenanceLogFormEvent()
    data class TechnicianNameChanged(val name: String) : MaintenanceLogFormEvent()
    data class TechnicianCompanyChanged(val company: String) : MaintenanceLogFormEvent()
    data class NotesChanged(val text: String) : MaintenanceLogFormEvent()
    object SaveLog : MaintenanceLogFormEvent()
    object BackPressed : MaintenanceLogFormEvent()
    object ConfirmDiscard : MaintenanceLogFormEvent()
    object DismissUnsavedDialog : MaintenanceLogFormEvent()
}