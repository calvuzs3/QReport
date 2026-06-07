package net.calvuz.qreport.client.facility.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.facility.data.local.dao.FacilityDao
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.facility.data.local.repository.FacilityRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FacilityRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFacilityRepository(
        facilityRepositoryImpl: FacilityRepositoryImpl
    ): FacilityRepository

    companion object {
        @Provides
        @Singleton
        fun provideFacilityDao(
            database: QReportDatabase
        ): FacilityDao = database.facilityDao()
    }
}