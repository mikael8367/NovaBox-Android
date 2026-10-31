package com.mikael.novabox.runtime

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElfAbiValidatorTest {
    @Test
    fun acceptsArm64ElfOnArm64Runtime() {
        assertTrue(ElfAbiValidator.isCompatible(binary(elfClass = 2, machine = 183), setOf("arm64-v8a")))
    }

    @Test
    fun rejectsX64ElfForArm64Runtime() {
        assertFalse(ElfAbiValidator.isCompatible(binary(elfClass = 2, machine = 62), setOf("arm64-v8a")))
    }

    @Test
    fun acceptsArm32ElfOnlyWhenArm32AbiIsAllowed() {
        val arm32 = binary(elfClass = 1, machine = 40)
        assertTrue(ElfAbiValidator.isCompatible(arm32, setOf("armeabi-v7a")))
        assertFalse(ElfAbiValidator.isCompatible(arm32, setOf("arm64-v8a")))
    }

    @Test
    fun rejectsNonElfAndBigEndianFiles() {
        assertFalse(ElfAbiValidator.isCompatible(fileWith(ByteArray(32)), setOf("arm64-v8a")))
        val bigEndian = binary(elfClass = 2, machine = 183).apply { write(2, 2) }
        assertFalse(ElfAbiValidator.isCompatible(bigEndian, setOf("arm64-v8a")))
    }

    private fun binary(elfClass: Int, machine: Int): File {
        val header = ByteArray(32)
        header[0] = 0x7f
        header[1] = 'E'.code.toByte()
        header[2] = 'L'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = elfClass.toByte()
        header[5] = 1
        header[18] = (machine and 0xff).toByte()
        header[19] = ((machine shr 8) and 0xff).toByte()
        return fileWith(header)
    }

    private fun fileWith(bytes: ByteArray): File =
        File.createTempFile("novabox-elf-test", ".bin").apply {
            writeBytes(bytes)
            deleteOnExit()
        }
}
