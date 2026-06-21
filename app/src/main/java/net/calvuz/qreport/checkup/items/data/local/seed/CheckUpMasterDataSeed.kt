package net.calvuz.qreport.checkup.items.data.local.seed

/**
 * Static seed data for the checklist master tables (`module_types`,
 * `criticality_levels`, `check_item_templates`, `check_item_template_island_types`),
 * the one-time replacement for the historical hardcoded `CheckItemModules` object.
 *
 * Used by `Migration4to5` (initial seed) and by
 * `BackfillCheckItemTemplateIslandTypesUseCase` (re-attempts the island-type link
 * for templates that found no matching `island_types` row yet — e.g. a fresh
 * install where the migration ran before the first sync populated `island_types`).
 *
 * [moduleTypeCode]/[criticalityCode] match `ModuleType.name`/`CriticalityLevel.name`
 * (the master `code` column is seeded from the same enum names), [islandTypeLabels]
 * match `island_types.label` exactly as already used by the legacy
 * `CheckItemModules.getTemplatesForIslandType`.
 */
data class CheckItemTemplateSeed(
    val id: String,
    val moduleTypeCode: String,
    val category: String,
    val description: String,
    val criticalityCode: String,
    val orderIndex: Int,
    val islandTypeLabels: List<String>
)

object CheckUpMasterDataSeed {

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

    val TEMPLATES: List<CheckItemTemplateSeed> = listOf(
        // SAFETY
        CheckItemTemplateSeed("SAF_001", "SAFETY", "Protezioni di Sicurezza", "Controllo integrità barriere fotoelettriche", "CRITICAL", 1, ALL_POLY_TYPES),
        CheckItemTemplateSeed("SAF_002", "SAFETY", "Protezioni di Sicurezza", "Verifica funzionamento pulsante di emergenza", "CRITICAL", 2, ALL_POLY_TYPES),
        CheckItemTemplateSeed("SAF_003", "SAFETY", "Protezioni di Sicurezza", "Controllo chiusura porte di accesso", "CRITICAL", 3, ALL_POLY_TYPES),
        CheckItemTemplateSeed("SAF_004", "SAFETY", "Segnalazioni", "Verifica funzionamento torretta luminosa", "IMPORTANT", 4, ALL_POLY_TYPES),
        CheckItemTemplateSeed("SAF_005", "SAFETY", "Segnalazioni", "Test allarmi sonori", "IMPORTANT", 5, ALL_POLY_TYPES),
        // MECHANICAL
        CheckItemTemplateSeed("MEC_001", "MECHANICAL", "Robot", "Controllo giochi meccanici assi robot", "CRITICAL", 10, ALL_POLY_TYPES),
        CheckItemTemplateSeed("MEC_002", "MECHANICAL", "Robot", "Verifica lubrificazione riduttori", "IMPORTANT", 11, ALL_POLY_TYPES),
        CheckItemTemplateSeed("MEC_003", "MECHANICAL", "Attrezzature", "Controllo usura utensili", "IMPORTANT", 12, ALL_POLY_TYPES),
        CheckItemTemplateSeed("MEC_004", "MECHANICAL", "Strutture", "Controllo stabilità strutture meccaniche", "CRITICAL", 13, ALL_POLY_TYPES),
        // ELECTRICAL
        CheckItemTemplateSeed("ELE_001", "ELECTRICAL", "Alimentazione", "Verifica tensioni di alimentazione", "CRITICAL", 20, ALL_POLY_TYPES),
        CheckItemTemplateSeed("ELE_002", "ELECTRICAL", "Cablaggi", "Controllo integrità cavi di potenza", "IMPORTANT", 21, ALL_POLY_TYPES),
        CheckItemTemplateSeed("ELE_003", "ELECTRICAL", "Cablaggi", "Verifica connessioni morsettiere", "ROUTINE", 22, ALL_POLY_TYPES),
        CheckItemTemplateSeed("ELE_004", "ELECTRICAL", "Protezioni", "Test interruttori magnetotermici", "CRITICAL", 23, ALL_POLY_TYPES),
        CheckItemTemplateSeed("ELE_005", "ELECTRICAL", "Controllo", "Verifica comunicazione fieldbus", "IMPORTANT", 24, ALL_POLY_TYPES),
        // PNEUMATIC
        CheckItemTemplateSeed("PNE_001", "PNEUMATIC", "Pneumatica", "Verifica pressione aria compressa (6-8 bar)", "CRITICAL", 30, ALL_POLY_TYPES),
        CheckItemTemplateSeed("PNE_002", "PNEUMATIC", "Pneumatica", "Controllo filtri aria e drenaggio condensa", "IMPORTANT", 31, ALL_POLY_TYPES),
        CheckItemTemplateSeed("PNE_003", "PNEUMATIC", "Pneumatica", "Test cilindri pneumatici e attuatori", "IMPORTANT", 32, ALL_POLY_TYPES),
        CheckItemTemplateSeed("PNE_004", "PNEUMATIC", "Pneumatica", "Controllo tubazioni pneumatiche e raccordi", "ROUTINE", 33, ALL_POLY_TYPES),
        // SOFTWARE
        CheckItemTemplateSeed("SW_001", "SOFTWARE", "Sistema", "Backup programmi robot", "CRITICAL", 40, ALL_POLY_TYPES),
        CheckItemTemplateSeed("SW_002", "SOFTWARE", "Sistema", "Verifica versioni firmware", "ROUTINE", 41, ALL_POLY_TYPES),
        CheckItemTemplateSeed("SW_003", "SOFTWARE", "Configurazioni", "Controllo parametri robot", "IMPORTANT", 42, ALL_POLY_TYPES),
        // ROBOT_TOOL
        CheckItemTemplateSeed("RT_001", "ROBOT_TOOL", "Tool/Pinza", "Controllo stato tubi aria e raccordi tool", "IMPORTANT", 50, ALL_POLY_TYPES),
        CheckItemTemplateSeed("RT_002", "ROBOT_TOOL", "Tool/Pinza", "Verifica usura chele/gommini pinza", "CRITICAL", 51, ALL_POLY_TYPES),
        CheckItemTemplateSeed("RT_003", "ROBOT_TOOL", "Tool/Pinza", "Controllo connessioni elettriche tool", "IMPORTANT", 52, ALL_POLY_TYPES),
        // ROBOT
        CheckItemTemplateSeed("ROB_001", "ROBOT", "Robot", "Controllo perdite olio riduttori robot", "CRITICAL", 60, ALL_POLY_TYPES),
        CheckItemTemplateSeed("ROB_002", "ROBOT", "Robot", "Controllo guaine e festoni cavi robot", "IMPORTANT", 61, ALL_POLY_TYPES),
        CheckItemTemplateSeed("ROB_003", "ROBOT", "Robot", "Controllo ore lavoro robot", "ROUTINE", 62, ALL_POLY_TYPES),
        // PLANT_SYSTEMS
        CheckItemTemplateSeed("PS_001", "PLANT_SYSTEMS", "Impianti", "Controllo filtri recupero condensa", "IMPORTANT", 70, ALL_POLY_TYPES),
        CheckItemTemplateSeed("PS_002", "PLANT_SYSTEMS", "Impianti", "Pulizia generale isola e container", "ROUTINE", 71, ALL_POLY_TYPES),
        // FUNCTIONAL_TESTS
        CheckItemTemplateSeed("FT_001", "FUNCTIONAL_TESTS", "Test Funzionali", "Esecuzione ciclo simulazione", "CRITICAL", 80, ALL_POLY_TYPES),
        CheckItemTemplateSeed("FT_002", "FUNCTIONAL_TESTS", "Test Funzionali", "Controllo comandi manuali da HMI", "IMPORTANT", 81, ALL_POLY_TYPES),
        // CONVEYOR_SYSTEMS — specifico POLY Move
        CheckItemTemplateSeed("CS_001", "CONVEYOR_SYSTEMS", "Rulliere", "Controllo presenza detriti rulliera", "ROUTINE", 100, listOf(POLY_MOVE)),
        CheckItemTemplateSeed("CS_002", "CONVEYOR_SYSTEMS", "Rulliere", "Controllo tensione catene trasporto", "IMPORTANT", 101, listOf(POLY_MOVE)),
        // VISION_SYSTEM — specifico Tag V
        CheckItemTemplateSeed("VS_001", "VISION_SYSTEM", "Visione", "Pulizia lenti sistema visione", "IMPORTANT", 110, listOf(POLY_TAG_V)),
        CheckItemTemplateSeed("VS_002", "VISION_SYSTEM", "Visione", "Controllo illuminazione sistema visione", "IMPORTANT", 111, listOf(POLY_TAG_V)),
        // LANCE_STORAGE — specifico EBT
        CheckItemTemplateSeed("LS_001", "LANCE_STORAGE", "Lance e Storage", "Controllo integrità lance EBT", "CRITICAL", 120, listOf(POLY_EBT)),
        CheckItemTemplateSeed("LS_002", "LANCE_STORAGE", "Lance e Storage", "Verifica magazzino lance", "IMPORTANT", 121, listOf(POLY_EBT)),
        // CARTRIDGE_SYSTEMS — specifico Sample
        CheckItemTemplateSeed("CTS_001", "CARTRIDGE_SYSTEMS", "Cartucce", "Pulizia magazzini cartucce", "ROUTINE", 130, listOf(POLY_SAMPLE)),
        CheckItemTemplateSeed("CTS_002", "CARTRIDGE_SYSTEMS", "Cartucce", "Controllo perdite aria magazzini cartucce", "IMPORTANT", 131, listOf(POLY_SAMPLE)),
        // LABELING_MACHINE — Tag BLE/FC/V
        CheckItemTemplateSeed("LM_001", "LABELING_MACHINE", "Etichettatura", "Pulizia generale macchina etichettatura", "ROUTINE", 140, listOf(POLY_TAG_BLE, POLY_TAG_FC, POLY_TAG_V)),
        CheckItemTemplateSeed("LM_002", "LABELING_MACHINE", "Etichettatura", "Pulizia lente stampante", "IMPORTANT", 141, listOf(POLY_TAG_BLE, POLY_TAG_FC, POLY_TAG_V)),
        // VIBRATORS — Tag BLE/V
        CheckItemTemplateSeed("VIB_001", "VIBRATORS", "Vibratori", "Controllo vibratore circolare", "IMPORTANT", 150, listOf(POLY_TAG_BLE)),
        CheckItemTemplateSeed("VIB_002", "VIBRATORS", "Vibratori", "Controllo vibratore lineare", "IMPORTANT", 151, listOf(POLY_TAG_BLE, POLY_TAG_V)),
        // DUAL_ROBOT — Tag V
        CheckItemTemplateSeed("DR_001", "DUAL_ROBOT", "Robot Duali", "Controllo coordinamento robot multipli", "CRITICAL", 160, listOf(POLY_TAG_V)),
        CheckItemTemplateSeed("DR_002", "DUAL_ROBOT", "Robot Duali", "Verifica sincronizzazione robot esterno/interno", "IMPORTANT", 161, listOf(POLY_TAG_V))
    )
}
