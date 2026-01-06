package net.calvuz.qreport.photo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.photo.data.local.repository.PhotoRepositoryImpl
import net.calvuz.qreport.photo.data.local.repository.PhotoFileRepositoryImpl
import net.calvuz.qreport.photo.domain.repository.PhotoFileRepository
import net.calvuz.qreport.photo.domain.repository.PhotoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoRepositoryModule {


    @Binds
    @Singleton
    abstract fun bindPhotoFileRepository(
        photoFileRepositoryImpl: PhotoFileRepositoryImpl
    ): PhotoFileRepository

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(
        photoRepositoryImpl: PhotoRepositoryImpl
    ): PhotoRepository

}