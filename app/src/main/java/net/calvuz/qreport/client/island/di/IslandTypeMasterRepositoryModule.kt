package net.calvuz.qreport.client.island.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.client.island.data.local.repository.IslandTypeMasterRepositoryImpl
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IslandTypeMasterRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindIslandTypeMasterRepository(
        impl: IslandTypeMasterRepositoryImpl
    ): IslandTypeMasterRepository
}
