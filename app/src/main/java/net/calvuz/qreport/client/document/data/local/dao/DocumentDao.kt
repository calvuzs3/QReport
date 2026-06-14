@file:Suppress("HardCodedStringLiteral")

package net.calvuz.qreport.client.document.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.document.data.local.entity.DocumentEntity

/**
 * DAO for the island_documents table.
 *
 * Query conventions:
 *  - All read queries filter WHERE is_deleted = 0.
 *  - Flow-returning queries are used for reactive UI; suspend queries for
 *    one-shot operations (use cases, sync).
 *  - Soft-delete is performed via UPDATE, never DELETE.
 */
@Dao
interface DocumentDao {

    // =========================================================================
    // REACTIVE — by scope
    // =========================================================================

    /** All active documents for an island, newest first. */
    @Query("""
        SELECT * FROM island_documents
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForIslandFlow(islandId: String): Flow<List<DocumentEntity>>

    /** All active documents for a facility, newest first. */
    @Query("""
        SELECT * FROM island_documents
        WHERE facility_id = :facilityId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForFacilityFlow(facilityId: String): Flow<List<DocumentEntity>>

    /** All active documents for a client, newest first. */
    @Query("""
        SELECT * FROM island_documents
        WHERE client_id = :clientId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForClientFlow(clientId: String): Flow<List<DocumentEntity>>

    /** All active global documents (no FK), newest first. */
    @Query("""
        SELECT * FROM island_documents
        WHERE scope = 'GLOBAL' AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getGlobalDocumentsFlow(): Flow<List<DocumentEntity>>

    // =========================================================================
    // REACTIVE — by scope + category
    // =========================================================================

    @Query("""
        SELECT * FROM island_documents
        WHERE island_id = :islandId
          AND category = :category
          AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForIslandByCategoryFlow(
        islandId: String,
        category: String
    ): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM island_documents
        WHERE facility_id = :facilityId
          AND category = :category
          AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    fun getDocumentsForFacilityByCategoryFlow(
        facilityId: String,
        category: String
    ): Flow<List<DocumentEntity>>
    
    // =========================================================================
    // SUSPEND — multi record
    // =========================================================================
    
    /** All active documents for a client, newest first. */
    @Query("""
        SELECT * FROM island_documents
        WHERE client_id = :clientId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    suspend fun getDocumentsForClient(clientId: String): List<DocumentEntity>
    
    /** All active documents for a facility, newest first. */
    @Query("""
        SELECT * FROM island_documents
        WHERE facility_id = :facilityId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    suspend fun getDocumentsForFacility(facilityId: String): List<DocumentEntity>
    
    /** All active documents for an island, newest first. */
    @Query("""
        SELECT * FROM island_documents
        WHERE island_id = :islandId AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    suspend fun getDocumentsForIsland(islandId: String): List<DocumentEntity>
    
    // =========================================================================
    // SUSPEND — single record
    // =========================================================================

    @Query("SELECT * FROM island_documents WHERE id = :id AND is_deleted = 0")
    suspend fun getDocumentById(id: String): DocumentEntity?

    // =========================================================================
    // SUSPEND — counts
    // =========================================================================

    @Query("""
        SELECT COUNT(*) FROM island_documents
        WHERE island_id = :islandId AND is_deleted = 0
    """)
    suspend fun countDocumentsForIsland(islandId: String): Int

    @Query("""
        SELECT COUNT(*) FROM island_documents
        WHERE facility_id = :facilityId AND is_deleted = 0
    """)
    suspend fun countDocumentsForFacility(facilityId: String): Int

    @Query("""
        SELECT COUNT(*) FROM island_documents
        WHERE scope = 'GLOBAL' AND is_deleted = 0
    """)
    suspend fun countGlobalDocuments(): Int

    // =========================================================================
    // SYNC QUERIES
    // =========================================================================

    /**
     * Records with local changes not yet pushed to the server.
     * Ordered ascending so the oldest changes are sent first.
     */
    @Query("""
        SELECT * FROM island_documents
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getPendingSync(): List<DocumentEntity>

    /**
     * Records changed since a given timestamp — used by the pull response
     * to determine what to return to the requesting device.
     */
    @Query("""
        SELECT * FROM island_documents
        WHERE updated_at > :since
        ORDER BY updated_at ASC
    """)
    suspend fun getChangedSince(since: Long): List<DocumentEntity>

    // =========================================================================
    // WRITE
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    /**
     * Stage 1 soft-delete: deactivate (hidden in UI, not yet purged).
     */
    @Query("""
        UPDATE island_documents
        SET is_active = 0, updated_at = :timestamp
        WHERE id = :id
    """)
    suspend fun deactivateDocument(
        id: String,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Stage 2 soft-delete: mark as deleted (sync marker, pending purge).
     * The physical file must be deleted by the use case before or after
     * this call — both is_active and is_deleted are set atomically here.
     */
    @Query("""
        UPDATE island_documents
        SET is_active = 0, is_deleted = 1, updated_at = :timestamp
        WHERE id = :id
    """)
    suspend fun markDocumentDeleted(
        id: String,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Mark a document as successfully synced.
     * Called after a push is acknowledged by the server.
     */
    @Query("""
        UPDATE island_documents
        SET synced_at = :syncedAt
        WHERE id = :id
    """)
    suspend fun markSynced(id: String, syncedAt: Long)

    /**
     * Bulk upsert used by sync pull — inserts new records and replaces
     * existing ones (last-write-wins via updated_at comparison at use case level).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<DocumentEntity>)

    // =========================================================================
    // BACKUP
    // =========================================================================

    /** Returns all documents regardless of lifecycle state — used for full backup export. */
    @Query("SELECT * FROM island_documents ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(documents: List<DocumentEntity>)

    @Query("DELETE FROM island_documents")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM island_documents")
    suspend fun count(): Int
}