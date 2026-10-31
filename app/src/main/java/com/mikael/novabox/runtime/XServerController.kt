package com.mikael.novabox.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/**
 * Starts the optional Termux:X11 server through Termux's documented RUN_COMMAND API.
 *
 * This only starts the external X server. It does not make the X11 socket accessible
 * to NovaBox's sandboxed Wine process; that transport still needs a separate backend.
 */
class XServerController(context: Context) {
    private val appContext = context.applicationContext

    fun isInstalled(): Boolean = packageInstalled(XServerCommand.X11_PACKAGE)

    fun isTermuxInstalled(): Boolean = packageInstalled(XServerCommand.TERMUX_PACKAGE)

    fun hasRunCommandPermission(): Boolean =
        appContext.checkSelfPermission(XServerCommand.RUN_COMMAND_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    fun launch(): XServerLaunchResult {
        if (!isInstalled()) return XServerLaunchResult.NOT_INSTALLED
        val intent = appContext.packageManager.getLaunchIntentForPackage(XServerCommand.X11_PACKAGE)
            ?: return XServerLaunchResult.NO_LAUNCH_ACTIVITY
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            XServerLaunchResult.OPENED
        }.getOrElse { XServerLaunchResult.FAILED }
    }

    fun startDisplay(): XServerLaunchResult {
        if (!isTermuxInstalled()) return XServerLaunchResult.TERMUX_NOT_INSTALLED
        if (!isInstalled()) return XServerLaunchResult.NOT_INSTALLED
        if (!hasRunCommandPermission()) return XServerLaunchResult.PERMISSION_REQUIRED

        val command = Intent(XServerCommand.RUN_COMMAND_ACTION).apply {
            setClassName(XServerCommand.TERMUX_PACKAGE, XServerCommand.RUN_COMMAND_SERVICE)
            putExtra(XServerCommand.EXTRA_COMMAND_PATH, XServerCommand.COMMAND_PATH)
            putExtra(XServerCommand.EXTRA_ARGUMENTS, XServerCommand.arguments())
            putExtra(XServerCommand.EXTRA_WORKDIR, XServerCommand.WORK_DIRECTORY)
            putExtra(XServerCommand.EXTRA_BACKGROUND, true)
            putExtra(XServerCommand.EXTRA_COMMAND_LABEL, "NovaBox: iniciar Termux:X11")
            putExtra(
                XServerCommand.EXTRA_COMMAND_DESCRIPTION,
                "Inicia o display X11 :1 solicitado pelo NovaBox."
            )
        }

        val serviceStarted = runCatching {
            appContext.startService(command) != null
        }.getOrDefault(false)
        if (!serviceStarted) return XServerLaunchResult.SERVICE_START_FAILED

        return when (launch()) {
            XServerLaunchResult.OPENED -> XServerLaunchResult.START_REQUESTED
            else -> XServerLaunchResult.DISPLAY_COMMAND_SENT
        }
    }

    fun openNovaBoxPermissionSettings(): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + appContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun packageInstalled(packageName: String): Boolean = runCatching {
        appContext.packageManager.getPackageInfo(packageName, 0)
    }.isSuccess
}

object XServerCommand {
    const val TERMUX_PACKAGE = "com.termux"
    const val X11_PACKAGE = "com.termux.x11"
    const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    const val COMMAND_PATH = "\$PREFIX/bin/termux-x11"
    const val DISPLAY = ":1"
    const val WORK_DIRECTORY = "~/"

    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
    const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"

    fun arguments(): Array<String> = arrayOf(DISPLAY)
}

enum class XServerLaunchResult {
    OPENED,
    NOT_INSTALLED,
    NO_LAUNCH_ACTIVITY,
    FAILED,
    TERMUX_NOT_INSTALLED,
    PERMISSION_REQUIRED,
    SERVICE_START_FAILED,
    START_REQUESTED,
    DISPLAY_COMMAND_SENT
}
