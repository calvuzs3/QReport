package net.calvuz.qreport.checkup.modules.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.modules.data.local.repository.ModuleTypeMasterRepositoryImpl
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ModuleTypeMasterRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindModuleTypeMasterRepository(
        impl: ModuleTypeMasterRepositoryImpl
    ): ModuleTypeMasterRepository
}
