package com.mikael.novabox.runtime

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class XServerControllerTest {
    @Test fun usesTermuxX11PackageIdentifier() {
        assertEquals("com.termux.x11", XServerCommand.X11_PACKAGE)
        assertEquals("com.termux", XServerCommand.TERMUX_PACKAGE)
    }

    @Test fun usesDocumentedTermuxRunCommandContract() {
        assertEquals("com.termux.permission.RUN_COMMAND", XServerCommand.RUN_COMMAND_PERMISSION)
        assertEquals("com.termux.RUN_COMMAND", XServerCommand.RUN_COMMAND_ACTION)
        assertEquals("com.termux.app.RunCommandService", XServerCommand.RUN_COMMAND_SERVICE)
        assertEquals("\$PREFIX/bin/termux-x11", XServerCommand.COMMAND_PATH)
        assertEquals(":1", XServerCommand.DISPLAY)
        assertArrayEquals(arrayOf(":1"), XServerCommand.arguments())
    }

    @Test fun exposesAllLaunchOutcomes() {
        assertEquals(
            listOf(
                "OPENED", "NOT_INSTALLED", "NO_LAUNCH_ACTIVITY", "FAILED",
                "TERMUX_NOT_INSTALLED", "PERMISSION_REQUIRED", "SERVICE_START_FAILED",
                "START_REQUESTED", "DISPLAY_COMMAND_SENT"
            ),
            XServerLaunchResult.values().map { it.name }
        )
    }
}
