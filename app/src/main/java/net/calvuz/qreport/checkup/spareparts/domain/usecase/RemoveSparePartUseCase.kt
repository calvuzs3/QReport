package net.calvuz.qreport.checkup.spareparts.domain.usecase

import net.calvuz.qreport.checkup.spareparts.domain.repository.CheckUpSparePartRepository
import javax.inject.Inject

class RemoveSparePartUseCase @Inject constructor(
    private val repository: CheckUpSparePartRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> =
        repository.removePart(id)
}
