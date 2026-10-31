package com.mikael.novabox.runtime

import android.content.Context
import com.github.luben.zstd.ZstdInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.ensureActive

/**
 * Reproducible runtime payloads pinned to one immutable Winlator source commit.
 * Git blob SHA-1 is checked using Git's "blob <size>\\0" object framing.
 */
data class UpstreamAsset(
    val name: String,
    val path: String,
    val size: Long,
    val gitBlobSha1: String,
    val maxExpandedBytes: Long
)

object WinlatorRuntimeManifest {
    const val SOURCE_COMMIT = "8077f374ef2d54127b088de9e8cd7b4af14d9c8b"
    const val SOURCE_REPOSITORY = "https://github.com/brunodev85/winlator-app"

    val assets = listOf(
        UpstreamAsset("rootfs", "rootfs.tzst", 65_254_618L, "3276da2f280707ccfded5dd76181e7bf7a53ec6e", 1_200L * 1024 * 1024),
        UpstreamAsset("rootfs-patches", "rootfs_patches.tzst", 4_174_031L, "5d2ce398eb79f425da09a31e5cd47550df9b8884", 256L * 1024 * 1024),
        UpstreamAsset("box64", "box64/box64-0.4.4.tzst", 4_540_371L, "c505bc89a765f1e259491730f3064772bfb5e00f", 128L * 1024 * 1024),
        UpstreamAsset("pulseaudio", "pulseaudio.tzst", 45_548L, "12df2f70b8b6fce7b47813e46075f32e7dc85c70", 32L * 1024 * 1024)
    )

    internal fun url(asset: UpstreamAsset): String =
        "https://raw.githubusercontent.com/brunodev85/winlator-app/$SOURCE_COMMIT/app/src/main/assets/${asset.path}"
}

data class BundleInstallProgress(
    val asset: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val phase: String
)

sealed interface BundleInstallResult {
    data object Installed : BundleInstallResult
    data class Failed(val reason: String) : BundleInstallResult
}

class WinlatorRuntimeBundleInstaller(context: Context) {
    private val app = context.applicationContext
    private val runtimes = File(app.filesDir, "runtimes")
    private val targetRoot = File(runtimes, "rootfs")

    fun isInstalled(): Boolean {
        val marker = File(targetRoot, ".novabox-winlator-runtime")
        return marker.isFile && marker.readText().contains(WinlatorRuntimeManifest.SOURCE_COMMIT) &&
            File(targetRoot, "usr/local/bin/box64").isFile &&
            File(targetRoot, "opt/wine/bin/wine").isFile &&
            File(targetRoot, "opt/wine/bin/wineboot").isFile
    }

    suspend fun install(
        onProgress: suspend (BundleInstallProgress) -> Unit = {}
    ): BundleInstallResult {
        var stage: File? = null
        val downloads = mutableListOf<File>()
        try {
            if (!runtimes.exists() && !runtimes.mkdirs()) {
                throw IOException("Não foi possível criar a pasta de runtimes.")
            }
            val required = WinlatorRuntimeManifest.assets.sumOf { it.size } +
                1_600L * 1024L * 1024L
            if (runtimes.usableSpace < required) {
                throw IOException("Espaço insuficiente: libere aproximadamente 1,7 GB antes de instalar o runtime.")
            }
            val stagingDirectory = File(runtimes, ".rootfs-staging-${UUID.randomUUID()}")
            stage = stagingDirectory
            if (!stagingDirectory.mkdirs()) throw IOException("Não foi possível criar a área temporária.")

            for (asset in WinlatorRuntimeManifest.assets) {
                val archive = File(app.cacheDir, "novabox-${asset.name}-${UUID.randomUUID()}.part")
                downloads += archive
                downloadAndVerify(asset, archive) { received ->
                    onProgress(BundleInstallProgress(asset.name, received, asset.size, "download"))
                }
                onProgress(BundleInstallProgress(asset.name, asset.size, asset.size, "extração"))
                SafeZstdTarExtractor.extract(archive, stagingDirectory, asset.maxExpandedBytes)
                archive.delete()
                downloads.remove(archive)
            }

            val requiredFiles = listOf(
                "usr/local/bin/box64",
                "opt/wine/bin/wine",
                "opt/wine/bin/wineboot"
            )
            requiredFiles.forEach { relative ->
                val file = File(stagingDirectory, relative).canonicalFile
                if (!file.toPath().startsWith(stagingDirectory.canonicalFile.toPath()) || !file.isFile || file.length() == 0L) {
                    throw IOException("O pacote upstream não contém o arquivo obrigatório: $relative")
                }
                if (!file.setExecutable(true, true) && !file.canExecute()) {
                    throw IOException("Não foi possível habilitar a execução de $relative.")
                }
            }
            File(stagingDirectory, ".novabox-winlator-runtime").writeText(
                "source=${WinlatorRuntimeManifest.SOURCE_REPOSITORY}\n" +
                    "commit=${WinlatorRuntimeManifest.SOURCE_COMMIT}\n" +
                    "box64=0.4.4\n"
            )

            val backup = File(runtimes, ".rootfs-backup-${UUID.randomUUID()}")
            if (targetRoot.exists() && !targetRoot.renameTo(backup)) {
                throw IOException("Não foi possível preservar o runtime anterior.")
            }
            if (!stagingDirectory.renameTo(targetRoot)) {
                val restored = !backup.exists() || backup.renameTo(targetRoot)
                if (!restored) throw IOException("Falha na troca; a versão anterior ficou preservada em ${backup.name}.")
                throw IOException("Falha ao ativar o runtime; a versão anterior foi restaurada.")
            }
            stage = null
            backup.deleteRecursively()
            onProgress(BundleInstallProgress("concluído", 1, 1, "concluído"))
            return BundleInstallResult.Installed
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return BundleInstallResult.Failed(error.message ?: "Falha ao instalar o runtime Wine/Box64.")
        } finally {
            downloads.forEach { it.delete() }
            stage?.deleteRecursively()
        }
    }

    private suspend fun downloadAndVerify(
        asset: UpstreamAsset,
        destination: File,
        onProgress: suspend (Long) -> Unit
    ) {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${asset.size}\u0000".toByteArray(Charsets.UTF_8))
        var current = URL(WinlatorRuntimeManifest.url(asset))
        val allowed = setOf("raw.githubusercontent.com", "github.com", "release-assets.githubusercontent.com")
        var redirects = 0
        while (true) {
            if (current.protocol != "https" || current.host.lowercase() !in allowed) {
                throw IOException("Origem de download não autorizada.")
            }
            val connection = current.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    if (++redirects > 5) throw IOException("Redirecionamentos demais.")
                    current = URL(current, connection.getHeaderField("Location")
                        ?: throw IOException("Redirecionamento sem destino."))
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) throw IOException("Download ${asset.name}: HTTP $code.")
                val declared = connection.contentLengthLong
                if (declared > 0 && declared != asset.size) throw IOException("Tamanho publicado divergente em ${asset.name}.")
                var total = 0L
                BufferedInputStream(connection.inputStream).use { input ->
                    FileOutputStream(destination).buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > asset.size) throw IOException("O download excedeu o tamanho esperado.")
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            onProgress(total)
                        }
                    }
                }
                if (total != asset.size) throw IOException("Download incompleto em ${asset.name}.")
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(asset.gitBlobSha1, true)) throw IOException("Hash Git do pacote ${asset.name} não confere.")
                return
            } finally {
                connection.disconnect()
            }
        }
    }
}

/** Extracts tar+zstd without accepting traversal, device nodes, FIFOs or escaping links. */
private object SafeZstdTarExtractor {
    fun extract(archive: File, destination: File, maxExpandedBytes: Long) {
        val root = destination.canonicalFile.toPath()
        var expanded = 0L
        ZstdInputStream(archive.inputStream().buffered()).use { zstd ->
            TarArchiveInputStream(zstd).use { tar ->
                while (true) {
                    val entry = tar.nextTarEntry ?: break
                    val relative = RuntimeArchivePathPolicy.safeEntryPath(entry.name) ?: throw IOException("Caminho inválido no pacote.")
                    if (relative.isEmpty()) continue
                    val output = root.resolve(relative).normalize()
                    if (!output.startsWith(root)) throw IOException("Entrada fora da raiz do runtime.")
                    when {
                        entry.isDirectory -> Files.createDirectories(output)
                        entry.isSymbolicLink -> {
                            Files.createDirectories(output.parent)
                            val link = RuntimeArchivePathPolicy.resolveLinkTarget(root, output.parent, entry.linkName)
                            Files.deleteIfExists(output)
                            Files.createSymbolicLink(output, output.parent.relativize(link))
                        }
                        entry.isLink -> {
                            val linkPath = RuntimeArchivePathPolicy.safeEntryPath(entry.linkName) ?: throw IOException("Hard link inválido.")
                            val source = root.resolve(linkPath).normalize()
                            if (!source.startsWith(root) || !Files.isRegularFile(source)) {
                                throw IOException("Hard link aponta para arquivo inexistente.")
                            }
                            Files.createDirectories(output.parent)
                            Files.deleteIfExists(output)
                            Files.copy(source, output)
                        }
                        entry.isFile -> {
                            if (entry.size < 0L || entry.size > maxExpandedBytes - expanded) {
                                throw IOException("O pacote excede o limite de extração.")
                            }
                            Files.createDirectories(output.parent)
                            FileOutputStream(output.toFile()).use { out ->
                                val buffer = ByteArray(64 * 1024)
                                var remaining = entry.size
                                while (remaining > 0) {
                                    val count = tar.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                                    if (count < 0) throw IOException("Arquivo TAR truncado.")
                                    out.write(buffer, 0, count)
                                    remaining -= count
                                    expanded += count
                                    if (expanded > maxExpandedBytes) throw IOException("O pacote excede o limite de extração.")
                                }
                            }
                            if ((entry.mode and 73) != 0) output.toFile().setExecutable(true, false)
                        }
                        else -> throw IOException("Tipo de entrada TAR não permitido.")
                    }
                }
            }
        }
    }
}
