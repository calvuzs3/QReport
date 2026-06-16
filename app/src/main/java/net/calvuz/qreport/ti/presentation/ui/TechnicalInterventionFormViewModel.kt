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
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.domain.usecase.intervention.CreateTechnicalInterventionUseCase
import net.calvuz.qreport.ti.domain.model.WorkLocation
import net.calvuz.qreport.ti.domain.model.WorkLocationType
import net.calvuz.qreport.ti.domain.usecase.AssociateTiToIslandUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for TechnicalIntervention creation form screen
 */
@HiltViewModel
class TechnicalInterventionFormViewModel @Inject constructor(
    private val createInterventionUseCase: CreateTechnicalInterventionUseCase,
    private val associateTiToIslandUseCase: AssociateTiToIslandUseCase,
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val islandRepository: IslandRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(TechnicalInterventionFormState())
    val state: StateFlow<TechnicalInterventionFormState> = _state.asStateFlow()

    init {
        val islandId = savedStateHandle.get<String>("islandId")
        if (!islandId.isNullOrBlank()) {
            preloadFromIsland(islandId)
        }
    }

    // ===== CUSTOMER SECTION UPDATES =====

    fun updateCustomerName(value: String) {
        _state.value = _state.value.copy(customerName = value, errorMessage = null)
    }

    fun updateCustomerContact(value: String) {
        _state.value = _state.value.copy(customerContact = value)
    }

    fun updateTicketNumber(value: String) {
        _state.value = _state.value.copy(ticketNumber = value, errorMessage = null)
    }

    fun updateCustomerOrderNumber(value: String) {
        _state.value = _state.value.copy(customerOrderNumber = value, errorMessage = null)
    }

    fun updateNotes(value: String) {
        _state.value = _state.value.copy(notes = value)
    }

    // ===== ROBOT SECTION UPDATES =====

    fun updateSerialNumber(value: String) {
        _state.value = _state.value.copy(serialNumber = value, errorMessage = null)
    }

    fun updateHoursOfDuty(value: String) {
        _state.value = _state.value.copy(hoursOfDuty = value, errorMessage = null)
    }

    // ===== WORK LOCATION UPDATES =====

    fun updateWorkLocation(type: WorkLocationType) {
        _state.value = _state.value.copy(
            workLocation = type,
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

        _state.value = _state.value.copy(technicians = value, errorMessage = errorMessage?.toUiText())
    }

    // ===== SOURCE SELECTION =====

    fun openSourceSelectionDialog() {
        _state.value = _state.value.copy(showSourceSelectionDialog = true)

        if (_state.value.availableClients.isEmpty()) {
            viewModelScope.launch {
                _state.value = _state.value.copy(isLoadingSelection = true)
                val clients = clientRepository.getActiveClients().getOrElse { emptyList() }
                _state.value = _state.value.copy(availableClients = clients, isLoadingSelection = false)
            }
        }
    }

    fun dismissSourceSelectionDialog() {
        _state.value = _state.value.copy(showSourceSelectionDialog = false)
    }

    fun onClientSelectedForSource(clientId: String) {
        _state.value = _state.value.copy(
            selectedClientId = clientId,
            selectedFacilityId = null,
            availableFacilities = emptyList(),
            availableIslands = emptyList()
        )

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingSelection = true)
            val facilities = facilityRepository.getActiveFacilitiesByClient(clientId).getOrElse { emptyList() }
            _state.value = _state.value.copy(availableFacilities = facilities, isLoadingSelection = false)
        }
    }

    fun onFacilitySelectedForSource(facilityId: String) {
        _state.value = _state.value.copy(selectedFacilityId = facilityId, availableIslands = emptyList())

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingSelection = true)
            val islands = islandRepository.getActiveIslandsByFacility(facilityId).getOrElse { emptyList() }
            _state.value = _state.value.copy(availableIslands = islands, isLoadingSelection = false)
        }
    }

    fun onIslandSelectedForSource(islandId: String) {
        val state = _state.value
        val client = state.availableClients.find { it.id == state.selectedClientId }
        val island = state.availableIslands.find { it.id == islandId }

        if (client != null && island != null) {
            _state.value = _state.value.copy(
                linkedClientName = client.companyName,
                linkedIslandLabel = "${island.serialNumber}",
                selectedIslandId = island.id,
                showSourceSelectionDialog = false
            )
        } else {
            _state.value = _state.value.copy(showSourceSelectionDialog = false)
        }
    }

    fun clearLinkedSource() {
        _state.value = _state.value.copy(
            selectedClientId = null,
            selectedFacilityId = null,
            selectedIslandId = null,
            linkedClientName = null,
            linkedIslandLabel = null
        )
    }

    private fun preloadFromIsland(islandId: String) {
        viewModelScope.launch {
            val island = islandRepository.getIslandById(islandId).getOrNull() ?: return@launch
            val facility = facilityRepository.getFacilityById(island.facilityId).getOrNull() ?: return@launch
            val client = clientRepository.getClientById(facility.clientId).getOrNull() ?: return@launch

            _state.value = _state.value.copy(
                selectedClientId = client.id,
                selectedFacilityId = facility.id,
                selectedIslandId = island.id,
                linkedClientName = client.companyName,
                linkedIslandLabel = island.serialNumber,
                availableClients = listOf(client),
                availableFacilities = listOf(facility),
                availableIslands = listOf(island)
            )
        }
    }

    // ===== MAIN ACTIONS =====

    fun saveIntervention() {
        createNewIntervention()
    }

    private fun createNewIntervention() {
        val currentState = _state.value

        if (!currentState.canSave) {
            _state.value = currentState.copy(
                errorMessage = QrError.CreateInterventionError.CreationFailed().toUiText()
            )
            return
        }

        viewModelScope.launch {
            _state.value = currentState.copy(isLoading = true, errorMessage = null)

            val techniciansList = currentState.technicians
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(6)

            val workLocation = WorkLocation(
                type = currentState.workLocation,
                customLocation = if (currentState.workLocation == WorkLocationType.OTHER) {
                    currentState.customLocation
                } else ""
            )

            val result = if (currentState.isLinked) {
                createInterventionUseCase(
                    clientId = currentState.selectedClientId!!,
                    islandId = currentState.selectedIslandId!!,
                    ticketNumber = currentState.ticketNumber,
                    customerOrderNumber = currentState.customerOrderNumber,
                    workLocation = workLocation,
                    technicians = techniciansList
                )
            } else {
                val hoursOfDutyInt = currentState.hoursOfDuty.toIntOrNull() ?: 0
                createInterventionUseCase.createWithManualData(
                    customerName = currentState.customerName,
                    serialNumber = currentState.serialNumber,
                    hoursOfDuty = hoursOfDutyInt,
                    ticketNumber = currentState.ticketNumber,
                    customerOrderNumber = currentState.customerOrderNumber,
                    customerContact = currentState.customerContact,
                    workLocation = workLocation,
                    technicians = techniciansList
                )
            }

            when (result) {
                is QrResult.Success -> {
                    val interventionId = result.data
                    if (currentState.isLinked && currentState.selectedIslandId != null) {
                        associateTiToIslandUseCase(interventionId, currentState.selectedIslandId)
                            .onFailure { Timber.w(it, "Failed to link TI $interventionId to island ${currentState.selectedIslandId}") }
                    }
                    _state.value = currentState.copy(
                        isLoading = false,
                        isSuccess = true,
                        savedInterventionId = interventionId
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

    // ===== LINKED SOURCE =====
    val showSourceSelectionDialog: Boolean = false,
    val isLoadingSelection: Boolean = false,
    val availableClients: List<Client> = emptyList(),
    val availableFacilities: List<Facility> = emptyList(),
    val availableIslands: List<Island> = emptyList(),
    val selectedClientId: String? = null,
    val selectedFacilityId: String? = null,
    val selectedIslandId: String? = null,
    val linkedClientName: String? = null,
    val linkedIslandLabel: String? = null,

    // ===== UI STATE =====
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: UiText? = null,
    val savedInterventionId: String? = null
) {

    val isLinked: Boolean get() = selectedClientId != null && selectedIslandId != null

    val canSave: Boolean
        get() = ticketNumber.isNotBlank() &&
                customerOrderNumber.isNotBlank() &&
                (workLocation != WorkLocationType.OTHER || customLocation.isNotBlank()) &&
                techniciansList.size <= 6 &&
                !isLoading &&
                if (isLinked) true
                else customerName.isNotBlank() &&
                        serialNumber.isNotBlank() &&
                        hoursOfDuty.isNotBlank() &&
                        hoursOfDuty.toIntOrNull() != null

    private val techniciansList: List<String>
        get() = technicians.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val hasUnsavedData: Boolean
        get() = customerName.isNotBlank() ||
                customerContact.isNotBlank() ||
                ticketNumber.isNotBlank() ||
                customerOrderNumber.isNotBlank() ||
                notes.isNotBlank() ||
                serialNumber.isNotBlank() ||
                hoursOfDuty.isNotBlank() ||
                workLocation != WorkLocationType.CLIENT_SITE ||
                customLocation.isNotBlank() ||
                technicians.isNotBlank() ||
                isLinked
}
