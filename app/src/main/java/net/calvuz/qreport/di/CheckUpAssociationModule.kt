package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.repository.CheckUpAssociationRepositoryImpl
import net.calvuz.qreport.domain.repository.CheckUpAssociationRepository
import javax.inject.Singleton

/**
 * Module DI per il sistema di associazioni CheckUp-Isole
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CheckUpAssociationModule {

    @Binds
    @Singleton
    abstract fun bindCheckUpAssociationRepository(
        checkUpAssociationRepositoryImpl: CheckUpAssociationRepositoryImpl
    ): CheckUpAssociationRepository
}