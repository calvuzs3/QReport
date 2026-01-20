package net.calvuz.qreport.ti.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.ti.data.local.repository.SignatureFileRepositoryImpl
import net.calvuz.qreport.ti.domain.repository.SignatureFileRepository
import javax.inject.Singleton


/**
 * Hilt module for TechnicalIntervention feature dependency injection
 *
 * Provides bindings for repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SignatureModule {

    /**
     * Provides SignatureFileRepository implementation
     * Uses CoreFileRepository internally for file operations
     */
    @Binds
    @Singleton
    abstract fun bindSignatureFileRepository(
        implementation: SignatureFileRepositoryImpl
    ): SignatureFileRepository
}