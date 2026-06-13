package net.calvuz.qreport.client.document.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.document.data.local.dao.DocumentDao
import net.calvuz.qreport.client.document.data.local.repository.DocumentRepositoryImpl
import net.calvuz.qreport.client.document.domain.repository.DocumentRepository
import javax.inject.Singleton

/**
 * Hilt module for the Document feature.
 *
 * Provides:
 *  - [DocumentDao] from the existing [QReportDatabase] singleton
 *  - Binding of [DocumentRepository] interface to [DocumentRepositoryImpl]
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentModule {

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryImpl
    ): DocumentRepository

    companion object {

        @Provides
        @Singleton
        fun provideIslandDocumentDao(
            database: QReportDatabase
        ): DocumentDao = database.islandDocumentDao()
    }
}