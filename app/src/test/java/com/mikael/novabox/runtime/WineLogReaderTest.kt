package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class WineLogReaderTest {
    @Test fun readsWineLogTailFromExpectedWorkspace() {
        val root = Files.createTempDirectory("novabox-logs").toFile()
        try {
            val log = File(root, "env-one/logs/wine-session.log")
            assertTrue(log.parentFile.mkdirs())
            log.writeText("wine boot completed")
            assertEquals("wine boot completed", WineLogReader.readTail(root, "env-one"))
        } finally { root.deleteRecursively() }
    }

    @Test fun limitsLogReadToLastThirtyTwoKiB() {
        val root = Files.createTempDirectory("novabox-log-limit").toFile()
        try {
            val log = File(root, "env-two/logs/wine-session.log")
            assertTrue(log.parentFile.mkdirs())
            log.writeText("x".repeat(WineLogReader.MAX_BYTES + 200))
            val content = WineLogReader.readTail(root, "env-two")
            assertTrue(content.startsWith("[Mostrando os últimos 32 KiB do log]"))
            assertTrue(content.endsWith("x".repeat(WineLogReader.MAX_BYTES)))
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsTraversalAndMissingLogs() {
        val root = Files.createTempDirectory("novabox-log-policy").toFile()
        try {
            assertThrows(IOException::class.java) { WineLogReader.readTail(root, "../outside") }
            assertThrows(IOException::class.java) { WineLogReader.readTail(root, "missing") }
        } finally { root.deleteRecursively() }
    }
}
