package net.calvuz.qreport.client.client.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.client.data.local.repository.ClientRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindClientRepository(
        clientRepositoryImpl: ClientRepositoryImpl
    ): ClientRepository

}