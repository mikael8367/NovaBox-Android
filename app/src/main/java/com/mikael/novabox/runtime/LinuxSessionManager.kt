package com.mikael.novabox.runtime

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

sealed interface LinuxCommandResult {
    data class Completed(val exitCode: Int, val output: String) : LinuxCommandResult
    data class Failed(val reason: String) : LinuxCommandResult
}

object LinuxCommandPolicy {
    const val MAX_COMMAND_LENGTH = 4096
    const val MAX_OUTPUT_CHARS = 128 * 1024

    fun validate(command: String): String? = when {
        command.isBlank() -> "Digite um comando."
        command.length > MAX_COMMAND_LENGTH -> "O comando excede o limite de 4.096 caracteres."
        command.contains('\u0000') -> "O comando contém um caractere inválido."
        else -> null
    }

    fun buildArguments(prootPath: String, rootfsPath: String, sharedPath: String, command: String): List<String> {
        require(validate(command) == null) { validate(command) ?: "Comando inválido." }
        return listOf(
            prootPath, "--kill-on-exit", "-0", "-r", rootfsPath,
            "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-b", "$sharedPath:/mnt/shared", "-w", "/root", "/bin/sh", "-lc", command
        )
    }
}

class LinuxSessionManager(context: Context) {
    private val context = context.applicationContext
    private val filesDir = context.filesDir.canonicalFile
    private val runtimeRoot = File(filesDir, "runtimes")
    private val proot = File(runtimeRoot, "proot/root/bin/proot")
    private val rootfs = File(runtimeRoot, "linux_rootfs/rootfs")
    private val shared = File(filesDir, "linux-shared")
    private val temp = File(context.cacheDir, "proot-tmp")

    fun isReady(): Boolean =
        ProotInstaller(context).isInstalled() && proot.isFile && proot.canExecute() &&
            rootfs.isDirectory && File(rootfs, "bin/sh").isFile &&
            isInside(runtimeRoot, proot) && isInside(runtimeRoot, rootfs)

    fun runCommand(command: String, timeoutSeconds: Long = 30): LinuxCommandResult {
        LinuxCommandPolicy.validate(command)?.let { return LinuxCommandResult.Failed(it) }
        if (timeoutSeconds !in 1..120) return LinuxCommandResult.Failed("Tempo limite inválido.")
        if (!isReady()) return LinuxCommandResult.Failed("Instale o PRoot ARM64 e o Alpine Linux antes de iniciar o terminal.")

        return try {
            shared.mkdirs()
            temp.mkdirs()
            val args = LinuxCommandPolicy.buildArguments(proot.canonicalPath, rootfs.canonicalPath, shared.canonicalPath, command)
            val process = ProcessBuilder(args).directory(shared).redirectErrorStream(true).apply {
                environment()["HOME"] = "/root"
                environment()["TMPDIR"] = "/tmp"
                environment()["TERM"] = "xterm-256color"
                environment()["PROOT_TMP_DIR"] = temp.canonicalPath
                environment()["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            }.start()

            val output = StringBuilder()
            val reader = Thread {
                process.inputStream.use { input ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        synchronized(output) {
                            val remaining = LinuxCommandPolicy.MAX_OUTPUT_CHARS - output.length
                            if (remaining > 0) output.append(String(buffer, 0, minOf(count, remaining), Charsets.UTF_8))
                        }
                    }
                }
            }.apply { isDaemon = true; start() }

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) process.destroyForcibly()
            }
            reader.join(1500)
            val text = synchronized(output) { output.toString() } + if (finished) "" else "\n[Comando interrompido: tempo limite excedido.]"
            if (finished) LinuxCommandResult.Completed(process.exitValue(), text) else LinuxCommandResult.Failed(text)
        } catch (error: Exception) {
            LinuxCommandResult.Failed(error.message ?: "Falha ao iniciar o shell Linux.")
        }
    }

    private fun isInside(parent: File, child: File): Boolean {
        val prefix = parent.canonicalPath.trimEnd(File.separatorChar) + File.separator
        return child.canonicalPath.startsWith(prefix)
    }
}
