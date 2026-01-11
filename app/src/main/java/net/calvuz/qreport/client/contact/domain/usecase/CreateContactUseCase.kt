package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.client.domain.usecase.MyCheckClientExistsUseCase
import net.calvuz.qreport.client.contact.domain.validator.ContactDataValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per creazione di un nuovo contatto
 *
 * Gestisce:
 * - Validazione dati contatto
 * - Verifica esistenza cliente
 * - Controllo duplicati email/telefono
 * - Gestione primary contact automatica
 *
 * Business Rules:
 * - Email deve essere unica globalmente
 * - Telefono deve essere unico globalmente
 * - Se è il primo contatto del cliente, diventa automaticamente primary
 * - Solo un contatto primary per cliente
 */
class CreateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val validateContactData: ContactDataValidator,
    private val checkClientExists: MyCheckClientExistsUseCase,
    private val checkEmailUniqueness: CheckEmailUniquenessUseCase,
    private val checkPhoneUniqueness: CheckPhoneUniquenessUseCase,
    private val handlePrimaryContactLogic: HandlePrimaryContactLogicUseCase
) {

    suspend operator fun invoke(contact: Contact): QrResult<Contact, QrError> {
        return try {
            Timber.d("CreateContactUseCase: Starting contact creation for client: ${contact.clientId}")

            // 1. Validazione dati base
            when (val validationResult = validateContactData(contact)) {
                is QrResult.Success -> {
                    // Validation passed, continue
                }
                is QrResult.Error -> {
                    Timber.w("CreateContactUseCase: Contact validation failed: ${validationResult.error}")
                    return QrResult.Error(validationResult.error)
                }
            }

            // 2. Verifica che il cliente esista
            when (val clientCheck = checkClientExists(contact.clientId)) {
                is QrResult.Success -> {
                    if (!clientCheck.data) {
                        Timber.w("CreateContactUseCase: Client does not exist: ${contact.clientId}")
                        return QrResult.Error(QrError.DatabaseError.NotFound(contact.clientId))
                    }
                }
                is QrResult.Error -> {
                    Timber.w("CreateContactUseCase: Client existence check failed: ${clientCheck.error}")
                    return QrResult.Error(clientCheck.error)
                }
            }

            // 3. Controllo duplicati email (se fornita)
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                when (val emailCheck = checkEmailUniqueness.checkEmailIsUnique(email)) {
                    is QrResult.Success -> {
                        // Email is unique, continue
                    }
                    is QrResult.Error -> {
                        Timber.w("CreateContactUseCase: Email uniqueness check failed for $email: ${emailCheck.error}")
                        return QrResult.Error(emailCheck.error)
                    }
                }
            }

            // 4. Controllo duplicati telefono (se fornito)
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                when (val phoneCheck = checkPhoneUniqueness.checkPhoneIsUnique(phone)) {
                    is QrResult.Success -> {
                        // Phone is unique, continue
                    }
                    is QrResult.Error -> {
                        Timber.w("CreateContactUseCase: Phone uniqueness check failed for $phone: ${phoneCheck.error}")
                        return QrResult.Error(phoneCheck.error)
                    }
                }
            }

            // 5. Controllo mobile phone (se fornito)
            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                when (val mobileCheck = checkPhoneUniqueness.checkPhoneIsUnique(mobile)) {
                    is QrResult.Success -> {
                        // Mobile phone is unique, continue
                    }
                    is QrResult.Error -> {
                        Timber.w("CreateContactUseCase: Mobile phone uniqueness check failed for $mobile: ${mobileCheck.error}")
                        return QrResult.Error(mobileCheck.error)
                    }
                }
            }

            // 6. Gestione primary contact automatica
            // NOTE: HandlePrimaryContactLogicUseCase should be converted to return QrResult<Contact, QrError>
            val finalContact = when (val primaryResult = handlePrimaryContactLogic(contact)) {
                is QrResult.Success -> primaryResult.data
                is QrResult.Error -> {
                    Timber.e("CreateContactUseCase: Primary contact logic failed: ${primaryResult.error}")
                    return QrResult.Error(primaryResult.error)
                }
            }

            // 7. Creazione nel repository
            when (val createResult = contactRepository.createContact(finalContact)) {
                is QrResult.Success -> {
                    val createdContact = createResult.data
                    Timber.d("CreateContactUseCase: Contact created successfully: ${createdContact.id}")
                    QrResult.Success(createdContact)
                }

                is QrResult.Error -> {
                    Timber.e("CreateContactUseCase: Repository error creating contact: ${createResult.error}")
                    QrResult.Error(createResult.error)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "CreateContactUseCase: Exception creating contact for client: ${contact.clientId}")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    suspend fun validateContactForCreation(contact: Contact): QrResult<Unit, QrError> {
        return try {
            // Run all validations without actually creating the contact

            // 1. Basic data validation
            when (val validationResult = validateContactData(contact)) {
                is QrResult.Success -> { /* continue */ }
                is QrResult.Error -> return QrResult.Error(validationResult.error)
            }

            // 2. Client existence check
            when (val clientCheck = checkClientExists(contact.clientId)) {
                is QrResult.Success -> {
                    if (!clientCheck.data) {
                        return QrResult.Error(QrError.DatabaseError.NotFound(contact.clientId))
                    }
                }
                is QrResult.Error -> return QrResult.Error(clientCheck.error)
            }

            // 3. Email uniqueness check
            contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                when (val emailCheck = checkEmailUniqueness.checkEmailIsUnique(email)) {
                    is QrResult.Success -> { /* continue */ }
                    is QrResult.Error -> return QrResult.Error(emailCheck.error)
                }
            }

            // 4. Phone uniqueness checks
            contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                when (val phoneCheck = checkPhoneUniqueness.checkPhoneIsUnique(phone)) {
                    is QrResult.Success -> { /* continue */ }
                    is QrResult.Error -> return QrResult.Error(phoneCheck.error)
                }
            }

            contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                when (val mobileCheck = checkPhoneUniqueness.checkPhoneIsUnique(mobile)) {
                    is QrResult.Success -> { /* continue */ }
                    is QrResult.Error -> return QrResult.Error(mobileCheck.error)
                }
            }

            Timber.d("CreateContactUseCase.validateContactForCreation: All validations passed")
            QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "CreateContactUseCase.validateContactForCreation: Exception validating contact")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }
}