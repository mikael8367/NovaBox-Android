package com.mikael.novabox.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
    private val supportedAbis = Build.SUPPORTED_ABIS.toSet()
    private val hardwareDescription = listOf(
        Build.HARDWARE,
        Build.BOARD,
        Build.MODEL,
        Build.MANUFACTURER,
        Build.SOC_MODEL
    ).joinToString(" ").lowercase()
    private val hasVulkan = context.packageManager.hasSystemFeature(
        PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
    )
    private val hasAdreno = listOf("adreno", "qualcomm", "qcom").any(hardwareDescription::contains)

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

        val executablePaths = required.flatMap { definition ->
            RuntimeComponentRegistry.get(definition).executablePaths
                .map { File(runtimeDirectory(definition), it) }
                .filter { it.isFile }
                .map { it.absolutePath }
        }
        return EngineAvailability.Ready(executablePaths)
    }

    fun componentStatus(id: RuntimeId): RuntimeComponentStatus {
        val definition = RuntimeComponentRegistry.get(id)
        val incompatibility = deviceIncompatibility(definition)
        if (incompatibility != null) {
            return RuntimeComponentStatus(
                id, RuntimeComponentState.UNSUPPORTED_DEVICE, incompatibility, definition.dependencies
            )
        }

        val directory = runtimeDirectory(id)
        val groups = definition.requiredFileGroups
        val groupResults = groups.map { group ->
            group.any { relativePath ->
                val file = File(directory, relativePath)
                file.isFile && file.length() > 0L &&
                    (relativePath !in definition.executablePaths || file.canExecute())
            }
        }

        val filesState = when {
            groupResults.all { it } -> RuntimeComponentState.INSTALLED
            groupResults.any { it } -> RuntimeComponentState.INCOMPLETE
            else -> RuntimeComponentState.NOT_INSTALLED
        }
        val missingDependencies = if (filesState == RuntimeComponentState.INSTALLED) {
            definition.dependencies.filterNot(::isComponentInstalled)
        } else {
            emptyList()
        }
        val state = if (missingDependencies.isNotEmpty()) {
            RuntimeComponentState.BLOCKED
        } else {
            filesState
        }
        val message = when (state) {
            RuntimeComponentState.INSTALLED -> "Arquivos principais e dependências encontrados."
            RuntimeComponentState.INCOMPLETE -> "Instalação incompleta: faltam arquivos obrigatórios."
            RuntimeComponentState.NOT_INSTALLED -> "Componente não instalado neste ambiente."
            RuntimeComponentState.UNSUPPORTED_DEVICE -> incompatibility.orEmpty()
            RuntimeComponentState.BLOCKED -> "Dependências ausentes: " + missingDependencies.joinToString { it.title }
        }
        return RuntimeComponentStatus(id, state, message, definition.dependencies)
    }

    fun isComponentInstalled(id: RuntimeId): Boolean =
        componentStatus(id).state == RuntimeComponentState.INSTALLED

    fun runtimeDirectory(id: RuntimeId): File =
        File(context.filesDir, "runtimes/${id.name.lowercase()}")

    private fun deviceIncompatibility(definition: RuntimeComponentDefinition): String? {
        if (definition.requiredAbi.isNotEmpty() &&
            supportedAbis.none { it in definition.requiredAbi }
        ) {
            return "Este pacote exige ABI ${definition.requiredAbi.joinToString()}."
        }
        if (definition.requiresAdreno && !hasAdreno) {
            return "Turnip exige GPU Qualcomm Adreno. Esta GPU não foi identificada como Adreno."
        }
        if (definition.requiresVulkan && !hasVulkan) {
            return "Este componente exige suporte Vulkan anunciado pelo Android."
        }
        return null
    }
}
