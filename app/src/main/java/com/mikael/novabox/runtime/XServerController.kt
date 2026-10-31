package com.mikael.novabox.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/** Starts Termux:X11 and a loopback-only TCP-to-UnIX socket bridge in Termux. */
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
        if (!sendTermuxCommand(XServerCommand.COMMAND_PATH, XServerCommand.arguments(),
                "NovaBox: iniciar Termux:X11", "Solicita o display X11 :1.")) {
            return XServerLaunchResult.SERVICE_START_FAILED
        }
        return when (launch()) {
            XServerLaunchResult.OPENED -> XServerLaunchResult.START_REQUESTED
            else -> XServerLaunchResult.DISPLAY_COMMAND_SENT
        }
    }

    /**
     * Installs socat if needed, waits for Termux:X11's Unix socket, then exposes it
     * only on Android loopback TCP port 6001 (X11 display :1). Never binds to LAN.
     */
    fun startSocketBridge(): XServerLaunchResult {
        if (!isTermuxInstalled()) return XServerLaunchResult.TERMUX_NOT_INSTALLED
        if (!isInstalled()) return XServerLaunchResult.NOT_INSTALLED
        if (!hasRunCommandPermission()) return XServerLaunchResult.PERMISSION_REQUIRED
        val sent = sendTermuxCommand(
            XServerCommand.SHELL_PATH,
            XServerCommand.bridgeArguments(),
            "NovaBox: ponte X11 local",
            "Instala socat se necessário e conecta 127.0.0.1:6001 ao socket X11 :1."
        )
        return if (sent) XServerLaunchResult.BRIDGE_REQUESTED else XServerLaunchResult.SERVICE_START_FAILED
    }

    fun openNovaBoxPermissionSettings(): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + appContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun sendTermuxCommand(path: String, arguments: Array<String>, label: String, description: String): Boolean {
        val command = Intent(XServerCommand.RUN_COMMAND_ACTION).apply {
            setClassName(XServerCommand.TERMUX_PACKAGE, XServerCommand.RUN_COMMAND_SERVICE)
            putExtra(XServerCommand.EXTRA_COMMAND_PATH, path)
            putExtra(XServerCommand.EXTRA_ARGUMENTS, arguments)
            putExtra(XServerCommand.EXTRA_WORKDIR, XServerCommand.WORK_DIRECTORY)
            putExtra(XServerCommand.EXTRA_BACKGROUND, true)
            putExtra(XServerCommand.EXTRA_COMMAND_LABEL, label)
            putExtra(XServerCommand.EXTRA_COMMAND_DESCRIPTION, description)
        }
        return runCatching { appContext.startService(command) != null }.getOrDefault(false)
    }

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
    const val SHELL_PATH = "\$PREFIX/bin/sh"
    const val DISPLAY = ":1"
    const val WORK_DIRECTORY = "~/"
    const val WINE_DISPLAY = "127.0.0.1:1"
    const val TCP_PORT = 6001

    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
    const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"

    fun arguments(): Array<String> = arrayOf(DISPLAY)

    fun bridgeScript(): String = listOf(
        "command -v socat >/dev/null 2>&1 || pkg install -y socat || exit 20",
        "i=0",
        "while [ ! -S \"\$TMPDIR/.X11-unix/X1\" ] && [ \"\$i\" -lt 45 ]; do sleep 1; i=\$((i + 1)); done",
        "[ -S \"\$TMPDIR/.X11-unix/X1\" ] || exit 21",
        "nohup socat TCP-LISTEN:$TCP_PORT,bind=127.0.0.1,reuseaddr,fork UNIX-CONNECT:\"\$TMPDIR/.X11-unix/X1\" >/dev/null 2>&1 </dev/null &"
    ).joinToString("; ")

    fun bridgeArguments(): Array<String> = arrayOf("-lc", bridgeScript())
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
    DISPLAY_COMMAND_SENT,
    BRIDGE_REQUESTED
}
