package net.calvuz.qreport.share.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.share.data.repository.ShareFileRepositoryImpl
import net.calvuz.qreport.share.domain.repository.ShareFileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareModule {

    /** Share file repository */
    @Binds
    @Singleton
    abstract fun bindShareFileRepository(
        shareFileRepositoryImpl: ShareFileRepositoryImpl
    ): ShareFileRepository

}