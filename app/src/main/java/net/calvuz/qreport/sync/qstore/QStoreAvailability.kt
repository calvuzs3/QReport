package net.calvuz.qreport.sync.qstore

import android.content.Context
import android.content.pm.PackageManager

object QStoreAvailability {
    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage("net.calvuz.qstore") != null

    fun hasReadPermission(context: Context): Boolean =
        context.checkSelfPermission(ArticleContract.PERMISSION_READ) ==
            PackageManager.PERMISSION_GRANTED
}
