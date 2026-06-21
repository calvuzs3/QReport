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
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per creare un nuovo check-up.
 *
 * Il checklist è generato a partire dai moduli associati al tipo isola
 * (`module_type_island_types`), e dai `check_item_templates` master data (DB) di
 * quei moduli — non più dall'object `CheckItemModules` hardcoded né dal vecchio
 * filtro per tipo isola sul singolo template.
 */
class CreateCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository,
    private val templateRepository: CheckItemTemplateMasterRepository,
    private val moduleTypeRepository: ModuleTypeMasterRepository
) {
    suspend operator fun invoke(
        header: CheckUpHeader,
        islandType: String,
        islandTypeId: String? = null,
        includeTemplateItems: Boolean = true
    ): Result<String> {
        return try {
            val checkUpId = UUID.randomUUID().toString()
            val now = Clock.System.now()

            val checkItems = if (includeTemplateItems && islandTypeId != null) {
                createCheckItemsFromTemplates(checkUpId, islandTypeId)
            } else {
                emptyList()
            }

            val checkUp = CheckUp(
                id = checkUpId,
                header = header,
                islandType = islandType,
                islandTypeId = islandTypeId,
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

    private suspend fun createCheckItemsFromTemplates(
        checkUpId: String,
        islandTypeId: String
    ): List<CheckItem> {
        val moduleTypeIds = moduleTypeRepository.getModuleTypeIdsForIslandType(islandTypeId).getOrElse {
            Timber.e(it, "Failed to load module types for island type $islandTypeId")
            emptyList()
        }
        if (moduleTypeIds.isEmpty()) return emptyList()

        val templates = templateRepository.getTemplatesForModuleTypes(moduleTypeIds).getOrElse {
            Timber.e(it, "Failed to load check item templates for modules $moduleTypeIds")
            emptyList()
        }

        return templates.map { template ->
            CheckItem(
                id = UUID.randomUUID().toString(),
                checkUpId = checkUpId,
                // code == enum name by seed convention (Migration4to5) — find+fallback
                // so a custom module/criticality created from Settings never crashes.
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
                orderIndex = template.orderIndex
            )
        }
    }
}