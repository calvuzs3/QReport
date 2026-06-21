package net.calvuz.qreport.sync.domain.usecase

import kotlinx.coroutines.flow.first
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.data.local.dao.IslandTypeDao
import net.calvuz.qreport.sync.data.local.SyncSettingsDataStore
import net.calvuz.qreport.sync.data.local.TokenStorage
import net.calvuz.qreport.sync.data.local.dao.SyncDao
import net.calvuz.qreport.sync.data.remote.RemoteDataSource
import net.calvuz.qreport.sync.data.remote.dto.SyncPayloadDto
import net.calvuz.qreport.sync.domain.model.SyncMode
import net.calvuz.qreport.sync.domain.model.SyncResult
import net.calvuz.qreport.sync.mapper.SyncMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Orchestrates a full bidirectional sync session:
 * 1. Check sync is enabled and token is available
 * 2. Build push payload from local pending changes
 * 3. Push to server — server responds with pull payload in the same call
 * 4. Apply pulled records to local Room DB
 * 5. Mark pushed records as synced
 * 6. Save sync timestamp
 */
class SyncUseCase @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val syncDao: SyncDao,
    private val islandTypeDao: IslandTypeDao,
    private val syncSettingsDataStore: SyncSettingsDataStore,
    private val tokenStorage: TokenStorage,
    private val syncMapper: SyncMapper
) {

    @Suppress("HardCodedStringLiteral")
    suspend operator fun invoke(): QrResult<SyncResult, QrError> {
        return try {

            // 1. Check sync mode
            val mode = syncSettingsDataStore.getSyncMode().first()
//            val mode = syncSettingsDataStore.getSyncMode()
//                .let { flow -> kotlinx.coroutines.flow.first(flow) }

            if (mode == SyncMode.LOCAL_ONLY) {
                Timber.w("SyncUseCase: sync is disabled (LOCAL_ONLY mode)")
                return QrResult.Error(QrError.NetworkError.SyncDisabled())
            }

            // 2. Check token
            val token = tokenStorage.getToken()
            if (token == null) {
                Timber.w("SyncUseCase: no auth token, user must login first")
                return QrResult.Error(QrError.NetworkError.Unauthorized())
            }

            val lastSync = syncSettingsDataStore.getLastSyncTimestampOnce()
            val now = System.currentTimeMillis()
            val deviceId = syncSettingsDataStore.getDeviceId()

            Timber.d("SyncUseCase: starting sync, last sync: $lastSync, device: $deviceId")

            // 3. Build push payload from local pending changes
            val payload = buildPushPayload(deviceId, now)
            Timber.d(buildString {
                append("SyncUseCase: pushing ${payload.islandTypes.size} island types, ")
                append("${payload.clients.size} clients, ")
                append("${payload.contacts.size} contacts, ${payload.contracts.size} contracts, ")
                append("${payload.facilities.size} facilities, ")
                append("${payload.facilityIslands.size} islands, ${payload.mechanicalUnits.size} units")
                append("${payload.maintenanceLogs.size} logs")
            })

            // 4. Push — server responds with pull payload in the same round-trip
            when (val pushResult = remoteDataSource.push(token, payload, lastSync)) {
                is QrResult.Error -> {
                    Timber.e("SyncUseCase: push failed: ${pushResult.error}")
                    return QrResult.Error(pushResult.error)
                }

                is QrResult.Success -> {
                    val response = pushResult.data
                    Timber.d("SyncUseCase: push accepted ${response.acceptedIds.size} records")

                    // 5. Apply pulled records to local DB
                    applyRemoteChanges(response.pulledPayload)

                    // 6. Mark pushed records as synced
                    markPushedAsSynced(payload, now)

                    // 7. Save sync timestamp
                    syncSettingsDataStore.setLastSyncTimestamp(now)

                    val result = SyncResult(
                        syncedAt = now,
                        pushedCount = response.acceptedIds.size,
                        pulledCount = countPulled(response.pulledPayload)
                    )

                    Timber.d("SyncUseCase: sync completed — pushed: ${result.pushedCount}, pulled: ${result.pulledCount}")
                    QrResult.Success(result)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "SyncUseCase: unexpected exception during sync")
            QrResult.Error(QrError.SystemError.UnknownError(e))
        }
    }

    // ===== PRIVATE HELPERS =====

    private suspend fun buildPushPayload(deviceId: String, now: Long): SyncPayloadDto {
        return SyncPayloadDto(
            deviceId = deviceId,
            syncTimestamp = now,
            islandTypes = islandTypeDao.getPendingSync().map { syncMapper.islandTypeToDto(it) },
            clients = syncDao.getClientsPendingSync().map { syncMapper.clientToDto(it) },
            contacts = syncDao.getContactsPendingSync().map { syncMapper.contactToDto(it) },
            contracts = syncDao.getContractsPendingSync().map { syncMapper.contractToDto(it) },
            facilities = syncDao.getFacilitiesPendingSync().map { syncMapper.facilityToDto(it) },
            facilityIslands = syncDao.getFacilityIslandsPendingSync()
                .map { syncMapper.facilityIslandToDto(it) },
            mechanicalUnits = syncDao.getMechanicalUnitsPendingSync()
                .map { syncMapper.mechanicalUnitToDto(it) },
            maintenanceLogs = syncDao.getMaintenanceLogsPendingSync()
                .map { syncMapper.maintenanceLogToDto(it) })
    }

    private suspend fun applyRemoteChanges(payload: SyncPayloadDto) {
        if (payload.islandTypes.isNotEmpty()) {
            islandTypeDao.upsertAll(payload.islandTypes.map { syncMapper.islandTypeToEntity(it) })
        }
        if (payload.clients.isNotEmpty()) syncDao.upsertClients(payload.clients.map {
            syncMapper.clientToEntity(
                it
            )
        })
        if (payload.contacts.isNotEmpty()) syncDao.upsertContacts(payload.contacts.map {
            syncMapper.contactToEntity(
                it
            )
        })
        if (payload.contracts.isNotEmpty()) syncDao.upsertContracts(payload.contracts.map {
            syncMapper.contractToEntity(
                it
            )
        })
        if (payload.facilities.isNotEmpty()) syncDao.upsertFacilities(payload.facilities.map {
            syncMapper.facilityToEntity(
                it
            )
        })
        if (payload.facilityIslands.isNotEmpty()) syncDao.upsertFacilityIslands(payload.facilityIslands.map {
            syncMapper.facilityIslandToEntity(
                it
            )
        })
        if (payload.mechanicalUnits.isNotEmpty()) syncDao.upsertMechanicalUnits(payload.mechanicalUnits.map {
            syncMapper.mechanicalUnitToEntity(
                it
            )
        })
        if (payload.maintenanceLogs.isNotEmpty()) syncDao.upsertMaintenanceLogs(payload.maintenanceLogs.map {
            syncMapper.maintenanceLogToEntity(
                it
            )
        })

        Timber.d("SyncUseCase: applied remote changes to local DB")
    }

    private suspend fun markPushedAsSynced(payload: SyncPayloadDto, now: Long) {
        payload.islandTypes.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { islandTypeDao.markSynced(it, now) }
        payload.clients.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { syncDao.markClientsSynced(it, now) }
        payload.contacts.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { syncDao.markContactsSynced(it, now) }
        payload.contracts.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { syncDao.markContractsSynced(it, now) }
        payload.facilities.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { syncDao.markFacilitiesSynced(it, now) }
        payload.facilityIslands.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { syncDao.markFacilityIslandsSynced(it, now) }
        payload.mechanicalUnits.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { syncDao.markMechanicalUnitsSynced(it, now) }
        payload.maintenanceLogs.map { it.id }.takeIf { it.isNotEmpty() }
            ?.let { syncDao.markMaintenanceLogsSynced(it, now) }
    }

    private fun countPulled(payload: SyncPayloadDto): Int =
        payload.islandTypes.size + payload.clients.size + payload.contacts.size + payload.contracts.size + payload.facilities.size + payload.facilityIslands.size + payload.mechanicalUnits.size + payload.maintenanceLogs.size
}