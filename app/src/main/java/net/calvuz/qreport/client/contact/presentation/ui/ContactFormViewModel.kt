package net.calvuz.qreport.client.contact.presentation.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.UiText.StringResource
import net.calvuz.qreport.app.error.presentation.UiText.StringResources
import net.calvuz.qreport.app.error.presentation.asUiText  // ✅ Using existing error system
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import net.calvuz.qreport.client.contact.domain.usecase.CreateContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.UpdateContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsByClientUseCase
import net.calvuz.qreport.client.contact.domain.usecase.CheckContactExistsUseCase
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
 *
 * ✅ Uses QrResult<T, QrError> pattern with existing QrErrorExt.kt system
 */

data class ContactFormUiState(
    // states
    val isDirty: Boolean = false,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val savedContactId: String? = null,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val originalContactId: String? = null,
    val clientId: String = "",
    // form fields
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
    // error
    val error: UiText? = null,
    val firstNameError: UiText? = null,
    val lastNameError: UiText? = null,
    val emailError: UiText? = null,
    val alternativeEmailError: UiText? = null,
    val phoneError: UiText? = null,
    val mobilePhoneError: UiText? = null,
    val generalValidationError: UiText? = null,
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
    private val checkContactExistsUseCase: CheckContactExistsUseCase,
) : ViewModel() {
    companion object {
        private const val MIN_NAME_LENGTH = 2
        private const val MAX_NAME_LENGTH = 100
        private const val MAX_SURNAME_LENGTH = 100
        private const val MAX_EMAIL_LENGTH = 100
    }

    private val _uiState = MutableStateFlow(ContactFormUiState())
    val uiState: StateFlow<ContactFormUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ContactFormViewModel initialized")
    }

    // ============================================================
    // INITIALIZATION
    // ============================================================

    fun initForCreate(clientId: String) {
        Timber.d("ContactFormViewModel: Initializing for create mode, client: $clientId")

        _uiState.value = ContactFormUiState(
            isEditMode = false,
            clientId = clientId,
            canSave = false
        )

        // Controlla se è il primo contatto (diventa automaticamente primary)
        checkIfShouldBePrimary(clientId)
    }

    fun initForEdit(contactId: String) {
        Timber.d("ContactFormViewModel: Initializing for edit mode, contact: $contactId")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                when (val result = checkContactExistsUseCase(contactId)) {
                    is QrResult.Success -> {
                        val contact = result.data
                        Timber.d("ContactFormViewModel: Contact loaded successfully: ${contact.id}")
                        populateFormFromContact(contact)
                    }

                    is QrResult.Error -> {
                        Timber.e("ContactFormViewModel: Failed to load contact $contactId: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.error.asUiText()  // ✅ Using existing error system
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "ContactFormViewModel: Exception loading contact $contactId")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResource(R.string.err_load)  // ✅ Using standard error resource
                )
            }
        }
    }

    private fun populateFormFromContact(contact: Contact) {
        Timber.d("ContactFormViewModel: Populating form from contact: ${contact.fullName}")

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
            try {
                when (val result = getContactsByClientUseCase(clientId)) {
                    is QrResult.Success -> {
                        val contacts = result.data
                        val shouldBePrimary = contacts.isEmpty()

                        Timber.d("ContactFormViewModel: Client has ${contacts.size} contacts, shouldBePrimary: $shouldBePrimary")
                        _uiState.value = _uiState.value.copy(isPrimary = shouldBePrimary)
                    }

                    is QrResult.Error -> {
                        // Ignora errore, mantieni default (isPrimary = false)
                        Timber.w("ContactFormViewModel: Error checking client contacts: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                // Ignora errore, mantieni default
                Timber.w(e, "ContactFormViewModel: Exception checking if should be primary")
            }
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

    private fun validateFirstName(value: String): UiText? {
        return when {
            value.isBlank() -> StringResource(R.string.err_validation_empty_field)  // ✅ Using standard validation error
            value.length < MIN_NAME_LENGTH -> StringResources(R.string.err_contact_form_name_min_char, MIN_NAME_LENGTH)
            value.length > MAX_NAME_LENGTH -> StringResources(R.string.err_contact_form_name_max_char, MAX_NAME_LENGTH)
            else -> null
        }
    }

    private fun validateLastName(value: String): UiText? {
        return when {
            value.length > MAX_SURNAME_LENGTH -> StringResources(R.string.err_contact_form_surname_max_char, MAX_SURNAME_LENGTH)
            else -> null
        }
    }

    private fun validateEmail(value: String): UiText? {
        if (value.isBlank()) return null

        return when {
            !Patterns.EMAIL_ADDRESS.matcher(value).matches() ->
                StringResource(R.string.err_contact_form_email_invalid)

            value.length > MAX_EMAIL_LENGTH ->
                StringResources(R.string.err_contact_form_email_max_char, MAX_EMAIL_LENGTH)
            else -> null
        }
    }

    private fun validatePhone(value: String): UiText? {
        if (value.isBlank()) return null

        val cleanPhone = value.replace("\\s+".toRegex(), "").replace("-", "")

        return when {
            !cleanPhone.matches("\\d{7,15}".toRegex()) &&
                    !cleanPhone.matches("\\+\\d{7,15}".toRegex()) &&
                    !cleanPhone.matches("\\+39\\d{9,10}".toRegex()) &&
                    !cleanPhone.matches("0\\d{8,10}".toRegex()) &&
                    !cleanPhone.matches("3\\d{8,9}".toRegex()) ->
                StringResource(R.string.err_contact_form_phone_invalid)
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

    private fun validateGeneralForm(): UiText? {
        val state = _uiState.value

        return when {
            !state.hasAtLeastOneContact ->
                StringResource(R.string.err_contact_form_at_least_one_contact)
            else -> null
        }
    }

    // ============================================================
    // SAVE OPERATIONS - Clean QrResult Pattern
    // ============================================================

    fun saveContact() {
        val currentState = _uiState.value

        if (!currentState.canSave) {
            Timber.w("ContactFormViewModel: Save attempted but canSave is false")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                val contact = _uiState.value.toContact()
                Timber.d("ContactFormViewModel: Saving contact: ${contact.fullName} (${if (_uiState.value.isEditMode) "edit" else "create"})")

                val result = if (_uiState.value.isEditMode) {
                    updateContactUseCase(contact)
                } else {
                    createContactUseCase(contact)
                }

                when (result) {
                    is QrResult.Success -> {
                        val savedContact = result.data
                        Timber.d("ContactFormViewModel: Contact saved successfully: ${savedContact.id} - ${savedContact.fullName}")

                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveCompleted = true,
                            savedContactId = savedContact.id,
                            isDirty = false,
                            error = null
                        )
                    }

                    is QrResult.Error -> {
                        Timber.e("ContactFormViewModel: Failed to save contact: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = result.error.asUiText()  // ✅ Using existing error system - handles all QrError types automatically
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "ContactFormViewModel: Exception saving contact")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = StringResource(R.string.err_save)  // ✅ Using standard save error
                )
            }
        }
    }

    // ============================================================
    // ERROR HANDLING - Simplified with Existing System
    // ============================================================

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSaveCompleted() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    // ============================================================
    // UTILITY METHODS
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

    fun getClientId(): String {
        return _uiState.value.clientId
    }

    fun resetForm() {
        Timber.d("ContactFormViewModel: Resetting form")
        _uiState.value = ContactFormUiState()
    }

    fun hasValidationErrors(): Boolean {
        val state = _uiState.value
        return state.firstNameError != null ||
                state.lastNameError != null ||
                state.emailError != null ||
                state.alternativeEmailError != null ||
                state.phoneError != null ||
                state.mobilePhoneError != null ||
                state.generalValidationError != null
    }

    fun validateAllFields() {
        val state = _uiState.value
        _uiState.value = state.copy(
            firstNameError = validateFirstName(state.firstName),
            lastNameError = validateLastName(state.lastName),
            emailError = validateEmail(state.email),
            alternativeEmailError = validateEmail(state.alternativeEmail),
            phoneError = validatePhone(state.phone),
            mobilePhoneError = validatePhone(state.mobilePhone),
            generalValidationError = validateGeneralForm()
        )
        updateCanSaveState()
    }
}