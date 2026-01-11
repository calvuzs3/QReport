package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import timber.log.Timber
import javax.inject.Inject

class CheckPhoneUniquenessUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    suspend operator fun invoke(
        phone: String,
        excludeContactId: String = ""
    ): QrResult<Unit, QrError> {
        return try {
            // Validazione input
            if (phone.isBlank()) {
                Timber.w("CheckPhoneUniquenessUseCase: phone is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(phone.toString()))
            }

            Timber.d("CheckPhoneUniquenessUseCase: Checking phone uniqueness: $phone (exclude: $excludeContactId)")

            // Controllo univocità tramite repository
            when (val result = contactRepository.isPhoneTaken(phone, excludeContactId)) {
                is QrResult.Success -> {
                    val isTaken = result.data
                    if (isTaken) {
                        Timber.w("CheckPhoneUniquenessUseCase: Phone already taken: $phone")
                        QrResult.Error(QrError.ValidationError.DuplicateEntry(phone))
                    } else {
                        Timber.d("CheckPhoneUniquenessUseCase: Phone is unique: $phone")
                        QrResult.Success(Unit)
                    }
                }

                is QrResult.Error -> {
                    Timber.e("CheckPhoneUniquenessUseCase: Repository error checking phone $phone: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "CheckPhoneUniquenessUseCase: Exception checking phone uniqueness: $phone")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    suspend fun checkPhoneIsUnique(phone: String): QrResult<Unit, QrError> {
        return invoke(phone, "")
    }
}