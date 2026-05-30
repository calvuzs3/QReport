package net.calvuz.qreport.sync.data.remote

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory holder for the server base URL.
 *
 * Retrofit requires a base URL at construction time, but the URL is stored
 * in DataStore and can change at runtime. This holder is initialized at app
 * startup from DataStore and updated whenever the user changes the URL in Settings.
 *
 * The [DynamicUrlInterceptor] reads from this holder on every HTTP request.
 */
@Singleton
class ServerUrlHolder @Inject constructor() {

    @Volatile
    var baseUrl: String = DEFAULT_URL

    fun isConfigured(): Boolean = baseUrl.isNotBlank() && baseUrl != DEFAULT_URL

    companion object {
        const val DEFAULT_URL = "http://localhost:8080/"
    }
}