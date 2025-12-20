package net.calvuz.qreport.presentation.model.checkup

import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.checkup.CheckUpStatistics
import net.calvuz.qreport.domain.model.CriticalityLevel
import net.calvuz.qreport.domain.model.checkup.CheckUpIslandAssociation
import net.calvuz.qreport.domain.model.module.ModuleType
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.domain.model.photo.Photo


data class CheckUpDetailUiState(

    // ============================================================
    // ASSOCIATION CHECK-UP CLIENTS
    // ============================================================

    val checkUpAssociations: List<CheckUpIslandAssociation> = emptyList(),

    // ============================================================
    // CORE CHECK-UP DATA
    // ============================================================

    val checkUp: CheckUp? = null,
    val checkItems: List<CheckItem> = emptyList(),
    val spareParts: List<SparePart> = emptyList(),
    val progress: CheckUpProgress = CheckUpProgress(), // ? =null
    val statistics: CheckUpStatistics = CheckUpStatistics(), // ? = null

    // ============================================================
    // PHOTO DATA - ✅ NUOVO
    // ============================================================
    /**
     * Foto raggruppate per check item ID.
     * Key: checkItemId, Value: Lista delle foto per quel check item
     */
    val photosByCheckItem: Map<String, List<Photo>> = emptyMap(),

    /**
     * Contatori foto per check item (per performance UI).
     * Key: checkItemId, Value: Numero di foto per quel check item
     */
    val photoCountsByCheckItem: Map<String, Int> = emptyMap(),

    /**
     * Stato loading foto (mostra skeleton/spinner durante caricamento)
     */
    val isLoadingPhotos: Boolean = false,

    // ============================================================
    // UI STATES
    // ============================================================
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isExporting: Boolean = false,
    val isAddingSparePart: Boolean = false,
    val isUpdatingHeader: Boolean = false,
    val expandedModules: Set<String> = emptySet(),  // ✅ NUOVO

    // ============================================================
    // DIALOG STATES
    // ============================================================
    val showAddSparePartDialog: Boolean = false,
    val showEditHeaderDialog: Boolean = false,
    val showExportDialog: Boolean = false,

// ============================================================
    // ERROR HANDLING
    // ============================================================
    val error: String? = null,
    val exportError: String? = null

) {
    val checkItemsByModule: Map<ModuleType, List<CheckItem>>
        get() = checkItems.groupBy { it.moduleType }


    // ============================================================
    // COMPUTED PROPERTIES - ✅ NUOVO: Helper per le foto
    // ============================================================

    /**
     * Ottiene le foto per un check item specifico
     */
    fun getPhotosForCheckItem(checkItemId: String): List<Photo> {
        return photosByCheckItem[checkItemId] ?: emptyList()
    }

    /**
     * Ottiene il numero di foto per un check item specifico
     */
    fun getPhotoCountForCheckItem(checkItemId: String): Int {
        return photoCountsByCheckItem[checkItemId] ?: 0
    }

    /**
     * Verifica se un check item ha foto
     */
    fun hasPhotosForCheckItem(checkItemId: String): Boolean {
        return getPhotoCountForCheckItem(checkItemId) > 0
    }

    /**
     * Numero totale di foto in tutto il check-up
     */
    val totalPhotoCount: Int
        get() = photoCountsByCheckItem.values.sum()

    /**
     * Check items che hanno foto associate
     */
    val checkItemsWithPhotos: List<String>
        get() = photoCountsByCheckItem.filter { it.value > 0 }.keys.toList()

    // ============================================================
    // EXISTING COMPUTED PROPERTIES
    // ============================================================

    val hasCheckUp: Boolean
        get() = checkUp != null

    val hasError: Boolean
        get() = error != null

    val hasProgress: Boolean
        get() = true

    val isCompleted: Boolean
        get() = progress.overallProgress == 1f

    val completionPercentage: Float
        get() = progress.overallProgress

    val totalItems: Int
        get() = checkItems.size

    val completedItems: Int
        get() = checkItems.count { it.status == CheckItemStatus.OK }

    val criticalItems: Int
        get() = checkItems.count { it.criticality == CriticalityLevel.CRITICAL && it.status == CheckItemStatus.NOK }

    val hasDialogOpen: Boolean
        get() = showAddSparePartDialog || showEditHeaderDialog || showExportDialog
}

/**
 * Aggiorna le foto per un check item specifico
 */
fun CheckUpDetailUiState.updatePhotosForCheckItem(
    checkItemId: String,
    photos: List<Photo>
): CheckUpDetailUiState {
    val updatedPhotos = photosByCheckItem.toMutableMap()
    val updatedCounts = photoCountsByCheckItem.toMutableMap()


    updatedPhotos[checkItemId] = photos
    updatedCounts[checkItemId] = photos.size

    return copy(
        photosByCheckItem = updatedPhotos,
        photoCountsByCheckItem = updatedCounts
    )
}

/**
 * Rimuove tutte le foto di un check item
 */
fun CheckUpDetailUiState.clearPhotosForCheckItem(checkItemId: String): CheckUpDetailUiState {
    val updatedPhotos = photosByCheckItem.toMutableMap()
    val updatedCounts = photoCountsByCheckItem.toMutableMap()

    updatedPhotos.remove(checkItemId)
    updatedCounts.remove(checkItemId)

    return copy(
        photosByCheckItem = updatedPhotos,
        photoCountsByCheckItem = updatedCounts
    )
}

/**
 * Aggiunge una foto a un check item
 */
fun CheckUpDetailUiState.addPhotoToCheckItem(
    checkItemId: String,
    photo: Photo
): CheckUpDetailUiState {
    val currentPhotos = getPhotosForCheckItem(checkItemId)
    val updatedPhotos = currentPhotos + photo

    return updatePhotosForCheckItem(checkItemId, updatedPhotos)
}

/**
 * Rimuove una foto da un check item
 */
fun CheckUpDetailUiState.removePhotoFromCheckItem(
    checkItemId: String,
    photoId: String
): CheckUpDetailUiState {
    val currentPhotos = getPhotosForCheckItem(checkItemId)
    val updatedPhotos = currentPhotos.filter { it.id != photoId }

    return updatePhotosForCheckItem(checkItemId, updatedPhotos)
}
