package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

class CheckFacilityNameUniquenessUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
){

    /**
     * Controllo univocità nome stabilimento per cliente
     */
    suspend operator fun invoke(clientId: String, name: String): Result<Unit> {
        return facilityRepository.isFacilityNameTakenForClient(clientId, name)
            .mapCatching { isTaken ->
                if (isTaken) {
                    throw IllegalArgumentException("Esiste già uno stabilimento '$name' per questo cliente")
                }
            }
    }
}