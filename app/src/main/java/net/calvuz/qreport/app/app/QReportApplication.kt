package net.calvuz.qreport.app.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.calvuz.qreport.BuildConfig
import timber.log.Timber

@HiltAndroidApp
class QReportApplication : Application() {

    companion object {
        const val DATABASE_NAME = "qreport_database"
        const val DATABASE_VERSION = 3
    }

    override fun onCreate() {
        super.onCreate()

        // Timber Initialization
        if (BuildConfig.DEBUG) {
            Timber.Forest.plant(Timber.DebugTree())
        }

        Timber.Forest.d("QReport Application started")
    }
}