package com.mikael.novabox.runtime

import java.io.IOException
import java.nio.file.Path

/** Path policy shared by runtime archive extraction and its security tests. */
internal object RuntimeArchivePathPolicy {
    fun safeEntryPath(raw: String): String? {
        var normalized = raw.replace('\\', '/').trimEnd('/')
        while (normalized.startsWith("./")) normalized = normalized.removePrefix("./")
        if (normalized.isBlank() || normalized.startsWith("/") ||
            Regex("^[A-Za-z]:").containsMatchIn(normalized)
        ) return null
        val parts = normalized.split('/')
        if (parts.any { it == ".." || it.isBlank() }) return null
        return parts.filter { it != "." }.joinToString("/")
    }

    fun resolveLinkTarget(root: Path, parent: Path, raw: String): Path {
        if (raw.isBlank() || raw.contains('\\')) throw IOException("Link simbólico inválido.")
        val resolved = if (raw.startsWith("/")) root.resolve(raw.removePrefix("/")).normalize()
            else parent.resolve(raw).normalize()
        if (!resolved.startsWith(root)) throw IOException("Link simbólico tenta sair da raiz.")
        return resolved
    }
}
