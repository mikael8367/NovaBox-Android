package com.mikael.novabox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramFilePolicyTest {
    @Test
    fun sanitizesUnixAndWindowsPathsToOnlyTheDisplayName() {
        assertEquals("game.exe", ProgramFilePolicy.sanitizeDisplayName("../../folder/game.exe"))
        assertEquals("setup.msi", ProgramFilePolicy.sanitizeDisplayName("C:\\Users\\Downloads\\setup.msi"))
    }

    @Test
    fun acceptsOnlySupportedWindowsInstallerExtensions() {
        assertTrue(ProgramFilePolicy.isSupportedProgram("game.exe"))
        assertTrue(ProgramFilePolicy.isSupportedProgram("setup.msi"))
        assertTrue(ProgramFilePolicy.isSupportedProgram("legacy.com"))
        assertFalse(ProgramFilePolicy.isSupportedProgram("script.sh"))
        assertFalse(ProgramFilePolicy.isSupportedProgram("archive.zip"))
    }

    @Test
    fun rejectsPathLikeOrEmptyEnvironmentIds() {
        assertTrue(ProgramFilePolicy.isSafeId("abc-123_45"))
        assertFalse(ProgramFilePolicy.isSafeId("../abc"))
        assertFalse(ProgramFilePolicy.isSafeId(""))
        assertFalse(ProgramFilePolicy.isSafeId("id with spaces"))
    }

    @Test
    fun enforces512MibImportLimit() {
        assertEquals(512L * 1024L * 1024L, ProgramFilePolicy.MAX_PROGRAM_BYTES)
    }
}
