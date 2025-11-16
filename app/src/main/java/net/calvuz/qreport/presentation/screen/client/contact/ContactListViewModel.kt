package net.calvuz.qreport.presentation.screen.client.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.usecase.client.contact.GetContactsByClientUseCase
import net.calvuz.qreport.domain.usecase.client.contact.DeleteContactUseCase
import net.calvuz.qreport.domain.usecase.client.contact.SetPrimaryContactUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per ContactListScreen
 *
 * Gestisce:
 * - Caricamento lista contatti per cliente
 * - Operazioni delete e set primary
 * - Stati loading/error/success
 * - Search filtering locale
 */

data class ContactListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val contacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val clientId: String = "",

    // Filtri e ordinamento
    val showActiveOnly: Boolean = true,

    // Stati operazioni
    val isDeletingContact: String? = null, // ID del contatto in corso di eliminazione
    val isSettingPrimary: String? = null   // ID del contatto in corso di promozione a primario
) {

    /**
     * Contatti filtrati in base a ricerca e filtri
     */
    val filteredContacts: List<Contact>
        get() = contacts
            .filter { contact ->
                // Filtro attivi/inattivi
                if (showActiveOnly) contact.isActive else true
            }
            .filter { contact ->
                // Filtro ricerca
                if (searchQuery.isBlank()) {
                    true
                } else {
                    val query = searchQuery.lowercase()
                    contact.firstName.lowercase().contains(query) ||
                            contact.lastName?.lowercase()?.contains(query) == true ||
                            contact.role?.lowercase()?.contains(query) == true ||
                            contact.department?.lowercase()?.contains(query) == true ||
                            contact.phone?.contains(query) == true ||
                            contact.email?.lowercase()?.contains(query) == true
                }
            }

    /**
     * Contatto primario corrente
     */
    val primaryContact: Contact?
        get() = contacts.find { it.isPrimary && it.isActive }

    /**
     * Statistiche contatti
     */
    val contactStats: ContactStats
        get() = ContactStats(
            totalContacts = contacts.count { it.isActive },
            inactiveContacts = contacts.count { !it.isActive },
            hasPrimaryContact = primaryContact != null
        )

    val isEmpty: Boolean
        get() = !isLoading && contacts.isEmpty() && error == null

    val isSearchActive: Boolean
        get() = searchQuery.isNotBlank()

    val hasContacts: Boolean
        get() = contacts.isNotEmpty()
}

data class ContactStats(
    val totalContacts: Int,
    val inactiveContacts: Int,
    val hasPrimaryContact: Boolean
)

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getContactsByClientUseCase: GetContactsByClientUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val setPrimaryContactUseCase: SetPrimaryContactUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ContactListViewModel initialized")
    }

    // ============================================================
    // CONTACT LOADING
    // ============================================================

    fun loadContacts(clientId: String) {
        if (clientId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "ID cliente non valido"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                clientId = clientId
            )

            try {
                getContactsByClientUseCase(clientId).fold(
                    onSuccess = { contacts ->
                        Timber.d("Loaded ${contacts.size} contacts for client $clientId")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            contacts = contacts,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load contacts for client $clientId")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Errore caricamento contatti: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception loading contacts")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun refreshContacts() {
        val clientId = _uiState.value.clientId
        if (clientId.isNotBlank()) {
            loadContacts(clientId)
        }
    }

    // ============================================================
    // SEARCH AND FILTERING
    // ============================================================

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleActiveFilter() {
        _uiState.value = _uiState.value.copy(
            showActiveOnly = !_uiState.value.showActiveOnly
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
    }

    // ============================================================
    // CONTACT OPERATIONS
    // ============================================================

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingContact = contactId)

            try {
                deleteContactUseCase(contactId).fold(
                    onSuccess = {
                        Timber.d("Contact deleted successfully: $contactId")
                        // Ricarica la lista per mostrare i cambiamenti
                        refreshContacts()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete contact $contactId")
                        _uiState.value = _uiState.value.copy(
                            isDeletingContact = null,
                            error = "Errore eliminazione contatto: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting contact")
                _uiState.value = _uiState.value.copy(
                    isDeletingContact = null,
                    error = "Errore imprevisto: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeletingContact = null)
            }
        }
    }

    fun setPrimaryContact(contactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSettingPrimary = contactId)

            try {
                setPrimaryContactUseCase(contactId).fold(
                    onSuccess = {
                        Timber.d("Contact set as primary successfully: $contactId")
                        // Ricarica la lista per mostrare i cambiamenti
                        refreshContacts()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to set primary contact $contactId")
                        _uiState.value = _uiState.value.copy(
                            isSettingPrimary = null,
                            error = "Errore impostazione referente primario: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception setting primary contact")
                _uiState.value = _uiState.value.copy(
                    isSettingPrimary = null,
                    error = "Errore imprevisto: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSettingPrimary = null)
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
    // UTILITY METHODS
    // ============================================================

    fun getContactById(contactId: String): Contact? {
        return _uiState.value.contacts.find { it.id == contactId }
    }

    fun hasContacts(): Boolean {
        return _uiState.value.contacts.isNotEmpty()
    }

    fun getActiveContactsCount(): Int {
        return _uiState.value.contacts.count { it.isActive }
    }

    fun isContactPrimary(contactId: String): Boolean {
        return _uiState.value.contacts.find { it.id == contactId }?.isPrimary == true
    }

    fun canDeleteContact(contactId: String): Boolean {
        val contact = getContactById(contactId)
        return contact != null && contact.isActive
    }

    fun canSetAsPrimary(contactId: String): Boolean {
        val contact = getContactById(contactId)
        return contact != null && contact.isActive && !contact.isPrimary
    }
}