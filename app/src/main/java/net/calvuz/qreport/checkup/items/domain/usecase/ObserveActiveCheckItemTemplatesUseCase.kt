package net.calvuz.qreport.checkup.items.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemTemplateMasterRepository
import javax.inject.Inject

/** Active checklist templates only — the reference list shown in the template editor. */
class ObserveActiveCheckItemTemplatesUseCase @Inject constructor(
    private val repository: CheckItemTemplateMasterRepository
) {
    operator fun invoke(): Flow<List<CheckItemTemplateMaster>> = repository.observeActiveTemplates()
}
