package net.calvuz.qreport.checkup.items.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemTemplateMasterRepository
import javax.inject.Inject

/** Observes the checklist template master data list (active and inactive), for the management screen. */
class ObserveCheckItemTemplatesUseCase @Inject constructor(
    private val repository: CheckItemTemplateMasterRepository
) {
    operator fun invoke(): Flow<List<CheckItemTemplateMaster>> = repository.observeTemplates()
}
