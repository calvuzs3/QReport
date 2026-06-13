@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.sync.di

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.calvuz.qreport.BuildConfig
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
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicUrlInterceptor: DynamicUrlInterceptor,
        tokenExpiryInterceptor: TokenExpiryInterceptor  // ← aggiungere
): OkHttpClient {
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
@Suppress("unused")
abstract class RemoteDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(
        impl: RetrofitRemoteDataSource
    ): RemoteDataSource
}