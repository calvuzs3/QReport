package net.calvuz.qreport.client.document.sync.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.client.document.sync.DocumentTransferProvider
import net.calvuz.qreport.client.document.sync.remote.DocumentFileApi
import net.calvuz.qreport.client.document.sync.remote.KtorDocumentTransferProvider
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for document sync.
 *
 * [DocumentFileApi] gets its own [OkHttpClient] with extended timeouts —
 * file uploads/downloads need longer read/write timeouts than the entity
 * sync JSON calls (which share the existing NetworkModule client).
 *
 * The base URL is a placeholder; [DynamicUrlInterceptor] rewrites it
 * per-request from [ServerUrlHolder], same as the entity sync Retrofit.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentSyncModule {

    @Binds
    @Singleton
    abstract fun bindDocumentTransferProvider(
        impl: KtorDocumentTransferProvider
    ): DocumentTransferProvider

    companion object {

        private const val PLACEHOLDER_URL = "https://placeholder.qreport.local/"

        /**
         * OkHttpClient for file transfers.
         * Extended timeouts vs entity sync client:
         *  - connect: 30s (same)
         *  - read:    120s (2 min — downloading a 50MB file on a slow connection)
         *  - write:   120s (2 min — uploading a 50MB file)
         *
         * Reuses the same interceptors from NetworkModule so Authorization
         * header and dynamic URL rewriting work identically.
         */
        @Provides
        @Singleton
        @Named("fileTransferClient")
        fun provideFileTransferOkHttpClient(
            // Inject the interceptors already configured in NetworkModule
            dynamicUrlInterceptor: net.calvuz.qreport.sync.data.remote.DynamicUrlInterceptor,
            tokenExpiryInterceptor: net.calvuz.qreport.sync.data.remote.TokenExpiryInterceptor
        ): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(dynamicUrlInterceptor)
                .addInterceptor(tokenExpiryInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

        @Provides
        @Singleton
        fun provideDocumentFileApi(
            @Named("fileTransferClient") client: OkHttpClient
        ): DocumentFileApi {
            val json = Json { ignoreUnknownKeys = true }
            return Retrofit.Builder()
                .baseUrl(PLACEHOLDER_URL)
                .client(client)
                .addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType())
                )
                .build()
                .create(DocumentFileApi::class.java)
        }
    }
}