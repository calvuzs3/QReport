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
            Timber.d("Checking phone uniqueness: $phone (exclude: $excludeContactId)")

            // Validate input
            if (phone.isBlank()) {
                Timber.w("phone is blank")
                return QrResult.Error(QrError.ContactsError.ValidationError.InvalidPhone())
            }

            // Check uniqueness via repository
            when (val result = contactRepository.isPhoneTaken(phone, excludeContactId)) {
                is QrResult.Success -> {
                    val isTaken = result.data
                    if (isTaken) {
                        Timber.w("Phone already taken: $phone")
                        QrResult.Error(QrError.ValidationError.PhoneAlreadyTaken())
                    } else {
                        Timber.d("Phone is unique: $phone")
                        QrResult.Success(Unit)
                    }
                }

                is QrResult.Error -> {
                    Timber.e("Repository error checking phone $phone: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception checking phone uniqueness: $phone")
            QrResult.Error(QrError.SystemError.UnknownError())
        }
    }
}