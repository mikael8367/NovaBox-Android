package com.mikael.novabox.runtime

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class AlpineRootfsExtractorTest {
    @Test fun extractsSafeFilesInsideRoot() {
        val temp = createTempDir(prefix = "novabox-alpine-test-")
        try {
            val archive = File(temp, "safe.tar.gz")
            writeArchive(archive) { tar ->
                val data = "alpine".toByteArray()
                val entry = TarArchiveEntry("./etc/test-release")
                entry.size = data.size.toLong()
                tar.putArchiveEntry(entry)
                tar.write(data)
                tar.closeArchiveEntry()
            }
            val destination = File(temp, "rootfs").apply { mkdirs() }
            SafeRootfsTarExtractor.extract(archive, destination, 1024L) {}
            assertEquals("alpine", File(destination, "etc/test-release").readText())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test fun rejectsTarPathTraversal() {
        val temp = createTempDir(prefix = "novabox-alpine-traversal-")
        try {
            val archive = File(temp, "bad.tar.gz")
            writeArchive(archive) { tar ->
                val data = byteArrayOf(1)
                val entry = TarArchiveEntry("../outside")
                entry.size = data.size.toLong()
                tar.putArchiveEntry(entry)
                tar.write(data)
                tar.closeArchiveEntry()
            }
            val destination = File(temp, "rootfs").apply { mkdirs() }
            assertThrows(IOException::class.java) { SafeRootfsTarExtractor.extract(archive, destination, 1024L) {} }
            assertFalse(File(temp, "outside").exists())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test fun rejectsSymlinkEscapingRootfs() {
        val temp = createTempDir(prefix = "novabox-alpine-symlink-")
        try {
            val archive = File(temp, "bad-link.tar.gz")
            writeArchive(archive) { tar ->
                val entry = TarArchiveEntry("escape", TarConstants.LF_SYMLINK)
                entry.linkName = "../../outside"
                tar.putArchiveEntry(entry)
                tar.closeArchiveEntry()
            }
            val destination = File(temp, "rootfs").apply { mkdirs() }
            assertThrows(IOException::class.java) { SafeRootfsTarExtractor.extract(archive, destination, 1024L) {} }
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun writeArchive(file: File, content: (TarArchiveOutputStream) -> Unit) {
        GZIPOutputStream(FileOutputStream(file)).use { gzip ->
            TarArchiveOutputStream(gzip).use { tar ->
                content(tar)
                tar.finish()
            }
        }
    }
}