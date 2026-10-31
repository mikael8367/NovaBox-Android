package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class XServerControllerTest {
    @Test fun usesTermuxX11PackageIdentifier() {
        assertEquals("com.termux.x11", XServerController.PACKAGE_NAME)
    }

    @Test fun exposesAllLaunchOutcomes() {
        assertEquals(
            listOf("OPENED", "NOT_INSTALLED", "NO_LAUNCH_ACTIVITY", "FAILED"),
            XServerLaunchResult.values().map { it.name }
        )
    }
}
