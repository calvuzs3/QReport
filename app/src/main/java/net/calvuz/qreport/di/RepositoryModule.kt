package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.repository.CheckItemRepositoryImpl
import net.calvuz.qreport.data.repository.CheckUpRepositoryImpl
import net.calvuz.qreport.domain.repository.CheckItemRepository
import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Singleton

/**
 * Repository Dependency Injection Module
 *
 * Modulo per fornire istanze dei repository che implementano
 * la logica di business e coordinano i diversi data source.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckUpRepository(
        checkUpRepositoryImpl: CheckUpRepositoryImpl
    ): CheckUpRepository

    @Binds
    @Singleton
    abstract fun bindCheckItemRepository(
        checkItemRepositoryImpl: CheckItemRepositoryImpl
    ): CheckItemRepository
}