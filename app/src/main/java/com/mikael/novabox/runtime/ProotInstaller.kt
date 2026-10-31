package com.mikael.novabox.runtime

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface ProotInstallResult {
    data object Installed : ProotInstallResult
    data class Failed(val reason: String) : ProotInstallResult
}

object ProotPackageManifest {
    const val SIZE = 179_448L
    const val GIT_BLOB_SHA1 = "45158586e587e1adf9a75db687dfdb08baec83c5"
    const val UPSTREAM_COMMIT = "01f83b8841358450c78333d1b33ab30d4943bec4"
    const val URL = "https://raw.githubusercontent.com/green-green-avk/build-proot-android/$UPSTREAM_COMMIT/packages/proot-android-aarch64.tar.gz"

    fun gitBlobSha1(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.US_ASCII))
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

class ProotInstaller(context: Context) {
    private val runtimeRoot = File(context.filesDir, "runtimes").canonicalFile
    private val destination = File(runtimeRoot, "proot")
    private val archive = File(context.cacheDir, "proot-arm64.tar.gz")

    fun isInstalled(): Boolean {
        val executable = File(destination, "root/bin/proot")
        val loaderDirectory = File(destination, "root/libexec/proot")
        return executable.isFile && executable.canExecute() &&
            loaderDirectory.isDirectory && loaderDirectory.listFiles()?.isNotEmpty() == true &&
            isInside(runtimeRoot, executable)
    }

    fun install(): ProotInstallResult {
        val staging = File(runtimeRoot, "proot-staging-${UUID.randomUUID()}")
        return try {
            runtimeRoot.mkdirs()
            archive.parentFile?.mkdirs()
            download()
            if (archive.length() != ProotPackageManifest.SIZE) {
                throw IllegalStateException("Tamanho do pacote PRoot não corresponde ao manifesto.")
            }
            val bytes = archive.readBytes()
            if (ProotPackageManifest.gitBlobSha1(bytes) != ProotPackageManifest.GIT_BLOB_SHA1) {
                throw SecurityException("A verificação de integridade Git do pacote PRoot falhou.")
            }
            staging.mkdirs()
            extractSafely(staging)
            val executable = File(staging, "root/bin/proot")
            val loaders = File(staging, "root/libexec/proot")
            if (!executable.isFile || !loaders.isDirectory || loaders.listFiles()?.isEmpty() != false) {
                throw IllegalStateException("O pacote não contém o executável e os loaders PRoot esperados.")
            }
            if (!executable.setExecutable(true, true) && !executable.canExecute()) {
                throw IllegalStateException("Não foi possível habilitar a execução do PRoot.")
            }
            val probe = ProcessBuilder(executable.absolutePath, "--version")
                .directory(staging)
                .redirectErrorStream(true)
                .apply { environment()["PROOT_TMP_DIR"] = File(staging, "tmp").absolutePath }
                .start()
            val probeFinished = probe.waitFor(8, TimeUnit.SECONDS)
            if (!probeFinished) {
                probe.destroyForcibly()
                throw IllegalStateException("O PRoot ARM64 não respondeu ao teste de versão.")
            }
            if (probe.exitValue() != 0) {
                throw IllegalStateException("O PRoot ARM64 não passou no teste de inicialização.")
            }
            val old = File(runtimeRoot, "proot-old")
            if (old.exists()) old.deleteRecursively()
            if (destination.exists() && !destination.renameTo(old)) {
                throw IllegalStateException("Não foi possível preservar a instalação PRoot anterior.")
            }
            if (!staging.renameTo(destination)) {
                if (old.exists()) old.renameTo(destination)
                throw IllegalStateException("Não foi possível ativar a instalação PRoot.")
            }
            old.deleteRecursively()
            ProotInstallResult.Installed
        } catch (error: Exception) {
            ProotInstallResult.Failed(error.message ?: "Falha ao instalar PRoot ARM64.")
        } finally {
            staging.deleteRecursively()
            archive.delete()
        }
    }

    private fun download() {
        val url = URL(ProotPackageManifest.URL)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NovaBox-Android")
        }
        try {
            connection.connect()
            val host = connection.url.host.lowercase()
            if (host != "raw.githubusercontent.com" && !host.endsWith(".githubusercontent.com")) {
                throw SecurityException("Host de download PRoot não autorizado.")
            }
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Servidor PRoot respondeu HTTP ${connection.responseCode}.")
            }
            val expected = ProotPackageManifest.SIZE
            if (connection.contentLengthLong > 0 && connection.contentLengthLong != expected) {
                throw IllegalStateException("Tamanho anunciado do pacote PRoot é inválido.")
            }
            connection.inputStream.use { input ->
                FileOutputStream(archive).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > expected) throw IllegalStateException("Pacote PRoot excedeu o tamanho permitido.")
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                    if (total != expected) throw IllegalStateException("Download PRoot incompleto.")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractSafely(target: File) {
        var entries = 0
        var expanded = 0L
        val canonicalRoot = target.canonicalFile
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archive)))).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                entries++
                if (entries > 128) throw IllegalStateException("Pacote PRoot contém entradas demais.")
                val name = entry.name.replace('\\', '/')
                if (name.startsWith("/") || name.split('/').any { it == ".." || it.isEmpty() }) {
                    if (name == "root/") continue
                    throw SecurityException("Caminho inválido no pacote PRoot.")
                }
                if (!name.startsWith("root/")) throw SecurityException("Entrada fora da pasta root do PRoot.")
                if (entry.isSymbolicLink || entry.isLink || entry.isCharacterDevice || entry.isBlockDevice || entry.isFIFO) {
                    throw SecurityException("Links e arquivos especiais não são permitidos no pacote PRoot.")
                }
                val output = File(canonicalRoot, name).canonicalFile
                if (!isInside(canonicalRoot, output)) throw SecurityException("Tentativa de sair da pasta de instalação PRoot.")
                if (entry.isDirectory) {
                    output.mkdirs()
                    continue
                }
                if (!entry.isFile || entry.size < 0L) throw SecurityException("Tipo de arquivo não permitido no pacote PRoot.")
                expanded += entry.size
                if (expanded > 8L * 1024L * 1024L) throw IllegalStateException("Expansão PRoot excedeu o limite.")
                output.parentFile?.mkdirs()
                FileOutputStream(output).use { out ->
                    tar.copyToBounded(out, entry.size)
                    out.fd.sync()
                }
                if (entry.mode and 0b001_001_001 != 0) output.setExecutable(true, true)
            }
        }
    }

    private fun TarArchiveInputStream.copyToBounded(output: FileOutputStream, expected: Long) {
        val buffer = ByteArray(8192)
        var copied = 0L
        while (copied < expected) {
            val count = read(buffer, 0, minOf(buffer.size.toLong(), expected - copied).toInt())
            if (count < 0) throw IllegalStateException("Arquivo PRoot truncado.")
            output.write(buffer, 0, count)
            copied += count
        }
    }

    private fun isInside(parent: File, child: File): Boolean {
        val prefix = parent.canonicalPath.trimEnd(File.separatorChar) + File.separator
        return child.canonicalPath.startsWith(prefix)
    }
}
