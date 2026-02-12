package net.calvuz.qreport.client.client.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.usecase.CreateClientUseCase
import net.calvuz.qreport.client.client.domain.usecase.GetClientByIdUseCase
import net.calvuz.qreport.client.client.domain.usecase.UpdateClientUseCase
import timber.log.Timber
import java.net.URL
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel per la creazione/modifica di un cliente
 *
 * Gestisce:
 * - ValidationError validation real-time
 * - State management
 * - Client creation/update
 * - Error handling
 */

data class ClientFormUiState(
    // Company data
    val companyName: String = "",
    val vatNumber: String = "",
    val fiscalCode: String = "",
    val industry: String = "",
    val website: String = "",
    val notes: String = "",

    // Address data
    val street: String = "",
    val streetNumber: String = "",
    val city: String = "",
    val province: String = "",
    val region: String = "",
    val postalCode: String = "",
    val country: String = "",

    // State
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveCompleted: Boolean = false,
    val savedClientId: String? = null,
    val savedClientName: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),

    // Edit mode
    val clientId: String? = null,
    val isEditMode: Boolean = false
) {
    val canSave: Boolean
        get() = companyName.isNotBlank() &&
                fieldErrors.isEmpty() &&
                !isSaving &&
                !isLoading

    val hasAddressData: Boolean
        get() = street.isNotBlank() || city.isNotBlank()
}

@HiltViewModel
class ClientFormViewModel @Inject constructor(
    private val createClientUseCase: CreateClientUseCase,
    private val updateClientUseCase: UpdateClientUseCase,
    private val getClientByIdUseCase: GetClientByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientFormUiState())
    val uiState: StateFlow<ClientFormUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ClientFormViewModel initialized")
    }

    // ============================================================
    // EDIT MODE LOADING
    // ============================================================

    fun loadClientForEdit(clientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                clientId = clientId,
                isEditMode = true
            )

            try {
                getClientByIdUseCase(clientId).fold(
                    onSuccess = { client ->
                        Timber.d("Client loaded for edit: ${client.companyName}")
                        populateFormFromClient(client)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load client for edit")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Errore caricamento cliente: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception loading client for edit")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    private fun populateFormFromClient(client: Client) {
        val address = client.headquarters

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            companyName = client.companyName,
            vatNumber = client.vatNumber ?: "",
            fiscalCode = client.fiscalCode ?: "",
            industry = client.industry ?: "",
            website = client.website ?: "",
            notes = client.notes ?: "",
            street = address?.street ?: "",
            streetNumber = address?.streetNumber ?: "",
            city = address?.city ?: "",
            province = address?.province ?: "",
            region = address?.region ?: "",
            postalCode = address?.postalCode ?: ""
        )
    }

    // ============================================================
    // COMPANY DATA UPDATES
    // ============================================================

    fun updateCompanyName(name: String) {
        _uiState.value = _uiState.value.copy(companyName = name)
        validateCompanyName(name)
    }

    fun updateVatNumber(vatNumber: String) {
        val cleaned = vatNumber.replace("\\s+".toRegex(), "")
        _uiState.value = _uiState.value.copy(vatNumber = cleaned)
        validateVatNumber(cleaned)
    }

    fun updateFiscalCode(fiscalCode: String) {
        val cleaned = fiscalCode.replace("\\s+".toRegex(), "").uppercase()
        _uiState.value = _uiState.value.copy(fiscalCode = cleaned)
        validateFiscalCode(cleaned)
    }

    fun updateIndustry(industry: String) {
        _uiState.value = _uiState.value.copy(industry = industry)
        validateIndustry(industry)
    }

    fun updateWebsite(website: String) {
        _uiState.value = _uiState.value.copy(website = website)
        validateWebsite(website)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    // ============================================================
    // ADDRESS UPDATES
    // ============================================================

    fun updateStreet(street: String) {
        _uiState.value = _uiState.value.copy(street = street)
    }

    fun updateStreetNumber(streetNumber: String) {
        _uiState.value = _uiState.value.copy(streetNumber = streetNumber)
    }

    fun updateCity(city: String) {
        _uiState.value = _uiState.value.copy(city = city)
    }

    fun updateProvince(province: String) {
        _uiState.value = _uiState.value.copy(province = province.uppercase())
    }

    fun updateRegion(region: String) {
        _uiState.value = _uiState.value.copy(region = region)
    }

    fun updatePostalCode(postalCode: String) {
        val cleaned = postalCode.replace("\\s+".toRegex(), "")
        _uiState.value = _uiState.value.copy(postalCode = cleaned)
    }

    fun updateCountry(country: String) {
        _uiState.value = _uiState.value.copy(country = country)
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    private fun validateCompanyName(name: String) {
        val errors = _uiState.value.fieldErrors.toMutableMap()

        when {
            name.isBlank() -> errors["companyName"] = "Ragione sociale è obbligatoria"
            name.length < 2 -> errors["companyName"] = "Minimo 2 caratteri"
            name.length > 255 -> errors["companyName"] = "Massimo 255 caratteri"
            else -> errors.remove("companyName")
        }

        _uiState.value = _uiState.value.copy(fieldErrors = errors)
    }

    private fun validateVatNumber(vatNumber: String) {
        val errors = _uiState.value.fieldErrors.toMutableMap()

        if (vatNumber.isNotBlank() && !vatNumber.matches("\\d{11}".toRegex())) {
            errors["vatNumber"] = "Formato non valido (11 cifre)"
        } else {
            errors.remove("vatNumber")
        }

        _uiState.value = _uiState.value.copy(fieldErrors = errors)
    }

    private fun validateFiscalCode(fiscalCode: String) {
        val errors = _uiState.value.fieldErrors.toMutableMap()

        if (fiscalCode.isNotBlank() && !fiscalCode.matches("[A-Z0-9]{16}".toRegex())) {
            errors["fiscalCode"] = "Formato non valido (16 caratteri)"
        } else {
            errors.remove("fiscalCode")
        }

        _uiState.value = _uiState.value.copy(fieldErrors = errors)
    }

    private fun validateIndustry(industry: String) {
        val errors = _uiState.value.fieldErrors.toMutableMap()

        if (industry.length > 100) {
            errors["industry"] = "Massimo 100 caratteri"
        } else {
            errors.remove("industry")
        }

        _uiState.value = _uiState.value.copy(fieldErrors = errors)
    }

    private fun validateWebsite(website: String) {
        val errors = _uiState.value.fieldErrors.toMutableMap()

        if (website.isNotBlank() && !isValidWebsite(website)) {
            errors["website"] = "Formato website non valido"
        } else {
            errors.remove("website")
        }

        _uiState.value = _uiState.value.copy(fieldErrors = errors)
    }

    private fun isValidWebsite(website: String): Boolean {
        return try {
            val cleanWebsite = if (!website.startsWith("http")) "https://$website" else website
            URL(cleanWebsite)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ============================================================
    // CLIENT SAVE
    // ============================================================

    fun saveClient() {
        val currentState = _uiState.value

        if (!currentState.canSave) {
            _uiState.value = currentState.copy(
                error = "Compilare tutti i campi obbligatori"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isSaving = true,
                error = null
            )

            try {
                val client = buildClientFromForm(currentState)

                val result = if (currentState.isEditMode && currentState.clientId != null) {
                    updateClientUseCase(client)
                } else {
                    createClientUseCase(client)
                }

                result.fold(
                    onSuccess = {
                        Timber.d("Client saved successfully: ${client.id} ${client.companyName }}")
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveCompleted = true,
                            savedClientId = client.id,
                            savedClientName = client.companyName,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to save client")
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = "Errore salvataggio: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception during client save")
                _uiState.value = currentState.copy(
                    isSaving = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    private suspend fun createNewClient(client: Client) {
        createClientUseCase(client).fold(
            onSuccess = {
                Timber.d("Client created successfully: ${client.id}")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedClientId = client.id,
                    savedClientName = client.companyName,
                    error = null
                )
            },
            onFailure = { error ->
                Timber.e(error, "Failed to create client")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Errore durante la creazione"
                )
            }
        )
    }

    private suspend fun updateClient(client: Client) {
        updateClientUseCase(client).fold(
            onSuccess = {
                Timber.d("Client updated successfully: ${client.id}")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedClientId = client.id,
                    savedClientName = client.companyName,
                    error = null
                )
            },
            onFailure = { error ->
                Timber.e(error, "Failed to update client")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Errore durante l'aggiornamento"
                )
            }
        )
    }

    private fun buildClientFromForm(state: ClientFormUiState): Client {
        val address = if (state.hasAddressData) {
            Address(
                street = state.street.takeIf { it.isNotBlank() },
                streetNumber = state.streetNumber.takeIf { it.isNotBlank() },
                city = state.city.takeIf { it.isNotBlank() },
                province = state.province.takeIf { it.isNotBlank() },
                region = state.region.takeIf { it.isNotBlank() },
                postalCode = state.postalCode.takeIf { it.isNotBlank() },
                country = "Italia"
            )
        } else null

        val now = Clock.System.now()

        return Client(
            id = state.clientId ?: UUID.randomUUID().toString(),
            companyName = state.companyName.trim(),
            vatNumber = state.vatNumber.takeIf { it.isNotBlank() },
            fiscalCode = state.fiscalCode.takeIf { it.isNotBlank() },
            industry = state.industry.takeIf { it.isNotBlank() },
            website = state.website.takeIf { it.isNotBlank() },
            notes = state.notes.takeIf { it.isNotBlank() },
            headquarters = address,
            isActive = true,
            createdAt = now, // Will be preserved in update
            updatedAt = now
        )
    }

    // ============================================================
    // ERROR HANDLING
    // ============================================================

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSaveCompleted() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }
}