package net.calvuz.qreport.sync.data.remote

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.sync.data.remote.dto.SyncPayloadDto
import net.calvuz.qreport.sync.data.remote.dto.SyncResponseDto

/**
 * Abstraction over the remote server API.
 * Implemented by [RetrofitRemoteDataSource].
 */
interface RemoteDataSource {
    suspend fun getServerVersion(): QrResult<String, QrError>
    suspend fun login(username: String, password: String): QrResult<String, QrError>
    suspend fun push(token: String, payload: SyncPayloadDto, since: Long): QrResult<SyncResponseDto, QrError>
    suspend fun pull(token: String, since: Long): QrResult<SyncPayloadDto, QrError>
}

