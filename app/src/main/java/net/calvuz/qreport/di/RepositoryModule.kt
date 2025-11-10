package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.repository.ExportRepositoryImpl
import net.calvuz.qreport.data.repository.CheckItemRepositoryImpl
import net.calvuz.qreport.data.repository.CheckUpRepositoryImpl
import net.calvuz.qreport.data.repository.PhotoRepositoryImpl
import net.calvuz.qreport.domain.repository.CheckItemRepository
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.domain.repository.ExportRepository
import net.calvuz.qreport.domain.repository.PhotoRepository
import javax.inject.Singleton

/**
 * Repository Dependency Injection Module
 *
 * Modulo per fornire istanze dei repository che implementano
 * la logica di business e coordinano i diversi data source.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckUpRepository(
        checkUpRepositoryImpl: CheckUpRepositoryImpl
    ): CheckUpRepository

    @Binds
    @Singleton
    abstract fun bindCheckItemRepository(
        checkItemRepositoryImpl: CheckItemRepositoryImpl
    ): CheckItemRepository

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(
        photoRepositoryImpl: PhotoRepositoryImpl
    ): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindExportRepository(
        exportRepositoryImpl: ExportRepositoryImpl
    ): ExportRepository

}