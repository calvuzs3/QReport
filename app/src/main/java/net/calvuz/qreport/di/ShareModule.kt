package net.calvuz.qreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qreport.data.share.ShareManagerImpl
import net.calvuz.qreport.domain.service.ShareManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareModule {

    /**
     * ✅ DICE A HILT: "Quando qualcuno chiede ShareManager interface,
     *                  fornisci ShareManagerImpl instance"
     */
    @Binds
    @Singleton
    abstract fun bindShareManager(
        shareManagerImpl: ShareManagerImpl
    ): ShareManager
}