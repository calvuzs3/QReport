package net.calvuz.qreport.checkup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.checkup.domain.model.CheckUpHeader
import net.calvuz.qreport.checkup.domain.usecase.CreateCheckUpUseCase
import net.calvuz.qreport.client.client.domain.model.ClientInfo
import net.calvuz.qreport.client.island.domain.model.IslandInfo
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import net.calvuz.qreport.app.error.domain.model.QrError
import timber.log.Timber
import javax.inject.Inject

data class NewCheckUpUiState(
    // Client info
    val clientName: String = "",
    val contactPerson: String = "",
    val site: String = "",

    // Island selection
    val selectedIslandType: IslandType? = null,

    // Island info
    val serialNumber: String = "",
    val model: String = "",

    // State
    val isCreating: Boolean = false,
    val error: QrError.Checkup? = null,
    val createdCheckUpId: String? = null
) {
    val canCreate: Boolean
        get() = clientName.isNotBlank() && selectedIslandType != null
}

@HiltViewModel
class NewCheckUpViewModel @Inject constructor(
    private val createCheckUpUseCase: CreateCheckUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewCheckUpUiState())
    val uiState: StateFlow<NewCheckUpUiState> = _uiState.asStateFlow()

    init {
        Timber.d("NewCheckUpViewModel initialized")
    }

    // ============================================================
    // CLIENT INFO UPDATES
    // ============================================================

    fun updateClientName(name: String) {
        _uiState.value = _uiState.value.copy(clientName = name)
    }

    fun updateContactPerson(person: String) {
        _uiState.value = _uiState.value.copy(contactPerson = person)
    }

    fun updateSite(site: String) {
        _uiState.value = _uiState.value.copy(site = site)
    }

    // ============================================================
    // ISLAND SELECTION
    // ============================================================

    fun selectIslandType(islandType: IslandType) {
        _uiState.value = _uiState.value.copy(selectedIslandType = islandType)
    }

    // ============================================================
    // ISLAND INFO UPDATES
    // ============================================================

    fun updateSerialNumber(serialNumber: String) {
        _uiState.value = _uiState.value.copy(serialNumber = serialNumber)
    }

    fun updateModel(model: String) {
        _uiState.value = _uiState.value.copy(model = model)
    }

    // ============================================================
    // CHECK-UP CREATION
    // ============================================================

    fun createCheckUp() {
        val currentState = _uiState.value

        if (!currentState.canCreate) {
            _uiState.value = currentState.copy(
                error = (QrError.Checkup.FIELDS_REQUIRED) //"Compilare tutti i campi obbligatori"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isCreating = true,
                error = null
            )

            try {
                val header = createCheckUpHeader(currentState)
                val islandType = currentState.selectedIslandType!!

                Timber.d("Creating check-up for client: ${currentState.clientName}, island: $islandType")

                // Use the simpler CreateCheckUpUseCase with templates
                val result = createCheckUpUseCase(
                    header = header,
                    islandType = islandType,
                    includeTemplateItems = true
                )

                result.fold(
                    onSuccess = { checkUpId ->
                        Timber.d("Check-up created successfully: $checkUpId")
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            createdCheckUpId = checkUpId,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to create check-up")
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            error = QrError.Checkup.CREATE  // "Errore creazione check-up: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception during check-up creation")
                _uiState.value = currentState.copy(
                    isCreating = false,
                    error = QrError.Checkup.UNKNOWN // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    // ============================================================
    // ERROR HANDLING
    // ============================================================

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private fun createCheckUpHeader(state: NewCheckUpUiState): CheckUpHeader {
        return CheckUpHeader(
            clientInfo = ClientInfo(
                companyName = state.clientName,
                contactPerson = state.contactPerson,
                site = state.site,
                address = "",
                phone = "",
                email = ""
            ),
            islandInfo = IslandInfo(
                serialNumber = state.serialNumber,
                model = state.model,
                installationDate = "",
                lastMaintenanceDate = "",
                operatingHours = 0,
                cycleCount = 0L
            ),
            technicianInfo = TechnicianInfo(
                name = "",
                company = "",
                certification = "",
                phone = "",
                email = ""
            ),
            checkUpDate = Clock.System.now(),
            notes = ""
        )
    }
}