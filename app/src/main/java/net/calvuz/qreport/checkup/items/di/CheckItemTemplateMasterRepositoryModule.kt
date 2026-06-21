package net.calvuz.qreport.checkup.items.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.items.data.local.repository.CheckItemTemplateMasterRepositoryImpl
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemTemplateMasterRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckItemTemplateMasterRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckItemTemplateMasterRepository(
        impl: CheckItemTemplateMasterRepositoryImpl
    ): CheckItemTemplateMasterRepository
}
