package net.calvuz.qreport.photo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.photo.data.local.repository.PhotoStorageManager
import net.calvuz.qreport.photo.domain.repository.PhotoFileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhotoModule {

    @Provides
    @Singleton
    fun providePhotoStorageManager(
        @ApplicationContext context: Context,
        photoFileRepository: PhotoFileRepository
    ): PhotoStorageManager = PhotoStorageManager(context, photoFileRepository)

}