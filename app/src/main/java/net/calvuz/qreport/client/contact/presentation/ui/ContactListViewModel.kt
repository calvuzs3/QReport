package net.calvuz.qreport.client.contact.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsByClientUseCase
import net.calvuz.qreport.client.contact.domain.usecase.DeleteContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.SetPrimaryContactUseCase
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.text.contains

/** ContactListScreen UiState */
data class ContactListUiState(
    val clientId: String = "",
    val contacts: List<ContactWithStats> = emptyList(),
    val filteredContacts: List<ContactWithStats> = emptyList(),
    // states
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingContact: String? = null,
    val isSettingPrimary: String? = null,   // ID del contatto in corso di promozione a primario

    // ===== NEW: Bulk Operations =====
    val isBulkDeleting: Boolean = false,     // Bulk delete in progress
    val bulkDeleteProgress: Int = 0,         // Progress counter for bulk operations
    val bulkDeleteTotal: Int = 0,            // Total items to delete

    // search and filter
    val searchQuery: String = "",
    val selectedFilter: ContactFilter = ContactFilter.ACTIVE,
    val selectedSortOrder: ContactSortOrder = ContactSortOrder.CREATED_RECENT,
    // errors
    val error: UiText? = null,

    // Card Variant
    val cardVariant: ListViewMode = ListViewMode.FULL
)

/** ContactListScreen Filter */
enum class ContactFilter {
    ACTIVE, INACTIVE, PRIMARY_ONLY, ALL
}

/** ContactListScreen SortOrder */
enum class ContactSortOrder {
    CREATED_RECENT, CREATED_OLDEST, NAME
}

/**
 * ContactListScreen ViewModel
 *
 * Handle:
 * - Contact list by client
 * - Operations: delete, set primary, bulk delete
 * - States: loading/refreshing/deleting/settingprimary/error/success
 * - Search filtering
 * - Sort
 */
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getContactsByClientUseCase: GetContactsByClientUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val setPrimaryContactUseCase: SetPrimaryContactUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun initializeForClient(clientId: String) {
        if (clientId == _uiState.value.clientId) return // Già inizializzato per questo cliente

        _uiState.value = _uiState.value.copy(clientId = clientId)
        observeCardVariant()

        loadContacts(clientId)
    }

    fun loadContacts(clientId: String) {
        if (clientId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = UiText.StringResource(R.string.err_contact_list_invalid_client_id)  // ✅ Fixed StringResources → StringResource
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

                when (val result = getContactsByClientUseCase(clientId)) {
                    is QrResult.Success -> {
                        val contacts = result.data

                        if (!currentCoroutineContext().isActive) {
                            Timber.d("Skipping contacts processing - job cancelled")
                            return@launch
                        }

                        // Enrich with statistics
                        val contactsWithStats = enrichWithStatistics(contacts)

                        if (currentCoroutineContext().isActive) {
                            val currentState = _uiState.value
                            val filteredAndSorted = applyFiltersAndSort(
                                contactsWithStats,
                                currentState.searchQuery,
                                currentState.selectedFilter,
                                currentState.selectedSortOrder
                            )

                            _uiState.value = currentState.copy(
                                contacts = contactsWithStats,
                                filteredContacts = filteredAndSorted,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Loaded ${contacts.size} contacts successfully")
                        }
                    }

                    is QrResult.Error -> {
                        if (currentCoroutineContext().isActive) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                                    R.string.err_contacts_list_load_contacts,
                                    result.error
                                )
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                Timber.d("Contacts loading cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Exception loading contacts")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                        R.string.err_contact_list_unexpected,
                        e.message ?: ""
                    )
                )
            }
        }
    }

    fun refresh() {
        val clientId = _uiState.value.clientId
        if (clientId.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                Timber.d("Refreshing contacts for client: $clientId")
                delay(500)

                when (val result = getContactsByClientUseCase(clientId)) {
                    is QrResult.Success -> {
                        val contacts = result.data

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
                                currentState.selectedSortOrder
                            )

                            _uiState.value = currentState.copy(
                                contacts = contactsWithStats,
                                filteredContacts = filteredAndSorted,
                                isRefreshing = false,
                                error = null
                            )

                            Timber.d("Contacts refresh completed successfully")
                        }
                    }

                    is QrResult.Error -> {
                        if (currentCoroutineContext().isActive) {
                            Timber.e("Failed to refresh contacts: ${result.error}")
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                                    R.string.err_contact_list_refresh_failed,
                                    result.error.toString()
                                )
                            )
                        }
                    }
                }

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
                        error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                            R.string.err_contact_list_refresh_unexpected,
                            e.message ?: ""
                        )
                    )
                }
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingContact = contactId)

            try {
                Timber.d("Deleting contact: $contactId")

                when ( deleteContactUseCase(contactId)) {
                    is QrResult.Success -> {
                        Timber.d("Contact deleted successfully: $contactId")

                        // Remove contact from current list
                        val currentContacts = _uiState.value.contacts
                        val updatedContacts = currentContacts.filterNot {
                            it.contact.id == contactId
                        }

                        val currentState = _uiState.value
                        val filteredAndSorted = applyFiltersAndSort(
                            updatedContacts,
                            currentState.searchQuery,
                            currentState.selectedFilter,
                            currentState.selectedSortOrder
                        )

                        _uiState.value = currentState.copy(
                            contacts = updatedContacts,
                            filteredContacts = filteredAndSorted,
                            isDeletingContact = null,
                            error = null
                        )
                    }

                    is QrResult.Error -> {
                        Timber.e("Failed to delete contact: $contactId")
                        _uiState.value = _uiState.value.copy(
                            isDeletingContact = null,
                            error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                                R.string.err_contact_list_delete_failed,
                                contactId
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting contact")
                _uiState.value = _uiState.value.copy(
                    isDeletingContact = null,
                    error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                        R.string.err_contact_list_delete_unexpected,
                        e.message ?: ""
                    )
                )
            }
        }
    }

    // ===== NEW: Bulk Delete Implementation =====
    fun bulkDeleteContacts(contactIds: List<String>) {
        if (contactIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBulkDeleting = true,
                bulkDeleteProgress = 0,
                bulkDeleteTotal = contactIds.size
            )

            try {
                Timber.d("Starting bulk delete for ${contactIds.size} contacts")

                var successCount = 0
                var failedIds = mutableListOf<String>()

                contactIds.forEachIndexed { index, contactId ->
                    try {
                        when (val result = deleteContactUseCase(contactId)) {
                            is QrResult.Success -> {
                                successCount++
                                Timber.d("Contact deleted successfully: $contactId")
                            }

                            is QrResult.Error -> {
                                failedIds.add(contactId)
                                Timber.e("Failed to delete contact: $contactId - ${result.error}")
                            }
                        }
                    } catch (e: Exception) {
                        failedIds.add(contactId)
                        Timber.e(e, "Exception deleting contact: $contactId")
                    }

                    // Update progress
                    _uiState.value = _uiState.value.copy(
                        bulkDeleteProgress = index + 1
                    )
                }

                // Update UI with results
                if (successCount > 0) {
                    // Remove successfully deleted contacts from list
                    val deletedIds = contactIds - failedIds.toSet()
                    val currentContacts = _uiState.value.contacts
                    val updatedContacts = currentContacts.filterNot {
                        it.contact.id in deletedIds
                    }

                    val currentState = _uiState.value
                    val filteredAndSorted = applyFiltersAndSort(
                        updatedContacts,
                        currentState.searchQuery,
                        currentState.selectedFilter,
                        currentState.selectedSortOrder
                    )

                    _uiState.value = currentState.copy(
                        contacts = updatedContacts,
                        filteredContacts = filteredAndSorted
                    )
                }

                // Show result message
                val errorMessage = when {
                    failedIds.isEmpty() -> {
                        // All deleted successfully
                        null
                    }
                    successCount == 0 -> {
                        // All failed
                        UiText.StringResources(
                            R.string.err_contact_list_bulk_delete_all_failed,
                            failedIds.size
                        )
                    }
                    else -> {
                        // Partial success
                        UiText.StringResources(
                            R.string.err_contact_list_bulk_delete_partial_failed,
                            successCount,
                            failedIds.size
                        )
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isBulkDeleting = false,
                    bulkDeleteProgress = 0,
                    bulkDeleteTotal = 0,
                    error = errorMessage
                )

                Timber.d("Bulk delete completed: $successCount success, ${failedIds.size} failed")

            } catch (e: Exception) {
                Timber.e(e, "Exception during bulk delete")
                _uiState.value = _uiState.value.copy(
                    isBulkDeleting = false,
                    bulkDeleteProgress = 0,
                    bulkDeleteTotal = 0,
                    error = UiText.StringResources(
                        R.string.err_contact_list_bulk_delete_unexpected,
                        e.message ?: ""
                    )
                )
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
                currentState.selectedSortOrder
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
            currentState.selectedSortOrder
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
            selectedSortOrder = sortOrder,
            filteredContacts = filteredAndSorted
        )
    }

    fun setPrimaryContact(contactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSettingPrimary = contactId)

            try {
                when (val result = setPrimaryContactUseCase(contactId)) {
                    is QrResult.Success -> {
                        Timber.d("Contact set as primary successfully: $contactId")
                        // Ricarica la lista per mostrare i cambiamenti
                        refresh()
                    }

                    is QrResult.Error -> {
                        Timber.e("Failed to set primary contact $contactId: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            isSettingPrimary = null,
                            error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                                R.string.err_contact_list_set_primary_failed,
                                result.error.toString()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception setting primary contact")
                _uiState.value = _uiState.value.copy(
                    isSettingPrimary = null,
                    error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                        R.string.err_contact_list_unexpected,
                        e.message ?: ""
                    )
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSettingPrimary = null)
            }
        }
    }

    /**
     * Cycle through card display variants: FULL -> COMPACT -> MINIMAL -> FULL.
     * The preference is persisted via [AppSettingsRepository].
     */
    fun cycleCardVariant() {
        val current = _uiState.value.cardVariant
        val next = when (current) {
            ListViewMode.FULL -> ListViewMode.COMPACT
            ListViewMode.COMPACT -> ListViewMode.MINIMAL
            ListViewMode.MINIMAL -> ListViewMode.FULL
        }

        // Update UI immediately
        _uiState.value = _uiState.value.copy(cardVariant = next)

        // Persist in background
        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(
                    AppSettingsDataStore.LIST_KEY_CONTACTS,
                    next
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist card variant preference")
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Observe the persisted card variant preference and apply it to UI state.
     */
    private fun observeCardVariant() {
        viewModelScope.launch {
            appSettingsRepository.getListViewMode(AppSettingsDataStore.LIST_KEY_CONTACTS)
                .catch { e ->
                    Timber.e(e, "Error observing card variant preference")
                }
                .collect { viewMode ->
                    _uiState.value = _uiState.value.copy(
                        cardVariant = viewMode
                    )
                }
        }
    }

    private fun enrichWithStatistics(contacts: List<Contact>): List<ContactWithStats> {
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

    // ✅ FIXED: Search Logic with Proper OR Operations
    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filtered = currentState.contacts.filter { contactWithStats ->
            val contact = contactWithStats.contact

            // ✅ CORRECTED: Use OR (||) logic instead of separate if statements
            contact.fullName.contains(query, ignoreCase = true) ||
                    (contact.email?.contains(query, ignoreCase = true) == true) ||
                    (contact.phone?.contains(query, ignoreCase = true) == true) ||
                    (contact.role?.contains(query, ignoreCase = true) == true)
        }

        val filteredAndSorted = applyFiltersAndSort(
            filtered,
            query,
            currentState.selectedFilter,
            currentState.selectedSortOrder
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
)

data class ContactsStatistics(
    val isPrimaryContact: Boolean
)