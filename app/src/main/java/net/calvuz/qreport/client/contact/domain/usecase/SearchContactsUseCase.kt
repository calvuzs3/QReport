package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Search Use Case
 * 
 * Updated to use QrResult<T, QrError> pattern for all methods
 */
class SearchContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
) {

    /**
     * Search by text query
     *
     * Search in: nome, cognome, email, telefono, ruolo, azienda
     *
     * @param query Search text query
     * @return QrResult.Success ontact list, QrResult.Error in case of errors
     */
    suspend operator fun invoke(query: String): QrResult<List<Contact>, QrError> {
        return try {

            Timber.d("Searching contacts with query: $query")

            // Validate input
            if (query.isBlank()) {
                Timber.w("query is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField())
            }

            if (query.length < 2) {
                Timber.w("query too short: ${query.length}")
                return QrResult.Error(QrError.ValidationError.EmptyField())
            }

            // Search
            when (val result = contactRepository.searchContacts(query.trim())) {
                is QrResult.Success -> {
                    val contacts = result.data

                    // Order
                    val sortedContacts = contacts.sortedWith(
                        compareBy<Contact> { !it.isPrimary } // Primary prima
                            .thenBy { contact ->
                                // Poi per relevanza nome/email
                                when {
                                    contact.firstName.equals(query.trim(), ignoreCase = true) -> 0
                                    contact.lastName?.equals(
                                        query.trim(),
                                        ignoreCase = true
                                    ) == true -> 1

                                    contact.email?.equals(
                                        query.trim(),
                                        ignoreCase = true
                                    ) == true -> 2

                                    contact.firstName.startsWith(
                                        query.trim(),
                                        ignoreCase = true
                                    ) -> 3

                                    else -> 4
                                }
                            }
                            .thenBy { it.firstName.lowercase() }
                    )

                    Timber.d("Found ${sortedContacts.size} contacts for query: $query")
                    QrResult.Success(sortedContacts)
                }

                is QrResult.Error -> {
                    Timber.d("Repository error for query '$query': ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, query)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }
}