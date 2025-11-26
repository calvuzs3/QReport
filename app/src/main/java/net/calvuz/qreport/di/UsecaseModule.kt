package net.calvuz.qreport.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.local.dao.PhotoDao
import net.calvuz.qreport.domain.repository.CheckItemRepository
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.domain.repository.FacilityRepository
import net.calvuz.qreport.domain.usecase.checkup.AddSparePartUseCase
import net.calvuz.qreport.domain.usecase.checkup.GetCheckUpDetailsUseCase
import net.calvuz.qreport.domain.usecase.checkup.UpdateCheckItemNotesUseCase
import net.calvuz.qreport.domain.usecase.checkup.UpdateCheckItemStatusUseCase
import net.calvuz.qreport.domain.usecase.client.facility.GetFacilityByIdUseCase

@Module
@InstallIn(SingletonComponent::class)
class UsecaseModule {

    @Provides
    fun provideUpdateCheckItemStatusUseCase(repository: CheckItemRepository) =
        UpdateCheckItemStatusUseCase(repository)

    @Provides
    fun provideUpdateCheckItemNotesUseCase(repository: CheckItemRepository) =
        UpdateCheckItemNotesUseCase(repository)

    @Provides
    fun provideGetCheckUpDetailsUseCase(
        checkUpRepository: CheckUpRepository,
        checkItemRepository: CheckItemRepository,
        photoDao: PhotoDao
    ) = GetCheckUpDetailsUseCase(checkUpRepository, checkItemRepository, photoDao)

    @Provides
    fun provideAddSparePartUseCase(repository: CheckUpRepository) =
        AddSparePartUseCase(repository)

    // FACILITY
    @Provides
    fun provideGetFacilityByIdUseCase(facilityRepository: FacilityRepository) =
        GetFacilityByIdUseCase(facilityRepository)

}