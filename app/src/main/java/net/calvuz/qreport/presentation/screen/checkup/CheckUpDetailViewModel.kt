package net.calvuz.qreport.presentation.screen.checkup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUpHeader
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoResult
import net.calvuz.qreport.domain.model.spare.SparePartCategory
import net.calvuz.qreport.domain.model.spare.SparePartUrgency
import net.calvuz.qreport.domain.usecase.checkup.*
import net.calvuz.qreport.domain.usecase.photo.CapturePhotoUseCase
import net.calvuz.qreport.domain.usecase.photo.DeletePhotoUseCase
import net.calvuz.qreport.domain.usecase.photo.GetCheckItemPhotosUseCase
import net.calvuz.qreport.presentation.model.checkup.CheckUpDetailUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel per CheckUpDetailScreen - VERSIONE COMPLETA
 *
 * Gestisce:
 * - Caricamento check-up details
 * - Aggiornamento stati check items
 * - Gestione note e foto
 * - Calcolo statistiche e progresso
 * - Export functionality
 * - Gestione spare parts
 */


// ✅ NUOVO: Actions per foto
sealed class PhotoAction {
    data class NavigateToCamera(val checkItemId: String) : PhotoAction()
    data class NavigateToGallery(val checkItemId: String) : PhotoAction()
    data class LoadItemPhotos(val checkItemId: String) : PhotoAction()
}

@HiltViewModel
class CheckUpDetailViewModel @Inject constructor(
    private val getCheckUpDetailsUseCase: GetCheckUpDetailsUseCase,
    private val updateCheckUpStatusUseCase: UpdateCheckUpStatusUseCase,
    private val updateCheckItemStatusUseCase: UpdateCheckItemStatusUseCase,
    private val updateCheckItemNotesUseCase: UpdateCheckItemNotesUseCase,
    private val addSparePartUseCase: AddSparePartUseCase,
    private val exportCheckUpUseCase: ExportCheckUpUseCase,
    private val updateCheckUpHeaderUseCase: UpdateCheckUpHeaderUseCase,
    // New
    private val getCheckItemPhotosUseCase: GetCheckItemPhotosUseCase,  // ✅ NUOVO
    private val capturePhotoUseCase: CapturePhotoUseCase,              // ✅ NUOVO
    private val deletePhotoUseCase: DeletePhotoUseCase                 // ✅ NUOVO
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckUpDetailUiState())
    val uiState: StateFlow<CheckUpDetailUiState> = _uiState.asStateFlow()

    init {
        Timber.d("CheckUpDetailViewModel initialized")
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
                Timber.d("Loading check-up details: $checkUpId")

                getCheckUpDetailsUseCase(checkUpId).fold(

                    onSuccess = { checkUpDetails ->
                        Timber.d("Check-up loaded: ${checkUpDetails.checkUp.id}")

                        _uiState.value = _uiState.value.copy(
                            checkUp = checkUpDetails.checkUp,
                            checkItems = checkUpDetails.checkItems,
                            spareParts = checkUpDetails.spareParts,
                            progress = checkUpDetails.progress,
                            statistics = checkUpDetails.statistics,
                            isLoading = false,
                            error = null
                        )
                        // ✅ AGGIUNGI QUESTA CHIAMATA:
                        loadPhotosForCheckUp()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load check-up details")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Errore caricamento check-up: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception loading check-up details")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    fun updateItemStatus(itemId: String, newStatus: CheckItemStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)

            try {
                Timber.d("Updating item status: $itemId -> $newStatus")

                updateCheckItemStatusUseCase(itemId, newStatus).fold(
                    onSuccess = {
                        Timber.d("Item status updated successfully")
                        // Ricarica i dati per sincronizzare tutto
                        val checkUpId = _uiState.value.checkUp?.id
                        if (checkUpId != null) {
                            reloadCheckUpData(checkUpId)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to update item status")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore aggiornamento status: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item status")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
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
                    onFailure = { error ->
                        Timber.e(error, "Failed to update item notes")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore aggiornamento note: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating item notes")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
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
            val checkUpId = _uiState.value.checkUp?.id
            if (checkUpId == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Check-up non disponibile"
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
                    onFailure = { error ->
                        Timber.e(error, "Failed to add spare part")
                        _uiState.value = _uiState.value.copy(
                            isAddingSparePart = false,
                            error = "Errore aggiunta ricambio: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception adding spare part")
                _uiState.value = _uiState.value.copy(
                    isAddingSparePart = false,
                    error = "Errore imprevisto: ${e.message}"
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
                    onFailure = { error ->
                        Timber.e(error, "Failed to complete check-up")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore completamento: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception completing check-up")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
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
                    onFailure = { error ->
                        Timber.e(error, "Failed to export report")
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Errore export: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception exporting report")
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Errore imprevisto: ${e.message}"
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
     * Carica le foto per tutti i CheckItems del CheckUp
     */
    fun loadPhotosForCheckUp() {
        viewModelScope.launch {
            val checkItems = _uiState.value.checkItems
            if (checkItems.isEmpty()) {
                Timber.d("Nessun check item trovato, skip caricamento foto")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoadingPhotos = true)
            Timber.d("Caricamento foto per ${checkItems.size} check items")


            try {
                val photosByItem = mutableMapOf<String, List<Photo>>()
                val photoCountsByItem = mutableMapOf<String, Int>()

                // ✅ CORRETTO: Usa first() invece di collect per evitare loop infiniti
                checkItems.forEach { checkItem ->
                    try {
                        val photosResult = getCheckItemPhotosUseCase(checkItem.id).first()

                        when (photosResult) {
                            is PhotoResult.Success -> {
                                photosByItem[checkItem.id] = photosResult.data
                                photoCountsByItem[checkItem.id] = photosResult.data.size
                                Timber.d("Caricate ${photosResult.data.size} foto per item ${checkItem.id}")
                            }
                            is PhotoResult.Error -> {
                                Timber.e("Errore caricamento foto per item ${checkItem.id}: ${photosResult.exception}")
                                photosByItem[checkItem.id] = emptyList()
                                photoCountsByItem[checkItem.id] = 0
                            }
                            is PhotoResult.Loading -> {
                                Timber.d("Loading foto per item ${checkItem.id}")
                                photosByItem[checkItem.id] = emptyList()
                                photoCountsByItem[checkItem.id] = 0
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Eccezione caricamento foto per item ${checkItem.id}")
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
                Timber.d("Caricamento foto completato: $totalPhotos foto totali")

            } catch (e: Exception) {
                Timber.e(e, "Eccezione durante caricamento foto del check-up")
                _uiState.value = _uiState.value.copy(
                    isLoadingPhotos = false,
                    error = "Errore caricamento foto: ${e.message}"
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

            // ✅ CORRETTO: Usa first() invece di collect
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

                    Timber.d("Ricaricate ${photosResult.data.size} foto per item $checkItemId")
                }
                is PhotoResult.Error -> {
                    Timber.e("Errore ricaricamento foto per item $checkItemId: ${photosResult.exception}")
                }
                is PhotoResult.Loading -> {
                    Timber.d("Loading ricaricamento foto per item $checkItemId")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Eccezione ricaricamento foto per item $checkItemId")
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
                    error = "Check-up non disponibile"
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
                    onFailure = { error ->
                        Timber.e(error, "Failed to update header")
                        _uiState.value = _uiState.value.copy(
                            isUpdatingHeader = false,
                            error = "Errore aggiornamento header: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Exception updating header")
                _uiState.value = _uiState.value.copy(
                    isUpdatingHeader = false,
                    error = "Errore imprevisto: ${e.message}"
                )
            }
        }
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Ricarica i dati del check-up senza mostrare il loading
     */
    private suspend fun reloadCheckUpData(checkUpId: String) {
        try {
            getCheckUpDetailsUseCase(checkUpId).fold(
                onSuccess = { checkUpDetails ->
                    _uiState.value = _uiState.value.copy(
                        checkUp = checkUpDetails.checkUp,
                        checkItems = checkUpDetails.checkItems,
                        spareParts = checkUpDetails.spareParts,
                        progress = checkUpDetails.progress,
                        statistics = checkUpDetails.statistics,
                        isUpdating = false,
                        isAddingSparePart = false,
                        isUpdatingHeader = false
                    )
                    // ✅ AGGIUNGI REFRESH FOTO:
                    loadPhotosForCheckUp()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to reload check-up data")
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        isAddingSparePart = false,
                        isUpdatingHeader = false,
                        error = "Errore ricaricamento dati: ${error.message}"
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception reloading check-up data")
            _uiState.value = _uiState.value.copy(
                isUpdating = false,
                isAddingSparePart = false,
                isUpdatingHeader = false,
                error = "Errore imprevisto: ${e.message}"
            )
        }
    }
}