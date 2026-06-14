@file:Suppress ("HardCodedStringLiteral", "unused")
package net.calvuz.qreport.app.app.domain

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qreport.BuildConfig
import net.calvuz.qreport.app.app.QReportApplication
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** AppVersionInfo */
@Singleton
class AppVersionInfo @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    // ===== APP VERSION =====

    /** Version name dell'app (es. "1.2.3") */
    val appVersion: String by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Failed to get app version, using BuildConfig")
            BuildConfig.VERSION_NAME
        }
    }

    /** App version code (incremental int)
     */
    val appVersionCode: Long by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Failed to get app version code")
            1L
        }
    }

    // ===== DATABASE VERSION =====
    
    /** ⚠️ IMPORTANT: Database schema version */
    val databaseVersion: Int = QReportApplication.DATABASE_VERSION
    
    /** Database schema version string per display */
    val databaseVersionString: String = "Schema v$databaseVersion"
    
    // ===== COMPLETE VERSION INFO =====
    
    /** Complete version info for logs/analytics */
    val completeVersionInfo: String by lazy {
        "QReport v${appVersion} ($appVersionCode) - DB v$databaseVersion - " +
                "Build: $BUILD_TYPE"
    }
    
    // ===== BACKUP VERSION INFO =====
    
    /** Version info for backup metadata */
    fun getBackupVersionInfo(): BackupVersionInfo {
        return BackupVersionInfo(
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            databaseVersion = databaseVersion,
            buildType = BUILD_TYPE,
            createdAt = System.currentTimeMillis()
        )
    }
    
    companion object {
       
        // ===== BUILD INFO =====
        
        /** Build type (debug, release, etc.) */
        const val BUILD_TYPE: String = BuildConfig.BUILD_TYPE
        
        /** Application ID */
        const val APPLICATION_ID: String = BuildConfig.APPLICATION_ID
        
        /** Version name */
        const val VERSION_NAME = BuildConfig.VERSION_NAME
        
        /** Version code */
        const val VERSION_CODE = BuildConfig.VERSION_CODE
        
        /** Is debug build */
        val isDebugBuild: Boolean = BuildConfig.DEBUG
        
    }
}

/** Version info per backup metadata */
data class BackupVersionInfo(
    val appVersion: String,
    val appVersionCode: Long,
    val databaseVersion: Int,
    val buildType: String,
    val createdAt: Long
)