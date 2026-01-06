package net.calvuz.qreport.app.file.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.file.data.repository.CoreFileRepositoryImpl
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FileModule {

    @Binds
    @Singleton
    abstract fun bindBackupFileRepository(
        coreFileRepository: CoreFileRepositoryImpl
    ): CoreFileRepository

}