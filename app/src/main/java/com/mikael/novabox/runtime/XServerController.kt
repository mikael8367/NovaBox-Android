package com.mikael.novabox.runtime

import android.content.Context
import android.content.Intent

/** Optional bridge to the Termux:X11 companion; NovaBox does not bundle an X server. */
class XServerController(context: Context) {
    private val appContext = context.applicationContext

    fun isInstalled(): Boolean = runCatching {
        appContext.packageManager.getPackageInfo(PACKAGE_NAME, 0)
    }.isSuccess

    fun launch(): XServerLaunchResult {
        if (!isInstalled()) return XServerLaunchResult.NOT_INSTALLED
        val intent = appContext.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            ?: return XServerLaunchResult.NO_LAUNCH_ACTIVITY
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            XServerLaunchResult.OPENED
        }.getOrElse { XServerLaunchResult.FAILED }
    }

    companion object {
        const val PACKAGE_NAME = "com.termux.x11"
    }
}

enum class XServerLaunchResult { OPENED, NOT_INSTALLED, NO_LAUNCH_ACTIVITY, FAILED }
