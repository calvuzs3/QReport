package net.calvuz.qreport.presentation.feature.client.facility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityType
import net.calvuz.qreport.domain.model.client.Address
import net.calvuz.qreport.domain.usecase.client.facility.CreateFacilityUseCase
import net.calvuz.qreport.domain.usecase.client.facility.UpdateFacilityUseCase
import net.calvuz.qreport.domain.usecase.client.facility.GetFacilitiesByClientUseCase
import kotlinx.datetime.Clock
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per FacilityFormScreen - Create/Edit facility
 *
 * Features:
 * - Gestione create/edit mode
 * - Validazioni complete form
 * - State management con eventi
 * - Gestione facility primaria
 * - Error handling
 */

data class FacilityFormUiState(
    // Form fields
    val name: String = "",
    val code: String = "",
    val facilityType: FacilityType = FacilityType.PRODUCTION,
    val description: String = "",

    // Address fields
    val street: String = "",
    val city: String = "",
    val postalCode: String = "",
    val province: String = "",
    val country: String = "IT",

    // Options
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,

    // Validation errors
    val nameError: String? = null,
    val codeError: String? = null,
    val cityError: String? = null,
    val streetError: String? = null,

    // State
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedFacilityId: String? = null,
    val clientId: String = "",
    val facilityId: String? = null, // null = create mode
    val isFormValid: Boolean = false
) {
    val isEditMode: Boolean = facilityId != null
}

sealed class FacilityFormEvent {
    data class NameChanged(val name: String) : FacilityFormEvent()
    data class CodeChanged(val code: String) : FacilityFormEvent()
    data class TypeChanged(val type: FacilityType) : FacilityFormEvent()
    data class DescriptionChanged(val description: String) : FacilityFormEvent()
    data class StreetChanged(val street: String) : FacilityFormEvent()
    data class CityChanged(val city: String) : FacilityFormEvent()
    data class PostalCodeChanged(val postalCode: String) : FacilityFormEvent()
    data class ProvinceChanged(val province: String) : FacilityFormEvent()
    data class CountryChanged(val country: String) : FacilityFormEvent()
    data class PrimaryChanged(val isPrimary: Boolean) : FacilityFormEvent()
    data class ActiveChanged(val isActive: Boolean) : FacilityFormEvent()
}

@HiltViewModel
class FacilityFormViewModel @Inject constructor(
    private val createFacilityUseCase: CreateFacilityUseCase,
    private val updateFacilityUseCase: UpdateFacilityUseCase,
    private val getFacilitiesByClientUseCase: GetFacilitiesByClientUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityFormUiState())
    val uiState: StateFlow<FacilityFormUiState> = _uiState.asStateFlow()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun initialize(clientId: String, facilityId: String?) {
        if (clientId == _uiState.value.clientId && facilityId == _uiState.value.facilityId) return

        _uiState.value = _uiState.value.copy(
            clientId = clientId,
            facilityId = facilityId
        )

        if (facilityId != null) {
            loadFacilityForEdit(facilityId)
        } else {
            // Set defaults for new facility
            validateForm()
        }
    }

//    fun initialize(clientId: String, facilityId: String?) {
//        viewModelScope.launch {
//            if (facilityId != null) {
//                // Edit mode: determina clientId dalla facility esistente
//                 getFacilitiesByClientUseCase.getFacilityById(facilityId).fold(
//                    onSuccess = { facility ->
//                        if (facility != null) {
//                            _uiState.value = _uiState.value.copy(
//                                clientId = facility.clientId, // ✅ Auto-determine clientId
//                                facilityId = facilityId
//                            )
//                            populateFormWithFacility(facility)
//                        }
//                    },
//                    onFailure = { error ->
//                        _uiState.value = _uiState.value.copy(
//                            error = "Errore caricamento: ${error.message}"
//                        )
//                    }
//                )
//            } else {
//                // Create mode: usa clientId passato
//                _uiState.value = _uiState.value.copy(
//                    clientId = clientId,
//                    facilityId = null
//                )
//            }
//            validateForm()
//        }
//    }

    fun onFormEvent(event: FacilityFormEvent) {
        when (event) {
            is FacilityFormEvent.NameChanged -> updateName(event.name)
            is FacilityFormEvent.CodeChanged -> updateCode(event.code)
            is FacilityFormEvent.TypeChanged -> updateType(event.type)
            is FacilityFormEvent.DescriptionChanged -> updateDescription(event.description)
            is FacilityFormEvent.StreetChanged -> updateStreet(event.street)
            is FacilityFormEvent.CityChanged -> updateCity(event.city)
            is FacilityFormEvent.PostalCodeChanged -> updatePostalCode(event.postalCode)
            is FacilityFormEvent.ProvinceChanged -> updateProvince(event.province)
            is FacilityFormEvent.CountryChanged -> updateCountry(event.country)
            is FacilityFormEvent.PrimaryChanged -> updatePrimary(event.isPrimary)
            is FacilityFormEvent.ActiveChanged -> updateActive(event.isActive)
        }
    }

    fun saveFacility() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                if (currentState.isEditMode) {
                    updateExistingFacility()
                } else {
                    createNewFacility()
                }
            } catch (_: CancellationException) {
                Timber.d("Save facility cancelled")
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to save facility")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore salvataggio: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS - Form Updates
    // ============================================================

    private fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = null
        )
        validateForm()
    }

    private fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(
            code = code,
            codeError = null
        )
        validateForm()
    }

    private fun updateType(type: FacilityType) {
        _uiState.value = _uiState.value.copy(facilityType = type)
        validateForm()
    }

    private fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
        validateForm()
    }

    private fun updateStreet(street: String) {
        _uiState.value = _uiState.value.copy(
            street = street,
            streetError = null
        )
        validateForm()
    }

    private fun updateCity(city: String) {
        _uiState.value = _uiState.value.copy(
            city = city,
            cityError = null
        )
        validateForm()
    }

    private fun updatePostalCode(postalCode: String) {
        _uiState.value = _uiState.value.copy(postalCode = postalCode)
        validateForm()
    }

    private fun updateProvince(province: String) {
        _uiState.value = _uiState.value.copy(province = province)
        validateForm()
    }

    private fun updateCountry(country: String) {
        _uiState.value = _uiState.value.copy(country = country)
        validateForm()
    }

    private fun updatePrimary(isPrimary: Boolean) {
        _uiState.value = _uiState.value.copy(isPrimary = isPrimary)
        validateForm()
    }

    private fun updateActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isActive = isActive)
        validateForm()
    }

    // ============================================================
    // PRIVATE METHODS - Validation
    // ============================================================

    private fun validateForm() {
        val currentState = _uiState.value

        // Validate name
        val nameError = when {
            currentState.name.isBlank() -> "Nome stabilimento è obbligatorio"
            currentState.name.length < 2 -> "Nome troppo corto (minimo 2 caratteri)"
            currentState.name.length > 100 -> "Nome troppo lungo (massimo 100 caratteri)"
            else -> null
        }

        // Validate code (optional but if provided must be valid)
        val codeError = when {
            currentState.code.length > 50 -> "Codice troppo lungo (massimo 50 caratteri)"
            else -> null
        }

        // Validate city
        val cityError = when {
            currentState.city.isBlank() -> "Città è obbligatoria"
            else -> null
        }

        // Validate street (optional but basic check)
        val streetError = when {
            currentState.street.isNotBlank() && currentState.street.length < 3 -> "Indirizzo troppo corto"
            else -> null
        }

        val isValid = nameError == null && codeError == null && cityError == null && streetError == null

        _uiState.value = currentState.copy(
            nameError = nameError,
            codeError = codeError,
            cityError = cityError,
            streetError = streetError,
            isFormValid = isValid
        )
    }

    // ============================================================
    // PRIVATE METHODS - Data Operations
    // ============================================================

    private fun loadFacilityForEdit(facilityId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                getFacilitiesByClientUseCase(_uiState.value.clientId).fold(
                    onSuccess = { facilities ->
                        val facility = facilities.find { it.id == facilityId }
                        if (facility != null && currentCoroutineContext().isActive) {
                            populateFormWithFacility(facility)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Stabilimento non trovato"
                            )
                        }
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Errore caricamento: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to load facility for edit")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore caricamento stabilimento"
                    )
                }
            }
        }
    }

    private fun populateFormWithFacility(facility: Facility) {
        _uiState.value = _uiState.value.copy(
            name = facility.name,
            code = facility.code ?: "",
            facilityType = facility.facilityType,
            description = facility.description ?: "",
            street = facility.address.street ?: "",
            city = facility.address.city ?: "",
            postalCode = facility.address.postalCode ?: "",
            province = facility.address.province ?: "",
            country = facility.address.country ?: "Italia",
            isPrimary = facility.isPrimary,
            isActive = facility.isActive,
            isLoading = false
        )
        validateForm()
    }

    private suspend fun createNewFacility() {
        val currentState = _uiState.value

        createFacilityUseCase(
            clientId = currentState.clientId,
            name = currentState.name.trim(),
            address = createAddress(currentState),
            facilityType = currentState.facilityType,
            code = currentState.code.trim().takeIf { it.isNotBlank() },
            description = currentState.description.trim().takeIf { it.isNotBlank() },
            isPrimary = currentState.isPrimary
        ).fold(
            onSuccess = { facilityId ->
                if (currentCoroutineContext().isActive) {
                    Timber.d("Facility created successfully: $facilityId")
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        savedFacilityId = facilityId
                    )
                }
            },
            onFailure = { error ->
                if (currentCoroutineContext().isActive) {
                    Timber.e(error, "Failed to create facility")
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Errore creazione: ${error.message}"
                    )
                }
            }
        )
    }

    private suspend fun updateExistingFacility() {
        val currentState = _uiState.value
        val facilityId = currentState.facilityId ?: return

        // First get the original facility to preserve fields we don't edit
        getFacilitiesByClientUseCase(currentState.clientId).fold(
            onSuccess = { facilities ->
                val originalFacility = facilities.find { it.id == facilityId }
                if (originalFacility != null) {
                    val updatedFacility = originalFacility.copy(
                        name = currentState.name.trim(),
                        code = currentState.code.trim().takeIf { it.isNotBlank() },
                        facilityType = currentState.facilityType,
                        description = currentState.description.trim().takeIf { it.isNotBlank() },
                        address = createAddress(currentState),
                        isPrimary = currentState.isPrimary,
                        isActive = currentState.isActive,
                        updatedAt = Clock.System.now()
                    )

                    updateFacilityUseCase(updatedFacility).fold(
                        onSuccess = {
                            if (currentCoroutineContext().isActive) {
                                Timber.d("Facility updated successfully: $facilityId")
                                _uiState.value = currentState.copy(
                                    isLoading = false,
                                    savedFacilityId = facilityId
                                )
                            }
                        },
                        onFailure = { error ->
                            if (currentCoroutineContext().isActive) {
                                Timber.e(error, "Failed to update facility")
                                _uiState.value = currentState.copy(
                                    isLoading = false,
                                    error = "Errore aggiornamento: ${error.message}"
                                )
                            }
                        }
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Stabilimento non trovato"
                    )
                }
            },
            onFailure = { error ->
                if (currentCoroutineContext().isActive) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Errore aggiornamento: ${error.message}"
                    )
                }
            }
        )
    }

    private fun createAddress(state: FacilityFormUiState): Address {
        return Address(
            street = state.street.trim().takeIf { it.isNotBlank() },
            city = state.city.trim().takeIf { it.isNotBlank() },
            postalCode = state.postalCode.trim().takeIf { it.isNotBlank() },
            province = state.province.trim().takeIf { it.isNotBlank() },
            country = state.country.trim().takeIf { it.isNotBlank() } ?: "Italia"
        )
    }
}