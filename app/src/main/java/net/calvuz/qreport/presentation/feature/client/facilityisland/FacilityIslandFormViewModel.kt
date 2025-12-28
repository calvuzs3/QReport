package net.calvuz.qreport.presentation.feature.client.facilityisland

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.usecase.client.facilityisland.CreateFacilityIslandUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.UpdateFacilityIslandUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.GetFacilityIslandByIdUseCase
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel per FacilityIslandFormScreen
 *
 * Gestisce:
 * - Create/Edit mode per isole
 * - Validazioni campi obbligatori
 * - Date validation per installazione/garanzia/manutenzione
 * - Serial number uniqueness check
 * - Save con error handling
 */
@HiltViewModel
class FacilityIslandFormViewModel @Inject constructor(
    private val createIslandUseCase: CreateFacilityIslandUseCase,
    private val updateIslandUseCase: UpdateFacilityIslandUseCase,
    private val getIslandByIdUseCase: GetFacilityIslandByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacilityIslandFormUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Inizializza form per creazione o modifica
     */
    fun initialize(facilityId: String, islandId: String? = null) {
        _uiState.update { it.copy(facilityId = facilityId, isEditMode = islandId != null) }

        islandId?.let { id ->
            loadIslandForEdit(id)
        }
    }

    /**
     * Carica isola esistente per modifica
     */
    private fun loadIslandForEdit(islandId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getIslandByIdUseCase(islandId).fold(
                onSuccess = { island ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            islandId = island.id,
                            serialNumber = island.serialNumber,
                            islandType = island.islandType,
                            model = island.model ?: "",
                            customName = island.customName ?: "",
                            location = island.location ?: "",
                            installationDate = island.installationDate,
                            warrantyExpiration = island.warrantyExpiration,
                            operatingHours = island.operatingHours.toString(),
                            cycleCount = island.cycleCount.toString(),
                            lastMaintenanceDate = island.lastMaintenanceDate,
                            nextScheduledMaintenance = island.nextScheduledMaintenance,
                            isActive = island.isActive,
                            notes = island.notes ?: ""
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Errore nel caricamento isola"
                        )
                    }
                }
            )
        }
    }

    /**
     * Gestisce eventi form
     */
    fun onFormEvent(event: FacilityIslandFormEvent) {
        when (event) {
            is FacilityIslandFormEvent.SerialNumberChanged -> updateSerialNumber(event.serialNumber)
            is FacilityIslandFormEvent.IslandTypeChanged -> updateIslandType(event.islandType)
            is FacilityIslandFormEvent.ModelChanged -> updateModel(event.model)
            is FacilityIslandFormEvent.CustomNameChanged -> updateCustomName(event.customName)
            is FacilityIslandFormEvent.LocationChanged -> updateLocation(event.location)
            is FacilityIslandFormEvent.InstallationDateChanged -> updateInstallationDate(event.date)
            is FacilityIslandFormEvent.WarrantyExpirationChanged -> updateWarrantyExpiration(event.date)
            is FacilityIslandFormEvent.OperatingHoursChanged -> updateOperatingHours(event.hours)
            is FacilityIslandFormEvent.CycleCountChanged -> updateCycleCount(event.count)
            is FacilityIslandFormEvent.LastMaintenanceDateChanged -> updateLastMaintenanceDate(event.date)
            is FacilityIslandFormEvent.NextMaintenanceChanged -> updateNextMaintenance(event.date)
            is FacilityIslandFormEvent.IsActiveChanged -> updateIsActive(event.isActive)
            is FacilityIslandFormEvent.NotesChanged -> updateNotes(event.notes)
        }
    }

    private fun updateSerialNumber(serialNumber: String) {
        _uiState.update {
            it.copy(
                serialNumber = serialNumber,
                serialNumberError = validateSerialNumber(serialNumber)
            )
        }
    }

    private fun updateIslandType(islandType: IslandType) {
        _uiState.update { it.copy(islandType = islandType) }
    }

    private fun updateModel(model: String) {
        _uiState.update {
            it.copy(
                model = model,
                modelError = validateModel(model)
            )
        }
    }

    private fun updateCustomName(customName: String) {
        _uiState.update {
            it.copy(
                customName = customName,
                customNameError = validateCustomName(customName)
            )
        }
    }

    private fun updateLocation(location: String) {
        _uiState.update {
            it.copy(
                location = location,
                locationError = validateLocation(location)
            )
        }
    }

    private fun updateInstallationDate(date: Instant?) {
        _uiState.update { currentState ->
            currentState.copy(
                installationDate = date,
                installationDateError = validateInstallationDate(date, currentState.warrantyExpiration)
            )
        }
    }

    private fun updateWarrantyExpiration(date: Instant?) {
        _uiState.update { currentState ->
            currentState.copy(
                warrantyExpiration = date,
                warrantyExpirationError = validateWarrantyExpiration(date, currentState.installationDate)
            )
        }
    }

    private fun updateOperatingHours(hours: String) {
        _uiState.update {
            it.copy(
                operatingHours = hours,
                operatingHoursError = validateOperatingHours(hours)
            )
        }
    }

    private fun updateCycleCount(count: String) {
        _uiState.update {
            it.copy(
                cycleCount = count,
                cycleCountError = validateCycleCount(count)
            )
        }
    }

    private fun updateLastMaintenanceDate(date: Instant?) {
        _uiState.update { currentState ->
            currentState.copy(
                lastMaintenanceDate = date,
                lastMaintenanceDateError = validateLastMaintenanceDate(
                    date,
                    currentState.installationDate,
                    currentState.nextScheduledMaintenance
                )
            )
        }
    }

    private fun updateNextMaintenance(date: Instant?) {
        _uiState.update { currentState ->
            currentState.copy(
                nextScheduledMaintenance = date,
                nextMaintenanceError = validateNextMaintenance(date, currentState.lastMaintenanceDate)
            )
        }
    }

    private fun updateIsActive(isActive: Boolean) {
        _uiState.update { it.copy(isActive = isActive) }
    }

    private fun updateNotes(notes: String) {
        _uiState.update {
            it.copy(
                notes = notes,
                notesError = validateNotes(notes)
            )
        }
    }

    /**
     * Salva isola (create o update)
     */
    fun saveIsland() {
        val currentState = _uiState.value

        // Validazione completa
        val allErrors = validateAllFields(currentState)
        if (allErrors.any { it.isNotBlank() }) {
            _uiState.update { currentState.copy(hasValidationErrors = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val island = createIslandFromState(currentState)

                val result = if (currentState.isEditMode) {
                    updateIslandUseCase(island)
                } else {
                    createIslandUseCase(island)
                }

                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                savedIslandId = if (currentState.isEditMode) currentState.islandId else UUID.randomUUID().toString()
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Errore nel salvataggio"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Errore imprevisto: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Crea oggetto FacilityIsland dal form state
     */
    private fun createIslandFromState(state: FacilityIslandFormUiState): FacilityIsland {
        val now = Clock.System.now()

        return FacilityIsland(
            id = state.islandId ?: UUID.randomUUID().toString(),
            facilityId = state.facilityId,
            islandType = state.islandType,
            serialNumber = state.serialNumber.trim(),
            model = state.model.trim().takeIf { it.isNotBlank() },
            installationDate = state.installationDate,
            warrantyExpiration = state.warrantyExpiration,
            isActive = state.isActive,
            operatingHours = state.operatingHours.toIntOrNull() ?: 0,
            cycleCount = state.cycleCount.toLongOrNull() ?: 0L,
            lastMaintenanceDate = state.lastMaintenanceDate,
            nextScheduledMaintenance = state.nextScheduledMaintenance,
            customName = state.customName.trim().takeIf { it.isNotBlank() },
            location = state.location.trim().takeIf { it.isNotBlank() },
            notes = state.notes.trim().takeIf { it.isNotBlank() },
            createdAt = if (state.isEditMode) state.createdAt ?: now else now,
            updatedAt = now
        )
    }

    /**
     * Validazioni individuali
     */
    private fun validateSerialNumber(serialNumber: String): String {
        return when {
            serialNumber.isBlank() -> "Serial number è obbligatorio"
            serialNumber.length < 3 -> "Serial number deve essere di almeno 3 caratteri"
            serialNumber.length > 50 -> "Serial number troppo lungo (max 50 caratteri)"
            !serialNumber.matches("[A-Za-z0-9\\-_]+".toRegex()) -> "Solo lettere, numeri, trattini e underscore"
            else -> ""
        }
    }

    private fun validateModel(model: String): String {
        return when {
            model.length > 100 -> "Modello troppo lungo (max 100 caratteri)"
            else -> ""
        }
    }

    private fun validateCustomName(customName: String): String {
        return when {
            customName.length > 100 -> "Nome personalizzato troppo lungo (max 100 caratteri)"
            else -> ""
        }
    }

    private fun validateLocation(location: String): String {
        return when {
            location.length > 200 -> "Ubicazione troppo lunga (max 200 caratteri)"
            else -> ""
        }
    }

    private fun validateInstallationDate(installationDate: Instant?, warrantyExpiration: Instant?): String {
        val now = Clock.System.now()
        return when {
            installationDate?.let { it > now } == true -> "Data installazione non può essere nel futuro"
            warrantyExpiration != null && installationDate != null && warrantyExpiration < installationDate ->
                "Scadenza garanzia non può essere precedente all'installazione"
            else -> ""
        }
    }

    private fun validateWarrantyExpiration(warrantyExpiration: Instant?, installationDate: Instant?): String {
        return when {
            installationDate != null && warrantyExpiration != null && warrantyExpiration < installationDate ->
                "Scadenza garanzia non può essere precedente all'installazione"
            else -> ""
        }
    }

    private fun validateOperatingHours(hours: String): String {
        return when {
            hours.isNotBlank() && hours.toIntOrNull() == null -> "Inserire un numero valido"
            hours.toIntOrNull()?.let { it < 0 } == true -> "Le ore non possono essere negative"
            else -> ""
        }
    }

    private fun validateCycleCount(count: String): String {
        return when {
            count.isNotBlank() && count.toLongOrNull() == null -> "Inserire un numero valido"
            count.toLongOrNull()?.let { it < 0 } == true -> "Il conteggio non può essere negativo"
            else -> ""
        }
    }

    private fun validateLastMaintenanceDate(
        lastMaintenance: Instant?,
        installation: Instant?,
        nextMaintenance: Instant?
    ): String {
        val now = Clock.System.now()
        return when {
            lastMaintenance?.let { it > now } == true -> "Ultima manutenzione non può essere nel futuro"
            installation != null && lastMaintenance != null && lastMaintenance < installation ->
                "Ultima manutenzione non può essere precedente all'installazione"
            nextMaintenance != null && lastMaintenance != null && nextMaintenance <= lastMaintenance ->
                "Prossima manutenzione deve essere successiva all'ultima"
            else -> ""
        }
    }

    private fun validateNextMaintenance(nextMaintenance: Instant?, lastMaintenance: Instant?): String {
        return when {
            lastMaintenance != null && nextMaintenance != null && nextMaintenance <= lastMaintenance ->
                "Prossima manutenzione deve essere successiva all'ultima"
            else -> ""
        }
    }

    private fun validateNotes(notes: String): String {
        return when {
            notes.length > 1000 -> "Note troppo lunghe (max 1000 caratteri)"
            else -> ""
        }
    }

    /**
     * Validazione completa di tutti i campi
     */
    private fun validateAllFields(state: FacilityIslandFormUiState): List<String> {
        return listOf(
            validateSerialNumber(state.serialNumber),
            validateModel(state.model),
            validateCustomName(state.customName),
            validateLocation(state.location),
            validateInstallationDate(state.installationDate, state.warrantyExpiration),
            validateWarrantyExpiration(state.warrantyExpiration, state.installationDate),
            validateOperatingHours(state.operatingHours),
            validateCycleCount(state.cycleCount),
            validateLastMaintenanceDate(state.lastMaintenanceDate, state.installationDate, state.nextScheduledMaintenance),
            validateNextMaintenance(state.nextScheduledMaintenance, state.lastMaintenanceDate),
            validateNotes(state.notes)
        )
    }

    /**
     * Dismisses current error
     */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State per form isola
 */
data class FacilityIslandFormUiState(
    // Metadati
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val facilityId: String = "",
    val islandId: String? = null,
    val createdAt: Instant? = null,

    // Campi form
    val serialNumber: String = "",
    val islandType: IslandType = IslandType.POLY_MOVE,
    val model: String = "",
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

    // Errori validazione
    val serialNumberError: String = "",
    val modelError: String = "",
    val customNameError: String = "",
    val locationError: String = "",
    val installationDateError: String = "",
    val warrantyExpirationError: String = "",
    val operatingHoursError: String = "",
    val cycleCountError: String = "",
    val lastMaintenanceDateError: String = "",
    val nextMaintenanceError: String = "",
    val notesError: String = "",

    // Stati
    val hasValidationErrors: Boolean = false,
    val savedIslandId: String? = null,
    val error: String? = null
) {
    /**
     * Form è valido per il salvataggio
     */
    val isFormValid: Boolean
        get() = serialNumber.isNotBlank() &&
                serialNumberError.isBlank() &&
                modelError.isBlank() &&
                customNameError.isBlank() &&
                locationError.isBlank() &&
                installationDateError.isBlank() &&
                warrantyExpirationError.isBlank() &&
                operatingHoursError.isBlank() &&
                cycleCountError.isBlank() &&
                lastMaintenanceDateError.isBlank() &&
                nextMaintenanceError.isBlank() &&
                notesError.isBlank()
}

/**
 * Eventi form
 */
sealed class FacilityIslandFormEvent {
    data class SerialNumberChanged(val serialNumber: String) : FacilityIslandFormEvent()
    data class IslandTypeChanged(val islandType: IslandType) : FacilityIslandFormEvent()
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
}