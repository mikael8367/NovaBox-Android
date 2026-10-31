package com.mikael.novabox.runtime

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

internal object TarGzipArchiveExtractor {
    private const val BLOCK_SIZE = 512
    private const val MAX_ENTRIES = 10_000

    fun extract(
        archive: File,
        destination: File,
        rootDirectory: String,
        outputDirectory: String,
        allowedFiles: Set<String>,
        maxExpandedBytes: Long
    ) {
        require(rootDirectory.isNotBlank() && !rootDirectory.contains('/')) {
            "Diretório raiz do pacote inválido."
        }
        require(isSafeRelativePath(outputDirectory)) { "Destino interno inválido." }
        require(allowedFiles.isNotEmpty() && allowedFiles.all(::isSafeRelativePath)) {
            "Lista de arquivos permitidos inválida."
        }

        if (!destination.exists() && !destination.mkdirs()) {
            throw IOException("Não foi possível preparar a pasta temporária.")
        }
        val root = destination.canonicalFile
        val outputRoot = File(root, outputDirectory).canonicalFile
        if (!outputRoot.toPath().startsWith(root.toPath())) {
            throw IOException("Destino de extração fora da pasta temporária.")
        }
        if (!outputRoot.exists() && !outputRoot.mkdirs()) {
            throw IOException("Não foi possível criar a pasta do componente.")
        }

        val written = mutableSetOf<String>()
        var totalExpanded = 0L
        var entryCount = 0

        GZIPInputStream(BufferedInputStream(FileInputStream(archive))).use { input ->
            while (true) {
                val header = ByteArray(BLOCK_SIZE)
                val headerRead = readHeader(input, header)
                if (headerRead == 0) break
                if (headerRead != BLOCK_SIZE) throw IOException("Cabeçalho TAR incompleto.")
                if (header.all { it == 0.toByte() }) break

                entryCount += 1
                if (entryCount > MAX_ENTRIES) throw IOException("O pacote contém entradas demais.")
                verifyHeaderChecksum(header)

                val name = readTarString(header, 0, 100)
                val prefix = readTarString(header, 345, 155)
                val fullName = if (prefix.isBlank()) name else "$prefix/$name"
                validateArchivePath(fullName)
                val relativeToRoot = stripRoot(fullName, rootDirectory)
                val size = parseOctal(header, 124, 12)
                if (size < 0L) throw IOException("Tamanho de arquivo inválido.")
                totalExpanded = Math.addExact(totalExpanded, size)
                if (totalExpanded > maxExpandedBytes) {
                    throw IOException("O pacote excede o limite de extração.")
                }

                when (header[156].toInt().toChar()) {
                    '5' -> skipFully(input, size)
                    '0', '\u0000' -> {
                        if (relativeToRoot != null && relativeToRoot in allowedFiles) {
                            if (!written.add(relativeToRoot)) {
                                throw IOException("O arquivo $relativeToRoot aparece mais de uma vez.")
                            }
                            val destinationFile = File(outputRoot, relativeToRoot).canonicalFile
                            if (!destinationFile.toPath().startsWith(outputRoot.toPath())) {
                                throw IOException("O pacote tentou sair da pasta de instalação.")
                            }
                            destinationFile.parentFile?.let {
                                if (!it.exists() && !it.mkdirs()) {
                                    throw IOException("Não foi possível criar uma pasta do pacote.")
                                }
                            }
                            FileOutputStream(destinationFile).use { output ->
                                copyExact(input, output, size)
                                output.fd.sync()
                            }
                        } else {
                            skipFully(input, size)
                        }
                    }
                    else -> throw IOException("O pacote contém link ou tipo de arquivo não permitido.")
                }

                val padding = (BLOCK_SIZE - (size % BLOCK_SIZE)) % BLOCK_SIZE
                skipFully(input, padding)
            }
        }

        val missing = allowedFiles - written
        if (missing.isNotEmpty()) {
            throw IOException("O pacote está incompleto. Arquivos ausentes: ${missing.joinToString()}")
        }
    }

    private fun readHeader(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val count = input.read(buffer, total, buffer.size - total)
            if (count < 0) return if (total == 0) 0 else total
            total += count
        }
        return total
    }

    private fun verifyHeaderChecksum(header: ByteArray) {
        val expected = parseOctal(header, 148, 8)
        var actual = 0L
        header.indices.forEach { index ->
            actual += if (index in 148..155) 32 else (header[index].toInt() and 0xff)
        }
        if (actual != expected) throw IOException("Checksum do cabeçalho TAR inválido.")
    }

    private fun readTarString(header: ByteArray, offset: Int, length: Int): String {
        var end = offset
        val limit = offset + length
        while (end < limit && header[end] != 0.toByte()) end += 1
        return String(header, offset, end - offset, StandardCharsets.UTF_8).trim()
    }

    private fun parseOctal(header: ByteArray, offset: Int, length: Int): Long {
        var result = 0L
        var foundDigit = false
        for (index in offset until offset + length) {
            val value = header[index].toInt() and 0xff
            if (value == 0 || value == 32) {
                if (foundDigit) break
                continue
            }
            if (value !in 48..55) throw IOException("Campo numérico TAR inválido.")
            foundDigit = true
            result = Math.addExact(Math.multiplyExact(result, 8L), (value - 48).toLong())
        }
        return if (foundDigit) result else 0L
    }

    private fun validateArchivePath(path: String) {
        if (path.isBlank() || path.startsWith('/') || path.contains('\\') ||
            Regex("^[A-Za-z]:").containsMatchIn(path)
        ) {
            throw IOException("O pacote contém um caminho absoluto ou inválido.")
        }
        val parts = path.split('/')
        if (parts.any { it == "." || it == ".." || it.isEmpty() }) {
            throw IOException("O pacote contém tentativa de sair da pasta de instalação.")
        }
    }

    private fun stripRoot(path: String, rootDirectory: String): String? {
        val parts = path.split('/')
        if (parts.firstOrNull() != rootDirectory) return null
        if (parts.size < 2) return null
        return parts.drop(1).joinToString("/")
    }

    private fun isSafeRelativePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') &&
            !Regex("^[A-Za-z]:").containsMatchIn(path) &&
            path.split('/').none { it.isBlank() || it == "." || it == ".." }

    private fun copyExact(input: InputStream, output: FileOutputStream, count: Long) {
        var remaining = count
        val buffer = ByteArray(32 * 1024)
        while (remaining > 0L) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw IOException("Arquivo TAR truncado.")
            output.write(buffer, 0, read)
            remaining -= read.toLong()
        }
    }

    private fun skipFully(input: InputStream, count: Long) {
        var remaining = count
        val buffer = ByteArray(16 * 1024)
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read < 0) throw IOException("Arquivo TAR truncado.")
                remaining -= read.toLong()
            }
        }
    }
}
