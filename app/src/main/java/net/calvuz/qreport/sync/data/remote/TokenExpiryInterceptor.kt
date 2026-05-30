package net.calvuz.qreport.sync.data.remote

import net.calvuz.qreport.sync.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that detects 401 Unauthorized responses and clears
 * the stored JWT token, forcing the user to re-login on the next sync attempt.
 *
 * This centralizes token expiry handling at the network layer so that
 * every API call benefits from it automatically.
 */
@Singleton
class TokenExpiryInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            Timber.w("TokenExpiryInterceptor: 401 received — clearing token, re-login required")
            tokenStorage.clearToken()
        }

        return response
    }
}