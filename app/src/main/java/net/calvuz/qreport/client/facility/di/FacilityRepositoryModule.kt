package net.calvuz.qreport.client.facility.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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

}