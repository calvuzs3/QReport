package net.calvuz.qreport.sync.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that replaces the placeholder base URL with the actual
 * server URL stored in [ServerUrlHolder].
 *
 * This allows Retrofit to be constructed once at app startup with a placeholder URL,
 * while actual requests always use the current URL configured by the user.
 */
@Singleton
class DynamicUrlInterceptor @Inject constructor(
    private val serverUrlHolder: ServerUrlHolder
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val serverUrl = serverUrlHolder.baseUrl

        val newBaseUrl = serverUrl.toHttpUrlOrNull()
        if (newBaseUrl == null) {
            Timber.e("DynamicUrlInterceptor: invalid server URL: $serverUrl")
            return chain.proceed(originalRequest)
        }

        // Rewrite host, scheme and port from the configured server URL
        val newUrl = originalRequest.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        Timber.d("DynamicUrlInterceptor: routing request to $newUrl")
        return chain.proceed(newRequest)
    }
}