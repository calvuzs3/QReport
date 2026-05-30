package net.calvuz.qreport.sync.di

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.calvuz.qreport.BuildConfig
//import net.calvuz.qreport.sync.data.local.SyncSettingsDataStore
import net.calvuz.qreport.sync.data.remote.DynamicUrlInterceptor
import net.calvuz.qreport.sync.data.remote.QReportApi
import net.calvuz.qreport.sync.data.remote.RemoteDataSource
import net.calvuz.qreport.sync.data.remote.RetrofitRemoteDataSource
import net.calvuz.qreport.sync.data.remote.ServerUrlHolder
import net.calvuz.qreport.sync.data.remote.TokenExpiryInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

//    @Provides
//    @Singleton
//    fun provideOkHttpClient(): OkHttpClient {
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//        return OkHttpClient.Builder()
//            .addInterceptor(logging)
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .build()
//    }
    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicUrlInterceptor: DynamicUrlInterceptor,
        tokenExpiryInterceptor: TokenExpiryInterceptor  // ← aggiungere
): OkHttpClient {
//    val logging = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
    val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC  // solo URL e status code
        else HttpLoggingInterceptor.Level.NONE                      // niente in produzione
    }
    return OkHttpClient.Builder()
        .addInterceptor(dynamicUrlInterceptor)
        .addInterceptor(tokenExpiryInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}


//    @Provides
//    @Singleton
//    fun provideRetrofit(
//        okHttpClient: OkHttpClient,
//        json: Json,
//        syncSettingsDataStore: SyncSettingsDataStore
//    ): Retrofit {
//        // Base URL is read at runtime from DataStore — use a placeholder here,
//        // actual URL is injected per-request via the interceptor below.
//        // For simplicity in Phase 3 we build Retrofit with a fixed base URL
//        // read once at startup; URL changes require app restart.
//        // Phase 5 can add a dynamic URL interceptor if needed.
//        val baseUrl = "https://server.domain.top/" // fallback; overridden by DynamicUrlInterceptor
//
//        return Retrofit.Builder()
//            .baseUrl(baseUrl)
//            .client(okHttpClient)
//            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
//            .build()
//    }
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        // Placeholder URL — actual URL is injected per-request by DynamicUrlInterceptor
        return Retrofit.Builder()
            .baseUrl(ServerUrlHolder.DEFAULT_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideQReportApi(retrofit: Retrofit): QReportApi =
        retrofit.create(QReportApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(
        impl: RetrofitRemoteDataSource
    ): RemoteDataSource
}