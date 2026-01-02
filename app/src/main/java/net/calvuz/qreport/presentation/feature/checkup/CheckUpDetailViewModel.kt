package net.calvuz.qreport.presentation.feature.checkup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.core.QrResult
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUpHeader
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.module.ModuleType
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoResult
import net.calvuz.qreport.domain.model.spare.SparePartCategory
import net.calvuz.qreport.domain.model.spare.SparePartUrgency
import net.calvuz.qreport.domain.usecase.checkup.*
import net.calvuz.qreport.domain.usecase.checkup.AssociateCheckUpToIslandUseCase
import net.calvuz.qreport.domain.usecase.checkup.GetAssociationsForCheckUpUseCase
import net.calvuz.qreport.domain.usecase.checkup.RemoveCheckUpAssociationUseCase
import net.calvuz.qreport.domain.usecase.client.client.GetAllActiveClientsUseCase
import net.calvuz.qreport.domain.usecase.client.facility.GetFacilitiesByClientUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.GetFacilityIslandsByFacilityUseCase
import net.calvuz.qreport.domain.usecase.export.ExportCheckUpUseCase
import net.calvuz.qreport.domain.usecase.photo.CapturePhotoUseCase
import net.calvuz.qreport.domain.usecase.photo.DeletePhotoUseCase
import net.calvuz.qreport.domain.usecase.photo.GetCheckItemPhotosUseCase
import net.calvuz.qreport.presentation.core.model.DataError
import net.calvuz.qreport.presentation.core.model.UiText
import net.calvuz.qreport.presentation.core.model.asUiText
import net.calvuz.qreport.presentation.feature.checkup.model.AssociationDialogState
import net.calvuz.qreport.presentation.feature.checkup.model.CheckUpDetailUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * CheckUpDetailScreen ViewModel
 */


// Photos' Actions
sealed class PhotoAction {
    data class NavigateToCamera(val checkItemId: String) : PhotoAction()
    data class NavigateToGallery(val checkItemId: String) : PhotoAction()
    data class LoadItemPhotos(val checkItemId: String) : PhotoAction()
}

@HiltViewModel
class CheckUpDetailViewModel @Inject constructor(
    private val getCheckUpDetailsUseCase: GetCheckUpDetailsUseCase,
    private val deleteCheckUpUseCase: DeleteCheckUpUseCase,
    private val updateCheckUpStatusUseCase: UpdateCheckUpStatusUseCase,
    private val updateCheckItemStatusUseCase: UpdateCheckItemStatusUseCase,
    private val updateCheckItemNotesUseCase: UpdateCheckItemNotesUseCase,
    private val addSparePartUseCase: AddSparePartUseCase,
    private val exportCheckUpUseCase: ExportCheckUpUseCase,
    private val updateCheckUpHeaderUseCase: UpdateCheckUpHeaderUseCase,

    // Photo
    private val getCheckItemPhotosUseCase: GetCheckItemPhotosUseCase,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val deletePhotoUseCase: DeletePhotoUseCase,

    // Association
    private val getAllActiveClientsUseCase: GetAllActiveClientsUseCase,
    private val getFacilitiesByClientUseCase: GetFacilitiesByClientUseCase,
    private val getIslandsByFacilityUseCase: GetFacilityIslandsByFacilityUseCase,
    private val associateCheckUpToIslandUseCase: AssociateCheckUpToIslandUseCase,
    private val getAssociationsForCheckUpUseCase: GetAssociationsForCheckUpUseCase,
    private val removeCheckUpAssociationUseCase: RemoveCheckUpAssociationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckUpDetailUiState())
    val uiState: StateFlow<CheckUpDetailUiState> = _uiState.asStateFlow()

    // Module expansion handler
    private val _expandedModules = MutableStateFlow<Set<String>>(emptySet())
    val expandedModules: StateFlow<Set<String>> = _expandedModules.asStateFlow()

    // Association state
    private val _associationState = MutableStateFlow(AssociationDialogState())
    val associationState = _associationState.asStateFlow()


    init {
        Timber.i("CheckUpDetailViewModel initialized")
    }

    // ============================================================
    // DELETE OPERATIONS
    // ============================================================

    /**
     * Mostra dialog di conferma prima di eliminare
     */
    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true
        )
    }

    /**
     * Nasconde dialog di conferma
     */
    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false
        )
    }

    /**
     * Delete main  function
     */
    fun deleteCheckUp() {
        val checkupId = _uiState.value.checkupId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeleting = true,
                deleteError = null,
                showDeleteConfirmation = false
            )

            try {
                when (val result = deleteCheckUpUseCase(checkupId)) {
                    is QrResult.Success -> {
                        Timber.d("Checkup deleted: $checkupId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteSuccess = true  // Trigger navigation back
                        )
                    }

                    is QrResult.Error -> {
                        Timber.e("Checkup delete failed: $checkupId")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deleteError = result.error.asUiText() // "Errore eliminazione: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Checkup delete failed")
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deleteError = DataError.CheckupError.UNKNOWN.asUiText() //"Errore imprevisto: ${e.message}"
                )
            }
//            try {
//                deleteCheckUpUseCase(checkupId).fold(
//                    onSuccess = {
//                        Timber.d("Checkup deleted: $checkupId")
//                        _uiState.value = _uiState.value.copy(
//                            isDeleting = false,
//                            deleteSuccess = true  // ✅ Trigger navigation back
//                        )
//                    },
//                    onFailure = { error ->
//                        Timber.e(error, "Checkup delete failed: $checkupId")
//                        _uiState.value = _uiState.value.copy(
//                            isDeleting = false,
//                            deleteError = "Errore eliminazione: ${error.message}"
//                        )
//                    }
//                )
//            } catch (e: Exception) {
//                Timber.e(e, "Checkup delete failed")
//                _uiState.value = _uiState.value.copy(
//                    isDeleting = false,
//                    deleteError = "Errore imprevisto: ${e.message}"
//                )
//            }
        }
    }

    /**
     * Reset delete states
     */
    fun resetDeleteState() {
        _uiState.value = _uiState.value.copy(
            deleteSuccess = false,
            deleteError = null
        )
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun loadCheckUp(checkUpId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Timber.d("Loading check-up details for: $checkUpId")

                when (val result = getCheckUpDetailsUseCase(checkUpId)) {
                    is QrResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            checkUp = result.data.checkUp,
                            checkItems = result.data.checkItems,
                            spareParts = result.data.spareParts,
                            progress = result.data.progress,
                            statistics = result.data.statistics,
                            isLoading = false,
                            error = null
                        )

                        // Load photos
                        loadPhotosForCheckUp()

                        //Load associations
                        loadCurrentAssociations()
                    }

                    is QrResult.Error -> {
                        Timber.e("Check-up details load failed")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = DataError.CheckupError.LOAD.asUiText()
                        )
                    }
                }
//                getCheckUpDetailsUseCase(checkUpId).fold(
//
//                    onSuccess = { checkUpDetails ->
//                        _uiState.value = _uiState.value.copy(
//                            checkUp = checkUpDetails.checkUp,
//                            checkItems = checkUpDetails.checkItems,
//                            spareParts = checkUpDetails.spareParts,
//                            progress = checkUpDetails.progress,
//                            statistics = checkUpDetails.statistics,
//                            isLoading = false,
//                            error = null
//                        )
//
//                        // Load photos
//                        loadPhotosForCheckUp()
//
//                        //Load associations
//                        loadCurrentAssociations()
//                    },
//                    onFailure = { error ->
//                        Timber.e(error, "Check-up details load failed")
//                        _uiState.value = _uiState.value.copy(
//                            isLoading = false,
//                            error = UiText.ErrStringResource(DataError.QrError.ERR_CHECKUP_LOAD_CHECKUP, error.message)
//                        )
//                    }
//                )

            } catch (e: Exception) {
                Timber.e(e, "Exception loading check-up details")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = (DataError.CheckupError.UNKNOWN.asUiText()) //"Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun updateItemStatus(itemId: String, newStatus: CheckItemStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)

            try {
                Timber.d("Updating check-up item status: $itemId -> $newStatus")

                updateCheckItemStatusUseCase(itemId, newStatus).fold(
                    onSuccess = {
                        Timber.d("Check-up item status updated successfully")
                        // Ricarica i dati per sincronizzare tutto
                        val checkUpId = _uiState.value.checkUp?.id
                        if (checkUpId != null) {
                            reloadCheckUpData(checkUpId)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to update check-up item status")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = UiText.ErrStringResource(
                                DataError.QrError.ERR_CHECKUP_UPDATE_STATUS,
                                error.message
                            ) // "Errore aggiornamento status: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item status")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun updateItemNotes(itemId: String, notes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)

            try {
                Timber.d("Updating item notes: $itemId")

                updateCheckItemNotesUseCase(itemId, notes).fold(
                    onSuccess = {
                        Timber.d("Item notes updated successfully")
                        // Ricarica i dati per sincronizzare tutto
                        val checkUpId = _uiState.value.checkUp?.id
                        if (checkUpId != null) {
                            reloadCheckUpData(checkUpId)
                        }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to update item notes")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = UiText.ErrStringResource(
                                DataError.QrError.ERR_CHECKUP_UPDATE_NOTES,
                                e.message
                            ) // "Errore aggiornamento note: ${e.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item notes")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun addSparePart(
        partNumber: String,
        description: String,
        quantity: Int,
        urgency: SparePartUrgency,
        category: SparePartCategory,
        estimatedCost: Double? = null,
        notes: String = "",
        supplierInfo: String = ""
    ) {
        viewModelScope.launch {
            Timber.d("🔵 ViewModel.addSparePart: INIZIO")
            Timber.d("🔵 ViewModel.addSparePart: partNumber=$partNumber")
            Timber.d("🔵 ViewModel.addSparePart: checkUpId=${_uiState.value.checkUp?.id}")

            val checkUpId = _uiState.value.checkUp?.id
            if (checkUpId == null) {
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(DataError.QrError.ERR_CHECKUP_NOT_AVAILABLE) //"Check-up non disponibile"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isAddingSparePart = true)

            try {
                Timber.d("Adding spare part to check-up: $checkUpId")

                addSparePartUseCase(
                    checkUpId = checkUpId,
                    partNumber = partNumber,
                    description = description,
                    quantity = quantity,
                    urgency = urgency,
                    category = category,
                    estimatedCost = estimatedCost,
                    notes = notes,
                    supplierInfo = supplierInfo
                ).fold(
                    onSuccess = { sparePartId ->
                        Timber.d("Spare part added successfully: $sparePartId")
                        // Ricarica i dati per includere il nuovo spare part
                        reloadCheckUpData(checkUpId)
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to add spare part")
                        _uiState.value = _uiState.value.copy(
                            isAddingSparePart = false,
                            error = UiText.ErrStringResource(
                                DataError.QrError.ERR_CHECKUP_SPARE_ADD,
                                e.message
                            ) //"Errore aggiunta ricambio: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception adding spare part")
                _uiState.value = _uiState.value.copy(
                    isAddingSparePart = false,
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun completeCheckUp() {
        viewModelScope.launch {
            try {
                val checkUpId = _uiState.value.checkUp?.id ?: return@launch
                Timber.d("Completing check-up: $checkUpId")

                _uiState.value = _uiState.value.copy(isUpdating = true)

                updateCheckUpStatusUseCase(checkUpId, CheckUpStatus.COMPLETED).fold(
                    onSuccess = {
                        Timber.d("Check-up completed successfully")
                        // Ricarica per aggiornare il status
                        reloadCheckUpData(checkUpId)
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to complete check-up")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = UiText.ErrStringResource(
                                DataError.QrError.ERR_CHECKUP_FINALIZE,
                                e.message
                            ) //"Errore completamento: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception completing check-up")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            try {
                val checkUpId = _uiState.value.checkUp?.id ?: return@launch
                Timber.d("Exporting report: $checkUpId")

                _uiState.value = _uiState.value.copy(isUpdating = true)

                exportCheckUpUseCase(checkUpId).fold(
                    onSuccess = { exportedFile ->
                        Timber.d("Report exported successfully")
                        _uiState.value = _uiState.value.copy(isUpdating = false)
                        // TODO: Show success message or open file
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to export report")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = UiText.ErrStringResource(
                                DataError.QrError.ERR_CHECKUP_EXPORT,
                                e.message
                            ) // "Errore export: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception exporting report")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun showAddSparePartDialog() {
        _uiState.value = _uiState.value.copy(showAddSparePartDialog = true)
    }

    fun hideAddSparePartDialog() {
        _uiState.value = _uiState.value.copy(showAddSparePartDialog = false)
    }

    /**
     * Load photos for all CheckItems
     */
    fun loadPhotosForCheckUp() {
        viewModelScope.launch {
            val checkItems = _uiState.value.checkItems
            if (checkItems.isEmpty()) {
                Timber.w("No check item found, skipping loading photos")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoadingPhotos = true)
            Timber.v("Loading photos for ${checkItems.size} check items")


            try {
                val photosByItem = mutableMapOf<String, List<Photo>>()
                val photoCountsByItem = mutableMapOf<String, Int>()

                // use first() instead of collect to avoid infinite loops
                checkItems.forEach { checkItem ->
                    try {
                        val photosResult = getCheckItemPhotosUseCase(checkItem.id).first()

                        when (photosResult) {
                            is PhotoResult.Success -> {
                                photosByItem[checkItem.id] = photosResult.data
                                photoCountsByItem[checkItem.id] = photosResult.data.size
                                Timber.v("Loaded ${photosResult.data.size} photos for item ${checkItem.id}")
                            }

                            is PhotoResult.Error -> {
                                Timber.e("Error loading photos for item ${checkItem.id}: ${photosResult.exception}")
                                photosByItem[checkItem.id] = emptyList()
                                photoCountsByItem[checkItem.id] = 0
                            }

                            is PhotoResult.Loading -> {
                                Timber.v("Loading photos for item ${checkItem.id}")
                                photosByItem[checkItem.id] = emptyList()
                                photoCountsByItem[checkItem.id] = 0
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Photo load for Item failed {${checkItem.id}}")
                        photosByItem[checkItem.id] = emptyList()
                        photoCountsByItem[checkItem.id] = 0
                    }
                }

                // ✅ Aggiorna lo stato con tutte le foto caricate
                _uiState.value = _uiState.value.copy(
                    photosByCheckItem = photosByItem,
                    photoCountsByCheckItem = photoCountsByItem,
                    isLoadingPhotos = false
                )

                val totalPhotos = photoCountsByItem.values.sum()
                Timber.d("Loaded photos: $totalPhotos")

            } catch (e: Exception) {
                Timber.e(e, "Photo load for Checkup failed")
                _uiState.value = _uiState.value.copy(
                    isLoadingPhotos = false,
                    error = UiText.ErrStringResource(
                        DataError.QrError.ERR_CHECKUP_LOAD_PHOTOS,
                        e.message
                    ) // "Errore caricamento foto: ${e.message}"
                )
            }
        }
    }

    /**
     * Carica le foto per un singolo CheckItem (utile dopo scatto foto)
     */
    suspend fun loadPhotosForCheckItem(checkItemId: String) {
        try {
            Timber.d("Ricaricamento foto per item: $checkItemId")

            // use first() instead of collect
            val photosResult = getCheckItemPhotosUseCase(checkItemId).first()

            when (photosResult) {
                is PhotoResult.Success -> {
                    val currentPhotos = _uiState.value.photosByCheckItem.toMutableMap()
                    val currentCounts = _uiState.value.photoCountsByCheckItem.toMutableMap()

                    currentPhotos[checkItemId] = photosResult.data
                    currentCounts[checkItemId] = photosResult.data.size

                    _uiState.value = _uiState.value.copy(
                        photosByCheckItem = currentPhotos,
                        photoCountsByCheckItem = currentCounts
                    )

                    Timber.v("Reloaded photos: ${photosResult.data.size}, item: $checkItemId")
                }

                is PhotoResult.Error -> {
                    Timber.e("Load photos for item failed {$checkItemId}: ${photosResult.exception}")
                }

                is PhotoResult.Loading -> {
                    Timber.v("Loading photo for item {$checkItemId}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Loading photo for item failed {$checkItemId}")
        }
    }

    /**
     * Refresh delle foto dopo operazioni camera/gallery
     */
    fun refreshPhotosAfterCameraReturn() {
        loadPhotosForCheckUp()
    }

    fun refreshPhotosForItem(checkItemId: String) {
        viewModelScope.launch {
            loadPhotosForCheckItem(checkItemId)
        }
    }

    /**
     * DEBUG: Mostra lo stato attuale delle foto
     */
    fun debugPhotoState() {
        val state = _uiState.value
        Timber.d("=== DEBUG PHOTO STATE ===")
        Timber.d("Total photos: ${state.totalPhotoCount}")
        Timber.d("Loading photos: ${state.isLoadingPhotos}")
        state.photosByCheckItem.forEach { (itemId, photos) ->
            Timber.d("Item $itemId: ${photos.size} foto")
            photos.forEach { photo ->
                Timber.d("  - ${photo.fileName}: ${photo.filePath}")
            }
        }
        Timber.d("========================")
    }

    // ============================================================
    // NUOVO METODO OPZIONALE: Observa foto in tempo reale
    // ============================================================

    /**
     * ✅ NUOVO: Metodo per osservare foto in tempo reale (se necessario)
     * Usa questo SOLO se vuoi updates automatici quando le foto cambiano
     */
    fun startObservingPhotos() {
        viewModelScope.launch {
            val checkItems = _uiState.value.checkItems
            if (checkItems.isEmpty()) return@launch

            checkItems.forEach { checkItem ->
                // Osserva ogni check item separatamente
                getCheckItemPhotosUseCase(checkItem.id)
                    .onEach { photosResult ->
                        if (photosResult is PhotoResult.Success) {
                            val currentPhotos = _uiState.value.photosByCheckItem.toMutableMap()
                            val currentCounts = _uiState.value.photoCountsByCheckItem.toMutableMap()

                            currentPhotos[checkItem.id] = photosResult.data
                            currentCounts[checkItem.id] = photosResult.data.size

                            _uiState.value = _uiState.value.copy(
                                photosByCheckItem = currentPhotos,
                                photoCountsByCheckItem = currentCounts
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    // ============================================================
    // HEADER EDITING METHODS
    // ============================================================

    fun showEditHeaderDialog() {
        _uiState.value = _uiState.value.copy(showEditHeaderDialog = true)
    }

    fun hideEditHeaderDialog() {
        _uiState.value = _uiState.value.copy(showEditHeaderDialog = false)
    }

    fun updateCheckUpHeader(newHeader: CheckUpHeader) {
        viewModelScope.launch {
            val checkUpId = _uiState.value.checkUp?.id
            if (checkUpId == null) {
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(DataError.QrError.ERR_CHECKUP_NOT_AVAILABLE) // "Check-up non disponibile"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUpdatingHeader = true)

            try {
                Timber.d("Updating check-up header: $checkUpId")

                updateCheckUpHeaderUseCase(checkUpId, newHeader).fold(
                    onSuccess = {
                        Timber.d("Header updated successfully")
                        // Ricarica i dati per sincronizzare tutto
                        reloadCheckUpData(checkUpId)
                        _uiState.value = _uiState.value.copy(
                            showEditHeaderDialog = false
                        )
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to update header")
                        _uiState.value = _uiState.value.copy(
                            isUpdatingHeader = false,
                            error = UiText.ErrStringResource(
                                DataError.QrError.ERR_CHECKUP_UPDATE_HEADER,
                                e.message
                            ) // "Errore aggiornamento header: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating header")
                _uiState.value = _uiState.value.copy(
                    isUpdatingHeader = false,
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    // Association handling

    /**
     * Show Association Dialog
     */
    fun showAssociationDialog() {
        _associationState.value = _associationState.value.copy(
            showDialog = true,
            isLoadingClients = true
        )

        loadAvailableClients()
        loadCurrentAssociations()
    }

    fun hideAssociationDialog() {
        _associationState.value = AssociationDialogState() // Reset completo
    }

    fun onClientSelected(clientId: String) {
        _associationState.value = _associationState.value.copy(
            selectedClientId = clientId,
            selectedFacilityId = null, // Reset facility
            availableFacilities = emptyList(),
            availableIslands = emptyList(),
            isLoadingFacilities = true
        )

        loadFacilitiesForClient(clientId)
    }

    fun onFacilitySelected(facilityId: String) {
        _associationState.value = _associationState.value.copy(
            selectedFacilityId = facilityId,
            availableIslands = emptyList(),
            isLoadingIslands = true
        )

        loadIslandsForFacility(facilityId)
    }

    fun onIslandSelected(islandId: String) {
        viewModelScope.launch {
            val checkUpId = _uiState.value.checkUp?.id
            if (checkUpId == null) {
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(DataError.QrError.ERR_CHECKUP_NOT_AVAILABLE) // "CheckUp non disponibile"
                )
                return@launch
            }

            try {
                associateCheckUpToIslandUseCase(
                    checkupId = checkUpId,
                    islandId = islandId
                ).onSuccess {
                    // Success - refresh e chiudi dialog
                    loadCurrentAssociations()
                    hideAssociationDialog()

                    _uiState.value = _uiState.value.copy(
                        error = null
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = UiText.ErrStringResource(
                            DataError.QrError.ERR_CHECKUP_ASSOCIATION,
                            e.message
                        ) // "Errore associazione: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun removeAssociation() {
        viewModelScope.launch {
            val checkUpId = _uiState.value.checkUp?.id
            if (checkUpId == null) return@launch

            try {
                removeCheckUpAssociationUseCase(checkUpId).onSuccess {
                    // Success - refresh e chiudi dialog
                    loadCurrentAssociations()
                    hideAssociationDialog()

                    _uiState.value = _uiState.value.copy(
                        error = null
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = UiText.ErrStringResource(
                            DataError.QrError.ERR_CHECKUP_ASSOCIATION_REMOVE,
                            e.message
                        ) // "Errore rimozione: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(
                        DataError.QrError.UNKNOWN,
                        e.message
                    ) // "Errore imprevisto: ${e.message}"
                )
            }
        }
    }


    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Reload CheckUp data without loading message show
     */
    private suspend fun reloadCheckUpData(checkUpId: String) {
        try {
            when (val result = getCheckUpDetailsUseCase(checkUpId)) {
                is QrResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        checkUp = result.data.checkUp,
                        checkItems = result.data.checkItems,
                        spareParts = result.data.spareParts,
                        progress = result.data.progress,
                        statistics = result.data.statistics,
                        isUpdating = false,
                        isAddingSparePart = false,
                        isUpdatingHeader = false
                    )
                    // Photos REFRESH
                    loadPhotosForCheckUp()
                }

                is QrResult.Error -> {
                    Timber.e("Failed to reload check-up data")
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        isAddingSparePart = false,
                        isUpdatingHeader = false,
                        error = DataError.CheckupError.RELOAD.asUiText() // "Errore ricaricamento dati: ${error.message}"
                    )
                }
            }
//            getCheckUpDetailsUseCase(checkUpId).fold(
//                onSuccess = { checkUpDetails ->
//                    _uiState.value = _uiState.value.copy(
//                        checkUp = checkUpDetails.checkUp,
//                        checkItems = checkUpDetails.checkItems,
//                        spareParts = checkUpDetails.spareParts,
//                        progress = checkUpDetails.progress,
//                        statistics = checkUpDetails.statistics,
//                        isUpdating = false,
//                        isAddingSparePart = false,
//                        isUpdatingHeader = false
//                    )
//                    // Photos REFRESH
//                    loadPhotosForCheckUp()
//                },
//                onFailure = { e ->
//                    Timber.e(e, "Failed to reload check-up data")
//                    _uiState.value = _uiState.value.copy(
//                        isUpdating = false,
//                        isAddingSparePart = false,
//                        isUpdatingHeader = false,
//                        error = UiText.ErrStringResource(DataError.QrError.ERR_RELOAD, e.message) // "Errore ricaricamento dati: ${error.message}"
//                    )
//                }
//            )
        } catch (e: Exception) {
            Timber.e(e, "Exception reloading check-up data")
            _uiState.value = _uiState.value.copy(
                isUpdating = false,
                isAddingSparePart = false,
                isUpdatingHeader = false,
                error = DataError.CheckupError.UNKNOWN.asUiText() // "Errore imprevisto: ${e.message}"
            )
        }
    }

    // Module expansion/collapsing toggle
    fun toggleModuleExpansion(moduleType: ModuleType) {
        val moduleKey = moduleType.name
        val current = _expandedModules.value
        _expandedModules.value = if (moduleKey in current) {
            current - moduleKey  // Collapse module
        } else {
            current + moduleKey  // Expand module
        }
    }

    fun isModuleExpanded(moduleType: ModuleType): Boolean {
        return moduleType.name in _expandedModules.value
    }

    /**
     * Load current associations for this checkup
     */
    private fun loadCurrentAssociations() {
        viewModelScope.launch {
            val checkUpId = _uiState.value.checkUp?.id ?: return@launch

            try {
                getAssociationsForCheckUpUseCase(checkUpId).onSuccess { associations ->
                    _uiState.value = _uiState.value.copy(
                        checkUpAssociations = associations
                    )

                    _associationState.value = _associationState.value.copy(
                        currentAssociations = associations
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Load current associations failed")
            }
        }
    }

    /**
     * Load all active clients
     */
    private fun loadAvailableClients() {
        viewModelScope.launch {
            try {
                getAllActiveClientsUseCase()
                    .onSuccess { clients ->
                        _associationState.value = _associationState.value.copy(
                            availableClients = clients,
                            isLoadingClients = false
                        )
                    }
                    .onFailure { e ->
                        _associationState.value = _associationState.value.copy(
                            isLoadingClients = false
                        )
                        _uiState.value = _uiState.value.copy(
                            error = UiText.ErrStringResource(
                                DataError.QrError.ERR_CLIENT_LOAD,
                                e.message
                            ) // "Errore caricamento clienti: ${error.message}"
                        )
                    }
            } catch (e: Exception) {
                _associationState.value = _associationState.value.copy(
                    isLoadingClients = false
                )
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(
                        DataError.QrError.ERR_CLIENT_LOAD,
                        e.message
                    ) // "Errore caricamento clienti: ${e.message}"
                )
            }
        }
    }

    /**
     * Load all facilities for this client
     */
    private fun loadFacilitiesForClient(clientId: String) {
        viewModelScope.launch {
            try {
                getFacilitiesByClientUseCase(clientId).onSuccess { facilities ->
                    _associationState.value = _associationState.value.copy(
                        availableFacilities = facilities,
                        isLoadingFacilities = false
                    )
                }.onFailure { e ->
                    _associationState.value = _associationState.value.copy(
                        isLoadingFacilities = false
                    )
                    _uiState.value = _uiState.value.copy(
                        error = UiText.ErrStringResource(
                            DataError.QrError.ERR_FACILITY_LOAD,
                            e.message
                        ) // "Errore caricamento stabilimenti: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                _associationState.value = _associationState.value.copy(
                    isLoadingFacilities = false
                )
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(
                        DataError.QrError.ERR_FACILITY_LOAD,
                        e.message
                    ) //"Errore caricamento stabilimenti: ${e.message}"
                )
            }
        }
    }

    /**
     * Load facility islands for this facility
     */
    private fun loadIslandsForFacility(facilityId: String) {
        viewModelScope.launch {
            try {
                getIslandsByFacilityUseCase(facilityId).onSuccess { islands ->
                    _associationState.value = _associationState.value.copy(
                        availableIslands = islands,
                        isLoadingIslands = false
                    )
                }.onFailure { e ->
                    _associationState.value = _associationState.value.copy(
                        isLoadingIslands = false
                    )
                    _uiState.value = _uiState.value.copy(
                        error = UiText.ErrStringResource(
                            DataError.QrError.ERR_ISLAND_LOAD,
                            e.message
                        ) // "Errore caricamento isole: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                _associationState.value = _associationState.value.copy(
                    isLoadingIslands = false
                )
                _uiState.value = _uiState.value.copy(
                    error = UiText.ErrStringResource(
                        DataError.QrError.ERR_ISLAND_LOAD,
                        e.message
                    ) // "Errore caricamento isole: ${e.message}"
                )
            }
        }
    }

}