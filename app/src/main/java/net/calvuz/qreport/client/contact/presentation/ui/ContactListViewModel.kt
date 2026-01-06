package net.calvuz.qreport.client.contact.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsByClientUseCase
import net.calvuz.qreport.client.contact.domain.usecase.DeleteContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.SetPrimaryContactUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.text.contains

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
    val contacts: List<ContactWithStats> = emptyList(),
    val filteredContacts: List<ContactWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingContact: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: ContactFilter = ContactFilter.ALL,
    val sortOrder: ContactSortOrder = ContactSortOrder.NAME,
    val clientId: String = "",

    val isSettingPrimary: String? = null   // ID del contatto in corso di promozione a primario
)


enum class ContactFilter {
    ALL, ACTIVE, INACTIVE, PRIMARY_ONLY
}

enum class ContactSortOrder {
    NAME, CREATED_RECENT, CREATED_OLDEST
}


@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getContactsByClientUseCase: GetContactsByClientUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val setPrimaryContactUseCase: SetPrimaryContactUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun initializeForClient(clientId: String) {
        if (clientId == _uiState.value.clientId) return // Già inizializzato per questo cliente

        _uiState.value = _uiState.value.copy(clientId = clientId)
        loadContacts(clientId)
    }

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
                Timber.d("Loading contacts for client: $clientId")

                getContactsByClientUseCase.observeContactsByClient(clientId)
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        Timber.e(exception, "Error in contacts flow")
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = "Errore caricamento Contatti: ${exception.message}"
                            )
                        }
                    }
                    .collect { contacts ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping contacts processing - job cancelled")
                            return@collect
                        }

                        // Enrich with statistics
                        val contactsithStats = enrichWithStatistics(contacts)

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                contactsithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.sortOrder
                            )

                            _uiState.value = currentState.copy(
                                contacts = contactsithStats,
                                filteredContacts = filteredAndSorted,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Loaded ${contacts.size} contacts successfully")
                        }
                    }
            } catch (_: CancellationException) {
                Timber.d("Contacts loading cancelled")
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
        if (clientId.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                Timber.d("Refreshing contacts for client: $clientId")

                getContactsByClientUseCase(clientId).fold(
                    onSuccess = { contacts ->
                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping refresh processing - job cancelled")
                            return@launch
                        }

                        val contactsWithStats = enrichWithStatistics(contacts)

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                contactsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.sortOrder
                            )

                            _uiState.value = currentState.copy(
                                contacts = contactsWithStats,
                                filteredContacts = filteredAndSorted,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Contacts refresh completed successfully")
                        }
                    },
                    onFailure = { error ->
                        if (currentCoroutineContext().isActive) {
                            Timber.e(error, "Failed to refresh contacts")
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = "Errore refresh: ${error.message}"
                            )
                        }
                    }
                )

            } catch (_: CancellationException) {
                Timber.d("Refresh cancelled")
                if (currentCoroutineContext().isActive) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Timber.e(e, "Failed to refresh contacts")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Errore refresh: ${e.message}"
                    )
                }
            }
        }
    }

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
                            error = "Errore eliminazione Contatto: ${error.message}"
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

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value

        if (query.length >= 3) {
            performSearch(query)
        } else {
            val filteredAndSorted = applyFiltersAndSort(
                currentState.contacts,
                query,
                currentState.selectedFilter,
                currentState.sortOrder
            )

            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredContacts = filteredAndSorted
            )
        }
    }

    fun updateFilter(filter: ContactFilter) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.contacts,
            currentState.searchQuery,
            filter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter,
            filteredContacts = filteredAndSorted
        )
    }

    fun updateSortOrder(sortOrder: ContactSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.contacts,
            currentState.searchQuery,
            currentState.selectedFilter,
            sortOrder
        )

        _uiState.value = currentState.copy(
            sortOrder = sortOrder,
            filteredContacts = filteredAndSorted
        )
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

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }


    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private suspend fun enrichWithStatistics(contacts: List<Contact>): List<ContactWithStats> {
        return contacts.map { contact ->
            val stats = try {
                ContactsStatistics(
                    isPrimaryContact = contact.isPrimary
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception getting stats for contact ${contact.id}")
                createEmptyStats()
            }

            ContactWithStats(contact = contact, stats = stats)
        }
    }

    private fun createEmptyStats() = ContactsStatistics(
        isPrimaryContact = false
    )

    private fun applyFiltersAndSort(
        contacts: List<ContactWithStats>,
        searchQuery: String,
        filter: ContactFilter,
        sortOrder: ContactSortOrder
    ): List<ContactWithStats> {
        var filtered = contacts

        // Apply status filter
        filtered = when (filter) {
            ContactFilter.ALL -> filtered
            ContactFilter.ACTIVE -> filtered.filter { it.contact.isActive }
            ContactFilter.INACTIVE -> filtered.filter { !it.contact.isActive }
            ContactFilter.PRIMARY_ONLY -> filtered.filter { it.contact.isPrimary }
        }

        // Apply local search query (per query corte)
        if (searchQuery.isNotBlank() && searchQuery.length <= 2) {
            filtered = filtered.filter { contactWithStats ->
                val contact = contactWithStats.contact
                contact.fullName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply sorting
        filtered = when (sortOrder) {
            ContactSortOrder.NAME -> filtered.sortedWith(
                compareByDescending<ContactWithStats> { it.contact.isPrimary }
                    .thenBy { it.contact.fullName.lowercase() }
            )

            ContactSortOrder.CREATED_RECENT -> filtered.sortedByDescending { it.contact.createdAt }
            ContactSortOrder.CREATED_OLDEST -> filtered.sortedBy { it.contact.createdAt }
        }

        return filtered
    }

    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filtered = currentState.contacts.filter { contactWithStats ->

            contactWithStats.contact.fullName.contains(query, ignoreCase = true)

            if (contactWithStats.contact.email != null)
                contactWithStats.contact.email.contains(query, ignoreCase = true)
            else false

            if (contactWithStats.contact.phone != null)
                contactWithStats.contact.phone.contains(query, ignoreCase = true)
            else false

            if (contactWithStats.contact.role != null)
                contactWithStats.contact.role.contains(query, ignoreCase = true)
            else false
        }

        val filteredAndSorted = applyFiltersAndSort(
            filtered,
            query,
            currentState.selectedFilter,
            currentState.sortOrder
        )

        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredContacts = filteredAndSorted
        )
    }
}

// ============================================================
// DATA CLASSES
// ============================================================

data class ContactWithStats(
    val contact: Contact,
    val stats: ContactsStatistics
) {
    val formattedLastModified: String
        get() {
            val now = Clock.System.now()
            val updated = contact.updatedAt
            val diffMillis = (now - updated).inWholeMilliseconds

            return when {
                diffMillis < 60000 -> "Aggiornato ora"
                diffMillis < 3600000 -> "Aggiornato ${diffMillis / 60000} min fa"
                diffMillis < 86400000 -> "Aggiornato ${diffMillis / 3600000}h fa"
                else -> "Aggiornato ${diffMillis / 86400000} giorni fa"
            }
        }
}

data class ContactsStatistics(
    val isPrimaryContact: Boolean
) {
}
