package net.calvuz.qreport.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.contact.data.local.entity.ContactEntity
import net.calvuz.qreport.client.contract.data.local.entity.ContractEntity
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity
import net.calvuz.qreport.client.unit.data.local.entity.MechanicalUnitEntity

/**
 * DAO for sync operations on the client management entity group.
 *
 * Covers: clients, contacts, contracts, facilities, facility_islands, mechanical_units.
 *
 * Two query types are used throughout:
 *  - "pending push": records modified locally after the last sync
 *    (updated_at > synced_at OR synced_at IS NULL), including soft-deleted ones
 *  - "upsert from server": insert or replace incoming records from the server
 */
@Dao
interface SyncDao {

    // ===== PENDING PUSH — records to send to the server =====

    /**
     * Returns clients that have local changes not yet pushed to the server.
     * Includes soft-deleted records so deletions propagate to other devices.
     */
    @Query("""
        SELECT * FROM clients
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getClientsPendingSync(): List<ClientEntity>

    @Query("""
        SELECT * FROM contacts
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getContactsPendingSync(): List<ContactEntity>

    @Query("""
        SELECT * FROM contracts
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getContractsPendingSync(): List<ContractEntity>

    @Query("""
        SELECT * FROM facilities
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getFacilitiesPendingSync(): List<FacilityEntity>

    @Query("""
        SELECT * FROM facility_islands
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getFacilityIslandsPendingSync(): List<IslandEntity>

    @Query("""
        SELECT * FROM mechanical_units
        WHERE updated_at > COALESCE(synced_at, 0)
        ORDER BY updated_at ASC
    """)
    suspend fun getMechanicalUnitsPendingSync(): List<MechanicalUnitEntity>

    @Query("""
        SELECT * FROM maintenance_logs
        WHERE updated_at > COALESCE(synced_at, 0) AND is_deleted = 1
        ORDER BY updated_at ASC
    """)
    suspend fun getMaintenanceLogsPendingSync(): List<MaintenanceLogEntity>

    // ===== MARK AS SYNCED — after a successful push =====

    /**
     * Stamps synced_at = [now] on records that were successfully pushed.
     * Uses the id list returned by the server acknowledgement.
     */
    @Query("UPDATE clients SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markClientsSynced(ids: List<String>, now: Long)

    @Query("UPDATE contacts SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markContactsSynced(ids: List<String>, now: Long)

    @Query("UPDATE contracts SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markContractsSynced(ids: List<String>, now: Long)

    @Query("UPDATE facilities SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markFacilitiesSynced(ids: List<String>, now: Long)

    @Query("UPDATE facility_islands SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markFacilityIslandsSynced(ids: List<String>, now: Long)

    @Query("UPDATE mechanical_units SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markMechanicalUnitsSynced(ids: List<String>, now: Long)

    @Query("UPDATE maintenance_logs SET synced_at = :now WHERE id IN (:ids)")
    suspend fun markMaintenanceLogsSynced(ids: List<String>, now: Long)

    // ===== UPSERT FROM SERVER — apply incoming records =====

    /**
     * Insert or replace records received from the server.
     * OnConflictStrategy.REPLACE implements last-write-wins:
     * the incoming record overwrites the local one unconditionally.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClients(clients: List<ClientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContracts(contracts: List<ContractEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFacilities(facilities: List<FacilityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFacilityIslands(islands: List<IslandEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMechanicalUnits(units: List<MechanicalUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaintenanceLogs(logs: List<MaintenanceLogEntity>)

    // ===== DIAGNOSTICS =====

    /** Total number of records pending push across all client-management tables. */
    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM clients WHERE updated_at > COALESCE(synced_at, 0)) +
            (SELECT COUNT(*) FROM contacts WHERE updated_at > COALESCE(synced_at, 0)) +
            (SELECT COUNT(*) FROM contracts WHERE updated_at > COALESCE(synced_at, 0)) +
            (SELECT COUNT(*) FROM facilities WHERE updated_at > COALESCE(synced_at, 0)) +
            (SELECT COUNT(*) FROM facility_islands WHERE updated_at > COALESCE(synced_at, 0)) +
            (SELECT COUNT(*) FROM mechanical_units WHERE updated_at > COALESCE(synced_at, 0)) +
            (SELECT COUNT(*) FROM maintenance_logs WHERE updated_at > COALESCE(synced_at, 0))
    """)
    suspend fun countPendingSync(): Int
}