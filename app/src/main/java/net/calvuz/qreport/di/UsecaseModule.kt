package net.calvuz.qreport.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.domain.repository.CheckItemRepository
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.domain.usecase.checkup.AddSparePartUseCase
import net.calvuz.qreport.domain.usecase.checkup.GetCheckUpDetailsUseCase
import net.calvuz.qreport.domain.usecase.checkup.UpdateCheckItemNotesUseCase
import net.calvuz.qreport.domain.usecase.checkup.UpdateCheckItemStatusUseCase

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
        checkItemRepository: CheckItemRepository
    ) = GetCheckUpDetailsUseCase(checkUpRepository, checkItemRepository)

    @Provides
    fun provideAddSparePartUseCase(repository: CheckUpRepository) =
        AddSparePartUseCase(repository)
}