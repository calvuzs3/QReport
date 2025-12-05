package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.repository.ContactRepository
import javax.inject.Inject


/**
 * Controllo univocità email globale
 */
class CheckEmailUniquenessUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {

    suspend operator fun invoke(email: String): Result<Unit> {
        return contactRepository.isEmailTaken(email)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Email '$email' già utilizzata")
                }
            }
    }
}