package net.calvuz.qreport.checkup.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.checkup.domain.model.CheckUpHeader
import net.calvuz.qreport.checkup.domain.usecase.AssociateCheckUpToIslandUseCase
import net.calvuz.qreport.checkup.domain.usecase.CreateCheckUpUseCase
import net.calvuz.qreport.checkup.domain.model.ClientInfo
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandInfo
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import net.calvuz.qreport.app.error.domain.model.QrError
import timber.log.Timber
import javax.inject.Inject

data class NewCheckUpUiState(
    // Client info
    val clientName: String = "",
    val contactPerson: String = "",
    val site: String = "",
    val address: String = "",

    // Island selection
    val selectedIslandType: IslandType? = null,

    // Island info
    val serialNumber: String = "",
    val model: String = "",
    val installationDate: String = "",
    val lastMaintenanceDate: String = "",
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L,

    // Linked source (Client -> Facility -> Island)
    val showSourceSelectionDialog: Boolean = false,
    val isLoadingSelection: Boolean = false,
    val availableClients: List<Client> = emptyList(),
    val availableFacilities: List<Facility> = emptyList(),
    val availableIslands: List<Island> = emptyList(),
    val selectedClientId: String? = null,
    val selectedFacilityId: String? = null,
    val selectedIslandId: String? = null,

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
    private val createCheckUpUseCase: CreateCheckUpUseCase,
    private val associateCheckUpToIslandUseCase: AssociateCheckUpToIslandUseCase,
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val islandRepository: IslandRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewCheckUpUiState())
    val uiState: StateFlow<NewCheckUpUiState> = _uiState.asStateFlow()

    init {
        Timber.d("NewCheckUpViewModel initialized")

        val islandId = savedStateHandle.get<String>("islandId")
        if (!islandId.isNullOrBlank()) {
            preloadFromIsland(islandId)
        }
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
    // SOURCE SELECTION (Cliente -> Stabilimento -> Isola)
    // ============================================================

    fun openSourceSelectionDialog() {
        _uiState.value = _uiState.value.copy(showSourceSelectionDialog = true)

        if (_uiState.value.availableClients.isEmpty()) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingSelection = true)
                val clients = clientRepository.getActiveClients().getOrElse { emptyList() }
                _uiState.value = _uiState.value.copy(
                    availableClients = clients,
                    isLoadingSelection = false
                )
            }
        }
    }

    fun dismissSourceSelectionDialog() {
        _uiState.value = _uiState.value.copy(showSourceSelectionDialog = false)
    }

    fun onClientSelectedForSource(clientId: String) {
        _uiState.value = _uiState.value.copy(
            selectedClientId = clientId,
            selectedFacilityId = null,
            availableFacilities = emptyList(),
            availableIslands = emptyList()
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSelection = true)
            val facilities = facilityRepository.getActiveFacilitiesByClient(clientId).getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(
                availableFacilities = facilities,
                isLoadingSelection = false
            )
        }
    }

    fun onFacilitySelectedForSource(facilityId: String) {
        _uiState.value = _uiState.value.copy(
            selectedFacilityId = facilityId,
            availableIslands = emptyList()
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSelection = true)
            val islands = islandRepository.getActiveIslandsByFacility(facilityId).getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(
                availableIslands = islands,
                isLoadingSelection = false
            )
        }
    }

    fun onIslandSelectedForSource(islandId: String) {
        val state = _uiState.value
        val client = state.availableClients.find { it.id == state.selectedClientId }
        val facility = state.availableFacilities.find { it.id == state.selectedFacilityId }
        val island = state.availableIslands.find { it.id == islandId }

        if (client != null && facility != null && island != null) {
            prefillFromSelection(client, facility, island)
        }

        _uiState.value = _uiState.value.copy(showSourceSelectionDialog = false)
    }

    fun clearLinkedSource() {
        _uiState.value = _uiState.value.copy(
            selectedClientId = null,
            selectedFacilityId = null,
            selectedIslandId = null
        )
    }

    private fun preloadFromIsland(islandId: String) {
        viewModelScope.launch {
            val island = islandRepository.getIslandById(islandId).getOrNull() ?: return@launch
            val facility = facilityRepository.getFacilityById(island.facilityId).getOrNull() ?: return@launch
            val client = clientRepository.getClientById(facility.clientId).getOrNull() ?: return@launch

            prefillFromSelection(client, facility, island)
            _uiState.value = _uiState.value.copy(
                selectedClientId = client.id,
                selectedFacilityId = facility.id
            )
        }
    }

    private fun prefillFromSelection(client: Client, facility: Facility, island: Island) {
        _uiState.value = _uiState.value.copy(
            clientName = client.companyName,
            site = facility.name,
            address = facility.address?.toDisplayString() ?: "",
            selectedIslandType = island.islandType,
            serialNumber = island.serialNumber,
            model = island.modelNumber ?: "",
            installationDate = island.installationDate?.toItalianDate() ?: "",
            lastMaintenanceDate = island.lastMaintenanceDate?.toItalianDate() ?: "",
            operatingHours = island.operatingHours,
            cycleCount = island.cycleCount,
            selectedIslandId = island.id
        )
    }

    // ============================================================
    // CHECK-UP CREATION
    // ============================================================

    fun createCheckUp() {
        val currentState = _uiState.value

        if (!currentState.canCreate) {
            _uiState.value = currentState.copy(
                error = (QrError.Checkup.FieldsRequired()) //"Compilare tutti i campi obbligatori"
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

                        currentState.selectedIslandId?.let { islandId ->
                            associateCheckUpToIslandUseCase(checkUpId, islandId)
                                .onFailure { Timber.w(it, "Failed to associate check-up $checkUpId with island $islandId") }
                        }

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
                            error = QrError.Checkup.Create()  // "Errore creazione check-up: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception during check-up creation")
                _uiState.value = currentState.copy(
                    isCreating = false,
                    error = QrError.Checkup.Unknown() // "Errore imprevisto: ${e.message}"
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
                address = state.address,
                phone = "",
                email = ""
            ),
            islandInfo = IslandInfo(
                serialNumber = state.serialNumber,
                model = state.model,
                installationDate = state.installationDate,
                lastMaintenanceDate = state.lastMaintenanceDate,
                operatingHours = state.operatingHours,
                cycleCount = state.cycleCount
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
