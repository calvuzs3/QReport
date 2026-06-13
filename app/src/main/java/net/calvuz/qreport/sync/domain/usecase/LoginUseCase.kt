@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.sync.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.sync.data.local.TokenStorage
import net.calvuz.qreport.sync.data.remote.RemoteDataSource
import timber.log.Timber
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val tokenStorage: TokenStorage
) {

    suspend operator fun invoke(
        username: String,
        password: String
    ): QrResult<Unit, QrError> {
        return try {
            if (username.isBlank()) {
                Timber.w("LoginUseCase: username is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField("username"))
            }
            if (password.isBlank()) {
                Timber.w("LoginUseCase: password is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField("password"))
            }

            Timber.d("LoginUseCase: attempting login for user: $username")

            when (val result = remoteDataSource.login(username, password)) {
                is QrResult.Success -> {
                    tokenStorage.saveToken(result.data)
                    Timber.d("LoginUseCase: login successful, token saved")
                    QrResult.Success(Unit)
                }
                is QrResult.Error -> {
                    Timber.e("LoginUseCase: login failed: ${result.error}")
                    QrResult.Error(result.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "LoginUseCase: unexpected exception")
            QrResult.Error(QrError.SystemError.UnknownError(e))
        }
    }
}