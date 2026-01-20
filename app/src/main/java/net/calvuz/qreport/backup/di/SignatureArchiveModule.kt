package net.calvuz.qreport.backup.di

/**
 * =============================================================================
 * ADDITIONS FOR BackupModule.kt (or create new SignatureArchiveModule.kt)
 * =============================================================================
 *
 * Add Hilt binding for SignatureArchiveRepository
 */

// Option 1: Add to existing BackupModule.kt
// =============================================================================
//
// @Binds
// abstract fun bindSignatureArchiveRepository(
//     impl: SignatureArchiveRepositoryImpl
// ): SignatureArchiveRepository

// Option 2: Create new module SignatureArchiveModule.kt
// =============================================================================

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.backup.data.repository.SignatureArchiveRepositoryImpl
import net.calvuz.qreport.backup.domain.repository.SignatureArchiveRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SignatureArchiveModule {

    @Binds
    @Singleton
    abstract fun bindSignatureArchiveRepository(
        impl: SignatureArchiveRepositoryImpl
    ): SignatureArchiveRepository
}