package net.calvuz.qreport.client.client.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.UiText.StringResources
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.usecase.CreateClientUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetClientByIdUseCase
import net.calvuz.qreport.client.client.domain.usecase.UpdateClientUseCase
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

// =============================================================================
// UI STATE
// =============================================================================

data class ClientFormUiState(

    // ===== FORM FIELDS =====
    val companyName: String = "",
    val notes: String = "",
    val street: String = "",
    val streetNumber: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",

    // ===== VALIDATION =====
    val companyNameError: UiText? = null,
    val hasErrors: Boolean = false,

    // ===== OPERATION STATE =====
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val error: UiText? = null,
    val saveCompleted: Boolean = false,
    val savedClientId: String? = null,
    val savedClientName: String? = null,

    // ===== EDIT MODE =====
    val clientId: String? = null,
    val isEditMode: Boolean = false

) {
    val canSave: Boolean
        get() = companyName.isNotBlank() &&
                companyNameError == null &&
                !isSaving &&
                !isLoading

    val hasAddressData: Boolean
        get() = street.isNotBlank() || city.isNotBlank()
}

// =============================================================================
// EVENTS
// =============================================================================

sealed class ClientFormEvent {
    data class CompanyNameChanged(val value: String) : ClientFormEvent()
    data class NotesChanged(val value: String) : ClientFormEvent()
    data class StreetChanged(val value: String) : ClientFormEvent()
    data class StreetNumberChanged(val value: String) : ClientFormEvent()
    data class CityChanged(val value: String) : ClientFormEvent()
    data class ProvinceChanged(val value: String) : ClientFormEvent()
    data class PostalCodeChanged(val value: String) : ClientFormEvent()
    data class CountryChanged(val value: String) : ClientFormEvent()
}

// =============================================================================
// VIEW MODEL
// =============================================================================

@HiltViewModel
class ClientFormViewModel @Inject constructor(
    private val createClientUseCase: CreateClientUseCase,
    private val updateClientUseCase: UpdateClientUseCase,
    private val getClientByIdUseCase: GetClientByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientFormUiState())
    val uiState: StateFlow<ClientFormUiState> = _uiState.asStateFlow()

    companion object {
        private const val COMPANY_NAME_MIN_CHAR = 2
        private const val COMPANY_NAME_MAX_CHAR = 255
    }

    init {
        Timber.d("ClientFormViewModel initialized")
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    fun initForEdit(clientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditMode = true, isLoading = true, clientId = clientId) }

            when (val result = getClientByIdUseCase(clientId)) {
                is QrResult.Success -> {
                    Timber.d("Client loaded for edit: ${result.data.companyName}")
                    populateFormFromClient(result.data)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to load client for edit: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.client_form_error_load)
                        )
                    }
                }
            }
        }
    }

    private fun populateFormFromClient(client: Client) {
        val address = client.headquarters
        _uiState.update {
            it.copy(
                isLoading = false,
                companyName = client.companyName,
                notes = client.notes ?: "",
                street = address?.street ?: "",
                streetNumber = address?.streetNumber ?: "",
                city = address?.city ?: "",
                province = address?.province ?: "",
                postalCode = address?.postalCode ?: ""
            )
        }
    }

    // =========================================================================
    // FORM EVENTS
    // =========================================================================

    fun onFormEvent(event: ClientFormEvent) {
        when (event) {
            is ClientFormEvent.CompanyNameChanged -> updateCompanyName(event.value)
            is ClientFormEvent.NotesChanged -> _uiState.update { it.copy(notes = event.value, isDirty = true) }
            is ClientFormEvent.StreetChanged -> _uiState.update { it.copy(street = event.value, isDirty = true) }
            is ClientFormEvent.StreetNumberChanged -> _uiState.update { it.copy(streetNumber = event.value, isDirty = true) }
            is ClientFormEvent.CityChanged -> _uiState.update { it.copy(city = event.value, isDirty = true) }
            is ClientFormEvent.ProvinceChanged -> _uiState.update { it.copy(province = event.value.uppercase(), isDirty = true) }
            is ClientFormEvent.PostalCodeChanged -> _uiState.update { it.copy(postalCode = event.value.replace("\\s+".toRegex(), ""), isDirty = true) }
            is ClientFormEvent.CountryChanged -> _uiState.update { it.copy(country = event.value, isDirty = true) }
        }
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    private fun updateCompanyName(value: String) {
        _uiState.update {
            it.copy(
                companyName = value,
                isDirty = true,
                companyNameError = validateCompanyName(value)
            )
        }
    }

    private fun validateCompanyName(value: String): UiText? = when {
        value.isBlank() -> UiText.StringResource(R.string.client_form_error_company_name_required)
        value.length < COMPANY_NAME_MIN_CHAR -> StringResources(R.string.client_form_error_company_name_min_length, COMPANY_NAME_MIN_CHAR)
        value.length > COMPANY_NAME_MAX_CHAR -> StringResources(R.string.client_form_error_company_name_max_length, COMPANY_NAME_MAX_CHAR)
        else -> null
    }

    private fun validateAll(state: ClientFormUiState): List<UiText?> = listOf(
        validateCompanyName(state.companyName)
    )

    // =========================================================================
    // SAVE
    // =========================================================================

    fun saveClient() {
        val currentState = _uiState.value

        // Run full validation before attempting save
        val allErrors = validateAll(currentState)
        if (allErrors.any { it != null }) {
            _uiState.update {
                it.copy(
                    hasErrors = true,
                    companyNameError = validateCompanyName(it.companyName)
                )
            }
            return
        }

        if (!currentState.canSave) {
            _uiState.update {
                it.copy(error = UiText.StringResource(R.string.client_form_error_fields_required))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val client = buildClientFromState(currentState)

            val result = if (currentState.isEditMode && currentState.clientId != null) {
                updateClientUseCase(client)
            } else {
                createClientUseCase(client)
            }

            when (result) {
                is QrResult.Success -> {
                    Timber.d("Client saved: ${client.id} ${client.companyName}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveCompleted = true,
                            isDirty = false,
                            savedClientId = client.id,
                            savedClientName = client.companyName,
                            error = null
                        )
                    }
                }
                is QrResult.Error -> {
                    Timber.e("Failed to save client: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.error.asUiText()
                        )
                    }
                }
            }
        }
    }

    private fun buildClientFromState(state: ClientFormUiState): Client {
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
        return Client(
            id = state.clientId ?: UUID.randomUUID().toString(),
            companyName = state.companyName.trim(),
            notes = state.notes.takeIf { it.isNotBlank() },
            headquarters = address,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
    }

    // =========================================================================
    // ERROR HANDLING / UTILITY
    // =========================================================================

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false) }
    }
}