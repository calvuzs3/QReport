package net.calvuz.qreport.app.app.domain

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qreport.BuildConfig
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppVersionInfo - Gestione versioni app e database globale
 *
 * Utilizzabile in tutta l'app per:
 * - Backup metadata
 * - Crash reports
 * - Analytics
 * - Migration logic
 */
@Singleton
class AppVersionInfo @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ===== APP VERSION =====

    /**
     * Version name dell'app (es. "1.2.3")
     */
    val appVersion: String by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Failed to get app version, using BuildConfig")
            BuildConfig.VERSION_NAME
        }
    }

    /**
     * Version code dell'app (numero intero incrementale)
     */
    val appVersionCode: Long by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Failed to get app version code")
            1L
        }
    }

    // ===== DATABASE VERSION =====

    /**
     * Database schema version
     * ⚠️ IMPORTANTE: Aggiorna quando modifichi schema Room
     */
    val databaseVersion: Int = QReportDatabase.DATABASE_VERSION

    /**
     * Database schema version string per display
     */
    val databaseVersionString: String = "Schema v$databaseVersion"

    // ===== BUILD INFO =====

    /**
     * Build type (debug, release, etc)
     */
    val buildType: String = BuildConfig.BUILD_TYPE

    /**
     * Is debug build
     */
    val isDebugBuild: Boolean = BuildConfig.DEBUG

    /**
     * Application ID
     */
    val applicationId: String = BuildConfig.APPLICATION_ID

    // ===== COMPLETE VERSION INFO =====

    /**
     * Complete version info for logs/analytics
     */
    val completeVersionInfo: String by lazy {
        "QReport v$appVersion ($appVersionCode) - DB v$databaseVersion - Build: $buildType"
    }

    /**
     * Version info for backup metadata
     */
    fun getBackupVersionInfo(): BackupVersionInfo {
        return BackupVersionInfo(
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            databaseVersion = databaseVersion,
            buildType = buildType,
            createdAt = System.currentTimeMillis()
        )
    }
}

/**
 * Version info per backup metadata
 */
data class BackupVersionInfo(
    val appVersion: String,
    val appVersionCode: Long,
    val databaseVersion: Int,
    val buildType: String,
    val createdAt: Long
)