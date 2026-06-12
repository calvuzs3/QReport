package net.calvuz.qreport.client.contact.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.toUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.ObserveClientsUseCase
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.usecase.DeleteContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.ObserveContactsUseCase
import net.calvuz.qreport.client.contact.domain.usecase.RestoreContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.SetPrimaryContactUseCase
import net.calvuz.qreport.client.contact.presentation.model.ContactFilter
import net.calvuz.qreport.client.contact.presentation.model.ContactPkg
import net.calvuz.qreport.client.contact.presentation.model.ContactSortOrder
import net.calvuz.qreport.client.facility.presentation.model.ClientOption
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import timber.log.Timber
import javax.inject.Inject

/** ContactListScreen UiState */
data class ContactListUiState(
    val clientId: String = "",
    val contacts: List<ContactWithStats> = emptyList(),
    val filteredContacts: List<ContactWithStats> = emptyList(),
    // states
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeletingContact: String? = null,
    val isRestoringContact: String? = null,
    val isSettingPrimary: String? = null,
    val isBulkDeleting: Boolean = false,
    val bulkDeleteProgress: Int = 0,
    val bulkDeleteTotal: Int = 0,
    // search and filter
    val searchQuery: String = "",
    val selectedFilter: ContactFilter = ContactPkg.selectedFilter,
    val selectedSortOrder: ContactSortOrder = ContactPkg.selectedSortOrder,
    // client selector
    val availableClients: List<ClientOption> = listOf(ClientOption.ALL),
    val selectedClient: ClientOption = ClientOption.ALL,
    // errors
    val error: UiText? = null,
    // Card Variant
    val cardVariant: ListViewMode = ListViewMode.FULL
)


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
    private val observeContactsUseCase: ObserveContactsUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val restoreContactUseCase: RestoreContactUseCase,
    private val setPrimaryContactUseCase: SetPrimaryContactUseCase,
    private val observeClientsUseCase: ObserveClientsUseCase,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    // Cancels previous collector before starting a new one (client switch)
    private var loadJob: kotlinx.coroutines.Job? = null

    private var currentClientId: String = ""

    init {
        observeCardVariant()
        loadClientsForDropdown()
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    /** Load all contacts regardless of client — called when no clientId is provided. */
    fun initialize() {
        loadContacts()
    }

    fun initializeForClient(clientId: String) {
        if (clientId == currentClientId) return
        currentClientId = clientId
        _uiState.value = _uiState.value.copy(
            clientId = clientId,
            selectedClient = _uiState.value.availableClients.find { it.id == clientId }
                ?: ClientOption.ALL)
        loadContacts()
    }

    // ============================================================
    // PRIVATE — FLOW OBSERVATION
    // ============================================================

    /**
     * Cancels any previous collector and starts a new one.
     * [clientId] null → all contacts; non-null → filtered by client.
     */
    private fun loadContacts() {
        loadJob?.cancel()

        val clientId = currentClientId.takeIf { it.isNotEmpty() }
        Timber.d("Observing contacts for client ${clientId ?: "all"}")

        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !_uiState.value.isRefreshing, error = null
            )
            try {
                observeContactsUseCase(clientId).catch { e ->
                    Timber.d("Error in contacts flow")
                    if (e is kotlinx.coroutines.CancellationException) throw e

                    if (currentCoroutineContext().isActive) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResource(R.string.err_facility_load)
                            )
                        }
                    }
                }.collect { contacts ->
                    if (!currentCoroutineContext().isActive) return@collect
                    val enriched = enrichWithStatistics(contacts)
                    val current = _uiState.value
                    _uiState.value = current.copy(
                        contacts = enriched, filteredContacts = applyFiltersAndSort(
                            enriched,
                            current.searchQuery,
                            current.selectedFilter,
                            current.selectedSortOrder
                        ), isLoading = false, isRefreshing = false, error = null
                    )
                    Timber.d("Contacts flow: ${contacts.size} records (clientId=${clientId ?: "all"})")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.d("Contacts observation cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error observing contacts")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResource(R.string.err_contact_list_unexpected)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            delay(500)
            // Future: call remote sync here before restarting the observer
            loadContacts()
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingContact = contactId) }

            try {
                Timber.d("Deleting contact $contactId")

                when (val result = deleteContactUseCase(contactId)) {
                    is QrResult.Success -> {
                        Timber.d("Successfully deleted contact $contactId")
                        // List updates automatically via Flow
                    }

                    is QrResult.Error -> {
                        Timber.e("Failed to delete contact $contactId")
                        _uiState.update {
                            it.copy(
                                error = result.error.toUiText()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception deleting contact")
                _uiState.update {
                    it.copy(
                        error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                            R.string.err_contact_list_delete_unexpected, e.message ?: ""
                        )
                    )
                }
            }.also { _uiState.value = _uiState.value.copy(isDeletingContact = null) }
        }
    }

    fun restoreContact(contactId: String) {

        viewModelScope.launch {
            _uiState.update { it.copy(isRestoringContact = contactId) }
            try {

                when (val result = restoreContactUseCase(contactId)) {
                    is QrResult.Success -> {
                        Timber.d("Successfully restored contact $contactId")
                        // List updates automatically via observeContactsUseCase Flow
                    }

                    is QrResult.Error -> {
                        Timber.e("Failed to restore contact $contactId")
                        _uiState.update {
                            it.copy(
                                isRestoringContact = null,
                                error = UiText.StringResources(R.string.err_contact_restore)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception restoring contact")

            }.also {
                _uiState.update { it.copy(isRestoringContact = null )}
            }
        }
    }

    // ===== NEW: Bulk Delete Implementation =====
    fun bulkDeleteContacts(contactIds: List<String>) {
        if (contactIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBulkDeleting = true, bulkDeleteProgress = 0, bulkDeleteTotal = contactIds.size
                )
            }

            try {
                Timber.d("Starting bulk delete for ${contactIds.size} contacts")

                var successCount = 0
                var failedIds = mutableListOf<String>()

                contactIds.forEachIndexed { index, contactId ->
                    try {
                        when (val result = deleteContactUseCase(contactId)) {
                            is QrResult.Success -> {
                                successCount++
                                Timber.d("Successfully deleted contact $contactId")
                            }

                            is QrResult.Error -> {
                                failedIds.add(contactId)
                                Timber.e("Failed to delete contact $contactId - ${result.error}")
                            }
                        }
                    } catch (e: Exception) {
                        failedIds.add(contactId)
                        Timber.e(e, "Exception deleting contact: $contactId")
                    }

                    // Update progress
                    _uiState.update {
                        it.copy(
                            bulkDeleteProgress = index + 1
                        )
                    }
                }

                // Update UI with results
                if (successCount > 0) {
                    // List updates automatically via Flow
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
                            R.string.err_contact_list_bulk_delete_all_failed, failedIds.size
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

                _uiState.update {
                    it.copy(
                        error = errorMessage
                    )
                }

                Timber.d("Bulk delete completed: $successCount success, ${failedIds.size} failed")

            } catch (e: Exception) {
                Timber.e(e, "Exception during bulk delete")
                _uiState.update {
                    it.copy(
                        error = UiText.StringResources(
                            R.string.err_contact_list_bulk_delete_unexpected, e.message ?: "")
                        )
                }
            }.also {
                _uiState.update {
                    it.copy(
                        isBulkDeleting = false,
                        bulkDeleteProgress = 0,
                        bulkDeleteTotal = 0
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value

        if (query.length >= 3) {
            performSearch(query)
        } else {
            _uiState.value = currentState.copy(
                searchQuery = query, filteredContacts = applyFiltersAndSort(
                    currentState.contacts,
                    query,
                    currentState.selectedFilter,
                    currentState.selectedSortOrder
                )
            )
        }
    }

    fun updateFilter(filter: ContactFilter) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.contacts, currentState.searchQuery, filter, currentState.selectedSortOrder
        )

        _uiState.value = currentState.copy(
            selectedFilter = filter, filteredContacts = filteredAndSorted
        )
    }

    fun updateSortOrder(sortOrder: ContactSortOrder) {
        val currentState = _uiState.value
        val filteredAndSorted = applyFiltersAndSort(
            currentState.contacts, currentState.searchQuery, currentState.selectedFilter, sortOrder
        )

        _uiState.value = currentState.copy(
            selectedSortOrder = sortOrder, filteredContacts = filteredAndSorted
        )
    }

    fun updateSelectedClient(client: ClientOption) {
        if (client == _uiState.value.selectedClient) return
        currentClientId = client.id
        _uiState.update { it.copy(selectedClient = client, clientId = client.id) }
        loadContacts()
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
                _uiState.update { it.copy(
                    isSettingPrimary = null,
                    error = UiText.StringResources(  // ✅ Fixed StringResources → StringResource
                        R.string.err_contact_list_unexpected, e.message ?: ""
                    )
                ) }
            }.also {
                _uiState.update { it.copy(isSettingPrimary = null) }
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
        _uiState.update { it.copy(cardVariant = next) }
        viewModelScope.launch {
            try {
                appSettingsRepository.setListViewMode(
                    AppSettingsDataStore.LIST_KEY_CONTACTS, next
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist card variant preference")
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
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
                }.collect { viewMode ->
                    _uiState.value = _uiState.value.copy(cardVariant = viewMode)
                }
        }
    }

    private fun loadClientsForDropdown() {
        viewModelScope.launch {
            try {
                observeClientsUseCase().catch { e ->
                    Timber.e(
                        e, "Error loading clients for dropdown"
                    )
                }.collect { clients ->
                    val options = listOf(ClientOption.ALL) + clients.map { c ->
                        ClientOption(id = c.id, companyName = c.companyName)
                    }
                    _uiState.value = _uiState.value.let { state ->
                        val synced = options.find { it.id == state.clientId } ?: ClientOption.ALL
                        state.copy(availableClients = options, selectedClient = synced)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start client observation")
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
            ContactSortOrder.NAME -> filtered.sortedWith(compareByDescending<ContactWithStats> { it.contact.isPrimary }.thenBy { it.contact.fullName.lowercase() })

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
            contact.fullName.contains(query, ignoreCase = true) || (contact.email?.contains(
                query, ignoreCase = true
            ) == true) || (contact.phone?.contains(
                query, ignoreCase = true
            ) == true) || (contact.role?.contains(query, ignoreCase = true) == true)
        }

        val filteredAndSorted = applyFiltersAndSort(
            filtered, query, currentState.selectedFilter, currentState.selectedSortOrder
        )

        _uiState.value = currentState.copy(
            searchQuery = query, filteredContacts = filteredAndSorted
        )
    }
}

// ============================================================
// DATA CLASSES
// ============================================================

data class ContactWithStats(
    val contact: Contact, val stats: ContactsStatistics
)

data class ContactsStatistics(
    val isPrimaryContact: Boolean
)