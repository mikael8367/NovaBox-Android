package com.mikael.novabox.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WinlatorRuntimeValidatorTest {
    @Test
    fun acceptsExpectedArm64HostAndX64GuestBinaries() {
        val root = createRootfs()
        writeElf(root, "usr/local/bin/box64", 2, 183)
        writeElf(root, "opt/wine/bin/wine", 2, 62)
        writeElf(root, "opt/wine/bin/wineboot", 2, 62)
        assertTrue(WinlatorRuntimeValidator.isValid(root))
        root.deleteRecursively()
    }

    @Test
    fun rejectsGuestBinaryWithWrongArchitecture() {
        val root = createRootfs()
        writeElf(root, "usr/local/bin/box64", 2, 183)
        writeElf(root, "opt/wine/bin/wine", 2, 183)
        writeElf(root, "opt/wine/bin/wineboot", 2, 62)
        val errors = WinlatorRuntimeValidator.validate(root)
        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("opt/wine/bin/wine"))
        root.deleteRecursively()
    }

    @Test
    fun rejectsMissingRequiredBinaries() {
        val root = createRootfs()
        assertFalse(WinlatorRuntimeValidator.isValid(root))
        assertEquals(3, WinlatorRuntimeValidator.validate(root).size)
        root.deleteRecursively()
    }

    private fun createRootfs(): File =
        File.createTempFile("novabox-rootfs", "test").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }

    private fun writeElf(root: File, path: String, elfClass: Int, machine: Int) {
        val file = File(root, path)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(32).apply {
            this[0] = 0x7f
            this[1] = 'E'.code.toByte()
            this[2] = 'L'.code.toByte()
            this[3] = 'F'.code.toByte()
            this[4] = elfClass.toByte()
            this[5] = 1
            this[18] = (machine and 0xff).toByte()
            this[19] = ((machine shr 8) and 0xff).toByte()
        })
    }
}
