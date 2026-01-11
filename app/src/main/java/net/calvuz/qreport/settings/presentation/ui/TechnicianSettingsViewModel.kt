package net.calvuz.qreport.settings.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import net.calvuz.qreport.settings.domain.repository.TechnicianSettingsRepository
import javax.inject.Inject

@HiltViewModel
class TechnicianSettingsViewModel @Inject constructor(
    private val technicianSettingsRepository: TechnicianSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TechnicianSettingsUiState())
    val uiState: StateFlow<TechnicianSettingsUiState> = _uiState.asStateFlow()

    // Current technician info from repository
    val currentTechnicianInfo = technicianSettingsRepository.getTechnicianInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TechnicianInfo()
        )

    // Check if technician data is available
    val hasTechnicianData = technicianSettingsRepository.hasTechnicianData()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // ValidationError state
    private val _formState = MutableStateFlow(TechnicianFormState())
    val formState: StateFlow<TechnicianFormState> = _formState.asStateFlow()

    init {
        // Initialize form with current data
        viewModelScope.launch {
            currentTechnicianInfo.collect { technicianInfo ->
                _formState.value = TechnicianFormState(
                    name = technicianInfo.name,
                    company = technicianInfo.company,
                    certification = technicianInfo.certification,
                    phone = technicianInfo.phone,
                    email = technicianInfo.email
                )
            }
        }
    }

    /**
     * Update form field
     */
    fun updateFormField(field: TechnicianField, value: String) {
        val currentForm = _formState.value
        val newForm = when (field) {
            TechnicianField.NAME -> currentForm.copy(name = value)
            TechnicianField.COMPANY -> currentForm.copy(company = value)
            TechnicianField.CERTIFICATION -> currentForm.copy(certification = value)
            TechnicianField.PHONE -> currentForm.copy(phone = value)
            TechnicianField.EMAIL -> currentForm.copy(email = value)
        }

        _formState.value = newForm.copy(
            validationErrors = validateForm(newForm),
            isModified = true
        )
    }

    /**
     * Save technician info
     */
    fun saveTechnicianInfo() {
        val form = _formState.value
        val validationErrors = validateForm(form)

        if (validationErrors.isNotEmpty()) {
            _formState.value = form.copy(validationErrors = validationErrors)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)

                val technicianInfo = TechnicianInfo(
                    name = form.name.trim(),
                    company = form.company.trim(),
                    certification = form.certification.trim(),
                    phone = form.phone.trim(),
                    email = form.email.trim()
                )

                val result = technicianSettingsRepository.updateTechnicianInfo(technicianInfo)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = "Informazioni tecnico salvate con successo"
                    )
                    _formState.value = form.copy(isModified = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Errore nel salvataggio: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    /**
     * Reset to default (clear all data)
     */
    fun resetToDefault() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = technicianSettingsRepository.resetToDefault()

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Impostazioni tecnico ripristinate"
                    )
                    _formState.value = TechnicianFormState() // Reset form
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore nel reset: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    /**
     * Load technician data for pre-population (used by EditHeaderDialog)
     */
    fun loadTechnicianDataForPrePopulation(onDataLoaded: (TechnicianInfo) -> Unit) {
        viewModelScope.launch {
            val technicianInfo = currentTechnicianInfo.first()
            if (technicianInfo.name.isNotBlank() || technicianInfo.company.isNotBlank()) {
                onDataLoaded(technicianInfo)
            }
        }
    }

    /**
     * Clear error and success messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            message = null
        )
    }

    /**
     * Validate form data
     */
    private fun validateForm(form: TechnicianFormState): List<String> {
        val errors = mutableListOf<String>()

        // Name validation
        if (form.name.isNotBlank() && form.name.trim().length < 2) {
            errors.add("Il nome deve avere almeno 2 caratteri")
        }

        // Company validation
        if (form.company.isNotBlank() && form.company.trim().length < 2) {
            errors.add("Il nome dell'azienda deve avere almeno 2 caratteri")
        }

        // Phone validation
        if (form.phone.isNotBlank() && !isValidPhone(form.phone)) {
            errors.add("Formato telefono non valido")
        }

        // Email validation
        if (form.email.isNotBlank() && !isValidEmail(form.email)) {
            errors.add("Formato email non valido")
        }

        return errors
    }

    /**
     * Validate phone number
     */
    private fun isValidPhone(phone: String): Boolean {
        val phoneRegex = """^[+]?[0-9\s\-()]{8,}$""".toRegex()
        return phone.matches(phoneRegex)
    }

    /**
     * Validate email
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = """^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""".toRegex()
        return email.matches(emailRegex)
    }
}

/**
 * UI State for technician settings
 */
data class TechnicianSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * ValidationError state for technician data
 */
data class TechnicianFormState(
    val name: String = "",
    val company: String = "",
    val certification: String = "",
    val phone: String = "",
    val email: String = "",
    val validationErrors: List<String> = emptyList(),
    val isModified: Boolean = false
) {
    val isValid: Boolean get() = validationErrors.isEmpty()
    val hasData: Boolean get() = name.isNotBlank() || company.isNotBlank()
}

/**
 * ValidationError fields enum for type-safe field updates
 */
enum class TechnicianField {
    NAME,
    COMPANY,
    CERTIFICATION,
    PHONE,
    EMAIL
}