package net.calvuz.qreport.client.unit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.client.unit.data.local.repository.MechanicalUnitRepositoryImpl
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MechanicalUnitRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMechanicalUnitRepository(
        impl: MechanicalUnitRepositoryImpl
    ): MechanicalUnitRepository
}