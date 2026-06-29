@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.app.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.app.domain.AppVersionInfo
import net.calvuz.qreport.sync.app.SyncForegroundObserver
import net.calvuz.qreport.sync.data.local.SyncSettingsDataStore
import net.calvuz.qreport.sync.data.remote.ServerUrlHolder
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class QReportApplication : Application() {

    // Application-scoped coroutine scope for initialization tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var syncSettingsDataStore: SyncSettingsDataStore
    @Inject lateinit var serverUrlHolder: ServerUrlHolder
    @Inject lateinit var syncForegroundObserver: SyncForegroundObserver


    companion object {
        const val DATABASE_NAME = "qreport_database"
        const val DATABASE_VERSION = 12
    }

    override fun onCreate() {
        super.onCreate()

        // Timber initialization
        if (AppVersionInfo.isDebugBuild) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("QReportApplication: started")

        // Initialize ServerUrlHolder from persisted DataStore value.
        // Runs on IO thread — does not block the main thread.
        applicationScope.launch {
            val savedUrl = syncSettingsDataStore.getServerUrl().first()
            if (savedUrl.isNotBlank()) {
                // Ensure URL ends with trailing slash as Retrofit requires
                serverUrlHolder.baseUrl = if (savedUrl.endsWith("/")) savedUrl
                else "$savedUrl/"
                Timber.d("QReportApplication: server URL initialized to ${serverUrlHolder.baseUrl}")
            } else {
                Timber.d("QReportApplication: no server URL configured, using default")
            }
        }

        // Register foreground lifecycle observer for automatic sync
        ProcessLifecycleOwner.get().lifecycle.addObserver(syncForegroundObserver)
        Timber.d("QReportApplication: foreground sync observer registered")

    }
}