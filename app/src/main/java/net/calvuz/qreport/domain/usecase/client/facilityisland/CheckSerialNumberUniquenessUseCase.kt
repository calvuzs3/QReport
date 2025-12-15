package net.calvuz.qreport.domain.usecase.client.facilityisland

import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import javax.inject.Inject

class CheckSerialNumberUniquenessUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository
){

    /**
     * Controllo univocità serial number
     */
    suspend operator fun invoke(serialNumber: String): Result<Unit> {
        return facilityIslandRepository.isSerialNumberTaken(serialNumber)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Serial number '$serialNumber' già esistente")
                }
            }
    }
}