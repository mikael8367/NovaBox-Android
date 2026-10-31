package com.mikael.novabox.runtime

import android.content.Context
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.EngineType
import org.json.JSONObject
import java.io.File
import java.io.IOException

enum class WineWorkspaceState {
    NOT_PREPARED,
    WORKSPACE_PREPARED,
    INVALID
}

data class WineWorkspaceStatus(
    val state: WineWorkspaceState,
    val message: String,
    val directory: File? = null
)

class WineWorkspaceManager(context: Context) {
    private val root = File(context.applicationContext.filesDir, "wine-workspaces")

    fun prepare(config: ContainerConfig): WineWorkspaceStatus {
        if (config.validate() != null || config.profile?.engine != EngineType.WINE_COMPATIBILITY) {
            return WineWorkspaceStatus(
                WineWorkspaceState.INVALID,
                "Este perfil não é um ambiente Windows/Wine válido."
            )
        }
        if (!SAFE_ID.matches(config.id)) {
            return WineWorkspaceStatus(WineWorkspaceState.INVALID, "Identificador do ambiente inválido.")
        }

        return try {
            val canonicalRoot = root.canonicalFile
            if (!canonicalRoot.exists() && !canonicalRoot.mkdirs()) {
                throw IOException("Não foi possível criar a pasta privada dos ambientes.")
            }
            val workspace = File(canonicalRoot, config.id).canonicalFile
            if (!workspace.toPath().startsWith(canonicalRoot.toPath())) {
                throw IOException("O caminho do ambiente saiu da pasta privada.")
            }

            val requiredDirectories = listOf(
                "prefix",
                "prefix/dosdevices",
                "prefix/drive_c",
                "prefix/drive_c/Program Files",
                "prefix/drive_c/users",
                "prefix/drive_c/windows",
                "home",
                "tmp",
                "logs",
                "shared"
            )
            requiredDirectories.forEach { relative ->
                val directory = File(workspace, relative)
                if (!directory.exists() && !directory.mkdirs()) {
                    throw IOException("Não foi possível preparar a pasta $relative.")
                }
                if (!directory.isDirectory) throw IOException("O caminho $relative não é uma pasta.")
            }

            val metadata = JSONObject()
                .put("schemaVersion", 1)
                .put("environmentId", config.id)
                .put("environmentName", config.name)
                .put("profileId", config.osProfileId)
                .put("architecture", config.architecture.name)
                .put("state", "WORKSPACE_ONLY")
                .put("wineBootCompleted", false)
                .put("createdAt", System.currentTimeMillis())
            val metadataFile = File(workspace, "workspace.json")
            val temporary = File(workspace, ".workspace.json.tmp")
            temporary.writeText(metadata.toString(2), Charsets.UTF_8)
            if (metadataFile.exists() && !metadataFile.delete()) {
                throw IOException("Não foi possível atualizar os metadados do ambiente.")
            }
            if (!temporary.renameTo(metadataFile)) {
                temporary.delete()
                throw IOException("Não foi possível salvar os metadados do ambiente.")
            }

            WineWorkspaceStatus(
                WineWorkspaceState.WORKSPACE_PREPARED,
                "Estrutura privada preparada. Ainda não é um prefixo Wine: falta instalar o Wine e concluir o wineboot.",
                workspace
            )
        } catch (error: Exception) {
            WineWorkspaceStatus(
                WineWorkspaceState.INVALID,
                error.message ?: "Falha ao preparar o ambiente."
            )
        }
    }

    fun inspect(config: ContainerConfig): WineWorkspaceStatus {
        if (!SAFE_ID.matches(config.id)) {
            return WineWorkspaceStatus(WineWorkspaceState.INVALID, "Identificador do ambiente inválido.")
        }
        return try {
            val canonicalRoot = root.canonicalFile
            val workspace = File(canonicalRoot, config.id).canonicalFile
            if (!workspace.toPath().startsWith(canonicalRoot.toPath())) {
                return WineWorkspaceStatus(WineWorkspaceState.INVALID, "Caminho de ambiente inválido.")
            }
            val metadataFile = File(workspace, "workspace.json")
            if (!metadataFile.isFile) {
                return WineWorkspaceStatus(WineWorkspaceState.NOT_PREPARED, "Estrutura ainda não preparada.")
            }
            val metadata = JSONObject(metadataFile.readText(Charsets.UTF_8))
            if (metadata.optInt("schemaVersion") != 1 ||
                metadata.optString("environmentId") != config.id ||
                metadata.optString("state") != "WORKSPACE_ONLY" ||
                metadata.optBoolean("wineBootCompleted", true)
            ) {
                return WineWorkspaceStatus(WineWorkspaceState.INVALID, "Metadados do ambiente não reconhecidos.")
            }
            WineWorkspaceStatus(
                WineWorkspaceState.WORKSPACE_PREPARED,
                "Estrutura preparada; Wine ainda não inicializou o prefixo.",
                workspace
            )
        } catch (error: Exception) {
            WineWorkspaceStatus(
                WineWorkspaceState.INVALID,
                error.message ?: "Não foi possível verificar o ambiente."
            )
        }
    }

    companion object {
        private val SAFE_ID = Regex("^[A-Za-z0-9_-]{1,80}$")
    }
}
