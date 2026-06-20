package net.calvuz.qreport.checkup.checkup.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.checkup.data.local.repository.CheckUpAssociationRepositoryImpl
import net.calvuz.qreport.checkup.checkup.data.local.repository.CheckUpRepositoryImpl
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpAssociationRepository
import net.calvuz.qreport.checkup.checkup.domain.repository.CheckUpRepository
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
    abstract fun bindCheckUpAssociationRepository(
        checkUpAssociationRepositoryImpl: CheckUpAssociationRepositoryImpl
    ): CheckUpAssociationRepository
}
