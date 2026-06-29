package net.calvuz.qreport.checkup.spareparts.domain.usecase

import net.calvuz.qreport.checkup.spareparts.domain.model.CheckUpSparePart
import net.calvuz.qreport.checkup.spareparts.domain.model.SelectedArticle
import net.calvuz.qreport.checkup.spareparts.domain.repository.CheckUpSparePartRepository
import java.util.UUID
import javax.inject.Inject

class AddSparePartsUseCase @Inject constructor(
    private val repository: CheckUpSparePartRepository
) {
    suspend operator fun invoke(
        checkupId: String,
        articles: List<SelectedArticle>,
        existingUuids: Set<String> = emptySet()
    ): Result<Unit> {
        val newParts = articles
            .filter { it.uuid !in existingUuids }
            .map { article ->
                CheckUpSparePart(
                    id          = UUID.randomUUID().toString(),
                    checkupId   = checkupId,
                    articleUuid = article.uuid,
                    name        = article.name,
                    codeOem     = article.codeOem,
                    codeErp     = article.codeErp,
                    codeBm      = article.codeBm,
                    unit        = article.unit,
                    addedAt     = System.currentTimeMillis()
                )
            }
        if (newParts.isEmpty()) return Result.success(Unit)
        return repository.addParts(newParts)
    }
}
