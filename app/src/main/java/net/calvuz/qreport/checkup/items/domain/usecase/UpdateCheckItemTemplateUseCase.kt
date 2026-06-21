package net.calvuz.qreport.checkup.items.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemTemplateMasterRepository
import timber.log.Timber
import javax.inject.Inject

/** Updates an existing checklist template after validating required fields. */
class UpdateCheckItemTemplateUseCase @Inject constructor(
    private val repository: CheckItemTemplateMasterRepository
) {
    suspend operator fun invoke(template: CheckItemTemplateMaster): QrResult<Unit, QrError> {

        if (template.category.isBlank() || template.description.isBlank() ||
            template.moduleTypeId.isBlank() || template.criticalityId.isBlank()
        ) {
            return QrResult.Error(QrError.ValidationError.EmptyField())
        }

        return repository.updateTemplate(template).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = {
                Timber.e(it, "Failed to update check item template ${template.id}")
                QrResult.Error(QrError.App.SaveError())
            }
        )
    }
}
