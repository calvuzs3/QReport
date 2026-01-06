package net.calvuz.qreport.client.contact.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.usecase.DeleteContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.GetContactByIdUseCase
import net.calvuz.qreport.client.contact.domain.usecase.UpdateContactUseCase
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

    // Delete states
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val showDeleteConfirmation: Boolean = false,

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
    private val updateContactUseCase: UpdateContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase
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

    // ============================================================
    // DELETE CLIENT OPERATIONS
    // ============================================================

    /**
     * Mostra dialog di conferma prima di eliminare
     */
    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true
        )
    }

    /**
     * Nasconde dialog di conferma
     */
    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false
        )
    }

    /**
     * Elimina il contatto corrente
     * ✅ FUNZIONE PRINCIPALE per delete
     */
    fun deleteContact() {
        val contactId = _uiState.value.contactId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeleting = true,
                deleteError = null,
                showDeleteConfirmation = false
            )

            try {
                deleteContactUseCase(contactId).fold(
                    onSuccess = {
                        Timber.d("Contact deleted successfully: $contactId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteSuccess = true  // ✅ Trigger navigation back
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete client: $contactId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteError = "Errore eliminazione: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting contact")
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deleteError = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    /**
     * Reset delete states
     */
    fun resetDeleteState() {
        _uiState.value = _uiState.value.copy(
            deleteSuccess = false,
            deleteError = null
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