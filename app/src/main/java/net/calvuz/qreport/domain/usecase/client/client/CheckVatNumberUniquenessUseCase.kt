package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.repository.ClientRepository
import javax.inject.Inject

class CheckVatNumberUniquenessUseCase @Inject constructor(
    val clientRepository: ClientRepository
){

    /**
     * Controllo univocità partita IVA escludendo il cliente corrente
     */
    suspend operator fun invoke(clientId: String, vatNumber: String): Result<Unit> {
        return clientRepository.isVatNumberTaken(vatNumber, clientId)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Partita IVA '$vatNumber' già esistente")
                }
            }
    }
}