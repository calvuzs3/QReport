package net.calvuz.qreport.client.document.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.IslandDocument

/**
 * Domain contract for document persistence.
 *
 * Returns [kotlin.Result] for suspend operations — error translation to
 * [net.calvuz.qreport.app.error.domain.model.QrError] happens in use cases,
 * not here.
 *
 * Flow-returning methods emit the latest DB state reactively and never fail
 * silently — Room propagates exceptions as Flow errors if the DB is unavailable.
 */
interface DocumentRepository {

    // =========================================================================
    // REACTIVE
    // =========================================================================

    fun getDocumentsForIslandFlow(islandId: String): Flow<List<IslandDocument>>

    fun getDocumentsForFacilityFlow(facilityId: String): Flow<List<IslandDocument>>

    fun getDocumentsForClientFlow(clientId: String): Flow<List<IslandDocument>>

    fun getGlobalDocumentsFlow(): Flow<List<IslandDocument>>

    fun getDocumentsForIslandByCategoryFlow(
        islandId: String,
        category: DocumentCategory
    ): Flow<List<IslandDocument>>

    fun getDocumentsForFacilityByCategoryFlow(
        facilityId: String,
        category: DocumentCategory
    ): Flow<List<IslandDocument>>

    // =========================================================================
    // SUSPEND — read
    // =========================================================================

    suspend fun getDocumentById(id: String): Result<IslandDocument?>

    suspend fun countDocumentsForIsland(islandId: String): Result<Int>

    suspend fun countDocumentsForFacility(facilityId: String): Result<Int>

    suspend fun countGlobalDocuments(): Result<Int>

    // =========================================================================
    // SUSPEND — write
    // =========================================================================

    suspend fun insertDocument(document: IslandDocument): Result<Unit>

    suspend fun updateDocument(document: IslandDocument): Result<Unit>

    /**
     * Two-stage soft-delete.
     * Calling once sets isActive=false (stage 1 — deactivated).
     * Calling a second time sets isDeleted=true (stage 2 — marked for purge).
     *
     * The physical file must be removed by the use case separately.
     */
    suspend fun markDeleted(id: String): Result<Unit>

    // =========================================================================
    // SYNC
    // =========================================================================

    /** Records with local changes not yet pushed to the server. */
    suspend fun getPendingSync(): Result<List<IslandDocument>>

    /** Records changed since the given epoch-ms timestamp. */
    suspend fun getChangedSince(since: Long): Result<List<IslandDocument>>

    /** Mark a single record as successfully synced at [syncedAt]. */
    suspend fun markSynced(id: String, syncedAt: Long): Result<Unit>

    /**
     * Bulk upsert used by sync pull.
     * Inserts new records and replaces existing ones (OnConflictStrategy.REPLACE).
     * Last-write-wins conflict resolution is handled at the use case level
     * before calling this method.
     */
    suspend fun upsertDocuments(documents: List<IslandDocument>): Result<Unit>
}