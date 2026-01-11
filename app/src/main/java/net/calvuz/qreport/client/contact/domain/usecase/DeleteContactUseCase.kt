package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per eliminazione di un contatto
 *
 * Gestisce:
 * - Validazione esistenza contatto
 * - Gestione eliminazione primary contact
 * - Assegnazione automatica nuovo primary
 * - Eliminazione sicura (soft delete)
 *
 * Business Rules:
 * - Se si elimina l'ultimo contatto del cliente, viene permesso
 * - Se si elimina un primary contact e ci sono altri contatti, il primo diventa primary
 * - Eliminazione soft per mantenere tracciabilità
 *
 * Updated to use QrResult<Unit, QrError> pattern
 */
class DeleteContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase,
    // NOTE: HandlePrimaryContactDeletionUseCase should also be converted to QrResult
    private val handlePrimaryContactDeletion: HandlePrimaryContactDeletionUseCase
) {

    /**
     * Elimina un contatto
     *
     * @param contactId ID del contatto da eliminare
     * @return QrResult.Success se eliminazione completata, QrResult.Error per errori
     */
    suspend operator fun invoke(contactId: String): QrResult<Unit, QrError> {
        return try {
            // 1. Validazione input
            if (contactId.isBlank()) {
                Timber.w("DeleteContactUseCase: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            Timber.d("DeleteContactUseCase: Starting contact deletion: $contactId")

            // 2. Verificare che il contatto esista
            val contact = when (val contactResult = checkContactExists(contactId)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("DeleteContactUseCase: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            // 3. Gestire primary contact se necessario
            if (contact.isPrimary) {
                Timber.d("DeleteContactUseCase: Handling primary contact deletion for: $contactId")
                when (val primaryDeletionResult = handlePrimaryContactDeletion(contact)) {
                    is QrResult.Success -> {
                        // Primary contact handling completed
                    }
                    is QrResult.Error -> {
                        Timber.e("DeleteContactUseCase: Primary contact deletion handling failed: ${primaryDeletionResult.error}")
                        return QrResult.Error(primaryDeletionResult.error)
                    }
                }
            }

            // 4. Eliminazione contatto (soft delete)
            when (val deleteResult = contactRepository.deleteContact(contactId)) {
                is QrResult.Success -> {
                    Timber.d("DeleteContactUseCase: Contact deleted successfully: $contactId")
                    QrResult.Success(Unit)
                }

                is QrResult.Error -> {
                    Timber.e("DeleteContactUseCase: Repository error deleting contact $contactId: ${deleteResult.error}")
                    QrResult.Error(deleteResult.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "DeleteContactUseCase: Exception deleting contact: $contactId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Ottiene informazioni sul contatto che diventerà primary se si elimina il contatto specificato
     *
     * @param contactId ID del contatto da eliminare
     * @return QrResult.Success con nuovo contatto primary (null se non ce ne sarà uno), QrResult.Error per errori
     */
    suspend fun getNewPrimaryContactAfterDeletion(contactId: String): QrResult<Contact?, QrError> {
        return try {
            // Validazione input
            if (contactId.isBlank()) {
                Timber.w("DeleteContactUseCase.getNewPrimaryContactAfterDeletion: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            Timber.d("DeleteContactUseCase.getNewPrimaryContactAfterDeletion: Checking new primary for: $contactId")

            // Verificare che il contatto esista
            val contact = when (val contactResult = checkContactExists(contactId)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("DeleteContactUseCase.getNewPrimaryContactAfterDeletion: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            // Se non è primary, niente cambia
            if (!contact.isPrimary) {
                Timber.d("DeleteContactUseCase.getNewPrimaryContactAfterDeletion: Contact is not primary, no changes needed")
                return QrResult.Success(null)
            }

            // Ottieni altri contatti del cliente
            val clientContacts = when (val contactsResult = contactRepository.getContactsByClient(contact.clientId)) {
                is QrResult.Success -> contactsResult.data
                is QrResult.Error -> {
                    Timber.e("DeleteContactUseCase.getNewPrimaryContactAfterDeletion: Error getting client contacts: ${contactsResult.error}")
                    return QrResult.Error(contactsResult.error)
                }
            }

            val otherActiveContacts = clientContacts.filter {
                it.id != contact.id && it.isActive
            }

            if (otherActiveContacts.isEmpty()) {
                Timber.d("DeleteContactUseCase.getNewPrimaryContactAfterDeletion: No other active contacts, no new primary")
                QrResult.Success(null) // Nessun altro contatto
            } else {
                // Ritorna il contatto che diventerà primary (ordinato per nome)
                val newPrimary = otherActiveContacts.sortedWith(
                    compareBy<Contact> { it.firstName.lowercase() }
                        .thenBy { it.lastName?.lowercase() ?: "" }
                ).first()

                Timber.d("DeleteContactUseCase.getNewPrimaryContactAfterDeletion: New primary contact will be: ${newPrimary.id}")
                QrResult.Success(newPrimary)
            }
        } catch (e: Exception) {
            Timber.e(e, "DeleteContactUseCase.getNewPrimaryContactAfterDeletion: Exception checking new primary for: $contactId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Helper method per verificare se un contatto può essere eliminato
     */
    suspend fun canDeleteContact(contactId: String): QrResult<Boolean, QrError> {
        return try {
            if (contactId.isBlank()) {
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            // Verifica che il contatto esista
            when (val contactResult = checkContactExists(contactId)) {
                is QrResult.Success -> {
                    // Contact exists and is active, can be deleted
                    Timber.d("DeleteContactUseCase.canDeleteContact: Contact can be deleted: $contactId")
                    QrResult.Success(true)
                }

                is QrResult.Error -> {
                    // Contact doesn't exist or other error
                    when (contactResult.error) {
                        is QrError.DatabaseError.NotFound -> {
                            Timber.d("DeleteContactUseCase.canDeleteContact: Contact not found: $contactId")
                            QrResult.Success(false) // Cannot delete what doesn't exist
                        }
                        else -> {
                            Timber.e("DeleteContactUseCase.canDeleteContact: Error checking contact: ${contactResult.error}")
                            QrResult.Error(contactResult.error)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "DeleteContactUseCase.canDeleteContact: Exception checking if contact can be deleted: $contactId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}