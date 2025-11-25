
package com.cs407.brickcollector

import android.content.Context
import android.content.pm.PackageManager

fun Context.getGeoapifyApiKey(): String? {
    return try {
        val appInfo = packageManager.getApplicationInfo(
            packageName,
            PackageManager.GET_META_DATA
        )
        appInfo.metaData?.getString("GEOAPIFY_API_KEY")
    } catch (e: Exception) {
        null
    }
}