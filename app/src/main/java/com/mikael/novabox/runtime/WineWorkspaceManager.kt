package com.mikael.novabox.runtime

import android.content.Context
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.EngineType
import org.json.JSONObject
import java.io.File
import java.io.IOException

object WineWorkspacePolicy {
    fun isSafeEnvironmentId(value: String): Boolean =
        value.matches(Regex("^[A-Za-z0-9_-]{1,80}$"))

    fun isWindowsWineProfile(config: ContainerConfig): Boolean =
        config.validate() == null && config.profile?.engine == EngineType.WINE_COMPATIBILITY
}

enum class WineWorkspaceState {
    NOT_PREPARED,
    WORKSPACE_PREPARED,
    WINE_INITIALIZED,
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
        if (!WineWorkspacePolicy.isWindowsWineProfile(config)) {
            return WineWorkspaceStatus(
                WineWorkspaceState.INVALID,
                "Este perfil não é um ambiente Windows/Wine válido."
            )
        }
        if (!WineWorkspacePolicy.isSafeEnvironmentId(config.id)) {
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

            val metadataFile = File(workspace, "workspace.json")
            val existing = if (metadataFile.isFile) runCatching {
                JSONObject(metadataFile.readText(Charsets.UTF_8))
            }.getOrNull() else null
            if (existing != null &&
                existing.optString("environmentId") == config.id &&
                existing.optString("state") == "WINE_INITIALIZED" &&
                existing.optBoolean("wineBootCompleted", false)
            ) {
                existing.put("environmentName", config.name)
                    .put("profileId", config.osProfileId)
                    .put("architecture", config.architecture.name)
                writeMetadata(workspace, existing)
                return WineWorkspaceStatus(
                    WineWorkspaceState.WINE_INITIALIZED,
                    "Prefixo Wine inicializado e validado.",
                    workspace
                )
            }

            val metadata = JSONObject()
                .put("schemaVersion", 1)
                .put("environmentId", config.id)
                .put("environmentName", config.name)
                .put("profileId", config.osProfileId)
                .put("architecture", config.architecture.name)
                .put("state", "WORKSPACE_ONLY")
                .put("wineBootCompleted", false)
                .put("createdAt", existing?.optLong("createdAt") ?: System.currentTimeMillis())
            writeMetadata(workspace, metadata)

            WineWorkspaceStatus(
                WineWorkspaceState.WORKSPACE_PREPARED,
                "Estrutura privada preparada. Execute wineboot para inicializar o prefixo.",
                workspace
            )
        } catch (error: Exception) {
            WineWorkspaceStatus(
                WineWorkspaceState.INVALID,
                error.message ?: "Falha ao preparar o ambiente."
            )
        }
    }

    fun markWineBootCompleted(configId: String): Boolean {
        if (!WineWorkspacePolicy.isSafeEnvironmentId(configId)) return false
        return runCatching {
            val canonicalRoot = root.canonicalFile
            val workspace = File(canonicalRoot, configId).canonicalFile
            if (!workspace.toPath().startsWith(canonicalRoot.toPath())) return false
            val metadataFile = File(workspace, "workspace.json")
            if (!metadataFile.isFile) return false
            val metadata = JSONObject(metadataFile.readText(Charsets.UTF_8))
            if (metadata.optInt("schemaVersion") != 1 ||
                metadata.optString("environmentId") != configId ||
                metadata.optString("state") != "WORKSPACE_ONLY" ||
                !File(workspace, "prefix/system.reg").isFile
            ) return false
            metadata.put("state", "WINE_INITIALIZED")
                .put("wineBootCompleted", true)
                .put("initializedAt", System.currentTimeMillis())
            writeMetadata(workspace, metadata)
            true
        }.getOrDefault(false)
    }

    private fun writeMetadata(workspace: File, metadata: JSONObject) {
        val temporary = File(workspace, ".workspace.json.tmp")
        val destination = File(workspace, "workspace.json")
        temporary.writeText(metadata.toString(2), Charsets.UTF_8)
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            throw IOException("Não foi possível salvar os metadados do ambiente.")
        }
    }

    fun delete(configId: String): Boolean {
        if (!WineWorkspacePolicy.isSafeEnvironmentId(configId)) return false
        return runCatching {
            val canonicalRoot = root.canonicalFile
            val workspace = File(canonicalRoot, configId).canonicalFile
            workspace.toPath().startsWith(canonicalRoot.toPath()) &&
                (!workspace.exists() || workspace.deleteRecursively())
        }.getOrDefault(false)
    }

    fun inspect(config: ContainerConfig): WineWorkspaceStatus {
        if (!WineWorkspacePolicy.isSafeEnvironmentId(config.id)) {
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
                metadata.optString("environmentId") != config.id
            ) {
                return WineWorkspaceStatus(WineWorkspaceState.INVALID, "Metadados do ambiente não reconhecidos.")
            }
            when (metadata.optString("state")) {
                "WORKSPACE_ONLY" -> {
                    if (metadata.optBoolean("wineBootCompleted", true)) {
                        WineWorkspaceStatus(WineWorkspaceState.INVALID, "Estado do prefixo inconsistente.")
                    } else {
                        WineWorkspaceStatus(
                            WineWorkspaceState.WORKSPACE_PREPARED,
                            "Estrutura preparada; execute wineboot para inicializar o prefixo.",
                            workspace
                        )
                    }
                }
                "WINE_INITIALIZED" -> {
                    if (!metadata.optBoolean("wineBootCompleted", false)) {
                        WineWorkspaceStatus(WineWorkspaceState.INVALID, "Estado do prefixo inconsistente.")
                    } else {
                        WineWorkspaceStatus(
                            WineWorkspaceState.WINE_INITIALIZED,
                            "Prefixo Wine inicializado.",
                            workspace
                        )
                    }
                }
                else -> WineWorkspaceStatus(WineWorkspaceState.INVALID, "Estado do ambiente não reconhecido.")
            }
        } catch (error: Exception) {
            WineWorkspaceStatus(
                WineWorkspaceState.INVALID,
                error.message ?: "Não foi possível verificar o ambiente."
            )
        }
    }

}
