package net.calvuz.qreport.sync.data.remote

import net.calvuz.qreport.sync.data.remote.dto.LoginRequest
import net.calvuz.qreport.sync.data.remote.dto.LoginResponse
import net.calvuz.qreport.sync.data.remote.dto.SyncPayloadDto
import net.calvuz.qreport.sync.data.remote.dto.SyncResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface QReportApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("sync/pull")
    suspend fun pull(
        @Header("Authorization") token: String,
        @Query("since") since: Long
    ): Response<SyncPayloadDto>

    @POST("sync/push")
    suspend fun push(
        @Header("Authorization") token: String,
        @Body payload: SyncPayloadDto,
        @Query("since") since: Long
    ): Response<SyncResponseDto>
}

