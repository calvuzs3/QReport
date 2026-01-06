package net.calvuz.qreport.client.facility.domain.validator

import net.calvuz.qreport.app.app.domain.model.Address
import javax.inject.Inject

class FacilityDataValidator @Inject constructor(){

    /**
     * Validazione input base
     */
    operator fun invoke(clientId: String, name: String, address: Address): Result<Unit> {
        return when {
            clientId.isBlank() ->
                Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))

            name.isBlank() ->
                Result.failure(IllegalArgumentException("Nome stabilimento è obbligatorio"))

            name.length < 2 ->
                Result.failure(IllegalArgumentException("Nome stabilimento deve essere di almeno 2 caratteri"))

            name.length > 100 ->
                Result.failure(IllegalArgumentException("Nome stabilimento troppo lungo (max 100 caratteri)"))

            !address.isComplete() ->
                Result.failure(IllegalArgumentException("Indirizzo stabilimento incompleto"))

            else -> Result.success(Unit)
        }
    }
}