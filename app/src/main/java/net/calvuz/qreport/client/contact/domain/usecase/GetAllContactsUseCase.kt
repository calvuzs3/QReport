package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns all contacts across all clients — active and inactive.
 * Used by ContactListScreen when no client filter is selected.
 */
class GetAllContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(): QrResult<List<Contact>, QrError> {
        return try {
            when (val result = contactRepository.getContacts()) {
                is QrResult.Success -> {
                    val sorted = result.data.sortedWith(
                        compareBy<Contact> { !it.isPrimary }
                            .thenBy { it.firstName.lowercase() }
                            .thenBy { it.lastName?.lowercase() ?: "" }
                    )
                    Timber.d("Retrieved ${sorted.size} contacts (all clients)")
                    QrResult.Success(sorted)
                }
                is QrResult.Error -> QrResult.Error(result.error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all contacts")
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }
}