package net.calvuz.qreport.client.document.presentation.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.toUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.DocumentScope
import net.calvuz.qreport.client.document.domain.model.IslandDocument
import net.calvuz.qreport.client.document.domain.usecase.AddDocumentUseCase
import net.calvuz.qreport.client.document.domain.usecase.DeleteDocumentResult
import net.calvuz.qreport.client.document.domain.usecase.DeleteDocumentUseCase
import net.calvuz.qreport.client.document.domain.usecase.GetDocumentsForClientUseCase
import net.calvuz.qreport.client.document.domain.usecase.GetDocumentsForFacilityUseCase
import net.calvuz.qreport.client.document.domain.usecase.GetDocumentsForIslandUseCase
import net.calvuz.qreport.client.document.domain.usecase.GetGlobalDocumentsUseCase
import net.calvuz.qreport.client.document.domain.usecase.OpenDocumentUseCase
import net.calvuz.qreport.client.document.domain.usecase.UpdateDocumentUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the document tab / screen.
 *
 * [documents] is the full list from the DB; [filteredDocuments] is derived
 * by applying [categoryFilter] in the ViewModel — the Composable never filters
 * directly.
 *
 * [pendingDelete] holds the id of a document awaiting confirmation in a
 * delete dialog. Null when no dialog is showing.
 *
 * [selectedIds] is non-empty during bulk-selection mode (long-press).
 */
data class DocumentUiState(
    val documents: List<IslandDocument>         = emptyList(),
    val filteredDocuments: List<IslandDocument> = emptyList(),
    val categoryFilter: DocumentCategory?       = null,
    val isLoading: Boolean                      = false,
    val error: UiText?                          = null,

    // ── Import ────────────────────────────────────────────────────────────────
    /** True while AddDocumentUseCase is running (picker returned a URI). */
    val isImporting: Boolean                    = false,

    // ── Delete ────────────────────────────────────────────────────────────────
    /** ID of document shown in the delete-confirmation dialog. */
    val pendingDelete: String?                  = null,

    // ── Selection (bulk delete) ───────────────────────────────────────────────
    val selectedIds: Set<String>                = emptySet(),

    // ── One-shot events ───────────────────────────────────────────────────────
    /** Snackbar message consumed by LaunchedEffect, then reset to null. */
    val snackbarMessage: UiText?                = null
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    val selectedCount: Int       get() = selectedIds.size
}

/**
 * ViewModel for the Document feature.
 *
 * A single ViewModel handles all scopes (ISLAND, FACILITY, CLIENT, GLOBAL).
 * The caller decides which load function to invoke after navigation.
 *
 * Flow:
 *  - UI collects [uiState] with collectAsStateWithLifecycle().
 *  - After navigation to a scope, the screen calls the appropriate load function.
 *  - Category filter is applied in the ViewModel; the Composable uses
 *    [DocumentUiState.filteredDocuments] directly.
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val getForIsland: GetDocumentsForIslandUseCase,
    private val getForFacility: GetDocumentsForFacilityUseCase,
    private val getForClient: GetDocumentsForClientUseCase,
    private val getGlobal: GetGlobalDocumentsUseCase,
    private val addDocument: AddDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
    private val openDocument: OpenDocumentUseCase,
    private val updateDocument: UpdateDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    // =========================================================================
    // LOAD — by scope
    // =========================================================================

    fun loadForIsland(islandId: String) {
        collectDocuments {
            getForIsland(islandId)
                .catch { e ->
                    Timber.e(e, "loadForIsland failed: id=$islandId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.DynStr(e.message ?: "")
                        )
                    }
                }
        }
    }

    fun loadForFacility(facilityId: String) {
        collectDocuments {
            getForFacility(facilityId)
                .catch { e ->
                    Timber.e(e, "loadForFacility failed: id=$facilityId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.DynStr(e.message ?: "")
                        )
                    }
                }
        }
    }

    fun loadForClient(clientId: String) {
        collectDocuments {
            getForClient(clientId)
                .catch { e ->
                    Timber.e(e, "loadForClient failed: id=$clientId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.DynStr(e.message ?: "")
                        )
                    }
                }
        }
    }

    fun loadGlobal() {
        collectDocuments {
            getGlobal()
                .catch { e ->
                    Timber.e(e, "loadGlobal failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.DynStr(e.message ?: "")
                        )
                    }
                }
        }
    }

    private fun collectDocuments(
        flowProvider: () -> kotlinx.coroutines.flow.Flow<List<IslandDocument>>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            flowProvider().collect { documents ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        documents = documents,
                        filteredDocuments = applyFilter(documents, state.categoryFilter)
                    )
                }
            }
        }
    }

    // =========================================================================
    // CATEGORY FILTER
    // =========================================================================

    fun onCategoryFilterSelected(category: DocumentCategory?) {
        _uiState.update { state ->
            state.copy(
                categoryFilter = category,
                filteredDocuments = applyFilter(state.documents, category)
            )
        }
    }

    private fun applyFilter(
        documents: List<IslandDocument>,
        category: DocumentCategory?
    ): List<IslandDocument> =
        if (category == null) documents
        else documents.filter { it.category == category }

    // =========================================================================
    // ADD
    // =========================================================================

    fun onDocumentPicked(
        scope: DocumentScope,
        scopeEntityId: String?,
        uri: Uri,
        category: DocumentCategory
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null) }

            when (val result = addDocument(
                scope = scope,
                scopeEntityId = scopeEntityId,
                sourceUri = uri,
                category = category
            )) {
                is QrResult.Success -> {
                    Timber.d("DocumentViewModel: imported ${result.data.fileName}")
                    _uiState.update { it.copy(isImporting = false) }
                    // The Flow collector will refresh the list automatically
                }

                is QrResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            error = result.error.toUiText()
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // OPEN
    // =========================================================================

    fun onOpenDocument(document: IslandDocument) {
        viewModelScope.launch {
            when (val result = openDocument(document)) {
                is QrResult.Success -> Unit
                is QrResult.Error -> _uiState.update {
                    it.copy(error = result.error.toUiText())
                }
            }
        }
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    fun onUpdateDocument(document: IslandDocument) {
        viewModelScope.launch {
            when (val result = updateDocument(document)) {
                is QrResult.Success -> {
                    _uiState.update {
                        it.copy(snackbarMessage = UiText.DynStr("Documento aggiornato"))
                    }
                }

                is QrResult.Error -> _uiState.update {
                    it.copy(error = result.error.toUiText())
                }
            }
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    fun onRequestDelete(documentId: String) {
        _uiState.update { it.copy(pendingDelete = documentId) }
    }

    fun onCancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun onConfirmDelete() {
        val id = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDelete = null) }

            when (val result = deleteDocument(id)) {
                is QrResult.Success -> {
                    val msg = when (result.data) {
                        DeleteDocumentResult.DEACTIVATED -> "Documento disattivato"
                        DeleteDocumentResult.MARKED_DELETED -> "Documento eliminato"
                    }
                    _uiState.update { it.copy(snackbarMessage = UiText.DynStr(msg)) }
                }

                is QrResult.Error -> _uiState.update {
                    it.copy(error = result.error.toUiText())
                }
            }
        }
    }

    // =========================================================================
    // BULK SELECTION
    // =========================================================================

    fun onToggleSelection(documentId: String) {
        _uiState.update { state ->
            val updated = if (documentId in state.selectedIds)
                state.selectedIds - documentId
            else
                state.selectedIds + documentId
            state.copy(selectedIds = updated)
        }
    }

    fun onClearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun onDeleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(selectedIds = emptySet()) }

            var deletedCount = 0
            ids.forEach { id ->
                if (deleteDocument(id) is QrResult.Success) deletedCount++
            }

            _uiState.update {
                it.copy(snackbarMessage = UiText.DynStr("$deletedCount documenti eliminati"))
            }
        }
    }

    // =========================================================================
    // EVENTS
    // =========================================================================

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }
}