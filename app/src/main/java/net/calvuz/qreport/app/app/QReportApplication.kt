package net.calvuz.qreport.app.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.calvuz.qreport.BuildConfig
import timber.log.Timber

@HiltAndroidApp
class QReportApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inizializza Timber per il logging
        if (BuildConfig.DEBUG) {
            Timber.Forest.plant(Timber.DebugTree())
        }

        Timber.Forest.d("QReport Application started")
    }
}