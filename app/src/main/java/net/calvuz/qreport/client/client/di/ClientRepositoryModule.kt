package net.calvuz.qreport.client.client.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import net.calvuz.qreport.client.client.data.local.repository.ClientRepositoryImpl
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindClientRepository(
        impl: ClientRepositoryImpl
    ): ClientRepository

    companion object {

        @Provides
        @Singleton
        fun provideClientDao(
            database: QReportDatabase
        ): ClientDao = database.clientDao()
    }
}