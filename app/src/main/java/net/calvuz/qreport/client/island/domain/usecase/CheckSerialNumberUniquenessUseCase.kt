package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject

class CheckSerialNumberUniquenessUseCase @Inject constructor(
    private val islandRepository: IslandRepository
){

    /**
     * Controllo univocità serial number
     */
    suspend operator fun invoke(serialNumber: String): Result<Unit> {
        return islandRepository.isSerialNumberTaken(serialNumber)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Serial number '$serialNumber' già esistente")
                }
            }
    }
}