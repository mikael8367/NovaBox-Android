package com.mikael.novabox.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

data class ProgramEntry(
    val id: String,
    val containerId: String,
    val displayName: String,
    val storedFileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val importedAt: Long
)

object ProgramFilePolicy {
    const val MAX_PROGRAM_BYTES = 512L * 1024L * 1024L
    private val allowedExtensions = setOf("exe", "msi", "com")

    fun sanitizeDisplayName(value: String): String {
        val baseName = value.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._() -]"), "_")
            .trim()
            .trim('.')
            .take(96)
        return baseName.ifBlank { "programa.exe" }
    }

    fun isSupportedProgram(value: String): Boolean =
        sanitizeDisplayName(value).substringAfterLast('.', "").lowercase() in allowedExtensions

    fun isSafeId(value: String): Boolean =
        value.matches(Regex("^[A-Za-z0-9_-]{1,80}$"))

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

class ProgramRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("novabox_program_store", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "programs")

    fun load(): List<ProgramEntry> {
        val raw = preferences.getString(KEY_PROGRAMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val entry = runCatching {
                        ProgramEntry(
                            id = item.getString("id"),
                            containerId = item.getString("containerId"),
                            displayName = item.getString("displayName"),
                            storedFileName = item.getString("storedFileName"),
                            sizeBytes = item.getLong("sizeBytes"),
                            sha256 = item.getString("sha256"),
                            importedAt = item.getLong("importedAt")
                        )
                    }.getOrNull() ?: continue
                    val file = fileFor(entry)
                    if (file?.isFile == true && file.length() == entry.sizeBytes &&
                        entry.sha256.matches(Regex("^[a-fA-F0-9]{64}$")) &&
                        runCatching { ProgramFilePolicy.sha256(file) == entry.sha256.lowercase() }.getOrDefault(false)
                    ) add(entry)
                }
            }
        }.getOrDefault(emptyList())
    }

    @Throws(IOException::class)
    fun importProgram(containerId: String, uri: Uri): ProgramEntry {
        if (!ProgramFilePolicy.isSafeId(containerId)) throw IOException("Ambiente inválido.")
        val resolver = context.contentResolver
        var displayName = "programa.exe"
        var declaredSize = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameColumn >= 0) displayName = cursor.getString(nameColumn) ?: displayName
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) declaredSize = cursor.getLong(sizeColumn)
                }
            }
        val safeName = ProgramFilePolicy.sanitizeDisplayName(displayName)
        if (!ProgramFilePolicy.isSupportedProgram(safeName)) {
            throw IOException("Formato não suportado. Selecione um arquivo EXE, MSI ou COM.")
        }
        if (declaredSize > ProgramFilePolicy.MAX_PROGRAM_BYTES) {
            throw IOException("O arquivo ultrapassa o limite de 512 MiB.")
        }
        if (context.filesDir.usableSpace < (declaredSize.coerceAtLeast(0L) + 8L * 1024L * 1024L)) {
            throw IOException("Espaço insuficiente no armazenamento do aplicativo.")
        }

        val targetDirectory = File(root, containerId).canonicalFile
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw IOException("Não foi possível criar a biblioteca privada.")
        }
        if (!targetDirectory.toPath().startsWith(root.canonicalFile.toPath())) {
            throw IOException("Destino de importação inválido.")
        }

        val id = UUID.randomUUID().toString()
        val destination = File(targetDirectory, "$id.program")
        val temporary = File(targetDirectory, ".$id.part")
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        try {
            val input = resolver.openInputStream(uri) ?: throw IOException("Não foi possível abrir o arquivo selecionado.")
            input.use { source ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        copied = Math.addExact(copied, count.toLong())
                        if (copied > ProgramFilePolicy.MAX_PROGRAM_BYTES) {
                            throw IOException("O arquivo ultrapassa o limite de 512 MiB.")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            if (declaredSize >= 0L && copied != declaredSize) {
                throw IOException("O tamanho do arquivo mudou durante a importação.")
            }
            if (copied == 0L) throw IOException("O arquivo está vazio.")
            if (!temporary.renameTo(destination)) throw IOException("Não foi possível concluir a cópia do programa.")

            val entry = ProgramEntry(
                id = id,
                containerId = containerId,
                displayName = safeName,
                storedFileName = destination.name,
                sizeBytes = copied,
                sha256 = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) },
                importedAt = System.currentTimeMillis()
            )
            save(load() + entry)
            return entry
        } catch (error: Exception) {
            temporary.delete()
            destination.delete()
            if (error is IOException) throw error
            throw IOException(error.message ?: "Falha ao importar o programa.", error)
        }
    }

    fun delete(entry: ProgramEntry): Boolean {
        val file = fileFor(entry) ?: return false
        val removed = !file.exists() || file.delete()
        if (removed) save(load().filterNot { it.id == entry.id })
        return removed
    }

    fun fileFor(entry: ProgramEntry): File? {
        if (!ProgramFilePolicy.isSafeId(entry.containerId)) return null
        if (!entry.storedFileName.matches(Regex("^[A-Za-z0-9-]+[.]program$"))) return null
        val directory = File(root, entry.containerId).canonicalFile
        val file = File(directory, entry.storedFileName).canonicalFile
        return file.takeIf {
            directory.toPath().startsWith(root.canonicalFile.toPath()) &&
                it.toPath().startsWith(directory.toPath())
        }
    }

    private fun save(entries: List<ProgramEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("containerId", entry.containerId)
                    .put("displayName", entry.displayName)
                    .put("storedFileName", entry.storedFileName)
                    .put("sizeBytes", entry.sizeBytes)
                    .put("sha256", entry.sha256)
                    .put("importedAt", entry.importedAt)
            )
        }
        preferences.edit().putString(KEY_PROGRAMS, array.toString()).apply()
    }

    companion object {
        private const val KEY_PROGRAMS = "programs_json"
    }
}
