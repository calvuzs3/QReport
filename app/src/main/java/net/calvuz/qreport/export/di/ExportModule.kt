package net.calvuz.qreport.export.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.photo.domain.model.ImageProcessor
import net.calvuz.qreport.export.data.photo.ImageProcessorImpl
import net.calvuz.qreport.export.data.repository.ExportFileRepositoryImpl
import net.calvuz.qreport.export.domain.reposirory.ExportFileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    /** Image Processor */
    @Binds
    @Singleton
    abstract fun bindImageProcessor(
        imageProcessorImpl: ImageProcessorImpl
    ): ImageProcessor

    /** Export file repository */
    @Binds
    @Singleton
    abstract fun bindExcportFileRepository(
        exportFileRepositoryImpl: ExportFileRepositoryImpl
    ): ExportFileRepository

}