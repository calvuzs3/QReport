package net.calvuz.qreport.checkup.items.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemTemplateMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes (deactivates) a checklist template. Never a hard delete: existing
 * check_items reference the template id via `item_code`, so deactivating just
 * hides it from future checkup creation without breaking historical records.
 */
class DeactivateCheckItemTemplateUseCase @Inject constructor(
    private val repository: CheckItemTemplateMasterRepository
) {
    suspend operator fun invoke(id: String): QrResult<Unit, QrError> {
        return repository.deactivateTemplate(id).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to deactivate check item template $id")
                QrResult.Error(QrError.App.DeleteError())
            }
        )
    }
}
