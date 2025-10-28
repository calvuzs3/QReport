package net.calvuz.qreport

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class QReportApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inizializza Timber per il logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("QReport Application started")
    }
}