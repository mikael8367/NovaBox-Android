package com.mikael.novabox.runtime

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object AlpineRootfsManifest {
    const val VERSION = "3.24.3"
    const val ARCHITECTURE = "aarch64"
    const val SIZE_BYTES = 4_029_999L
    const val SHA256 = "4d3f80afaebd447c5591a63a73bde380a149fe8b2d85488cafabb2ff97551f50"
    const val URL = "https://dl-cdn.alpinelinux.org/alpine/latest-stable/releases/aarch64/alpine-minirootfs-3.24.3-aarch64.tar.gz"
    const val SOURCE = "https://alpinelinux.org/"
    const val MAX_EXPANDED_BYTES = 256L * 1024L * 1024L
}

sealed interface LinuxRootfsInstallResult {
    data object Installed : LinuxRootfsInstallResult
    data class Failed(val reason: String) : LinuxRootfsInstallResult
}

class AlpineRootfsInstaller(context: Context) {
    private val app = context.applicationContext
    private val runtimeRoot = File(app.filesDir, "runtimes/linux_rootfs")
    private val target = File(runtimeRoot, "rootfs")

    fun isInstalled(): Boolean {
        val marker = File(target, ".novabox-alpine-rootfs")
        val busybox = File(target, "bin/busybox")
        return marker.isFile && marker.readText().contains("version=" + AlpineRootfsManifest.VERSION) &&
            busybox.isFile && busybox.canExecute() &&
            ElfAbiValidator.isCompatible(busybox, setOf("arm64-v8a")) &&
            File(target, "lib/ld-musl-aarch64.so.1").exists() &&
            File(target, "etc/alpine-release").isFile
    }

    suspend fun install(onProgress: suspend (RuntimeDownloadProgress) -> Unit = {}): LinuxRootfsInstallResult {
        var archive: File? = null
        var staging: File? = null
        val jobContext = currentCoroutineContext()
        try {
            if (!runtimeRoot.exists() && !runtimeRoot.mkdirs()) throw IOException("Não foi possível criar a pasta Linux.")
            if (runtimeRoot.usableSpace < 192L * 1024L * 1024L) {
                throw IOException("Libere pelo menos 192 MB para instalar o Alpine Linux.")
            }
            archive = File(app.cacheDir, "alpine-minirootfs-" + UUID.randomUUID() + ".part")
            download(archive, onProgress)
            val actual = sha256(archive)
            if (!actual.equals(AlpineRootfsManifest.SHA256, ignoreCase = true)) {
                throw IOException("SHA-256 do Alpine Linux não confere; download descartado.")
            }
            val stage = File(runtimeRoot, ".rootfs-staging-" + UUID.randomUUID())
            staging = stage
            if (!stage.mkdirs()) throw IOException("Não foi possível criar a pasta temporária do rootfs.")
            SafeRootfsTarExtractor.extract(archive, stage, AlpineRootfsManifest.MAX_EXPANDED_BYTES) {
                jobContext.ensureActive()
            }
            jobContext.ensureActive()
            val busybox = File(stage, "bin/busybox")
            val required = listOf(busybox, File(stage, "lib/ld-musl-aarch64.so.1"), File(stage, "etc/alpine-release"))
            if (required.any { !it.exists() || (it.isFile && it.length() == 0L) }) {
                throw IOException("O rootfs Alpine está incompleto: arquivos essenciais ausentes.")
            }
            if (!busybox.isFile || !ElfAbiValidator.isCompatible(busybox, setOf("arm64-v8a"))) {
                throw IOException("O BusyBox do rootfs não é um executável ELF ARM64 válido.")
            }
            if (!busybox.setExecutable(true, true) && !busybox.canExecute()) {
                throw IOException("Não foi possível habilitar o BusyBox para execução.")
            }
            File(stage, ".novabox-alpine-rootfs").writeText(
                "source=" + AlpineRootfsManifest.SOURCE + "\nversion=" + AlpineRootfsManifest.VERSION +
                    "\narchitecture=" + AlpineRootfsManifest.ARCHITECTURE + "\nsha256=" + AlpineRootfsManifest.SHA256 + "\n"
            )
            jobContext.ensureActive()
            val backup = File(runtimeRoot, ".rootfs-backup-" + UUID.randomUUID())
            if (target.exists() && !target.renameTo(backup)) throw IOException("Não foi possível preservar o rootfs anterior.")
            if (!stage.renameTo(target)) {
                val restored = !backup.exists() || backup.renameTo(target)
                if (!restored) throw IOException("Falha na troca; o rootfs anterior foi preservado em " + backup.name + ".")
                throw IOException("Não foi possível ativar o novo rootfs; a versão anterior foi restaurada.")
            }
            staging = null
            backup.deleteRecursively()
            onProgress(RuntimeDownloadProgress(1L, 1L))
            return LinuxRootfsInstallResult.Installed
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return LinuxRootfsInstallResult.Failed(error.message ?: "Falha ao instalar o Alpine Linux.")
        } finally {
            archive?.delete()
            staging?.deleteRecursively()
        }
    }

    private suspend fun download(destination: File, onProgress: suspend (RuntimeDownloadProgress) -> Unit) {
        val context = currentCoroutineContext()
        var current = URL(AlpineRootfsManifest.URL)
        val allowedHosts = setOf("dl-cdn.alpinelinux.org", "alpinelinux.mirror.garr.it", "mirrors.lug.mtu.edu")
        var redirects = 0
        while (true) {
            if (current.protocol != "https" || current.host.lowercase() !in allowedHosts) {
                throw IOException("Origem do rootfs Alpine não autorizada.")
            }
            val connection = current.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    if (++redirects > 5) throw IOException("Redirecionamentos demais ao baixar Alpine.")
                    current = URL(current, connection.getHeaderField("Location") ?: throw IOException("Redirecionamento sem destino."))
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) throw IOException("Download Alpine: HTTP " + code + ".")
                val declared = connection.contentLengthLong
                if (declared > 0L && declared != AlpineRootfsManifest.SIZE_BYTES) throw IOException("Tamanho publicado do Alpine divergente.")
                var total = 0L
                var lastReported = 0L
                BufferedInputStream(connection.inputStream).use { input ->
                    FileOutputStream(destination).buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            context.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > AlpineRootfsManifest.SIZE_BYTES) throw IOException("Download Alpine excedeu o tamanho esperado.")
                            output.write(buffer, 0, count)
                            if (total - lastReported >= 128L * 1024L || total == AlpineRootfsManifest.SIZE_BYTES) {
                                lastReported = total
                                onProgress(RuntimeDownloadProgress(total, AlpineRootfsManifest.SIZE_BYTES))
                            }
                        }
                    }
                }
                if (total != AlpineRootfsManifest.SIZE_BYTES) throw IOException("Download Alpine incompleto.")
                return
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

}

internal object SafeRootfsTarExtractor {
    fun extract(archive: File, destination: File, maxExpandedBytes: Long, checkCancelled: () -> Unit) {
        val root = destination.canonicalFile.toPath()
        var expanded = 0L
        var entries = 0
        GZIPInputStream(archive.inputStream().buffered()).use { gzip ->
            TarArchiveInputStream(gzip).use { tar ->
                while (true) {
                    checkCancelled()
                    val entry = tar.nextTarEntry ?: break
                    entries++
                    if (entries > 100_000) throw IOException("O rootfs contém entradas demais.")
                    val relative = RuntimeArchivePathPolicy.safeEntryPath(entry.name)
                        ?: throw IOException("Caminho inválido no rootfs Alpine.")
                    if (relative.isEmpty()) continue
                    val output = root.resolve(relative).normalize()
                    ensureInsideRoot(root, output)
                    when {
                        entry.isDirectory -> Files.createDirectories(output)
                        entry.isSymbolicLink -> {
                            Files.createDirectories(output.parent)
                            ensureInsideRoot(root, output.parent)
                            val link = RuntimeArchivePathPolicy.resolveLinkTarget(root, output.parent, entry.linkName)
                            ensureInsideRoot(root, link)
                            Files.deleteIfExists(output)
                            Files.createSymbolicLink(output, output.parent.relativize(link))
                        }
                        entry.isLink -> {
                            val linkName = RuntimeArchivePathPolicy.safeEntryPath(entry.linkName)
                                ?: throw IOException("Hard link inválido no rootfs.")
                            val source = root.resolve(linkName).normalize()
                            ensureInsideRoot(root, source)
                            if (!source.startsWith(root) || !Files.isRegularFile(source)) throw IOException("Hard link inválido no rootfs.")
                            Files.createDirectories(output.parent)
                            Files.deleteIfExists(output)
                            Files.copy(source, output)
                        }
                        entry.isFile -> {
                            if (entry.size < 0L || entry.size > maxExpandedBytes - expanded) {
                                throw IOException("O rootfs excede o limite de extração.")
                            }
                            Files.createDirectories(output.parent)
                            FileOutputStream(output.toFile()).use { out ->
                                val buffer = ByteArray(64 * 1024)
                                var remaining = entry.size
                                while (remaining > 0L) {
                                    checkCancelled()
                                    val count = tar.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                                    if (count < 0) throw IOException("Arquivo TAR do rootfs truncado.")
                                    out.write(buffer, 0, count)
                                    remaining -= count
                                    expanded += count
                                    if (expanded > maxExpandedBytes) throw IOException("O rootfs excede o limite de extração.")
                                }
                            }
                            if ((entry.mode and 73) != 0) output.toFile().setExecutable(true, false)
                        }
                        else -> throw IOException("Tipo de entrada TAR não permitido no rootfs.")
                    }
                }
            }
        }
    }

    private fun ensureInsideRoot(root: java.nio.file.Path, path: java.nio.file.Path) {
        val canonical = path.toFile().canonicalFile.toPath()
        if (!canonical.startsWith(root)) throw IOException("Entrada tenta escapar do rootfs.")
    }
}