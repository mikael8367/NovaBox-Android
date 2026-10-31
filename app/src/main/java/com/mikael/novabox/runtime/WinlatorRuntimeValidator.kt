package com.mikael.novabox.runtime

import java.io.File

/** Checks the bundle's native ARM64 host and x86-64 guest executable split. */
internal object WinlatorRuntimeValidator {
    private val requiredBinaries = listOf(
        "usr/local/bin/box64" to setOf("arm64-v8a"),
        "opt/wine/bin/wine" to setOf("x86_64"),
        "opt/wine/bin/wineboot" to setOf("x86_64")
    )

    fun validate(rootfs: File): List<String> {
        val root = rootfs.canonicalFile
        return requiredBinaries.mapNotNull { (relativePath, abi) ->
            val file = File(root, relativePath).canonicalFile
            when {
                !file.toPath().startsWith(root.toPath()) ->
                    "$relativePath aponta para fora do runtime."
                !file.isFile || file.length() == 0L ->
                    "$relativePath está ausente ou vazio."
                !ElfAbiValidator.isCompatible(file, abi) ->
                    "$relativePath possui arquitetura ELF incompatível; esperado " + abi.joinToString() + "."
                else -> null
            }
        }
    }

    fun isValid(rootfs: File): Boolean = validate(rootfs).isEmpty()
}
