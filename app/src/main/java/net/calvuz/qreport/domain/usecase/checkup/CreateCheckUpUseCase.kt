package net.calvuz.qreport.domain.usecase.checkup

import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.CheckItem
import net.calvuz.qreport.domain.model.CheckItemModules
import net.calvuz.qreport.domain.model.CheckItemStatus
import net.calvuz.qreport.domain.model.CheckUp
import net.calvuz.qreport.domain.model.CheckUpHeader
import net.calvuz.qreport.domain.model.CheckUpStatus
import net.calvuz.qreport.domain.model.IslandType
import net.calvuz.qreport.domain.model.ModuleType
import net.calvuz.qreport.domain.repository.CheckUpRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per creare un nuovo check-up
 *
 * AGGIORNATO per:
 * - Usare solo ModuleType definiti in ModuleType.kt
 * - Supportare solo IslandType della famiglia POLY
 * - Allinearsi con CheckItemModules esistente
 * - Gestire correttamente i template per tipo di isola
 */
class CreateCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(
        header: CheckUpHeader,
        islandType: IslandType,
        includeTemplateItems: Boolean = true
    ): Result<String> {
        return try {
            val checkUpId = UUID.randomUUID().toString()
            val now = Clock.System.now()

            val checkItems = if (includeTemplateItems) {
                createCheckItemsFromModules(checkUpId, islandType)
            } else {
                emptyList()
            }

            val checkUp = CheckUp(
                id = checkUpId,
                header = header,
                islandType = islandType,
                status = CheckUpStatus.DRAFT,
                checkItems = checkItems,
                spareParts = emptyList(),
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

    /**
     * Crea CheckItem dai moduli template - AGGIORNATO
     * Usa CheckItemModules con gestione specifica per famiglia POLY
     */
    private fun createCheckItemsFromModules(
        checkUpId: String,
        islandType: IslandType
    ): List<CheckItem> {
        // Ottieni tutti i template disponibili
        val allTemplates = CheckItemModules.getAllTemplates()

        // Filtra per il tipo di isola specificato
        // Nota: tutti i template base sono applicabili a tutte le isole POLY
        // mentre alcuni potrebbero essere specifici per certi tipi
        val templatesForIsland = allTemplates.filter { template ->
            template.islandTypes.contains(islandType)
        }

        return templatesForIsland.mapIndexed { index, template ->
            CheckItem(
                id = UUID.randomUUID().toString(),
                checkUpId = checkUpId,
                moduleType = mapStringToModuleType(template.moduleType),
                itemCode = template.id,
                description = template.description,
                status = CheckItemStatus.PENDING,
                criticality = template.criticality,
                notes = "",
                photos = emptyList(),
                checkedAt = null,
                orderIndex = template.orderIndex
            )
        }
    }

    /**
     * Mapping da String a ModuleType enum - CORRETTO
     * Usa solo i ModuleType effettivamente definiti in ModuleType.kt
     */
    private fun mapStringToModuleType(moduleTypeString: String): ModuleType {
        return when (moduleTypeString.lowercase()) {
            // Moduli base comuni
            "safety" -> ModuleType.SAFETY
            "mechanical" -> ModuleType.MECHANICAL
            "electrical" -> ModuleType.ELECTRICAL
            "pneumatic" -> ModuleType.PNEUMATIC
            "software" -> ModuleType.SOFTWARE

            // Moduli specifici robot
            "robot_tool" -> ModuleType.ROBOT_TOOL
            "robot" -> ModuleType.ROBOT
            "plant_systems" -> ModuleType.PLANT_SYSTEMS
            "functional_tests" -> ModuleType.FUNCTIONAL_TESTS

            // Moduli trasporto
            "conveyor_systems" -> ModuleType.CONVEYOR_SYSTEMS

            // Moduli visione
            "vision_system" -> ModuleType.VISION_SYSTEM

            // Moduli storage
            "lance_storage" -> ModuleType.LANCE_STORAGE
            "cartridge_systems" -> ModuleType.CARTRIDGE_SYSTEMS

            // Moduli etichettatura
            "labeling_machine" -> ModuleType.LABELING_MACHINE

            // Moduli vibratori
            "vibrators" -> ModuleType.VIBRATORS

            // Moduli robot duali
            "dual_robot" -> ModuleType.DUAL_ROBOT

            // Default per tipi non riconosciuti
            else -> {
                // Log o warning per debug
                println("ModuleType non riconosciuto: $moduleTypeString - usando MECHANICAL come default")
                ModuleType.MECHANICAL
            }
        }
    }
}