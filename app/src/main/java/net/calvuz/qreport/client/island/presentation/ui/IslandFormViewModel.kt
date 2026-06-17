package net.calvuz.qreport.client.island.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.client.island.data.local.mapper.parse
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.usecase.CreateIslandUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandByIdUseCase
import net.calvuz.qreport.client.island.domain.usecase.ObserveActiveIslandTypesUseCase
import net.calvuz.qreport.client.island.domain.usecase.UpdateIslandUseCase
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

// =============================================================================
// UI STATE
// =============================================================================

data class FacilityIslandFormUiState(
    // ===== MODE =====
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val facilityId: String = "",
    val islandId: String? = null,
    val createdAt: Instant? = null,

    // ===== FORM FIELDS =====
    val serialNumber: String = "",
    val islandType: IslandType = IslandType.POLY_MOVE,
    val islandTypeId: String? = null,
    val availableIslandTypes: List<IslandTypeEntity> = emptyList(),
    val modelNumber: String = "",
    val customName: String = "",
    val location: String = "",
    val installationDate: Instant? = null,
    val warrantyExpiration: Instant? = null,
    val operatingHours: String = "0",
    val cycleCount: String = "0",
    val lastMaintenanceDate: Instant? = null,
    val nextScheduledMaintenance: Instant? = null,
    val isActive: Boolean = true,
    val notes: String = "",

    // ===== VALIDATION — null means no error =====
    val serialNumberError: UiText? = null,
    val modelNumberError: UiText? = null,
    val customNameError: UiText? = null,
    val locationError: UiText? = null,
    val installationDateError: UiText? = null,
    val warrantyExpirationError: UiText? = null,
    val operatingHoursError: UiText? = null,
    val cycleCountError: UiText? = null,
    val lastMaintenanceDateError: UiText? = null,
    val nextMaintenanceError: UiText? = null,
    val notesError: UiText? = null,

    // ===== OPERATION STATE =====
    val savedIslandId: String? = null,
    val error: UiText? = null
) {
    val isFormValid: Boolean
        get() = serialNumber.isNotBlank() &&
                serialNumberError == null &&
                modelNumberError == null &&
                customNameError == null &&
                locationError == null &&
                installationDateError == null &&
                warrantyExpirationError == null &&
                operatingHoursError == null &&
                cycleCountError == null &&
                lastMaintenanceDateError == null &&
                nextMaintenanceError == null &&
                notesError == null
}

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class IslandFormViewModel @Inject constructor(
    private val createIslandUseCase: CreateIslandUseCase,
    private val updateIslandUseCase: UpdateIslandUseCase,
    private val getIslandByIdUseCase: GetIslandByIdUseCase,
    private val observeActiveIslandTypes: ObserveActiveIslandTypesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandFormUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveIslandTypes().collect { types ->
                _uiState.update { it.copy(availableIslandTypes = types) }
            }
        }
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    fun initialize(facilityId: String, islandId: String? = null) {
        _uiState.update { it.copy(facilityId = facilityId, isEditMode = islandId != null) }
        islandId?.let { loadIslandForEdit(it) }
    }

    private fun loadIslandForEdit(islandId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getIslandByIdUseCase(islandId)) {
                is QrResult.Success -> populateFormWithIsland(result.data)
                is QrResult.Error -> {
                    Timber.e("Failed to load island for edit: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.err_island_load)
                        )
                    }
                }
            }
        }
    }

    private fun populateFormWithIsland(island: Island) {
        _uiState.update {
            it.copy(
                isLoading = false,
                islandId = island.id,
                facilityId = island.facilityId,
                serialNumber = island.serialNumber,
                islandType = island.islandType,
                islandTypeId = island.islandTypeId,
                modelNumber = island.modelNumber ?: "",
                customName = island.customName ?: "",
                location = island.location ?: "",
                installationDate = island.installationDate,
                warrantyExpiration = island.warrantyExpiration,
                operatingHours = island.operatingHours.toString(),
                cycleCount = island.cycleCount.toString(),
                lastMaintenanceDate = island.lastMaintenanceDate,
                nextScheduledMaintenance = island.nextScheduledMaintenance,
                isActive = island.isActive,
                notes = island.notes ?: "",
                createdAt = island.createdAt
            )
        }
    }

    // =========================================================================
    // FORM EVENTS
    // =========================================================================

    fun onFormEvent(event: FacilityIslandFormEvent) {
        when (event) {
            is FacilityIslandFormEvent.SerialNumberChanged -> {
                _uiState.update { it.copy(serialNumber = event.serialNumber, serialNumberError = validateSerialNumber(event.serialNumber)) }
            }
            is FacilityIslandFormEvent.IslandTypeChanged ->
                _uiState.update {
                    it.copy(
                        islandTypeId = event.type.id,
                        islandType = IslandType.parse(event.type.code)
                    )
                }
            is FacilityIslandFormEvent.ModelChanged ->
                _uiState.update { it.copy(modelNumber = event.model, modelNumberError = validateModelNumber(event.model)) }
            is FacilityIslandFormEvent.CustomNameChanged ->
                _uiState.update { it.copy(customName = event.customName, customNameError = validateCustomName(event.customName)) }
            is FacilityIslandFormEvent.LocationChanged ->
                _uiState.update { it.copy(location = event.location, locationError = validateLocation(event.location)) }
            is FacilityIslandFormEvent.InstallationDateChanged ->
                _uiState.update { s -> s.copy(installationDate = event.date, installationDateError = validateInstallationDate(event.date, s.warrantyExpiration)) }
            is FacilityIslandFormEvent.WarrantyExpirationChanged ->
                _uiState.update { s -> s.copy(warrantyExpiration = event.date, warrantyExpirationError = validateWarrantyExpiration(event.date, s.installationDate)) }
            is FacilityIslandFormEvent.OperatingHoursChanged ->
                _uiState.update { it.copy(operatingHours = event.hours, operatingHoursError = validateOperatingHours(event.hours)) }
            is FacilityIslandFormEvent.CycleCountChanged ->
                _uiState.update { it.copy(cycleCount = event.count, cycleCountError = validateCycleCount(event.count)) }
            is FacilityIslandFormEvent.LastMaintenanceDateChanged ->
                _uiState.update { s -> s.copy(lastMaintenanceDate = event.date, lastMaintenanceDateError = validateLastMaintenanceDate(event.date, s.installationDate, s.nextScheduledMaintenance)) }
            is FacilityIslandFormEvent.NextMaintenanceChanged ->
                _uiState.update { s -> s.copy(nextScheduledMaintenance = event.date, nextMaintenanceError = validateNextMaintenance(event.date, s.lastMaintenanceDate)) }
            is FacilityIslandFormEvent.IsActiveChanged ->
                _uiState.update { it.copy(isActive = event.isActive) }
            is FacilityIslandFormEvent.NotesChanged ->
                _uiState.update { it.copy(notes = event.notes, notesError = validateNotes(event.notes)) }
            is FacilityIslandFormEvent.SaveIsland -> saveIsland()
        }
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    private fun saveIsland() {
        val state = _uiState.value
        if (!state.isFormValid || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val island = buildIslandFromState(state)
            val result = if (state.isEditMode && state.islandId != null) {
                updateIslandUseCase(island)
            } else {
                createIslandUseCase(island)
            }

            when (result) {
                is QrResult.Success -> {
                    Timber.d("Island saved: ${island.id}")
                    _uiState.update { it.copy(isLoading = false, savedIslandId = island.id) }
                }
                is QrResult.Error -> {
                    Timber.e("Failed to save island: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.err_island_create)
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // VALIDATION — returns UiText? (null = no error)
    // =========================================================================

    private fun validateSerialNumber(value: String): UiText? = when {
        value.isBlank() -> UiText.StringResource(R.string.err_island_missing_serial)
        value.length < 3 -> UiText.StringResources(R.string.island_form_error_serial_min_length, 3)
        value.length > 50 -> UiText.StringResources(R.string.island_form_error_serial_max_length, 50)
        !value.matches("[A-Za-z0-9\\-_]+".toRegex()) -> UiText.StringResource(R.string.err_island_validation_invalid_serial_number)
        else -> null
    }

    private fun validateModelNumber(value: String): UiText? = when {
        value.length > 100 -> UiText.StringResources(R.string.island_form_error_model_max_length, 100)
        else -> null
    }

    private fun validateCustomName(value: String): UiText? = when {
        value.length > 100 -> UiText.StringResources(R.string.island_form_error_custom_name_max_length, 100)
        else -> null
    }

    private fun validateLocation(value: String): UiText? = when {
        value.length > 200 -> UiText.StringResources(R.string.island_form_error_location_max_length, 200)
        else -> null
    }

    private fun validateInstallationDate(date: Instant?, warrantyExpiration: Instant?): UiText? {
        val now = Clock.System.now()
        return when {
            date?.let { it > now } == true ->
                UiText.StringResource(R.string.island_form_error_installation_future)
            warrantyExpiration != null && date != null && warrantyExpiration < date ->
                UiText.StringResource(R.string.island_form_error_warranty_before_installation)
            else -> null
        }
    }

    private fun validateWarrantyExpiration(date: Instant?, installationDate: Instant?): UiText? = when {
        installationDate != null && date != null && date < installationDate ->
            UiText.StringResource(R.string.island_form_error_warranty_before_installation)
        else -> null
    }

    private fun validateOperatingHours(value: String): UiText? = when {
        value.isNotBlank() && value.toIntOrNull() == null ->
            UiText.StringResource(R.string.island_form_error_numeric)
        value.toIntOrNull()?.let { it < 0 } == true ->
            UiText.StringResource(R.string.island_form_error_negative)
        else -> null
    }

    private fun validateCycleCount(value: String): UiText? = when {
        value.isNotBlank() && value.toLongOrNull() == null ->
            UiText.StringResource(R.string.island_form_error_numeric)
        value.toLongOrNull()?.let { it < 0 } == true ->
            UiText.StringResource(R.string.island_form_error_negative)
        else -> null
    }

    private fun validateLastMaintenanceDate(last: Instant?, installation: Instant?, next: Instant?): UiText? {
        val now = Clock.System.now()
        return when {
            last?.let { it > now } == true ->
                UiText.StringResource(R.string.island_form_error_maintenance_future)
            installation != null && last != null && last < installation ->
                UiText.StringResource(R.string.island_form_error_maintenance_before_installation)
            next != null && last != null && next <= last ->
                UiText.StringResource(R.string.island_form_error_next_before_last)
            else -> null
        }
    }

    private fun validateNextMaintenance(next: Instant?, last: Instant?): UiText? = when {
        last != null && next != null && next <= last ->
            UiText.StringResource(R.string.island_form_error_next_before_last)
        else -> null
    }

    private fun validateNotes(value: String): UiText? = when {
        value.length > 1000 -> UiText.StringResources(R.string.island_form_error_notes_max_length, 1000)
        else -> null
    }

    // =========================================================================
    // BUILD DOMAIN OBJECT
    // =========================================================================

    private fun buildIslandFromState(state: FacilityIslandFormUiState): Island {
        val now = Clock.System.now()
        return Island(
            id = state.islandId ?: UUID.randomUUID().toString(),
            facilityId = state.facilityId,
            serialNumber = state.serialNumber.trim(),
            islandType = state.islandType,
            islandTypeId = state.islandTypeId,
            modelNumber = state.modelNumber.trim().takeIf { it.isNotBlank() },
            customName = state.customName.trim().takeIf { it.isNotBlank() },
            location = state.location.trim().takeIf { it.isNotBlank() },
            installationDate = state.installationDate,
            warrantyExpiration = state.warrantyExpiration,
            operatingHours = state.operatingHours.toIntOrNull() ?: 0,
            cycleCount = state.cycleCount.toLongOrNull() ?: 0L,
            lastMaintenanceDate = state.lastMaintenanceDate,
            nextScheduledMaintenance = state.nextScheduledMaintenance,
            isActive = state.isActive,
            notes = state.notes.trim().takeIf { it.isNotBlank() },
            createdAt = state.createdAt ?: now,
            updatedAt = now
        )
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

// =============================================================================
// EVENTS
// =============================================================================

sealed class FacilityIslandFormEvent {
    data class SerialNumberChanged(val serialNumber: String) : FacilityIslandFormEvent()
    data class IslandTypeChanged(val type: IslandTypeEntity) : FacilityIslandFormEvent()
    data class ModelChanged(val model: String) : FacilityIslandFormEvent()
    data class CustomNameChanged(val customName: String) : FacilityIslandFormEvent()
    data class LocationChanged(val location: String) : FacilityIslandFormEvent()
    data class InstallationDateChanged(val date: Instant?) : FacilityIslandFormEvent()
    data class WarrantyExpirationChanged(val date: Instant?) : FacilityIslandFormEvent()
    data class OperatingHoursChanged(val hours: String) : FacilityIslandFormEvent()
    data class CycleCountChanged(val count: String) : FacilityIslandFormEvent()
    data class LastMaintenanceDateChanged(val date: Instant?) : FacilityIslandFormEvent()
    data class NextMaintenanceChanged(val date: Instant?) : FacilityIslandFormEvent()
    data class IsActiveChanged(val isActive: Boolean) : FacilityIslandFormEvent()
    data class NotesChanged(val notes: String) : FacilityIslandFormEvent()
    object SaveIsland : FacilityIslandFormEvent()
}
