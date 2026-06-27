package net.calvuz.qreport.checkup.checkup.presentation.model

import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityLevel
import net.calvuz.qreport.checkup.items.domain.model.CheckItem
import net.calvuz.qreport.checkup.items.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpIslandAssociation
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpProgress
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.app.error.presentation.UiText

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
    val moduleTypes: List<ModuleTypeMaster> = emptyList(),
    val statusMasters: List<CheckUpStatusMaster> = emptyList(),
    val progress: CheckUpProgress = CheckUpProgress(), // ? =null
    val statistics: CheckUpSingleStatistics = CheckUpSingleStatistics(), // ? = null

    // Photos
    val photosByCheckItem: Map<String, List<Photo>> = emptyMap(),
    val photoCountsByCheckItem: Map<String, Int> = emptyMap(),
    val isLoadingPhotos: Boolean = false,

    // Delete states
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: UiText? = null,
    val showDeleteConfirmation: Boolean = false,

    // Ui states
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isExporting: Boolean = false,
    val isUpdatingHeader: Boolean = false,
    val expandedModules: Set<String> = emptySet(),

    // Dialog states
    val showEditHeaderDialog: Boolean = false,
    val showExportDialog: Boolean = false,

// ============================================================
    // ERROR HANDLING
    // ============================================================
    val error: UiText? = null,
    val exportError: String? = null

) {
    val checkItemsByModule: Map<String, List<CheckItem>>
        get() = checkItems.groupBy { it.moduleTypeId }

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

    val hasData: Boolean
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
        get() = showEditHeaderDialog || showExportDialog


    val checkupId: String?
        get() = checkUp?.id

    val statusMaster: CheckUpStatusMaster?
        get() = statusMasters.find { it.id == checkUp?.status }


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