package net.calvuz.qreport.domain.model.share

import android.graphics.drawable.Drawable

data class ShareAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)