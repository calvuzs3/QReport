package net.calvuz.qreport.checkup.criticality.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.criticality.data.local.repository.CriticalityMasterRepositoryImpl
import net.calvuz.qreport.checkup.criticality.domain.repository.CriticalityMasterRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CriticalityMasterRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCriticalityMasterRepository(
        impl: CriticalityMasterRepositoryImpl
    ): CriticalityMasterRepository
}
