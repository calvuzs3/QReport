package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Inject

class CheckCompanyNameUniquenessUseCase @Inject constructor(
    val clientRepository: ClientRepository
){

    /**
     * Controllo univocità ragione sociale escludendo il cliente corrente
     */
    suspend operator fun invoke(client: Client): Result<Unit> {
        return clientRepository.isCompanyNameTaken(client.companyName, client.id)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Ragione sociale '${client.companyName}' già esistente")
                }
            }
    }
}