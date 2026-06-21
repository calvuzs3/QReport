package net.calvuz.qreport.checkup.checkup.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.items.domain.model.CheckItem
import net.calvuz.qreport.checkup.items.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemTemplateMasterRepository
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpHeader
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.modules.domain.model.ModuleType
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per creare un check-up da una selezione manuale di template
 * (`check_item_templates` master data, non più dall'object `CheckItemModules`).
 */
class CreateCheckUpFromTemplateUseCase @Inject constructor(
    private val repository: CheckUpRepository,
    private val templateRepository: CheckItemTemplateMasterRepository
) {
    suspend operator fun invoke(
        header: CheckUpHeader,
        islandType: String,
        selectedTemplateIds: List<String>
    ): Result<String> {
        return try {
            val checkUpId = UUID.randomUUID().toString()
            val now = Clock.System.now()

            val allTemplates = templateRepository.getTemplates().getOrElse {
                return Result.failure(it)
            }
            val selectedTemplates = allTemplates.filter { it.id in selectedTemplateIds }

            val checkItems = selectedTemplates.mapIndexed { index, template ->
                CheckItem(
                    id = UUID.randomUUID().toString(),
                    checkUpId = checkUpId,
                    moduleType = ModuleType.entries.find { it.name == template.moduleTypeId } ?: ModuleType.MECHANICAL,
                    moduleTypeId = template.moduleTypeId,
                    itemCode = template.id,
                    description = template.description,
                    status = CheckItemStatus.PENDING,
                    criticality = CriticalityLevel.entries.find { it.name == template.criticalityId } ?: CriticalityLevel.ROUTINE,
                    criticalityId = template.criticalityId,
                    notes = "",
                    photos = emptyList(),
                    checkedAt = null,
                    orderIndex = index
                )
            }

            val checkUp = CheckUp(
                id = checkUpId,
                header = header,
                islandType = islandType,
                status = CheckUpStatus.DRAFT,
                checkItems = checkItems,
                createdAt = now,
                updatedAt = now,
                completedAt = null
            )

            val createdId = repository.createCheckUp(checkUp)
            Result.success(createdId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}