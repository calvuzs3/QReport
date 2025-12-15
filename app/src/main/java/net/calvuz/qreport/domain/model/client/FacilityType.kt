package net.calvuz.qreport.domain.model.client

import kotlinx.serialization.Serializable

/**
 * Tipologie di stabilimento
 */
@Serializable
enum class FacilityType(val displayName: String, val description: String) {
    PRODUCTION("Produzione", "Stabilimento produttivo principale"),
    WAREHOUSE("Magazzino", "Deposito e stoccaggio merci"),
    ASSEMBLY("Assemblaggio", "Linea di assemblaggio e montaggio"),
    TESTING("Test e Collaudi", "Laboratorio prove e certificazioni"),
    LOGISTICS("Logistica", "Hub distributivo e spedizioni"),
    OFFICE("Uffici", "Sede amministrativa e direzionale"),
    MAINTENANCE("Manutenzione", "Centro assistenza e riparazioni"),
    R_AND_D("Ricerca e Sviluppo", "Laboratorio ricerca e prototipazione"),
    OTHER("Altro", "Tipologia personalizzata")
}