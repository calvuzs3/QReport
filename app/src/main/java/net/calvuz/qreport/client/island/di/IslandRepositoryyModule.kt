package net.calvuz.qreport.client.island.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.island.data.local.repository.IslandRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IslandRepositoryyModule {

    @Binds
    @Singleton
    abstract fun bindFacilityIslandRepository(
        facilityIslandRepositoryImpl: IslandRepositoryImpl
    ): IslandRepository

}