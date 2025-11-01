package net.calvuz.qreport.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.local.file.FileManagerImpl
import net.calvuz.qreport.data.photo.PhotoStorageManager
import net.calvuz.qreport.domain.model.file.FileManager
import javax.inject.Singleton

/**
 * Utility Dependency Injection Module
 *
 * Modulo per utilità e servizi comuni dell'applicazione.
 */
@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    /**
     * File provider per gestione storage interno
     */
    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context
    ): FileManager {
        return FileManagerImpl(context)
    }

    @Provides
    @Singleton
    fun providePhotoStorageManager(
        @ApplicationContext context: Context,
        fileManager: FileManager // ✅ Riusa esistente
    ): PhotoStorageManager = PhotoStorageManager(context, fileManager)
}