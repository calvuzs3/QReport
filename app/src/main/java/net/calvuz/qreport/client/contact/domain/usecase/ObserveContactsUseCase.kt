package net.calvuz.qreport.client.contact.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Reactive Flow of contacts, optionally filtered by client.
 *
 * - [clientId] null or blank → all contacts across all clients (active + inactive)
 * - [clientId] provided     → all contacts for that client (active + inactive)
 *
 * Status filtering (ACTIVE / INACTIVE / ALL) is responsibility of the
 * presentation layer (ContactListViewModel.applyFiltersAndSort).
 *
 * Sort order: primary first, then alphabetical by first/last name.
 */
class ObserveContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(clientId: String? = null): Flow<List<Contact>> {

        Timber.v("Observing contacts clientId=${clientId ?: "all"}")

        val flow = if (clientId.isNullOrBlank()) {
            contactRepository.getContactsFlow()          // all clients, active + inactive
        } else {
            contactRepository.getContactsByClientFlow(clientId)   // one client, active + inactive
        }

        return flow.map { contacts ->
            contacts.sortedWith(
                compareByDescending<Contact> { it.isPrimary }
                    .thenBy { it.firstName.lowercase() }
                    .thenBy { it.lastName?.lowercase() ?: "" }
            )
        }
    }
}