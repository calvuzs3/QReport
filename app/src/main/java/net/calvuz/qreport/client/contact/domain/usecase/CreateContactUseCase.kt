package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.domain.validator.ContactDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Creates a new contact after validating data and checking uniqueness.
 *
 * Business rules:
 * - Email must be globally unique.
 * - Phone/mobile must be globally unique.
 * - If the client has no contacts yet, this contact automatically becomes primary.
 * - Only one primary contact per client is allowed.
 *
 * Primary contact logic is handled inline — no separate handler use case needed.
 */
class CreateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val validateContactData: ContactDataValidator,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkEmailUniqueness: CheckEmailUniquenessUseCase,
    private val checkPhoneUniqueness: CheckPhoneUniquenessUseCase
) {
    suspend operator fun invoke(contact: Contact): QrResult<Contact, QrError> {
        return try {
            Timber.d("Creating contact for client: ${contact.clientId}")

            // 1. Validate basic fields
            when (val v = validateContactData(contact)) {
                is QrResult.Error -> return QrResult.Error(v.error)
                is QrResult.Success -> Unit
            }

            // 2. Verify client exists
            when (val c = checkClientExists(contact.clientId)) {
                is QrResult.Error -> return QrResult.Error(c.error)
                is QrResult.Success -> if (!c.data) return QrResult.Error(QrError.ContactsError.MissingClientId())
            }

            // 3. Email uniqueness
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                when (val e = checkEmailUniqueness(email)) {
                    is QrResult.Error -> return QrResult.Error(e.error)
                    is QrResult.Success -> Unit
                }
            }

            // 4. Phone uniqueness
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                when (val p = checkPhoneUniqueness(phone)) {
                    is QrResult.Error -> return QrResult.Error(p.error)
                    is QrResult.Success -> Unit
                }
            }

            // 5. Mobile phone uniqueness
            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                when (val m = checkPhoneUniqueness(mobile)) {
                    is QrResult.Error -> return QrResult.Error(m.error)
                    is QrResult.Success -> Unit
                }
            }

            // 6. Primary contact logic — if client has no primary yet, this becomes primary.
            //    Uses repository.setPrimaryContact so the DAO transaction atomically clears
            //    any stale primary flags before assigning the new one.
            val finalContact = resolvePrimaryForCreate(contact)
                ?: return QrResult.Error(QrError.ContactsError.LoadError())

            // 7. Persist
            when (val result = contactRepository.createContact(finalContact)) {
                is QrResult.Success -> {
                    Timber.d("Contact created: ${result.data.id} isPrimary=${result.data.isPrimary}")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to create contact: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, contact.clientId)
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }

    /**
     * Returns the contact to persist, with [Contact.isPrimary] set correctly.
     * If the client has no primary contact yet, forces isPrimary = true.
     * Returns null only on repository failure.
     */
    private suspend fun resolvePrimaryForCreate(contact: Contact): Contact? {
        if (contact.isPrimary) return contact  // caller explicitly set it

        val hasPrimary = when (val r = contactRepository.hasPrimaryContact(contact.clientId)) {
            is QrResult.Success -> r.data
            is QrResult.Error -> {
                Timber.e("Failed to check primary status for client ${contact.clientId}: ${r.error}")
                return null
            }
        }
        return if (hasPrimary) contact else contact.copy(isPrimary = true)
    }
}