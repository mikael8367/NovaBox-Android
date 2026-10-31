package com.mikael.novabox.runtime

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TarGzipArchiveExtractorTest {
    @Test
    fun extractsOnlyWhitelistedFilesAndStripsReleaseFolder() {
        val root = createTempDir(prefix = "novabox-tar-test-")
        try {
            val archive = File(root, "package.tar.gz")
            writeTarGz(
                archive,
                listOf(
                    TarEntry("dxvk-3.1.1/", byteArrayOf(), '5'),
                    TarEntry("dxvk-3.1.1/x64/dxgi.dll", byteArrayOf(1, 2, 3)),
                    TarEntry("dxvk-3.1.1/x64/d3d11.dll", byteArrayOf(4, 5, 6)),
                    TarEntry("dxvk-3.1.1/LICENSE", "license text".toByteArray()),
                    TarEntry("dxvk-3.1.1/README.txt", "not installed".toByteArray())
                )
            )
            val destination = File(root, "staging")
            TarGzipArchiveExtractor.extract(
                archive = archive,
                destination = destination,
                rootDirectory = "dxvk-3.1.1",
                outputDirectory = "lib/dxvk",
                allowedFiles = setOf("x64/dxgi.dll", "x64/d3d11.dll", "LICENSE"),
                requiredFiles = setOf("x64/dxgi.dll", "x64/d3d11.dll"),
                maxExpandedBytes = 1024L
            )

            assertArrayEquals(
                byteArrayOf(1, 2, 3),
                File(destination, "lib/dxvk/x64/dxgi.dll").readBytes()
            )
            assertArrayEquals(
                byteArrayOf(4, 5, 6),
                File(destination, "lib/dxvk/x64/d3d11.dll").readBytes()
            )
            assertEquals("license text", File(destination, "lib/dxvk/LICENSE").readText())
            assertTrue(!File(destination, "lib/dxvk/README.txt").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsLinksInsteadOfCreatingThem() {
        val root = createTempDir(prefix = "novabox-tar-link-")
        try {
            val archive = File(root, "link.tar.gz")
            writeTarGz(
                archive,
                listOf(TarEntry("dxvk-3.1.1/x64/dxgi.dll", byteArrayOf(), '2'))
            )
            val error = runCatching {
                TarGzipArchiveExtractor.extract(
                    archive = archive,
                    destination = File(root, "staging"),
                    rootDirectory = "dxvk-3.1.1",
                    outputDirectory = "lib/dxvk",
                    allowedFiles = setOf("x64/dxgi.dll"),
                    maxExpandedBytes = 1024L
                )
            }.exceptionOrNull()
            assertNotNull(error)
            assertTrue(!File(root, "outside.dll").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun failsWhenARequiredFileIsMissing() {
        val root = createTempDir(prefix = "novabox-tar-missing-")
        try {
            val archive = File(root, "partial.tar.gz")
            writeTarGz(
                archive,
                listOf(TarEntry("dxvk-3.1.1/x64/dxgi.dll", byteArrayOf(1, 2, 3)))
            )
            val error = runCatching {
                TarGzipArchiveExtractor.extract(
                    archive = archive,
                    destination = File(root, "staging"),
                    rootDirectory = "dxvk-3.1.1",
                    outputDirectory = "lib/dxvk",
                    allowedFiles = setOf("x64/dxgi.dll", "x64/d3d11.dll"),
                    maxExpandedBytes = 1024L
                )
            }.exceptionOrNull()
            assertNotNull(error)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsParentDirectoryTraversal() {
        val root = createTempDir(prefix = "novabox-tar-slip-")
        try {
            val archive = File(root, "malicious.tar.gz")
            writeTarGz(
                archive,
                listOf(TarEntry("dxvk-3.1.1/../outside.dll", byteArrayOf(7)))
            )
            val destination = File(root, "staging")
            val error = runCatching {
                TarGzipArchiveExtractor.extract(
                    archive = archive,
                    destination = destination,
                    rootDirectory = "dxvk-3.1.1",
                    outputDirectory = "lib/dxvk",
                    allowedFiles = setOf("x64/dxgi.dll"),
                    maxExpandedBytes = 1024L
                )
            }.exceptionOrNull()

            assertNotNull(error)
            assertTrue(!File(root, "outside.dll").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private data class TarEntry(
        val name: String,
        val data: ByteArray,
        val type: Char = '0'
    )

    private fun writeTarGz(file: File, entries: List<TarEntry>) {
        val tar = ByteArrayOutputStream()
        entries.forEach { entry ->
            val header = ByteArray(512)
            writeString(header, 0, 100, entry.name)
            writeOctal(header, 100, 8, 420L)
            writeOctal(header, 108, 8, 0L)
            writeOctal(header, 116, 8, 0L)
            writeOctal(header, 124, 12, entry.data.size.toLong())
            writeOctal(header, 136, 12, 0L)
            for (index in 148 until 156) header[index] = 32
            header[156] = entry.type.code.toByte()
            writeString(header, 257, 6, "ustar\u0000")
            writeString(header, 263, 2, "00")
            val checksum = header.sumOf { it.toInt() and 0xff }
            val checksumText = String.format(java.util.Locale.ROOT, "%06o", checksum)
            writeString(header, 148, 6, checksumText)
            header[154] = 0
            header[155] = 32
            tar.write(header)
            tar.write(entry.data)
            val padding = (512 - (entry.data.size % 512)) % 512
            repeat(padding) { tar.write(0) }
        }
        tar.write(ByteArray(1024))
        GZIPOutputStream(FileOutputStream(file)).use { it.write(tar.toByteArray()) }
    }

    private fun writeString(buffer: ByteArray, offset: Int, length: Int, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= length)
        System.arraycopy(bytes, 0, buffer, offset, bytes.size)
    }

    private fun writeOctal(buffer: ByteArray, offset: Int, length: Int, value: Long) {
        val formatted = String.format(java.util.Locale.ROOT, "%0${length - 1}o", value)
        writeString(buffer, offset, length - 1, formatted)
        buffer[offset + length - 1] = 0
    }
}
