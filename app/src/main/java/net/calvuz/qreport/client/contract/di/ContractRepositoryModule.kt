package net.calvuz.qreport.client.contract.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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

}