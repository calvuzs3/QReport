package net.calvuz.qreport.presentation.feature.checkup.model

import net.calvuz.qreport.domain.model.client.CriticalityLevel
import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpIslandAssociation
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.checkup.CheckUpSingleStatistics
import net.calvuz.qreport.domain.model.module.ModuleType
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.presentation.core.model.UiText

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
    val statistics: CheckUpSingleStatistics = CheckUpSingleStatistics(), // ? = null

    // ============================================================
    // PHOTO DATA
    // ============================================================
    /**
     * Photos grouped by check item ID
     */
    val photosByCheckItem: Map<String, List<Photo>> = emptyMap(),

    /**
     * Counters photo per chek item
     */
    val photoCountsByCheckItem: Map<String, Int> = emptyMap(),

    /**
     * State loading photo (show skeleton/spinner during loading)
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
    val expandedModules: Set<String> = emptySet(),

    // ============================================================
    // DIALOG STATES
    // ============================================================
    val showAddSparePartDialog: Boolean = false,
    val showEditHeaderDialog: Boolean = false,
    val showExportDialog: Boolean = false,

// ============================================================
    // ERROR HANDLING
    // ============================================================
    val error: UiText? = null,
    val exportError: String? = null

) {
    val checkItemsByModule: Map<ModuleType, List<CheckItem>>
        get() = checkItems.groupBy { it.moduleType }


    // ============================================================
    // COMPUTED PROPERTIES
    // ============================================================

    /**
     * check item' photos
     */
    fun getPhotosForCheckItem(checkItemId: String): List<Photo> {
        return photosByCheckItem[checkItemId] ?: emptyList()
    }

    /**
     * check item photos count
     */
    fun getPhotoCountForCheckItem(checkItemId: String): Int {
        return photoCountsByCheckItem[checkItemId] ?: 0
    }

    /**
     * check if a chek item has photos
     */
    fun hasPhotosForCheckItem(checkItemId: String): Boolean {
        return getPhotoCountForCheckItem(checkItemId) > 0
    }

    /**
     *  total number of photos
     */
    val totalPhotoCount: Int
        get() = photoCountsByCheckItem.values.sum()

    /**
     * Check items with photos
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


    /**
     * update photos for a check item
     */
    fun updatePhotosForCheckItem(
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
     * remove all photos from a check item
     */
    fun clearPhotosForCheckItem(checkItemId: String): CheckUpDetailUiState {
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
     * Add a photo to a check item
     */
    fun addPhotoToCheckItem(
        checkItemId: String,
        photo: Photo
    ): CheckUpDetailUiState {
        val currentPhotos = getPhotosForCheckItem(checkItemId)
        val updatedPhotos = currentPhotos + photo

        return updatePhotosForCheckItem(checkItemId, updatedPhotos)
    }

    /**
     * remove a photo from a check item
     */
    fun removePhotoFromCheckItem(
        checkItemId: String,
        photoId: String
    ): CheckUpDetailUiState {
        val currentPhotos = getPhotosForCheckItem(checkItemId)
        val updatedPhotos = currentPhotos.filter { it.id != photoId }

        return updatePhotosForCheckItem(checkItemId, updatedPhotos)
    }
}