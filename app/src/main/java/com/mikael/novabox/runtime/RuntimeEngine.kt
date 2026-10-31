package com.mikael.novabox.runtime

import android.content.Context
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.EngineType
import com.mikael.novabox.model.RuntimeId
import com.mikael.novabox.model.RuntimePlanner
import java.io.File

sealed interface EngineAvailability {
    data class Ready(val executablePaths: List<String>) : EngineAvailability
    data class Missing(val components: List<RuntimeId>) : EngineAvailability
    data class Unsupported(val reason: String) : EngineAvailability
}

class RuntimeEngine(private val context: Context) {
    fun inspect(config: ContainerConfig): EngineAvailability {
        val profile = config.profile
            ?: return EngineAvailability.Unsupported("Perfil de sistema desconhecido.")

        if (profile.engine == EngineType.UNSUPPORTED) {
            return EngineAvailability.Unsupported("Este perfil não possui um motor compatível configurado.")
        }

        val required = RuntimePlanner.requiredFor(config)
        if (required.isEmpty()) {
            return EngineAvailability.Unsupported("Não existe uma cadeia de execução configurada para este perfil.")
        }

        val missing = required.filterNot(::isComponentInstalled)
        if (missing.isNotEmpty()) return EngineAvailability.Missing(missing)

        return EngineAvailability.Ready(required.map { executableFor(it).absolutePath })
    }

    fun isComponentInstalled(id: RuntimeId): Boolean = executableFor(id).canExecute()

    fun runtimeDirectory(id: RuntimeId): File =
        File(context.filesDir, "runtimes/${id.name.lowercase()}")

    private fun executableFor(id: RuntimeId): File {
        val executableName = when (id) {
            RuntimeId.WINE -> "bin/wine"
            RuntimeId.BOX64 -> "bin/box64"
            RuntimeId.BOX86 -> "bin/box86"
            RuntimeId.PROOT -> "bin/proot"
            RuntimeId.DOSBOX -> "bin/dosbox"
            RuntimeId.QEMU -> "bin/qemu-system"
            RuntimeId.MESA, RuntimeId.VIRGL, RuntimeId.ZINK, RuntimeId.TURNIP,
            RuntimeId.DXVK, RuntimeId.VKD3D -> "lib/installed.marker"
        }
        return File(runtimeDirectory(id), executableName)
    }
}
