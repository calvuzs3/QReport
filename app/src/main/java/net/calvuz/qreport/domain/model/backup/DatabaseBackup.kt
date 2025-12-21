package net.calvuz.qreport.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * DatabaseBackup - Contenitore per tutti i dati del database
 */
@Serializable
data class DatabaseBackup(
    // ===== CORE CHECKUP =====
    val checkUps: List<CheckUpBackup>,
    val checkItems: List<CheckItemBackup>,
    val photos: List<PhotoBackup>,
    val spareParts: List<SparePartBackup>,

    // ===== CLIENT MANAGEMENT =====
    val clients: List<ClientBackup>,
    val contacts: List<ContactBackup>,
    val facilities: List<FacilityBackup>,
    val facilityIslands: List<FacilityIslandBackup>,

    // ===== ASSOCIATIONS =====
    val checkUpAssociations: List<CheckUpAssociationBackup>,

    // ===== METADATA =====
    @Contextual val exportedAt: Instant
) {
    /**
     * Conta totale record per validazione
     */
    fun getTotalRecordCount(): Int {
        return checkUps.size +
                checkItems.size +
                photos.size +
                spareParts.size +
                clients.size +
                contacts.size +
                facilities.size +
                facilityIslands.size +
                checkUpAssociations.size
    }

    /**
     * Verifica se il backup è vuoto
     */
    fun isEmpty(): Boolean {
        return getTotalRecordCount() == 0
    }
}