package net.calvuz.qreport.sync.data.remote

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.sync.data.local.TokenStorage
import net.calvuz.qreport.sync.data.remote.dto.LoginRequest
import net.calvuz.qreport.sync.data.remote.dto.SyncPayloadDto
import net.calvuz.qreport.sync.data.remote.dto.SyncResponseDto
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitRemoteDataSource @Inject constructor(
    private val api: QReportApi,
    private val tokenStorage: TokenStorage
) : RemoteDataSource {

    override suspend fun getServerVersion(): QrResult<String, QrError> {
        return try {
            val response = api.getVersion()
            if (response.isSuccessful) {
                val version = response.body()?.version
                if (version != null) {
                    Timber.d("RemoteDataSource: server version=$version")
                    QrResult.Success(version)
                } else {
                    QrResult.Error(QrError.NetworkError.ParseError("Missing version in response"))
                }
            } else {
                QrResult.Error(QrError.NetworkError.ServerError(response.code()))
            }
        } catch (e: IOException) {
            QrResult.Error(QrError.NetworkError.NoConnection())
        } catch (e: Exception) {
            Timber.e(e, "RemoteDataSource: unexpected error fetching server version")
            QrResult.Error(QrError.SystemError.UnknownError(e))
        }
    }

    override suspend fun login(
        username: String,
        password: String
    ): QrResult<String, QrError> {
        return try {
            Timber.d("RemoteDataSource: login attempt for user: $username")
            val response = api.login(LoginRequest(username, password))

            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.token
                if (token != null) {
                    tokenStorage.saveRole(body.role)
                    Timber.d("RemoteDataSource: login successful, role=${body.role}")
                    QrResult.Success(token)
                } else {
                    Timber.e("RemoteDataSource: login response body is null")
                    QrResult.Error(QrError.NetworkError.ParseError("Empty token in response"))
                }
            } else {
                Timber.e("RemoteDataSource: login failed with code ${response.code()}")
                when (response.code()) {
                    401 -> QrResult.Error(QrError.NetworkError.Unauthorized())
                    else -> QrResult.Error(QrError.NetworkError.ServerError(response.code()))
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "RemoteDataSource: network error during login")
            QrResult.Error(QrError.NetworkError.NoConnection())
        } catch (e: HttpException) {
            Timber.e(e, "RemoteDataSource: HTTP error during login")
            QrResult.Error(QrError.NetworkError.ServerError(e.code()))
        } catch (e: Exception) {
            Timber.e(e, "RemoteDataSource: unexpected error during login")
            QrResult.Error(QrError.SystemError.UnknownError(e))
        }
    }

    override suspend fun push(
        token: String,
        payload: SyncPayloadDto,
        since: Long
    ): QrResult<SyncResponseDto, QrError> {

        return try {
            Timber.d(
                "RemoteDataSource: pushing ${payload.clients.size} clients, " +
                        "${payload.contacts.size} contacts, ${payload.facilities.size} facilities"
            )

            val response = api.push("Bearer $token", payload, since)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Timber.d("RemoteDataSource: push successful, accepted ${body.acceptedIds.size} records")
                    QrResult.Success(body)
                } else {
                    QrResult.Error(QrError.NetworkError.ParseError("Empty push response"))
                }
            } else {
                Timber.e("RemoteDataSource: push failed with code ${response.code()}")
                when (response.code()) {
                    401 -> QrResult.Error(QrError.NetworkError.Unauthorized())
                    else -> QrResult.Error(QrError.NetworkError.ServerError(response.code()))
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "RemoteDataSource: network error during push")
            QrResult.Error(QrError.NetworkError.NoConnection())
        } catch (e: Exception) {
            Timber.e(e, "RemoteDataSource: unexpected error during push")
            QrResult.Error(QrError.SystemError.UnknownError(e))
        }
    }

    override suspend fun pull(
        token: String,
        since: Long
    ): QrResult<SyncPayloadDto, QrError> {
        return try {
            Timber.d("RemoteDataSource: pulling records since $since")
            val response = api.pull("Bearer $token", since)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Timber.d("RemoteDataSource: pull successful, received ${body.clients.size} clients")
                    QrResult.Success(body)
                } else {
                    QrResult.Error(QrError.NetworkError.ParseError("Empty pull response"))
                }
            } else {
                Timber.e("RemoteDataSource: pull failed with code ${response.code()}")
                when (response.code()) {
                    401 -> QrResult.Error(QrError.NetworkError.Unauthorized())
                    else -> QrResult.Error(QrError.NetworkError.ServerError(response.code()))
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "RemoteDataSource: network error during pull")
            QrResult.Error(QrError.NetworkError.NoConnection())
        } catch (e: Exception) {
            Timber.e(e, "RemoteDataSource: unexpected error during pull")
            QrResult.Error(QrError.SystemError.UnknownError(e))
        }
    }
}