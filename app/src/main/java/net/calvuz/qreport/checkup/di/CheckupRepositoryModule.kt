package net.calvuz.qreport.checkup.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.data.local.repository.CheckItemRepositoryImpl
import net.calvuz.qreport.checkup.data.local.repository.CheckUpAssociationRepositoryImpl
import net.calvuz.qreport.checkup.data.local.repository.CheckUpRepositoryImpl
import net.calvuz.qreport.checkup.domain.repository.CheckItemRepository
import net.calvuz.qreport.checkup.domain.repository.CheckUpAssociationRepository
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckupRepositoryModule {

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

    @Binds
    @Singleton
    abstract fun bindCheckUpAssociationRepository(
        checkUpAssociationRepositoryImpl: CheckUpAssociationRepositoryImpl
    ): CheckUpAssociationRepository
}