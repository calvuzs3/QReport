package net.calvuz.qreport.client.contact.domain.usecase

import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.domain.validator.ContactDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per aggiornamento di un contatto esistente
 *
 * Gestisce:
 * - Validazione esistenza contatto
 * - Validazione dati aggiornati
 * - Controllo duplicati escludendo il contatto corrente
 * - Gestione cambio primary contact
 * - Aggiornamento timestamp
 *
 * Updated to use QrResult<Contact, QrError> pattern
 */
class UpdateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase,
    private val checkEmailUniqueness: CheckEmailUniquenessUseCase,
    private val checkPhoneUniqueness: CheckPhoneUniquenessUseCase,
    private val validateContactData: ContactDataValidator,
    private val handlePrimaryContactUpdate: HandlePrimaryContactUpdateUseCase
) {

    /**
     * Aggiorna un contatto esistente
     *
     * @param contact Contatto con dati aggiornati (deve avere ID esistente)
     * @return QrResult.Success con Contact aggiornato, QrResult.Error per errori
     */
    suspend operator fun invoke(contact: Contact): QrResult<Contact, QrError> {
        return try {
            Timber.d("UpdateContactUseCase: Starting contact update: ${contact.id}")

            // 1. Validazione esistenza contatto
            val originalContact = when (val contactResult = checkContactExists(contact.id)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("UpdateContactUseCase: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            // 2. Validazione dati aggiornati
            when (val validationResult = validateContactData(contact)) {
                is QrResult.Success -> {
                    // Validation passed, continue
                }
                is QrResult.Error -> {
                    Timber.w("UpdateContactUseCase: Contact validation failed: ${validationResult.error}")
                    return QrResult.Error(validationResult.error)
                }
            }

            // 3. Controllo duplicati email (se cambiata e fornita)
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                if (email != originalContact.email) {
                    when (val emailCheck = checkEmailUniqueness(email, contact.id)) {
                        is QrResult.Success -> {
                            // Email is unique, continue
                        }
                        is QrResult.Error -> {
                            Timber.w("UpdateContactUseCase: Email uniqueness check failed for $email: ${emailCheck.error}")
                            return QrResult.Error(emailCheck.error)
                        }
                    }
                }
            }

            // 4. Controllo duplicati telefono (se cambiato e fornito)
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                if (phone != originalContact.phone) {
                    when (val phoneCheck = checkPhoneUniqueness(phone, contact.id)) {
                        is QrResult.Success -> {
                            // Phone is unique, continue
                        }
                        is QrResult.Error -> {
                            Timber.w("UpdateContactUseCase: Phone uniqueness check failed for $phone: ${phoneCheck.error}")
                            return QrResult.Error(phoneCheck.error)
                        }
                    }
                }
            }

            // 5. Controllo mobile phone (se cambiato e fornito)
            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                if (mobile != originalContact.mobilePhone) {
                    when (val mobileCheck = checkPhoneUniqueness(mobile, contact.id)) {
                        is QrResult.Success -> {
                            // Mobile phone is unique, continue
                        }
                        is QrResult.Error -> {
                            Timber.w("UpdateContactUseCase: Mobile phone uniqueness check failed for $mobile: ${mobileCheck.error}")
                            return QrResult.Error(mobileCheck.error)
                        }
                    }
                }
            }

            // 6. Gestione cambio primary contact
            val finalContact = when (val primaryResult = handlePrimaryContactUpdate(originalContact, contact)) {
                is QrResult.Success -> primaryResult.data
                is QrResult.Error -> {
                    Timber.e("UpdateContactUseCase: Primary contact update handling failed: ${primaryResult.error}")
                    return QrResult.Error(primaryResult.error)
                }
            }

            // 7. Aggiornamento con timestamp corrente
            val updatedContact = finalContact.copy(
                updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )

            when (val updateResult = contactRepository.updateContact(updatedContact)) {
                is QrResult.Success -> {
                    val savedContact = updateResult.data
                    Timber.d("UpdateContactUseCase: Contact updated successfully: ${savedContact.id}")
                    QrResult.Success(savedContact)
                }

                is QrResult.Error -> {
                    Timber.e("UpdateContactUseCase: Repository error updating contact ${contact.id}: ${updateResult.error}")
                    QrResult.Error(updateResult.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "UpdateContactUseCase: Exception updating contact: ${contact.id}")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Valida un contatto per aggiornamento senza salvarlo
     *
     * @param contact Contatto da validare
     * @return QrResult.Success se validazione passa, QrResult.Error per errori
     */
    suspend fun validateContactForUpdate(contact: Contact): QrResult<Unit, QrError> {
        return try {
            Timber.d("UpdateContactUseCase.validateContactForUpdate: Validating contact for update: ${contact.id}")

            // 1. Validazione esistenza contatto
            val originalContact = when (val contactResult = checkContactExists(contact.id)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("UpdateContactUseCase.validateContactForUpdate: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            // 2. Validazione dati aggiornati
            when (val validationResult = validateContactData(contact)) {
                is QrResult.Success -> {
                    // Validation passed, continue
                }
                is QrResult.Error -> {
                    Timber.w("UpdateContactUseCase.validateContactForUpdate: Contact validation failed: ${validationResult.error}")
                    return QrResult.Error(validationResult.error)
                }
            }

            // 3. Controllo duplicati email (se cambiata)
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                if (email != originalContact.email) {
                    when (val emailCheck = checkEmailUniqueness(email, contact.id)) {
                        is QrResult.Success -> {
                            // Email is unique, continue
                        }
                        is QrResult.Error -> {
                            Timber.w("UpdateContactUseCase.validateContactForUpdate: Email uniqueness check failed: ${emailCheck.error}")
                            return QrResult.Error(emailCheck.error)
                        }
                    }
                }
            }

            // 4. Controllo duplicati telefono (se cambiato)
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                if (phone != originalContact.phone) {
                    when (val phoneCheck = checkPhoneUniqueness(phone, contact.id)) {
                        is QrResult.Success -> {
                            // Phone is unique, continue
                        }
                        is QrResult.Error -> {
                            Timber.w("UpdateContactUseCase.validateContactForUpdate: Phone uniqueness check failed: ${phoneCheck.error}")
                            return QrResult.Error(phoneCheck.error)
                        }
                    }
                }
            }

            // 5. Controllo mobile phone (se cambiato)
            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                if (mobile != originalContact.mobilePhone) {
                    when (val mobileCheck = checkPhoneUniqueness(mobile, contact.id)) {
                        is QrResult.Success -> {
                            // Mobile phone is unique, continue
                        }
                        is QrResult.Error -> {
                            Timber.w("UpdateContactUseCase.validateContactForUpdate: Mobile phone uniqueness check failed: ${mobileCheck.error}")
                            return QrResult.Error(mobileCheck.error)
                        }
                    }
                }
            }

            // 6. Validazione cambio primary contact (senza applicarlo)
            when (val primaryResult = handlePrimaryContactUpdate(originalContact, contact)) {
                is QrResult.Success -> {
                    // Primary contact logic validation passed
                }
                is QrResult.Error -> {
                    Timber.w("UpdateContactUseCase.validateContactForUpdate: Primary contact update validation failed: ${primaryResult.error}")
                    return QrResult.Error(primaryResult.error)
                }
            }

            Timber.d("UpdateContactUseCase.validateContactForUpdate: All validations passed for contact: ${contact.id}")
            QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "UpdateContactUseCase.validateContactForUpdate: Exception validating contact: ${contact.id}")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Aggiorna parzialmente un contatto (solo i campi specificati)
     *
     * @param contactId ID del contatto da aggiornare
     * @param updates Map con i campi da aggiornare
     * @return QrResult.Success con Contact aggiornato, QrResult.Error per errori
     */
    suspend fun partialUpdate(contactId: String, updates: Map<String, Any?>): QrResult<Contact, QrError> {
        return try {
            Timber.d("UpdateContactUseCase.partialUpdate: Starting partial update for contact: $contactId")

            // 1. Validazione input
            if (contactId.isBlank()) {
                Timber.w("UpdateContactUseCase.partialUpdate: contactId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(contactId.toString()))
            }

            if (updates.isEmpty()) {
                Timber.w("UpdateContactUseCase.partialUpdate: No updates provided")
                return QrResult.Error(QrError.ValidationError.EmptyField(updates.toString()))
            }

            // 2. Ottenere contatto esistente
            val existingContact = when (val contactResult = checkContactExists(contactId)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> {
                    Timber.w("UpdateContactUseCase.partialUpdate: Contact existence check failed: ${contactResult.error}")
                    return QrResult.Error(contactResult.error)
                }
            }

            // 3. Applicare aggiornamenti parziali
            val updatedContact = applyPartialUpdates(existingContact, updates)

            // 4. Validare e aggiornare usando il metodo principale
            invoke(updatedContact)

        } catch (e: Exception) {
            Timber.e(e, "UpdateContactUseCase.partialUpdate: Exception in partial update for contact: $contactId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Applica aggiornamenti parziali al contatto
     */
    private fun applyPartialUpdates(contact: Contact, updates: Map<String, Any?>): Contact {
        var updatedContact = contact

        updates.forEach { (field, value) ->
            updatedContact = when (field.lowercase()) {
                "firstname" -> updatedContact.copy(firstName = value as? String ?: contact.firstName)
                "lastname" -> updatedContact.copy(lastName = value as? String)
                "email" -> updatedContact.copy(email = value as? String)
                "phone" -> updatedContact.copy(phone = value as? String)
                "mobilephone" -> updatedContact.copy(mobilePhone = value as? String)
                "role" -> updatedContact.copy(role = value as? String)
                "department" -> updatedContact.copy(department = value as? String)
                "isprimary" -> updatedContact.copy(isPrimary = value as? Boolean ?: contact.isPrimary)
                "preferredcontactmethod" -> updatedContact.copy(preferredContactMethod = value as? net.calvuz.qreport.client.contact.domain.model.ContactMethod)
                "notes" -> updatedContact.copy(notes = value as? String)
                "isactive" -> updatedContact.copy(isActive = value as? Boolean ?: contact.isActive)
                else -> {
                    Timber.w("UpdateContactUseCase.applyPartialUpdates: Unknown field for partial update: $field")
                    updatedContact
                }
            }
        }

        return updatedContact
    }

    /**
     * Verifica se un contatto ha cambiamenti rispetto all'originale
     *
     * @param original Contatto originale
     * @param updated Contatto con potenziali modifiche
     * @return QrResult.Success con Boolean (true se ci sono cambiamenti), QrResult.Error per errori
     */
    suspend fun hasChanges(original: Contact, updated: Contact): QrResult<Boolean, QrError> {
        return try {
            if (original.id != updated.id) {
                Timber.w("UpdateContactUseCase.hasChanges: Contact IDs don't match")
                return QrResult.Error(QrError.ValidationError.EmptyField(original.id))
            }

            // Confronta tutti i campi principali (escludendo timestamp)
            val hasChanges = original.firstName != updated.firstName ||
                    original.lastName != updated.lastName ||
                    original.email != updated.email ||
                    original.phone != updated.phone ||
                    original.mobilePhone != updated.mobilePhone ||
                    original.role != updated.role ||
                    original.department != updated.department ||
                    original.isPrimary != updated.isPrimary ||
                    original.preferredContactMethod != updated.preferredContactMethod ||
                    original.notes != updated.notes ||
                    original.isActive != updated.isActive

            Timber.d("UpdateContactUseCase.hasChanges: Contact ${original.id} ${if (hasChanges) "has" else "has no"} changes")
            QrResult.Success(hasChanges)

        } catch (e: Exception) {
            Timber.e(e, "UpdateContactUseCase.hasChanges: Exception checking changes for contact: ${original.id}")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}