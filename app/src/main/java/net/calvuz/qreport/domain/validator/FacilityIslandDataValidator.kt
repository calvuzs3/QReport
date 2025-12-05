package net.calvuz.qreport.domain.validator

import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.repository.FacilityRepository
import javax.inject.Inject

class FacilityIslandDataValidator @Inject constructor() {

    /**
     * Validazione dati isola
     */
    operator fun invoke(island: FacilityIsland): Result<Unit> {
        return when {
            island.facilityId.isBlank() ->
                Result.failure(IllegalArgumentException("ID facility è obbligatorio"))

            island.serialNumber.isBlank() ->
                Result.failure(IllegalArgumentException("Serial number è obbligatorio"))

            island.serialNumber.length < 3 ->
                Result.failure(IllegalArgumentException("Serial number deve essere di almeno 3 caratteri"))

            island.serialNumber.length > 50 ->
                Result.failure(IllegalArgumentException("Serial number troppo lungo (max 50 caratteri)"))

            !isValidSerialNumber(island.serialNumber) ->
                Result.failure(IllegalArgumentException("Formato serial number non valido (solo lettere, numeri, trattini)"))

            (island.model?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Modello troppo lungo (max 100 caratteri)"))

            (island.customName?.length ?: 0) > 100 ->
                Result.failure(IllegalArgumentException("Nome personalizzato troppo lungo (max 100 caratteri)"))

            (island.location?.length ?: 0) > 200 ->
                Result.failure(IllegalArgumentException("Ubicazione troppo lunga (max 200 caratteri)"))

            island.operatingHours < 0 ->
                Result.failure(IllegalArgumentException("Ore operative non possono essere negative"))

            island.cycleCount < 0 ->
                Result.failure(IllegalArgumentException("Conteggio cicli non può essere negativo"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Validazione formato serial number (alfanumerico + trattini)
     */
    private fun isValidSerialNumber(serialNumber: String): Boolean {
        return serialNumber.matches("[A-Za-z0-9\\-_]+".toRegex())
    }
}