package com.mikael.novabox.runtime

import java.io.File
import java.io.RandomAccessFile

/**
 * Validates native runtime executables before the catalog can label them installed.
 * This checks the ELF container/CPU architecture only; it does not prove that
 * Android's linker can load every dependency or that SELinux permits execution.
 */
internal object ElfAbiValidator {
    private const val ELF_HEADER_MIN_SIZE = 20
    private const val ELFCLASS32 = 1
    private const val ELFCLASS64 = 2
    private const val ELFDATA2LSB = 1
    private const val EM_ARM = 40
    private const val EM_AARCH64 = 183
    private const val EM_386 = 3
    private const val EM_X86_64 = 62

    fun isCompatible(file: File, requiredAbi: Set<String>): Boolean {
        if (requiredAbi.isEmpty() || !file.isFile || file.length() < ELF_HEADER_MIN_SIZE) return false
        return runCatching {
            RandomAccessFile(file, "r").use { input ->
                val header = ByteArray(ELF_HEADER_MIN_SIZE)
                input.readFully(header)
                if (header[0] != 0x7f.toByte() || header[1] != 'E'.code.toByte() ||
                    header[2] != 'L'.code.toByte() || header[3] != 'F'.code.toByte()
                ) return@use false

                val elfClass = header[4].toInt() and 0xff
                val dataEncoding = header[5].toInt() and 0xff
                if (dataEncoding != ELFDATA2LSB) return@use false

                val machine = (header[18].toInt() and 0xff) or
                    ((header[19].toInt() and 0xff) shl 8)
                val detectedAbi = when (elfClass to machine) {
                    ELFCLASS64 to EM_AARCH64 -> "arm64-v8a"
                    ELFCLASS32 to EM_ARM -> "armeabi-v7a"
                    ELFCLASS32 to EM_386 -> "x86"
                    ELFCLASS64 to EM_X86_64 -> "x86_64"
                    else -> null
                }
                detectedAbi != null && detectedAbi in requiredAbi
            }
        }.getOrDefault(false)
    }
}
