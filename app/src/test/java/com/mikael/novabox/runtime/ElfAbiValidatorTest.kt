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
        val bigEndianHeader = binaryHeader(elfClass = 2, machine = 183).apply { this[5] = 2 }
        assertFalse(ElfAbiValidator.isCompatible(fileWith(bigEndianHeader), setOf("arm64-v8a")))
    }

    private fun binary(elfClass: Int, machine: Int): File =
        fileWith(binaryHeader(elfClass, machine))

    private fun binaryHeader(elfClass: Int, machine: Int): ByteArray =
        ByteArray(32).apply {
            this[0] = 0x7f
            this[1] = 'E'.code.toByte()
            this[2] = 'L'.code.toByte()
            this[3] = 'F'.code.toByte()
            this[4] = elfClass.toByte()
            this[5] = 1
            this[18] = (machine and 0xff).toByte()
            this[19] = ((machine shr 8) and 0xff).toByte()
        }

    private fun fileWith(bytes: ByteArray): File =
        File.createTempFile("novabox-elf-test", ".bin").apply {
            writeBytes(bytes)
            deleteOnExit()
        }
}
