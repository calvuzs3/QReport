package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject


/**
 * Controllo univocità email globale
 */
class CheckEmailUniquenessUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    suspend operator fun invoke(
        email: String,
        excludeContactId: String = ""
    ): QrResult<Unit, QrError> {
        return try {
            // Validazione input
            if (email.isBlank()) {
                Timber.w("CheckEmailUniquenessUseCase: email is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(email.toString()))
            }

            Timber.d("CheckEmailUniquenessUseCase: Checking email uniqueness: $email (exclude: $excludeContactId)")

            // Controllo univocità tramite repository
            when (val result = contactRepository.isEmailTaken(email, excludeContactId)) {
                is QrResult.Success -> {
                    val isTaken = result.data
                    if (isTaken) {
                        Timber.w("CheckEmailUniquenessUseCase: Email already taken: $email")
                        QrResult.Error(QrError.ValidationError.DuplicateEntry(email))
                    } else {
                        Timber.d("CheckEmailUniquenessUseCase: Email is unique: $email")
                        QrResult.Success(Unit)
                    }
                }

                is QrResult.Error -> {
                    Timber.e("CheckEmailUniquenessUseCase: Repository error checking email $email: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "CheckEmailUniquenessUseCase: Exception checking email uniqueness: $email")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    suspend fun checkEmailIsUnique(email: String): QrResult<Unit, QrError> {
        return invoke(email, "")
    }
}