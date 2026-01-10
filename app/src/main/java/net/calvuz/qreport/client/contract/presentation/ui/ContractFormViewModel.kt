package net.calvuz.qreport.client.contract.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.UiText.StringResources
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.usecase.CreateContractUseCase
import net.calvuz.qreport.client.contract.domain.usecase.GetContractByIdUseCase
import net.calvuz.qreport.client.contract.domain.usecase.UpdateContractUseCase
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class ContractFormUiState(

    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isEditMode: Boolean = false,
    val originalContractId: String? = null,
    val clientId: String = "",

    // ===== FORM FIELDS =====
    val name: String = "",
    val description: String = "",
    val title: String = "",
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val hasPriority: Boolean = true,           // rensponse priority (48h)
    val hasRemoteAssistance: Boolean = true,   // remote assistance (24h)
    val hasMaintenance: Boolean = true,        // island|rob maintenance
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,

    // ===== VALIDATION STATES =====
    val errors: UiText? = null,

    val nameError: UiText? = null,
    val descriptionError: UiText? = null,
    val startDateError: UiText? = null,
    val endDateError: UiText? = null,

    val hasErrors: Boolean = false,

    // ===== FORM STATES =====
    val isDirty: Boolean = false,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,

    val savedContractId: String? = null

) {
    val isFormValid: Boolean
        get() = errors != null
}

@HiltViewModel
class ContractFormViewModel @Inject constructor(
    private  val getContractByIdUseCase: GetContractByIdUseCase,
    private  val createContractUseCase: CreateContractUseCase,
    private  val updateContractUseCase: UpdateContractUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractFormUiState())
    val uiState: StateFlow<ContractFormUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ContractFormViewModel initialized")
    }

    companion object {
        private const val NAME_MIN_CHAR = 2
        private const val NAME_MAX_CHAR = 100
        private const val DESC_MIN_CHAR = 2
        private const val DESC_MAX_CHAR = 255

    }

    // ============================================================
    // INITIALIZATION
    // ============================================================

    fun init(clientId: String, contractId: String? = null) {
        _uiState.update {
            it.copy(
                clientId = clientId,
                isEditMode = contractId != null
            )
        }

        contractId?.let { id ->
            loadContract(id)
        }
    }

    fun initForEdit(contactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Contract Repo
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error =  QrError.App.UnknownError(e.message).asUiText()
                )
            }
        }
    }

    private fun loadContract(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }


            when (val contract = getContractByIdUseCase(id)) {
                is QrResult.Success -> {
                    val contract = contract.data
                    _uiState.value = ContractFormUiState(
                        isEditMode = true,
                        isLoading = false,
                        canSave = false,
                        originalContractId = contract.id,
                        clientId = contract.clientId,
                        name = contract.name ?: "",
                        description = contract.description ?: "",
                        startDate = contract.startDate,
                        endDate = contract.endDate,
                        hasPriority = contract.hasPriority,
                        hasRemoteAssistance = contract.hasRemoteAssistance,
                        hasMaintenance = contract.hasMaintenance,
                        createdAt = contract.createdAt,
                        updatedAt = contract.updatedAt
                    )
                    updateCanSaveState()

                }

                is QrResult.Error -> {
                    Timber.w("Errore caricamento contatto: ${contract.error}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error =  QrError.App.LoadError().asUiText()
                        )
                    }
                }
            }
        }
    }

    private fun toContract(state: ContractFormUiState): Contract {
        val now = now()
        return Contract(
            id = state.originalContractId ?: UUID.randomUUID().toString(),
            clientId = state.clientId,
            name = state.name.trim(),
            description = state.description.trim(),
            startDate = state.startDate?.let { it } ?: now,
            endDate = state.endDate?.let { it } ?: now,
            hasPriority = state.hasPriority,
            hasRemoteAssistance = state.hasRemoteAssistance,
            hasMaintenance = state.hasMaintenance,
            createdAt = if (state.isEditMode) state.createdAt ?: now else now,
            updatedAt = now
        )
    }

    // ============================================================
    // FORM FIELD UPDATES
    // ============================================================

    fun onFormEvent(event: ContractFormEvent) {
        when (event) {
            is ContractFormEvent.NameChanged -> updateName(event.s)
            is ContractFormEvent.DescriptionChanged -> updateDescription(event.s)
            is ContractFormEvent.StartDateChanged -> updateStartDate(event.i)
            is ContractFormEvent.EndDateChanged -> updateEndDate(event.i)
            is ContractFormEvent.HasPriorityChanged -> updateHasPriority(event.b)
            is ContractFormEvent.HasMaintenanceChanged -> updateHasMaintenance(event.b)
            is ContractFormEvent.HasRemoteAssistanceChanged -> updateHasRemoteAssistance(event.b)
        }
    }

    private fun updateName(value: String) {
        _uiState.update {
            it.copy(
                name = value,
                isDirty = true,
                nameError = validateName(value)
            )
        }
    }

    private fun updateDescription(value: String) {
        _uiState.update {
            it.copy(
                description = value,
                isDirty = true,
                descriptionError = validateDescription(value)
            )
        }
    }

    private fun updateStartDate(value: Instant?) {
        _uiState.update {
            it.copy(
                startDate = value,
                isDirty = true,
                startDateError = validateStartDate(value, it.endDate)
            )
        }
        updateCanSaveState()
    }

    private fun updateEndDate(value: Instant?) {
        _uiState.update {
            it.copy(
                endDate = value,
                isDirty = true,
                endDateError = validateEndDate(value, it.startDate)
            )
        }
        updateCanSaveState()
    }

    private fun updateHasPriority(value: Boolean) {
        _uiState.update {
            it.copy(
                hasPriority = value,
                isDirty = true
            )
        }
    }

    private fun updateHasMaintenance(value: Boolean) {
        _uiState.update {
            it.copy(
                hasMaintenance = value,
                isDirty = true
            )
        }
    }

    private fun updateHasRemoteAssistance(value: Boolean) {
        _uiState.update {
            it.copy(
                hasRemoteAssistance = value,
                isDirty = true
            )
        }
    }

    private fun updateCanSaveState() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            canSave = currentState.startDate != null &&
                    currentState.endDate != null &&
                    currentState.startDateError == null &&
                    currentState.endDateError == null
                    && currentState.nameError == null
                    && currentState.descriptionError == null
        )
    }

// ============================================================
// VALIDATIONS
// ============================================================

    private fun validateName(value: String): UiText? {
        return when {
            //value.isBlank() -> "Nome è vuoto"
            value.length < NAME_MIN_CHAR -> StringResources(R.string.err_contracts_contract_name_min_char, NAME_MIN_CHAR)
            value.length > NAME_MAX_CHAR -> StringResources(R.string.err_contracts_contract_name_max_char, NAME_MAX_CHAR) //"Nome troppo lungo (max $NAME_MAX_CHAR caratteri)"
            else -> null
        }
    }

    private fun validateDescription(value: String): UiText? {
        return when {
            //value.isBlank() -> "Nome è vuoto"
            value.length < DESC_MIN_CHAR -> StringResources(R.string.err_contracts_contract_description_min_char, DESC_MIN_CHAR) //"Nome deve essere di almeno $DESC_MIN_CHAR caratteri"
            value.length > DESC_MAX_CHAR -> StringResources(R.string.err_contracts_contract_description_max_char, DESC_MAX_CHAR) //"Dscrizione troppo lunga (max $DESC_MAX_CHAR caratteri)"
            else -> null
        }
    }

    private fun validateEndDate(endDate: Instant?, startDate: Instant?): UiText? {
        return when {
            endDate != null && startDate != null && endDate < startDate -> StringResources(R.string.err_contracts_contract_end_date_before_start_date) //" La data fine non puo essere antecedente alla data inizio"
            else -> null
        }

    }

    private fun validateStartDate(startDate: Instant?, endDate: Instant?): UiText? {
        return when {
            startDate?.let { it > now() } == true -> UiText.StringResource(R.string.err_contracts_contract_start_date_future)  //"Data inizio non puo essere nel futuro"
            endDate != null && startDate != null && endDate < startDate -> UiText.StringResource(R.string.err_contracts_contract_end_date_before_start_date) //" La data fine non puo essere antecedente alla data inizio"
            else -> null
        }
    }

    private fun validateAll(state: ContractFormUiState): List<UiText?> {
        return listOf(
            validateName(state.name),
            validateDescription(state.description),
            validateStartDate(state.startDate, state.endDate),
            validateEndDate(state.endDate, state.startDate)
        )
    }


// ============================================================
// SAVE OPERATIONS
// ============================================================

    fun saveContract() {

        Timber.d("saveContract() {canSave=${_uiState.value.canSave}}")

        //Check required fields
        val currentState = _uiState.value
        if (!currentState.canSave) return

        // Validation
        val allErrors = validateAll(currentState)
        if (allErrors.any { it != null }) {
            _uiState.update {
                it.copy(
                    hasErrors = true,
                    nameError = validateName(it.name),
                    descriptionError = validateDescription(it.description),
                    startDateError = validateStartDate(it.startDate, it.endDate),
                    endDateError = validateEndDate(it.endDate, it.startDate)
                )
            }

            Timber.d("saveContract() {validationErrors=$allErrors}")

            return
        }

        // save()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                // Create object
                val contract = toContract(currentState)

                // Save it
                val result = if (currentState.isEditMode) { updateContractUseCase(contract) }
                else { createContractUseCase(contract) }

                Timber.d("saveContract() {result=$result}")

                when (result) {
                    is QrResult.Success -> {

                        Timber.d("savedContract {result.data=${result.data}}")

                        _uiState.update {
                            it.copy(
                                savedContractId = when (result) {
                                    is QrResult.Success -> result.data  // ✅ ID reale dal database
                                    is QrResult.Error -> null
                                },
                                isSaving = false,
                                saveCompleted = true,
                                isDirty = false
                            )
                        }
                    }
                    is QrResult.Error -> {

                        Timber.d("error savedContract {result.error=${result.error}}")

                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = QrError.App.SaveError(result.error).asUiText()
                            )
                        }
                    }
                }


            } catch (e: Exception) {
                Timber.e(e, "Exception saving contact")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = QrError.App.SaveError().asUiText()
                )
            }
        }
    }

    fun resetSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false) }
    }

// ============================================================
// ERROR HANDLING
// ============================================================

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

// ============================================================
// UTILITY
// ============================================================

    fun hasUnsavedChanges(): Boolean {
        return _uiState.value.isDirty && !_uiState.value.saveCompleted
    }

    fun getContractId(): String? {
        return _uiState.value.originalContractId
    }

    fun isNewContact(): Boolean {
        return !_uiState.value.isEditMode
    }

}

sealed class ContractFormEvent {
    data class NameChanged(val s: String) : ContractFormEvent()
    data class DescriptionChanged(val s: String) : ContractFormEvent()
    data class StartDateChanged(val i: Instant?) : ContractFormEvent()
    data class EndDateChanged(val i: Instant?) : ContractFormEvent()
    data class HasPriorityChanged(val b: Boolean) : ContractFormEvent()
    data class HasRemoteAssistanceChanged(val b: Boolean) : ContractFormEvent()
    data class HasMaintenanceChanged(val b: Boolean) : ContractFormEvent()
}
