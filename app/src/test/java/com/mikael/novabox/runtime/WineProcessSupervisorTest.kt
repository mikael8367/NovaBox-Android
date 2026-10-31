package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WineProcessSupervisorTest {
    private fun sleepingProcess(): Process = ProcessBuilder("sh", "-c", "sleep 20").start()

    @Test fun tracksAndStopsAnEnvironmentProcess() {
        val process = WineProcessSupervisor.start("session-test") { sleepingProcess() }
        try {
            assertTrue(WineProcessSupervisor.isRunning("session-test"))
            assertTrue("session-test" in WineProcessSupervisor.activeEnvironmentIds())
            assertTrue(WineProcessSupervisor.stop("session-test", 100))
            assertFalse(process.isAlive)
            assertFalse(WineProcessSupervisor.isRunning("session-test"))
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    @Test fun rejectsASecondProcessForTheSameEnvironment() {
        val first = WineProcessSupervisor.start("single-session") { sleepingProcess() }
        try {
            assertThrows(IOException::class.java) {
                WineProcessSupervisor.start("single-session") { sleepingProcess() }
            }
            assertTrue(first.isAlive)
        } finally {
            WineProcessSupervisor.stop("single-session", 100)
        }
    }

    @Test fun rejectsUnsafeEnvironmentIdsBeforeStartingAProcess() {
        assertThrows(IllegalArgumentException::class.java) {
            WineProcessSupervisor.start("../outside") { sleepingProcess() }
        }
        assertEquals(emptySet<String>(), WineProcessSupervisor.activeEnvironmentIds())
    }

    @Test fun stoppingUnknownEnvironmentReturnsFalse() {
        assertFalse(WineProcessSupervisor.stop("not-running"))
    }
}