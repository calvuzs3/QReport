package net.calvuz.qreport.checkup.spareparts.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.spareparts.domain.model.CheckUpSparePart
import net.calvuz.qreport.checkup.spareparts.domain.repository.CheckUpSparePartRepository
import javax.inject.Inject

class ObserveSparePartsUseCase @Inject constructor(
    private val repository: CheckUpSparePartRepository
) {
    operator fun invoke(checkupId: String): Flow<List<CheckUpSparePart>> =
        repository.observeByCheckup(checkupId)
}
