package net.calvuz.qreport.export.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.export.data.repository.ExportRepositoryImpl
import net.calvuz.qreport.export.domain.reposirory.ExportRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExportRepository(
        exportRepositoryImpl: ExportRepositoryImpl
    ): ExportRepository

}