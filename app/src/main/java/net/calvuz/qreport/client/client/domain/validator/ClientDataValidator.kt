package net.calvuz.qreport.client.client.domain.validator

import net.calvuz.qreport.client.client.domain.model.Client
import javax.inject.Inject

class ClientDataValidator @Inject constructor(){

    /**
     * Validazione dati client
     */
    operator fun invoke(client: Client): Result<Unit> {
        return when {
            client.id.isBlank() ->
                Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))

            client.companyName.isBlank() ->
                Result.failure(IllegalArgumentException("Ragione sociale è obbligatoria"))

            client.companyName.length < 2 ->
                Result.failure(IllegalArgumentException("Ragione sociale deve essere di almeno 2 caratteri"))

            client.companyName.length > 255 ->
                Result.failure(IllegalArgumentException("Ragione sociale troppo lunga (max 255 caratteri)"))

            else -> Result.success(Unit)
        }
    }
}