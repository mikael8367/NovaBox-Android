package com.mikael.novabox.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

data class RuntimeDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val fraction: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

sealed interface RuntimeInstallResult {
    data class Installed(val runtimeId: String, val version: String) : RuntimeInstallResult
    data class Failed(val reason: String) : RuntimeInstallResult
}

class RuntimePackageInstaller(private val context: Context) {
    suspend fun install(
        spec: RuntimePackageSpec,
        onProgress: suspend (RuntimeDownloadProgress) -> Unit = {}
    ): RuntimeInstallResult = withContext(Dispatchers.IO) {
        var archive: File? = null
        var staging: File? = null
        try {
            validateSpec(spec)
            val runtimeDefinition = RuntimeComponentRegistry.get(spec.runtimeId)
            val availableBytes = context.filesDir.usableSpace
            val minimumRequired = Math.addExact(spec.expectedSizeBytes, spec.maxExpandedBytes / 2L)
            if (availableBytes < minimumRequired) {
                throw IOException("Espaço insuficiente. Libere armazenamento e tente novamente.")
            }

            val downloadDirectory = File(context.cacheDir, "runtime-downloads")
            if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
                throw IOException("Não foi possível criar a pasta temporária de download.")
            }
            archive = File(downloadDirectory, "${spec.runtimeId.name.lowercase(Locale.ROOT)}-${UUID.randomUUID()}.part")
            download(spec, archive!!, onProgress)

            val actualHash = sha256(archive!!)
            if (!actualHash.equals(spec.sha256, ignoreCase = true)) {
                throw IOException("Falha na verificação SHA-256. O pacote foi descartado.")
            }

            val runtimeRoot = File(context.filesDir, "runtimes").canonicalFile
            if (!runtimeRoot.exists() && !runtimeRoot.mkdirs()) {
                throw IOException("Não foi possível preparar a pasta de motores.")
            }
            staging = File(runtimeRoot, ".${spec.runtimeId.name.lowercase(Locale.ROOT)}-staging-${UUID.randomUUID()}")
            if (!staging!!.mkdirs()) throw IOException("Não foi possível preparar a instalação temporária.")

            when (spec.archiveFormat) {
                RuntimeArchiveFormat.TAR_GZ -> TarGzipArchiveExtractor.extract(
                    archive = archive!!,
                    destination = staging!!,
                    rootDirectory = spec.archiveRootDirectory,
                    outputDirectory = spec.outputDirectory,
                    allowedFiles = spec.allowedArchiveFiles,
                    requiredFiles = spec.requiredArchiveFiles,
                    maxExpandedBytes = spec.maxExpandedBytes
                )
            }

            validateInstalledFiles(staging!!, runtimeDefinition)
            runtimeDefinition.executablePaths.forEach { relative ->
                val executable = File(staging!!, relative)
                if (executable.exists() && !executable.setExecutable(true, true)) {
                    throw IOException("Não foi possível marcar $relative como executável.")
                }
            }

            val target = File(runtimeRoot, spec.runtimeId.name.lowercase(Locale.ROOT))
            val backup = File(runtimeRoot, ".${spec.runtimeId.name.lowercase(Locale.ROOT)}-backup-${UUID.randomUUID()}")
            if (target.exists() && !target.renameTo(backup)) {
                throw IOException("Não foi possível preservar a instalação anterior.")
            }
            if (!staging!!.renameTo(target)) {
                val restored = !backup.exists() || backup.renameTo(target)
                if (!restored) {
                    throw IOException("A instalação falhou. A cópia anterior foi preservada em ${backup.name}.")
                }
                throw IOException("Não foi possível concluir a instalação; a versão anterior foi restaurada.")
            }
            if (backup.exists()) backup.deleteRecursively()
            staging = null
            archive!!.delete()
            archive = null
            RuntimeInstallResult.Installed(spec.runtimeId.name, spec.version)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            RuntimeInstallResult.Failed(error.message ?: "Falha desconhecida ao instalar o componente.")
        } finally {
            archive?.delete()
            staging?.deleteRecursively()
        }
    }

    private suspend fun download(
        spec: RuntimePackageSpec,
        destination: File,
        onProgress: suspend (RuntimeDownloadProgress) -> Unit
    ) {
        val coroutineContext = currentCoroutineContext()
        var currentUrl = URL(spec.downloadUrl)
        val allowedHosts = setOf(
            "github.com",
            "release-assets.githubusercontent.com",
            "objects.githubusercontent.com"
        )
        repeat(6) { redirectCount ->
            if (currentUrl.protocol != "https" || currentUrl.host.lowercase(Locale.ROOT) !in allowedHosts) {
                throw IOException("O download tentou usar uma origem não autorizada.")
            }
            val connection = (currentUrl.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "NovaBox-Android")
            }
            try {
                val response = connection.responseCode
                if (response in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirecionamento sem destino.")
                    if (redirectCount == 5) throw IOException("O servidor redirecionou muitas vezes.")
                    currentUrl = URL(currentUrl, location)
                    return@repeat
                }
                if (response != HttpURLConnection.HTTP_OK) {
                    throw IOException("Servidor respondeu HTTP $response.")
                }
                val contentLength = connection.getHeaderFieldLong("Content-Length", -1L)
                if (contentLength >= 0L && contentLength != spec.expectedSizeBytes) {
                    throw IOException("O tamanho recebido não corresponde ao pacote publicado.")
                }
                // The signed-off release manifest is the source of truth when a CDN omits Content-Length.
                val length = spec.expectedSizeBytes

                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                var lastReported = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            downloaded = Math.addExact(downloaded, read.toLong())
                            if (downloaded > spec.expectedSizeBytes) {
                                throw IOException("O servidor enviou mais bytes que o esperado.")
                            }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            if (downloaded - lastReported >= 256 * 1024L || downloaded == length) {
                                lastReported = downloaded
                                withContext(Dispatchers.Main.immediate) {
                                    onProgress(RuntimeDownloadProgress(downloaded, length))
                                }
                            }
                        }
                        output.fd.sync()
                    }
                }
                if (downloaded != spec.expectedSizeBytes) {
                    throw IOException("Download incompleto: $downloaded de ${spec.expectedSizeBytes} bytes.")
                }
                val actualHash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                if (!actualHash.equals(spec.sha256, ignoreCase = true)) {
                    throw IOException("Falha na verificação SHA-256. O pacote foi descartado.")
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Não foi possível concluir o download.")
    }

    private fun validateSpec(spec: RuntimePackageSpec) {
        require(RuntimePackageCatalog.packageFor(spec.runtimeId) == spec) {
            "Este pacote não pertence ao catálogo verificado do NovaBox."
        }
        val source = URL(spec.downloadUrl)
        require(source.protocol == "https" && source.host.equals("github.com", ignoreCase = true)) {
            "A origem do pacote precisa ser o GitHub oficial por HTTPS."
        }
        require(source.userInfo == null && source.query == null && source.ref == null) {
            "A URL do pacote contém parâmetros inesperados."
        }
        require(spec.sha256.matches(Regex("^[a-fA-F0-9]{64}$"))) { "SHA-256 inválido." }
        require(spec.expectedSizeBytes in 1L..(128L * 1024L * 1024L)) { "Tamanho do pacote inválido." }
        require(spec.maxExpandedBytes in 1L..(512L * 1024L * 1024L)) { "Limite de extração inválido." }
        require(Regex("^[A-Za-z0-9._-]+[.]tar[.]gz$").matches(spec.fileName)) {
            "Nome de arquivo não permitido."
        }
        require(spec.archiveFormat == RuntimeArchiveFormat.TAR_GZ) { "Formato de arquivo não suportado." }
        require(Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,80}$").matches(spec.archiveRootDirectory)) {
            "Diretório raiz do pacote inválido."
        }
        require(isSafeRelativePath(spec.outputDirectory)) { "Destino de instalação inválido." }
        require(spec.allowedArchiveFiles.isNotEmpty() && spec.allowedArchiveFiles.all(::isSafeRelativePath)) {
            "Lista de arquivos permitidos inválida."
        }
        require(spec.requiredArchiveFiles.isNotEmpty() &&
            spec.requiredArchiveFiles.all { it in spec.allowedArchiveFiles }
        ) { "A lista de arquivos obrigatórios do pacote é inválida." }
    }

    private fun validateInstalledFiles(
        staging: File,
        definition: RuntimeComponentDefinition
    ) {
        definition.requiredFileGroups.forEach { group ->
            val hasOne = group.any { relative ->
                val file = File(staging, relative).canonicalFile
                file.toPath().startsWith(staging.canonicalFile.toPath()) &&
                    file.isFile && file.length() > 0L
            }
            if (!hasOne) throw IOException("Faltam arquivos obrigatórios do componente.")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
    private fun isSafeRelativePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith("/") && !path.contains('\\') &&
            !Regex("^[A-Za-z]:").containsMatchIn(path) &&
            path.split("/").none { it.isBlank() || it == "." || it == ".." }
}
