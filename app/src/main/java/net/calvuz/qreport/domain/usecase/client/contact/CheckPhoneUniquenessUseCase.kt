package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.repository.ContactRepository
import javax.inject.Inject

class CheckPhoneUniquenessUseCase @Inject constructor(
    private val contactRepository: ContactRepository
){

    /**
     * Controllo univocità telefono globale
     */
    suspend operator fun invoke(phone: String): Result<Unit> {
        return contactRepository.isPhoneTaken(phone)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Numero di telefono '$phone' già utilizzato")
                }
            }
    }
}