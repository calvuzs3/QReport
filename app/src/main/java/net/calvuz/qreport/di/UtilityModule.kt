package net.calvuz.qreport.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.export.photo.ImageProcessorImpl
import net.calvuz.qreport.data.photo.PhotoStorageManager
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.model.photo.ImageProcessor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    @Provides
    @Singleton
    fun providePhotoStorageManager(
        @ApplicationContext context: Context,
        fileManager: FileManager
    ): PhotoStorageManager = PhotoStorageManager(context, fileManager)

    @Provides
    @Singleton
    fun provideImageProcessor(): ImageProcessor {
        return ImageProcessorImpl()
    }
}