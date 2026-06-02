package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject


/**
 * Checks global email uniqueness.
 */
class CheckEmailUniquenessUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    suspend operator fun invoke(
        email: String,
        excludeContactId: String = ""
    ): QrResult<Unit, QrError> {
        return try {

            // Validate input
            if (email.isBlank()) {
                Timber.w("Email is blank")
                return QrResult.Error(QrError.ContactsError.ValidationError.InvalidEmail())
            }

            // Check uniqueness via repository
            when (val result = contactRepository.isEmailTaken(email, excludeContactId)) {
                is QrResult.Success -> {
                    val isTaken = result.data
                    if (isTaken) {
                        Timber.w("Email already taken: $email")
                        QrResult.Error(QrError.ValidationError.EmailAlreadyTaken(email))
                    } else {
                        Timber.d("Email is unique: $email")
                        QrResult.Success(Unit)
                    }
                }

                is QrResult.Error -> {
                    Timber.e("Repository error checking email $email: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception checking email uniqueness: $email")
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }
}