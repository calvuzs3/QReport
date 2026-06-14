package net.calvuz.qreport.backup.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.backup.data.repository.DocumentArchiveRepositoryImpl
import net.calvuz.qreport.backup.domain.repository.DocumentArchiveRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentArchiveModule {

    @Binds
    @Singleton
    abstract fun bindDocumentArchiveRepository(
        impl: DocumentArchiveRepositoryImpl
    ): DocumentArchiveRepository
}
