package net.calvuz.qreport.checkup.spareparts.domain.usecase

import net.calvuz.qreport.checkup.spareparts.domain.repository.CheckUpSparePartRepository
import javax.inject.Inject

class UpdateSparePartQuantityUseCase @Inject constructor(
    private val repository: CheckUpSparePartRepository
) {
    suspend operator fun invoke(id: String, quantity: Double?): Result<Unit> =
        repository.updateQuantity(id, quantity)
}
