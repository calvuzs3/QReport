package net.calvuz.qreport.checkup.status.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.status.data.local.repository.CheckUpStatusMasterRepositoryImpl
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckUpStatusMasterRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckUpStatusMasterRepository(
        impl: CheckUpStatusMasterRepositoryImpl
    ): CheckUpStatusMasterRepository
}
