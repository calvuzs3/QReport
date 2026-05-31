package net.calvuz.qreport.client.facility.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.model.FacilityType
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilitiesByClientUseCase
import net.calvuz.qreport.client.facility.domain.usecase.NewFacilityUseCase
import net.calvuz.qreport.client.facility.domain.usecase.UpdateFacilityUseCase
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

// =============================================================================
// UI STATE
// =============================================================================

data class FacilityFormUiState(
    // ===== FORM FIELDS =====
    val name: String = "",
    val code: String = "",
    val facilityType: FacilityType = FacilityType.PRODUCTION,
    val notes: String = "",
    val street: String = "",
    val streetNumber: String = "",
    val city: String = "",
    val postalCode: String = "",
    val province: String = "",
    val country: String = "Italia",
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,

    // ===== VALIDATION =====
    val nameError: UiText? = null,
    val codeError: UiText? = null,
    val isFormValid: Boolean = false,

    // ===== OPERATION STATE =====
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val saveCompleted: Boolean = false,
    val savedFacilityId: String? = null,

    // ===== MODE =====
    val clientId: String = "",
    val facilityId: String? = null   // null = create mode
) {
    val isEditMode: Boolean get() = facilityId != null
    val hasAddressData: Boolean get() = street.isNotBlank() || city.isNotBlank()
}

// =============================================================================
// EVENTS
// =============================================================================

sealed class FacilityFormEvent {
    data class NameChanged(val name: String) : FacilityFormEvent()
    data class CodeChanged(val code: String) : FacilityFormEvent()
    data class TypeChanged(val type: FacilityType) : FacilityFormEvent()
    data class NotesChanged(val notes: String) : FacilityFormEvent()
    data class StreetChanged(val street: String) : FacilityFormEvent()
    data class StreetNumberChanged(val streetNumber: String) : FacilityFormEvent()
    data class CityChanged(val city: String) : FacilityFormEvent()
    data class PostalCodeChanged(val postalCode: String) : FacilityFormEvent()
    data class ProvinceChanged(val province: String) : FacilityFormEvent()
    data class CountryChanged(val country: String) : FacilityFormEvent()
    data class PrimaryChanged(val isPrimary: Boolean) : FacilityFormEvent()
}

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class FacilityFormViewModel @Inject constructor(
    private val newFacilityUseCase: NewFacilityUseCase,
    private val updateFacilityUseCase: UpdateFacilityUseCase,
    private val getFacilitiesByClientUseCase: GetFacilitiesByClientUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityFormUiState())
    val uiState: StateFlow<FacilityFormUiState> = _uiState.asStateFlow()

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    fun initialize(clientId: String, facilityId: String?) {
        if (clientId == _uiState.value.clientId && facilityId == _uiState.value.facilityId) return
        _uiState.update { it.copy(clientId = clientId, facilityId = facilityId) }
        if (facilityId != null) loadFacilityForEdit(facilityId)
        else validateForm()
    }

    // =========================================================================
    // FORM EVENTS
    // =========================================================================

    fun onFormEvent(event: FacilityFormEvent) {
        when (event) {
            is FacilityFormEvent.NameChanged -> updateName(event.name)
            is FacilityFormEvent.CodeChanged -> updateCode(event.code)
            is FacilityFormEvent.TypeChanged -> _uiState.update { it.copy(facilityType = event.type) }.also { validateForm() }
            is FacilityFormEvent.NotesChanged -> _uiState.update { it.copy(notes = event.notes) }.also { validateForm() }
            is FacilityFormEvent.StreetChanged -> _uiState.update { it.copy(street = event.street) }.also { validateForm() }
            is FacilityFormEvent.StreetNumberChanged -> _uiState.update { it.copy(streetNumber = event.streetNumber) }.also { validateForm() }
            is FacilityFormEvent.CityChanged -> _uiState.update { it.copy(city = event.city) }.also { validateForm() }
            is FacilityFormEvent.PostalCodeChanged -> _uiState.update { it.copy(postalCode = event.postalCode) }.also { validateForm() }
            is FacilityFormEvent.ProvinceChanged -> _uiState.update { it.copy(province = event.province) }.also { validateForm() }
            is FacilityFormEvent.CountryChanged -> _uiState.update { it.copy(country = event.country) }.also { validateForm() }
            is FacilityFormEvent.PrimaryChanged -> _uiState.update { it.copy(isPrimary = event.isPrimary) }.also { validateForm() }
        }
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    fun saveFacility() {
        val currentState = _uiState.value
        if (!currentState.isFormValid || currentState.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSaving = true, error = null) }

            val facility = buildFacilityFromState(currentState)

            val result = if (currentState.isEditMode && currentState.facilityId != null) {
                updateFacilityUseCase(facility)
            } else {
                newFacilityUseCase(facility)
            }

            when (result) {
                is QrResult.Success -> {
                    Timber.d("Facility saved: ${facility.id} ${facility.displayName}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isLoading = false,
                            saveCompleted = true,
                            savedFacilityId = facility.id,
                            error = null
                        )
                    }
                }
                is QrResult.Error -> {
                    Timber.e("Failed to save facility: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isLoading = false,
                            error = UiText.StringResource(R.string.err_facility_create)
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
        validateForm()
    }

    private fun updateCode(code: String) {
        _uiState.update { it.copy(code = code, codeError = null) }
        validateForm()
    }

    private fun validateForm() {
        val state = _uiState.value

        val nameError: UiText? = when {
            state.name.isBlank() ->
                UiText.StringResource(R.string.facility_form_error_name_required)
            state.name.length < 2 ->
                UiText.StringResources(R.string.facility_form_error_name_min_length, 2)
            state.name.length > 100 ->
                UiText.StringResources(R.string.facility_form_error_name_max_length, 100)
            else -> null
        }

        val codeError: UiText? = when {
            state.code.length > 50 ->
                UiText.StringResources(R.string.facility_form_error_code_max_length, 50)
            else -> null
        }

        _uiState.update {
            it.copy(
                nameError = nameError,
                codeError = codeError,
                isFormValid = nameError == null && codeError == null
            )
        }
    }

    // =========================================================================
    // LOAD FOR EDIT
    // =========================================================================

    private fun loadFacilityForEdit(facilityId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = getFacilitiesByClientUseCase(_uiState.value.clientId)) {
                is QrResult.Success -> {
                    val facility = result.data.find { it.id == facilityId }
                    if (facility != null && currentCoroutineContext().isActive) {
                        populateFormWithFacility(facility)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = UiText.StringResource(R.string.err_facility_not_found)
                            )
                        }
                    }
                }
                is QrResult.Error -> {
                    if (currentCoroutineContext().isActive) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = UiText.StringResource(R.string.err_facility_load)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun populateFormWithFacility(facility: Facility) {
        _uiState.update {
            it.copy(
                name = facility.name,
                code = facility.code ?: "",
                facilityType = facility.facilityType,
                notes = facility.notes ?: "",
                street = facility.address?.street ?: "",
                streetNumber = facility.address?.streetNumber ?: "",
                city = facility.address?.city ?: "",
                postalCode = facility.address?.postalCode ?: "",
                province = facility.address?.province ?: "",
                country = facility.address?.country ?: "Italia",
                isPrimary = facility.isPrimary,
                isActive = facility.isActive,
                isLoading = false
            )
        }
        validateForm()
    }

    private fun buildFacilityFromState(state: FacilityFormUiState): Facility {
        val address = if (state.hasAddressData) {
            Address(
                street = state.street.takeIf { it.isNotBlank() },
                streetNumber = state.streetNumber.takeIf { it.isNotBlank() },
                city = state.city.takeIf { it.isNotBlank() },
                province = state.province.takeIf { it.isNotBlank() },
                postalCode = state.postalCode.takeIf { it.isNotBlank() },
                country = "Italia"
            )
        } else null

        val now = Clock.System.now()
        return Facility(
            id = state.facilityId ?: UUID.randomUUID().toString(),
            clientId = state.clientId,
            name = state.name.trim(),
            code = state.code.trim().takeIf { it.isNotBlank() },
            notes = state.notes.takeIf { it.isNotBlank() },
            facilityType = state.facilityType,
            address = address,
            isPrimary = state.isPrimary,
            isActive = true,
            createdAt = now,    // Preserved in update by the use case
            updatedAt = now
        )
    }

    // =========================================================================
    // ERROR / UTILITY
    // =========================================================================

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false) }
    }
}