package net.calvuz.qreport.checkup.domain.model

import net.calvuz.qreport.app.app.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.domain.model.module.ModuleType

/**
 * Moduli di checklist predefiniti - VERSIONE CORRETTA per famiglia POLY
 *
 * AGGIORNATO per usare:
 * - ModuleType enum effettivamente definiti
 * - Tipi isola della famiglia POLY, identificati per label (es. "POLY Move") invece
 *   del vecchio enum IslandType — coerente con IslandTypeMaster/IslandTypeIconRegistry
 * - CriticalityLevel corretto
 *
 * Basato sulla struttura esistente ma completamente allineato
 * con gli enum domain corretti per la famiglia POLY.
 */
object CheckItemModules {

    // ============================================================
    // LABEL DEI TIPI ISOLA POLY (storiche, seedate anche server-side)
    // ============================================================

    private const val POLY_MOVE = "POLY Move"
    private const val POLY_CAST = "POLY Cast"
    private const val POLY_EBT = "POLY EBT"
    private const val POLY_TAG_BLE = "POLY Tag BLE"
    private const val POLY_TAG_FC = "POLY Tag FC"
    private const val POLY_TAG_V = "POLY Tag V"
    private const val POLY_SAMPLE = "POLY Sample"
    private const val POLY_WELD = "POLY Weld"
    private const val POLY_PAINT = "POLY Paint"
    private const val OTHER = "Altro"

    private val ALL_POLY_TYPES = listOf(
        POLY_MOVE, POLY_CAST, POLY_EBT, POLY_TAG_BLE, POLY_TAG_FC,
        POLY_TAG_V, POLY_SAMPLE, POLY_WELD, POLY_PAINT, OTHER
    )

    // ============================================================
    // MODULI BASE COMUNI (applicabili a tutte le isole POLY)
    // ============================================================

    val SAFETY_MODULE = listOf(
        CheckItemTemplate(
            id = "SAF_001",
            moduleType = ModuleType.SAFETY.name,
            category = "Protezioni di Sicurezza",
            description = "Controllo integrità barriere fotoelettriche",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 1,
            islandTypes = ALL_POLY_TYPES // Applicabile a tutte le isole POLY
        ),
        CheckItemTemplate(
            id = "SAF_002",
            moduleType = ModuleType.SAFETY.name,
            category = "Protezioni di Sicurezza",
            description = "Verifica funzionamento pulsante di emergenza",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 2,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "SAF_003",
            moduleType = ModuleType.SAFETY.name,
            category = "Protezioni di Sicurezza",
            description = "Controllo chiusura porte di accesso",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 3,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "SAF_004",
            moduleType = ModuleType.SAFETY.name,
            category = "Segnalazioni",
            description = "Verifica funzionamento torretta luminosa",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 4,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "SAF_005",
            moduleType = ModuleType.SAFETY.name,
            category = "Segnalazioni",
            description = "Test allarmi sonori",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 5,
            islandTypes = ALL_POLY_TYPES
        )
    )

    val MECHANICAL_MODULE = listOf(
        CheckItemTemplate(
            id = "MEC_001",
            moduleType = ModuleType.MECHANICAL.name,
            category = "Robot",
            description = "Controllo giochi meccanici assi robot",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 10,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "MEC_002",
            moduleType = ModuleType.MECHANICAL.name,
            category = "Robot",
            description = "Verifica lubrificazione riduttori",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 11,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "MEC_003",
            moduleType = ModuleType.MECHANICAL.name,
            category = "Attrezzature",
            description = "Controllo usura utensili",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 12,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "MEC_004",
            moduleType = ModuleType.MECHANICAL.name,
            category = "Strutture",
            description = "Controllo stabilità strutture meccaniche",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 13,
            islandTypes = ALL_POLY_TYPES
        )
    )

    val ELECTRICAL_MODULE = listOf(
        CheckItemTemplate(
            id = "ELE_001",
            moduleType = ModuleType.ELECTRICAL.name,
            category = "Alimentazione",
            description = "Verifica tensioni di alimentazione",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 20,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "ELE_002",
            moduleType = ModuleType.ELECTRICAL.name,
            category = "Cablaggi",
            description = "Controllo integrità cavi di potenza",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 21,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "ELE_003",
            moduleType = ModuleType.ELECTRICAL.name,
            category = "Cablaggi",
            description = "Verifica connessioni morsettiere",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 22,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "ELE_004",
            moduleType = ModuleType.ELECTRICAL.name,
            category = "Protezioni",
            description = "Test interruttori magnetotermici",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 23,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "ELE_005",
            moduleType = ModuleType.ELECTRICAL.name,
            category = "Controllo",
            description = "Verifica comunicazione fieldbus",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 24,
            islandTypes = ALL_POLY_TYPES
        )
    )

    val PNEUMATIC_MODULE = listOf(
        CheckItemTemplate(
            id = "PNE_001",
            moduleType = ModuleType.PNEUMATIC.name,
            category = "Pneumatica",
            description = "Verifica pressione aria compressa (6-8 bar)",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 30,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "PNE_002",
            moduleType = ModuleType.PNEUMATIC.name,
            category = "Pneumatica",
            description = "Controllo filtri aria e drenaggio condensa",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 31,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "PNE_003",
            moduleType = ModuleType.PNEUMATIC.name,
            category = "Pneumatica",
            description = "Test cilindri pneumatici e attuatori",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 32,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "PNE_004",
            moduleType = ModuleType.PNEUMATIC.name,
            category = "Pneumatica",
            description = "Controllo tubazioni pneumatiche e raccordi",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 33,
            islandTypes = ALL_POLY_TYPES
        )
    )

    val SOFTWARE_MODULE = listOf(
        CheckItemTemplate(
            id = "SW_001",
            moduleType = ModuleType.SOFTWARE.name,
            category = "Sistema",
            description = "Backup programmi robot",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 40,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "SW_002",
            moduleType = ModuleType.SOFTWARE.name,
            category = "Sistema",
            description = "Verifica versioni firmware",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 41,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "SW_003",
            moduleType = ModuleType.SOFTWARE.name,
            category = "Configurazioni",
            description = "Controllo parametri robot",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 42,
            islandTypes = ALL_POLY_TYPES
        )
    )

    // ============================================================
    // MODULI SPECIFICI PER ROBOT
    // ============================================================

    val ROBOT_TOOL_MODULE = listOf(
        CheckItemTemplate(
            id = "RT_001",
            moduleType = ModuleType.ROBOT_TOOL.name,
            category = "Tool/Pinza",
            description = "Controllo stato tubi aria e raccordi tool",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 50,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "RT_002",
            moduleType = ModuleType.ROBOT_TOOL.name,
            category = "Tool/Pinza",
            description = "Verifica usura chele/gommini pinza",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 51,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "RT_003",
            moduleType = ModuleType.ROBOT_TOOL.name,
            category = "Tool/Pinza",
            description = "Controllo connessioni elettriche tool",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 52,
            islandTypes = ALL_POLY_TYPES
        )
    )

    val ROBOT_MODULE = listOf(
        CheckItemTemplate(
            id = "ROB_001",
            moduleType = ModuleType.ROBOT.name,
            category = "Robot",
            description = "Controllo perdite olio riduttori robot",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 60,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "ROB_002",
            moduleType = ModuleType.ROBOT.name,
            category = "Robot",
            description = "Controllo guaine e festoni cavi robot",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 61,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "ROB_003",
            moduleType = ModuleType.ROBOT.name,
            category = "Robot",
            description = "Controllo ore lavoro robot",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 62,
            islandTypes = ALL_POLY_TYPES
        )
    )

    val PLANT_SYSTEMS_MODULE = listOf(
        CheckItemTemplate(
            id = "PS_001",
            moduleType = ModuleType.PLANT_SYSTEMS.name,
            category = "Impianti",
            description = "Controllo filtri recupero condensa",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 70,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "PS_002",
            moduleType = ModuleType.PLANT_SYSTEMS.name,
            category = "Impianti",
            description = "Pulizia generale isola e container",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 71,
            islandTypes = ALL_POLY_TYPES
        )
    )

    val FUNCTIONAL_TESTS_MODULE = listOf(
        CheckItemTemplate(
            id = "FT_001",
            moduleType = ModuleType.FUNCTIONAL_TESTS.name,
            category = "Test Funzionali",
            description = "Esecuzione ciclo simulazione",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 80,
            islandTypes = ALL_POLY_TYPES
        ),
        CheckItemTemplate(
            id = "FT_002",
            moduleType = ModuleType.FUNCTIONAL_TESTS.name,
            category = "Test Funzionali",
            description = "Controllo comandi manuali da HMI",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 81,
            islandTypes = ALL_POLY_TYPES
        )
    )

    // ============================================================
    // MODULI SPECIFICI PER TIPO ISOLA POLY
    // ============================================================

    val CONVEYOR_SYSTEMS_MODULE = listOf(
        CheckItemTemplate(
            id = "CS_001",
            moduleType = ModuleType.CONVEYOR_SYSTEMS.name,
            category = "Rulliere",
            description = "Controllo presenza detriti rulliera",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 100,
            islandTypes = listOf(POLY_MOVE) // Specifico per POLY Move
        ),
        CheckItemTemplate(
            id = "CS_002",
            moduleType = ModuleType.CONVEYOR_SYSTEMS.name,
            category = "Rulliere",
            description = "Controllo tensione catene trasporto",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 101,
            islandTypes = listOf(POLY_MOVE)
        )
    )

    val VISION_SYSTEM_MODULE = listOf(
        CheckItemTemplate(
            id = "VS_001",
            moduleType = ModuleType.VISION_SYSTEM.name,
            category = "Visione",
            description = "Pulizia lenti sistema visione",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 110,
            islandTypes = listOf(POLY_TAG_V) // Specifico per Tag V
        ),
        CheckItemTemplate(
            id = "VS_002",
            moduleType = ModuleType.VISION_SYSTEM.name,
            category = "Visione",
            description = "Controllo illuminazione sistema visione",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 111,
            islandTypes = listOf(POLY_TAG_V)
        )
    )

    val LANCE_STORAGE_MODULE = listOf(
        CheckItemTemplate(
            id = "LS_001",
            moduleType = ModuleType.LANCE_STORAGE.name,
            category = "Lance e Storage",
            description = "Controllo integrità lance EBT",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 120,
            islandTypes = listOf(POLY_EBT) // Specifico per EBT
        ),
        CheckItemTemplate(
            id = "LS_002",
            moduleType = ModuleType.LANCE_STORAGE.name,
            category = "Lance e Storage",
            description = "Verifica magazzino lance",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 121,
            islandTypes = listOf(POLY_EBT)
        )
    )

    val CARTRIDGE_SYSTEMS_MODULE = listOf(
        CheckItemTemplate(
            id = "CTS_001",
            moduleType = ModuleType.CARTRIDGE_SYSTEMS.name,
            category = "Cartucce",
            description = "Pulizia magazzini cartucce",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 130,
            islandTypes = listOf(POLY_SAMPLE) // Specifico per Sample
        ),
        CheckItemTemplate(
            id = "CTS_002",
            moduleType = ModuleType.CARTRIDGE_SYSTEMS.name,
            category = "Cartucce",
            description = "Controllo perdite aria magazzini cartucce",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 131,
            islandTypes = listOf(POLY_SAMPLE)
        )
    )

    val LABELING_MACHINE_MODULE = listOf(
        CheckItemTemplate(
            id = "LM_001",
            moduleType = ModuleType.LABELING_MACHINE.name,
            category = "Etichettatura",
            description = "Pulizia generale macchina etichettatura",
            criticality = CriticalityLevel.ROUTINE,
            orderIndex = 140,
            islandTypes = listOf(POLY_TAG_BLE, POLY_TAG_FC, POLY_TAG_V)
        ),
        CheckItemTemplate(
            id = "LM_002",
            moduleType = ModuleType.LABELING_MACHINE.name,
            category = "Etichettatura",
            description = "Pulizia lente stampante",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 141,
            islandTypes = listOf(POLY_TAG_BLE, POLY_TAG_FC, POLY_TAG_V)
        )
    )

    val VIBRATORS_MODULE = listOf(
        CheckItemTemplate(
            id = "VIB_001",
            moduleType = ModuleType.VIBRATORS.name,
            category = "Vibratori",
            description = "Controllo vibratore circolare",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 150,
            islandTypes = listOf(POLY_TAG_BLE) // Specifico per Tag BLE
        ),
        CheckItemTemplate(
            id = "VIB_002",
            moduleType = ModuleType.VIBRATORS.name,
            category = "Vibratori",
            description = "Controllo vibratore lineare",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 151,
            islandTypes = listOf(POLY_TAG_BLE, POLY_TAG_V)
        )
    )

    val DUAL_ROBOT_MODULE = listOf(
        CheckItemTemplate(
            id = "DR_001",
            moduleType = ModuleType.DUAL_ROBOT.name,
            category = "Robot Duali",
            description = "Controllo coordinamento robot multipli",
            criticality = CriticalityLevel.CRITICAL,
            orderIndex = 160,
            islandTypes = listOf(POLY_TAG_V) // Tag V ha robot duali
        ),
        CheckItemTemplate(
            id = "DR_002",
            moduleType = ModuleType.DUAL_ROBOT.name,
            category = "Robot Duali",
            description = "Verifica sincronizzazione robot esterno/interno",
            criticality = CriticalityLevel.IMPORTANT,
            orderIndex = 161,
            islandTypes = listOf(POLY_TAG_V)
        )
    )

    // ============================================================
    // FUNZIONI PUBBLICHE
    // ============================================================

    /**
     * Restituisce tutti i template per un tipo di isola, identificato per label
     * (es. "POLY Move", coerente con [net.calvuz.qreport.client.island.domain.model.IslandTypeMaster.label]).
     */
    fun getTemplatesForIslandType(islandType: String): List<CheckItemTemplate> {
        // Moduli base comuni a tutte le isole POLY
        val baseModules = SAFETY_MODULE + MECHANICAL_MODULE + ELECTRICAL_MODULE +
                PNEUMATIC_MODULE + SOFTWARE_MODULE + ROBOT_TOOL_MODULE +
                ROBOT_MODULE + PLANT_SYSTEMS_MODULE + FUNCTIONAL_TESTS_MODULE

        // Moduli specifici per tipo di isola
        val specificModules = when (islandType) {
            POLY_MOVE -> CONVEYOR_SYSTEMS_MODULE
            POLY_CAST -> emptyList() // Cast può usare solo moduli base
            POLY_EBT -> LANCE_STORAGE_MODULE
            POLY_TAG_BLE -> LABELING_MACHINE_MODULE + VIBRATORS_MODULE
            POLY_TAG_FC -> LABELING_MACHINE_MODULE
            POLY_TAG_V -> VISION_SYSTEM_MODULE + LABELING_MACHINE_MODULE + VIBRATORS_MODULE + DUAL_ROBOT_MODULE
            POLY_SAMPLE -> CARTRIDGE_SYSTEMS_MODULE
            POLY_WELD -> emptyList() // Weld può usare solo moduli base
            POLY_PAINT -> emptyList() // Paint può usare solo moduli base
            else -> emptyList() // Tipologia non classificata (es. "Altro" o custom), solo moduli base
        }

        return (baseModules + specificModules)
            .filter { it.islandTypes.contains(islandType) }
            .sortedBy { it.orderIndex }
    }

    /**
     * Ottieni tutti i template disponibili
     */
    fun getAllTemplates(): List<CheckItemTemplate> {
        return ALL_POLY_TYPES.flatMap { islandType ->
            getTemplatesForIslandType(islandType)
        }.distinctBy { it.id }
    }

    /**
     * Statistiche template per debugging
     */
    fun getTemplateStats(): Map<String, Any> = mapOf(
        "totalTemplates" to getAllTemplates().size,
        "byIslandType" to ALL_POLY_TYPES.associateWith { type ->
            getTemplatesForIslandType(type).size
        },
        "byModule" to getAllTemplates().groupBy { it.moduleType }.mapValues { it.value.size },
        "byCriticality" to getAllTemplates().groupBy { it.criticality }.mapValues { it.value.size }
    )
}
