package net.calvuz.qreport.client.contact.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.contact.data.local.dao.ContactDao
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.data.local.repository.ContactRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContactRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository

    companion object {
        @Provides
        @Singleton
        fun provideContactDao(
            database: QReportDatabase
        ): ContactDao = database.contactDao()
    }
}