package com.mikael.novabox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File
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
    fun computesSha256AndDetectsChangedProgramContents() {
        val file = File.createTempFile("novabox-program-hash", ".bin")
        try {
            file.writeText("original")
            val originalHash = ProgramFilePolicy.sha256(file)
            assertEquals(
                "0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5",
                originalHash
            )
            file.writeText("modified")
            assertFalse(originalHash == ProgramFilePolicy.sha256(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun enforces512MibImportLimit() {
        assertEquals(512L * 1024L * 1024L, ProgramFilePolicy.MAX_PROGRAM_BYTES)
    }
}
