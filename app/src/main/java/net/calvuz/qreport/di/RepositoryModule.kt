package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.repository.ExportRepositoryImpl
import net.calvuz.qreport.data.repository.CheckItemRepositoryImpl
import net.calvuz.qreport.data.repository.CheckUpAssociationRepositoryImpl
import net.calvuz.qreport.data.repository.CheckUpRepositoryImpl
import net.calvuz.qreport.data.repository.ClientRepositoryImpl
import net.calvuz.qreport.data.repository.ContactRepositoryImpl
import net.calvuz.qreport.data.repository.FacilityIslandRepositoryImpl
import net.calvuz.qreport.data.repository.FacilityRepositoryImpl
import net.calvuz.qreport.data.repository.PhotoRepositoryImpl
import net.calvuz.qreport.domain.repository.CheckItemRepository
import net.calvuz.qreport.domain.repository.CheckUpAssociationRepository
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.repository.ContactRepository
import net.calvuz.qreport.domain.repository.ExportRepository
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import net.calvuz.qreport.domain.repository.FacilityRepository
import net.calvuz.qreport.domain.repository.PhotoRepository
import javax.inject.Singleton

/**
 * Repository Dependency Injection Module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckUpRepository(
        checkUpRepositoryImpl: CheckUpRepositoryImpl
    ): CheckUpRepository

    @Binds
    @Singleton
    abstract fun bindCheckItemRepository(
        checkItemRepositoryImpl: CheckItemRepositoryImpl
    ): CheckItemRepository

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(
        photoRepositoryImpl: PhotoRepositoryImpl
    ): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindExportRepository(
        exportRepositoryImpl: ExportRepositoryImpl
    ): ExportRepository

    @Binds
    @Singleton
    abstract fun bindClientRepository(
        clientRepositoryImpl: ClientRepositoryImpl
    ): ClientRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository

    @Binds
    @Singleton
    abstract fun bindFacilityRepository(
        facilityRepositoryImpl: FacilityRepositoryImpl
    ): FacilityRepository

    @Binds
    @Singleton
    abstract fun bindFacilityIslandRepository(
        facilityIslandRepositoryImpl: FacilityIslandRepositoryImpl
    ): FacilityIslandRepository

    @Binds
    @Singleton
    abstract fun bindCheckUpAssociationRepository(
        checkUpAssociationRepositoryImpl: CheckUpAssociationRepositoryImpl
    ): CheckUpAssociationRepository
}