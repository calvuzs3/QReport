package net.calvuz.qreport.presentation.feature.client.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.ContactMethod
import net.calvuz.qreport.domain.usecase.client.contact.CreateContactUseCase
import net.calvuz.qreport.domain.usecase.client.contact.UpdateContactUseCase
import net.calvuz.qreport.domain.usecase.client.contact.GetContactsByClientUseCase
import net.calvuz.qreport.domain.usecase.client.contact.GetContactByIdUseCase
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel per ContactFormScreen
 *
 * Gestisce:
 * - Creazione nuovo contatto
 * - Modifica contatto esistente
 * - Validazioni form in tempo reale
 * - Gestione stati loading/error/success
 * - Tutti i campi del modello Contact
 */

data class ContactFormUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val originalContactId: String? = null,
    val clientId: String = "",

    // ===== FORM FIELDS =====
    val firstName: String = "",
    val lastName: String = "",
    val title: String = "",
    val role: String = "",
    val department: String = "",
    val email: String = "",
    val alternativeEmail: String = "",
    val phone: String = "",
    val mobilePhone: String = "",
    val preferredContactMethod: ContactMethod? = null,
    val notes: String = "",
    val isPrimary: Boolean = false,

    // ===== VALIDATION STATES =====
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val alternativeEmailError: String? = null,
    val phoneError: String? = null,
    val mobilePhoneError: String? = null,
    val generalValidationError: String? = null,

    // ===== FORM STATES =====
    val isDirty: Boolean = false,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val savedContactId: String? = null
) {

    /**
     * Verifica se il form è valido
     */
    val isFormValid: Boolean
        get() = firstName.isNotBlank() &&
                firstNameError == null &&
                emailError == null &&
                alternativeEmailError == null &&
                phoneError == null &&
                mobilePhoneError == null &&
                hasAtLeastOneContact

    /**
     * Verifica se ha almeno un metodo di contatto
     */
    val hasAtLeastOneContact: Boolean
        get() = email.isNotBlank() || phone.isNotBlank() || mobilePhone.isNotBlank()

    /**
     * Crea l'oggetto Contact dal form
     */
    fun toContact(): Contact {
        val now = Clock.System.now()
        return Contact(
            id = originalContactId ?: UUID.randomUUID().toString(),
            clientId = clientId,
            firstName = firstName.trim(),
            lastName = lastName.trim().takeIf { it.isNotBlank() },
            title = title.trim().takeIf { it.isNotBlank() },
            role = role.trim().takeIf { it.isNotBlank() },
            department = department.trim().takeIf { it.isNotBlank() },
            email = email.trim().takeIf { it.isNotBlank() },
            alternativeEmail = alternativeEmail.trim().takeIf { it.isNotBlank() },
            phone = phone.trim().takeIf { it.isNotBlank() },
            mobilePhone = mobilePhone.trim().takeIf { it.isNotBlank() },
            preferredContactMethod = preferredContactMethod,
            notes = notes.trim().takeIf { it.isNotBlank() },
            isPrimary = isPrimary,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
    }
}

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    private val createContactUseCase: CreateContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val getContactsByClientUseCase: GetContactsByClientUseCase,
    private val getContactByIdUseCase: GetContactByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactFormUiState())
    val uiState: StateFlow<ContactFormUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ContactFormViewModel initialized")
    }

    // ============================================================
    // INITIALIZATION
    // ============================================================

    fun initForCreate(clientId: String) {
        _uiState.value = ContactFormUiState(
            isEditMode = false,
            clientId = clientId,
            canSave = false
        )

        // Controlla se è il primo contatto (diventa automaticamente primary)
        checkIfShouldBePrimary(clientId)
    }

    fun initForEdit(contactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                //contactRepository
                getContactByIdUseCase(contactId).fold(
                    onSuccess = { contact ->
                        populateFormFromContact(contact)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Contatto non trovato: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    private fun populateFormFromContact(contact: Contact) {
        _uiState.value = ContactFormUiState(
            isEditMode = true,
            originalContactId = contact.id,
            clientId = contact.clientId,
            firstName = contact.firstName,
            lastName = contact.lastName ?: "",
            title = contact.title ?: "",
            role = contact.role ?: "",
            department = contact.department ?: "",
            email = contact.email ?: "",
            alternativeEmail = contact.alternativeEmail ?: "",
            phone = contact.phone ?: "",
            mobilePhone = contact.mobilePhone ?: "",
            preferredContactMethod = contact.preferredContactMethod,
            notes = contact.notes ?: "",
            isPrimary = contact.isPrimary,
            isLoading = false,
            canSave = true
        )
    }

    private fun checkIfShouldBePrimary(clientId: String) {
        viewModelScope.launch {
            getContactsByClientUseCase(clientId).fold(
                onSuccess = { contacts ->
                    val shouldBePrimary = contacts.isEmpty()
                    _uiState.value = _uiState.value.copy(isPrimary = shouldBePrimary)
                },
                onFailure = { /* Ignora errore, mantieni default */ }
            )
        }
    }

    // ============================================================
    // FORM FIELD UPDATES
    // ============================================================

    fun updateFirstName(value: String) {
        _uiState.value = _uiState.value.copy(
            firstName = value,
            isDirty = true,
            firstNameError = validateFirstName(value)
        )
        updateCanSaveState()
    }

    fun updateLastName(value: String) {
        _uiState.value = _uiState.value.copy(
            lastName = value,
            isDirty = true,
            lastNameError = validateLastName(value)
        )
        updateCanSaveState()
    }

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(
            title = value,
            isDirty = true
        )
        updateCanSaveState()
    }

    fun updateRole(value: String) {
        _uiState.value = _uiState.value.copy(
            role = value,
            isDirty = true
        )
        updateCanSaveState()
    }

    fun updateDepartment(value: String) {
        _uiState.value = _uiState.value.copy(
            department = value,
            isDirty = true
        )
        updateCanSaveState()
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            isDirty = true,
            emailError = validateEmail(value)
        )
        updateCanSaveState()
    }

    fun updateAlternativeEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            alternativeEmail = value,
            isDirty = true,
            alternativeEmailError = validateEmail(value)
        )
        updateCanSaveState()
    }

    fun updatePhone(value: String) {
        _uiState.value = _uiState.value.copy(
            phone = value,
            isDirty = true,
            phoneError = validatePhone(value)
        )
        updateCanSaveState()
    }

    fun updateMobilePhone(value: String) {
        _uiState.value = _uiState.value.copy(
            mobilePhone = value,
            isDirty = true,
            mobilePhoneError = validatePhone(value)
        )
        updateCanSaveState()
    }

    fun updatePreferredContactMethod(value: ContactMethod?) {
        _uiState.value = _uiState.value.copy(
            preferredContactMethod = value,
            isDirty = true
        )
        updateCanSaveState()
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(
            notes = value,
            isDirty = true
        )
        updateCanSaveState()
    }

    fun updateIsPrimary(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            isPrimary = value,
            isDirty = true
        )
        updateCanSaveState()
    }

    // ============================================================
    // VALIDATIONS
    // ============================================================

    private fun validateFirstName(value: String): String? {
        return when {
            value.isBlank() -> "Nome è obbligatorio"
            value.length < 2 -> "Nome deve essere di almeno 2 caratteri"
            value.length > 100 -> "Nome troppo lungo (max 100 caratteri)"
            else -> null
        }
    }

    private fun validateLastName(value: String): String? {
        return when {
            value.length > 100 -> "Cognome troppo lungo (max 100 caratteri)"
            else -> null
        }
    }

    private fun validateEmail(value: String): String? {
        if (value.isBlank()) return null

        return when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() ->
                "Formato email non valido"

            value.length > 100 -> "Email troppo lunga (max 100 caratteri)"
            else -> null
        }
    }

    private fun validatePhone(value: String): String? {
        if (value.isBlank()) return null

        val cleanPhone = value.replace("\\s+".toRegex(), "").replace("-", "")

        return when {
            !cleanPhone.matches("\\d{7,15}".toRegex()) &&
                    !cleanPhone.matches("\\+\\d{7,15}".toRegex()) &&
                    !cleanPhone.matches("\\+39\\d{9,10}".toRegex()) &&
                    !cleanPhone.matches("0\\d{8,10}".toRegex()) &&
                    !cleanPhone.matches("3\\d{8,9}".toRegex()) ->
                "Formato telefono non valido"

            else -> null
        }
    }

    private fun updateCanSaveState() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            canSave = currentState.isFormValid && currentState.isDirty,
            generalValidationError = validateGeneralForm()
        )
    }

    private fun validateGeneralForm(): String? {
        val state = _uiState.value

        return when {
            !state.hasAtLeastOneContact ->
                "Deve essere fornito almeno un contatto (email, telefono o cellulare)"

            else -> null
        }
    }

    // ============================================================
    // SAVE OPERATIONS
    // ============================================================

    fun saveContact() {
        if (!_uiState.value.canSave) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                val contact = _uiState.value.toContact()

                val result = if (_uiState.value.isEditMode) {
                    updateContactUseCase(contact)
                } else {
                    createContactUseCase(contact)
                }

                result.fold(
                    onSuccess = {
                        Timber.d("Contact saved successfully: ${contact.id} ${contact.fullName}")
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveCompleted = true,
                            savedContactId = contact.id,
                            isDirty = false
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to save contact")
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = "Errore salvataggio: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception saving contact")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Errore imprevisto: ${e.message}"
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

    fun resetSaveCompleted() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    // ============================================================
    // UTILITY
    // ============================================================

    fun hasUnsavedChanges(): Boolean {
        return _uiState.value.isDirty && !_uiState.value.saveCompleted
    }

    fun getContactId(): String? {
        return _uiState.value.originalContactId
    }

    fun isNewContact(): Boolean {
        return !_uiState.value.isEditMode
    }
}