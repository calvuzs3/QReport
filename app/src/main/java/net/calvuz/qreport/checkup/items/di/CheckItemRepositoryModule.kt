package net.calvuz.qreport.checkup.items.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.items.data.local.repository.CheckItemRepositoryImpl
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckItemRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckItemRepository(
        checkItemRepositoryImpl: CheckItemRepositoryImpl
    ): CheckItemRepository
}
