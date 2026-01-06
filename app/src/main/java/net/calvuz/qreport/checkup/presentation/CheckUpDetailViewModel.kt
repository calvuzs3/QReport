package net.calvuz.qreport.checkup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.domain.model.CheckUpHeader
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.domain.model.module.ModuleType
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoResult
import net.calvuz.qreport.checkup.domain.model.spare.SparePartCategory
import net.calvuz.qreport.checkup.domain.model.spare.SparePartUrgency
import net.calvuz.qreport.checkup.domain.usecase.AddSparePartUseCase
import net.calvuz.qreport.checkup.domain.usecase.DeleteCheckUpUseCase
import net.calvuz.qreport.checkup.domain.usecase.GetCheckUpDetailsUseCase
import net.calvuz.qreport.checkup.domain.usecase.UpdateCheckItemNotesUseCase
import net.calvuz.qreport.checkup.domain.usecase.UpdateCheckUpHeaderUseCase
import net.calvuz.qreport.checkup.domain.usecase.AssociateCheckUpToIslandUseCase
import net.calvuz.qreport.checkup.domain.usecase.GetAssociationsForCheckUpUseCase
import net.calvuz.qreport.checkup.domain.usecase.RemoveCheckUpAssociationUseCase
import net.calvuz.qreport.checkup.domain.usecase.UpdateCheckItemStatusUseCase
import net.calvuz.qreport.checkup.domain.usecase.UpdateCheckUpStatusUseCase
import net.calvuz.qreport.checkup.presentation.model.AssociationDialogState
import net.calvuz.qreport.checkup.presentation.model.CheckUpDetailUiState
import net.calvuz.qreport.client.client.domain.usecase.GetAllActiveClientsUseCase
import net.calvuz.qreport.client.facility.domain.usecase.GetFacilitiesByClientUseCase
import net.calvuz.qreport.client.island.domain.usecase.GetIslandsByFacilityUseCase
import net.calvuz.qreport.photo.domain.usecase.CapturePhotoUseCase
import net.calvuz.qreport.photo.domain.usecase.DeletePhotoUseCase
import net.calvuz.qreport.photo.domain.usecase.GetCheckItemPhotosUseCase
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.asErrorUiText
import net.calvuz.qreport.app.error.presentation.asUiText
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
    private val updateCheckUpHeaderUseCase: UpdateCheckUpHeaderUseCase,

    // Photo
    private val getCheckItemPhotosUseCase: GetCheckItemPhotosUseCase,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val deletePhotoUseCase: DeletePhotoUseCase,

    // Association
    private val getAllActiveClientsUseCase: GetAllActiveClientsUseCase,
    private val getFacilitiesByClientUseCase: GetFacilitiesByClientUseCase,
    private val getIslandsByFacilityUseCase: GetIslandsByFacilityUseCase,
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
                            deleteError = result.asErrorUiText() // "Errore eliminazione: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Checkup delete failed")
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deleteError = QrError.Checkup.UNKNOWN.asUiText() //"Errore imprevisto: ${e.message}"
                )
            }
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
                            error = result.asErrorUiText() // "Errore caricamento dettagli: ${error.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "Exception loading check-up details")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = (QrError.Checkup.UNKNOWN.asUiText()) //"Errore imprevisto: ${e.message}"
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
                            error =
                                // "Errore aggiornamento status: ${error.message}"
                                QrError.Checkup.UPDATE_STATUS.asUiText()
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item status")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error =
                        // "Errore imprevisto: ${e.message}"
                        QrError.Checkup.UNKNOWN.asUiText()
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
                            error =
                                // "Errore aggiornamento note: ${e.message}"
                                QrError.Checkup.UPDATE_NOTES.asUiText()
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item notes")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error =
                        // "Errore imprevisto: ${e.message}"
                        QrError.Checkup.UNKNOWN.asUiText()
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
                    error = QrError.Checkup.NOT_AVAILABLE.asUiText() //"Check-up non disponibile"
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
                            error =
                                //"Errore aggiunta ricambio: ${error.message}"
                                QrError.Checkup.SPARE_ADD.asUiText()
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception adding spare part")
                _uiState.value = _uiState.value.copy(
                    isAddingSparePart = false,
                    error =
                        // "Errore imprevisto: ${e.message}"
                        QrError.Checkup.UNKNOWN.asUiText()
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

                when( val result = updateCheckUpStatusUseCase(checkUpId, CheckUpStatus.COMPLETED)) {
                    is QrResult.Success -> {
                        Timber.d("Check-up finalized")
                        // Reload to update status
                        reloadCheckUpData(checkUpId)
                    }
                    is QrResult.Error -> {
                        Timber.e("Check-up completion failed")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error =
                                //"Errore completamento: ${error.message}"
                                QrError.Checkup.FINALIZE.asUiText()
                        )
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "Exception completing check-up")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error =
                        // "Errore imprevisto: ${e.message}"
                        QrError.Checkup.UNKNOWN.asUiText()
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
                    error =
                        // "Errore caricamento foto: ${e.message}"
                        QrError.Checkup.LOAD_PHOTOS.asUiText()
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
                    // "Check-up non disponibile"
                    error = QrError.Checkup.NOT_AVAILABLE.asUiText()
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
                            error =
                                // "Errore aggiornamento header: ${error.message}"
                                QrError.Checkup.UPDATE_HEADER.asUiText()
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating header")
                _uiState.value = _uiState.value.copy(
                    isUpdatingHeader = false,
                    error =
                        // "Errore imprevisto: ${e.message}"
                        QrError.Checkup.UNKNOWN.asUiText(),
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
                    // "CheckUp non disponibile"
                    error = QrError.Checkup.NOT_AVAILABLE.asUiText()
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
                        error =
                            // "Errore associazione: ${e.message}"
                            QrError.Checkup.ASSOCIATION.asUiText()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error =
                        // "Errore imprevisto: ${e.message}"
                        QrError.Checkup.UNKNOWN.asUiText()
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
                        error =
                            // "Errore rimozione: ${error.message}"
                            QrError.Checkup.ASSOCIATION_REMOVE.asUiText()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error =
                        // "Errore imprevisto: ${e.message}"
                        QrError.Checkup.UNKNOWN.asUiText()
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
                        error = QrError.Checkup.RELOAD.asUiText() // "Errore ricaricamento dati: ${error.message}"
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
//                        error = UiText.ErrStringResource(QrError.Checkup.ERR_RELOAD, e.message) // "Errore ricaricamento dati: ${error.message}"
//                    )
//                }
//            )
        } catch (e: Exception) {
            Timber.e(e, "Exception reloading check-up data")
            _uiState.value = _uiState.value.copy(
                isUpdating = false,
                isAddingSparePart = false,
                isUpdatingHeader = false,
                error = QrError.Checkup.UNKNOWN.asUiText() // "Errore imprevisto: ${e.message}"
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
                            error =
                                // "Errore caricamento clienti: ${error.message}"
                                QrError.Checkup.CLIENT_LOAD.asUiText(),
                        )
                    }
            } catch (e: Exception) {
                _associationState.value = _associationState.value.copy(
                    isLoadingClients = false
                )
                _uiState.value = _uiState.value.copy(
                    error =
                        // "Errore caricamento clienti: ${e.message}"
                        QrError.Checkup.CLIENT_LOAD.asUiText()
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
                        error =
                            // "Errore caricamento stabilimenti: ${error.message}"
                            QrError.Checkup.FACILITY_LOAD.asUiText()
                    )
                }
            } catch (e: Exception) {
                _associationState.value = _associationState.value.copy(
                    isLoadingFacilities = false
                )
                _uiState.value = _uiState.value.copy(
                    error =
                        //"Errore caricamento stabilimenti: ${e.message}"
                        QrError.Checkup.FACILITY_LOAD.asUiText()
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
                        error =
                            // "Errore caricamento isole: ${error.message}"
                            QrError.Checkup.ISLAND_LOAD.asUiText()
                    )
                }
            } catch (e: Exception) {
                _associationState.value = _associationState.value.copy(
                    isLoadingIslands = false
                )
                _uiState.value = _uiState.value.copy(
                    error =
                        // "Errore caricamento isole: ${e.message}"
                        QrError.Checkup.ISLAND_LOAD.asUiText()
                )
            }
        }
    }

}