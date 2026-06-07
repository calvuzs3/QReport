package net.calvuz.qreport.client.contract.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.contract.data.local.dao.ContractDao
import net.calvuz.qreport.client.contract.data.local.repository.ContractRepositoryImpl
import net.calvuz.qreport.client.contract.domain.repository.ContractRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContractRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContractRepository(
        contractRepositoryImpl: ContractRepositoryImpl
    ): ContractRepository

    companion object {
        @Provides
        @Singleton
        fun provideContractDao(
            database: QReportDatabase
        ): ContractDao = database.contractDao()
    }
}