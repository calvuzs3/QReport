package net.calvuz.qreport.ti.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.ti.data.repository.TechnicalInterventionRepositoryImpl
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository

/**
 * Hilt module for TechnicalIntervention feature dependency injection
 *
 * Provides bindings for repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InterventionModule {

    /**
     * Bind TechnicalInterventionRepositoryImpl to TechnicalInterventionRepository interface
     */
    @Binds
    abstract fun bindTechnicalInterventionRepository(
        impl: TechnicalInterventionRepositoryImpl
    ): TechnicalInterventionRepository
}