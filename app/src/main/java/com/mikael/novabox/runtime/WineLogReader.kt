package com.mikael.novabox.runtime

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/** Reads only the tail of an app-private Wine log to keep diagnostics bounded. */
object WineLogReader {
    const val MAX_BYTES = 32 * 1024

    @Throws(IOException::class)
    fun readTail(workspacesRoot: File, environmentId: String): String {
        if (!environmentId.matches(Regex("^[A-Za-z0-9_-]{1,80}$"))) {
            throw IOException("Identificador do ambiente inválido.")
        }
        val root = workspacesRoot.canonicalFile
        val workspace = File(root, environmentId).canonicalFile
        if (!workspace.toPath().startsWith(root.toPath()) || workspace == root) {
            throw IOException("Pasta do ambiente inválida.")
        }
        val log = File(workspace, "logs/wine-session.log").canonicalFile
        if (!log.toPath().startsWith(workspace.toPath()) || !log.isFile) {
            throw IOException("Ainda não existe log do Wine para este ambiente.")
        }
        RandomAccessFile(log, "r").use { file ->
            val length = file.length()
            if (length == 0L) return "O log está vazio."
            val start = (length - MAX_BYTES).coerceAtLeast(0L)
            file.seek(start)
            val bytes = ByteArray((length - start).toInt())
            file.readFully(bytes)
            val content = String(bytes, Charsets.UTF_8)
            return if (start > 0L) "[Mostrando os últimos 32 KiB do log]\n$content" else content
        }
    }
}
