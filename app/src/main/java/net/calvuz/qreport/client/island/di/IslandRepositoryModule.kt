package net.calvuz.qreport.client.island.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.island.data.local.dao.IslandDao
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.island.data.local.repository.IslandRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IslandRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFacilityIslandRepository(
        facilityIslandRepositoryImpl: IslandRepositoryImpl
    ): IslandRepository

    companion object {
        @Provides
        @Singleton
        fun provideFacilityIslandDao(
            database: QReportDatabase
        ): IslandDao = database.facilityIslandDao()
    }
}