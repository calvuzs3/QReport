package net.calvuz.qreport.presentation.screen.client.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.usecase.client.contact.GetContactByIdUseCase
import net.calvuz.qreport.domain.usecase.client.contact.UpdateContactUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per ContactDetailScreen
 * Pattern identico a ClientDetailViewModel
 */
data class ContactDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val contact: Contact? = null,

    // Edit states
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,
    val updateError: String? = null,
    val showEditDialog: Boolean = false,

    // Quick access data
    val fullName: String = "",
    val roleDescription: String = "",
    val primaryContact: String = "",
    val clientId: String = ""
) {
    val hasData: Boolean get() = contact != null
    val isEmpty: Boolean get() = !isLoading && !hasData && error == null
    val contactId: String? get() = contact?.id
}

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val getContactByIdUseCase: GetContactByIdUseCase,
    private val updateContactUseCase: UpdateContactUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ContactDetailViewModel initialized")
    }

    fun loadContact(contactId: String) {
        if (contactId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "ID contatto non valido"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                getContactByIdUseCase(contactId).fold(
                    onSuccess = { contact ->
                        Timber.d("Contact loaded: ${contact.fullName}")
                        populateUiState(contact)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load contact: $contactId")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Errore caricamento contatto: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception loading contact")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    private fun populateUiState(contact: Contact) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            contact = contact,
            fullName = contact.fullName,
            roleDescription = contact.roleDescription,
            primaryContact = contact.primaryContact ?: "Non specificato",
            clientId = contact.clientId
        )
    }

    fun showEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = true)
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            updateError = null
        )
    }

    fun updateContact(updatedContact: Contact) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdating = true,
                updateError = null
            )

            try {
                updateContactUseCase(updatedContact).fold(
                    onSuccess = {
                        Timber.d("Contact updated successfully")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            updateSuccess = true,
                            showEditDialog = false
                        )
                        // Ricarica dati aggiornati
                        loadContact(updatedContact.id)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to update contact")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            updateError = "Errore aggiornamento: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception updating contact")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    updateError = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        val currentContactId = _uiState.value.contactId
        if (currentContactId != null) {
            loadContact(currentContactId)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null, updateError = null)
    }

    fun resetUpdateState() {
        _uiState.value = _uiState.value.copy(
            updateSuccess = false,
            updateError = null
        )
    }
}