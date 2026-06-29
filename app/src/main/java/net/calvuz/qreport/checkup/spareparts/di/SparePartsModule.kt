package net.calvuz.qreport.checkup.spareparts.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.checkup.spareparts.data.local.repository.CheckUpSparePartRepositoryImpl
import net.calvuz.qreport.checkup.spareparts.domain.repository.CheckUpSparePartRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SparePartsModule {

    @Binds
    @Singleton
    abstract fun bindCheckUpSparePartRepository(
        impl: CheckUpSparePartRepositoryImpl
    ): CheckUpSparePartRepository
}
