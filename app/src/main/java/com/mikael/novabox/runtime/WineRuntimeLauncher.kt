package com.mikael.novabox.runtime

import android.content.Context
import com.mikael.novabox.data.ProgramEntry
import com.mikael.novabox.model.ContainerConfig
import java.io.File
import java.io.IOException

/** Builds the same host-ARM64 Box64 -> guest x86_64 Wine chain used by Winlator. */
object WineCommandBuilder {
    fun build(box64Executable: String, wineExecutable: String, guestArguments: List<String>): List<String> {
        require(box64Executable.isNotBlank() && wineExecutable.isNotBlank()) {
            "Caminho do runtime vazio."
        }
        require(guestArguments.isNotEmpty() && guestArguments.none { it.isBlank() }) {
            "Argumentos do Wine vazios."
        }
        return listOf(box64Executable, wineExecutable) + guestArguments
    }
}

class WineRuntimeLauncher(context: Context) {
    private val appContext = context.applicationContext
    private val filesRoot = appContext.filesDir.canonicalFile
    private val rootfs = File(filesRoot, "runtimes/rootfs")
    private val workspaces = File(filesRoot, "wine-workspaces")
    private val programsRoot = File(filesRoot, "programs")

    @Throws(IOException::class)
    fun initializePrefix(config: ContainerConfig): Process {
        val workspace = validateWorkspace(config)
        return start(config, workspace, listOf("--init"))
    }

    @Throws(IOException::class)
    fun launchProgram(config: ContainerConfig, entry: ProgramEntry): Process {
        if (entry.containerId != config.id) throw IOException("O programa pertence a outro ambiente.")
        val programDirectory = File(programsRoot, config.id).canonicalFile
        val stored = File(programDirectory, entry.storedFileName).canonicalFile
        if (!stored.toPath().startsWith(programDirectory.toPath()) || !stored.isFile ||
            stored.length() != entry.sizeBytes
        ) {
            throw IOException("Arquivo do programa ausente ou inválido.")
        }
        if (!entry.storedFileName.matches(Regex("^[A-Za-z0-9-]+[.]program$"))) {
            throw IOException("Nome interno do programa inválido.")
        }
        val workspace = validateWorkspace(config)
        return start(config, workspace, listOf(stored.absolutePath))
    }

    private fun validateWorkspace(config: ContainerConfig): File {
        if (!WineWorkspacePolicy.isWindowsWineProfile(config)) {
            throw IOException("O perfil selecionado não é compatível com Wine.")
        }
        if (!WineWorkspacePolicy.isSafeEnvironmentId(config.id)) {
            throw IOException("Identificador do ambiente inválido.")
        }
        val workspace = File(workspaces, config.id).canonicalFile
        if (!workspace.toPath().startsWith(workspaces.canonicalFile.toPath()) ||
            !File(workspace, "workspace.json").isFile
        ) {
            throw IOException("Prepare a estrutura do ambiente antes de iniciar o Wine.")
        }
        return workspace
    }

    @Throws(IOException::class)
    private fun start(config: ContainerConfig, workspace: File, guestArguments: List<String>): Process {
        val box64 = File(rootfs, "usr/local/bin/box64")
        val wine = File(rootfs, "opt/wine/bin/wine")
        val wineboot = File(rootfs, "opt/wine/bin/wineboot")
        if (!WinlatorRuntimeBundleInstaller(appContext).isInstalled() ||
            !box64.isFile || !box64.canExecute() ||
            !wine.isFile || !wineboot.isFile
        ) {
            throw IOException("Runtime real Wine/Box64 incompleto. Instale o bundle oficial antes de executar.")
        }
        val executable = if (guestArguments == listOf("--init")) wineboot else wine
        val command = WineCommandBuilder.build(box64.absolutePath, executable.absolutePath, guestArguments)
        val home = File(workspace, "home")
        val prefix = File(workspace, "prefix")
        val temp = File(workspace, "tmp")
        listOf(home, prefix, temp).forEach {
            if (!it.exists() && !it.mkdirs()) throw IOException("Não foi possível preparar ${it.name}.")
        }
        val logDirectory = File(workspace, "logs")
        if (!logDirectory.exists() && !logDirectory.mkdirs()) throw IOException("Não foi possível criar os logs.")
        val logFile = File(logDirectory, "wine-session.log")

        val process = ProcessBuilder(command)
            .directory(workspace)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        process.environment().apply {
            put("HOME", home.absolutePath)
            put("WINEPREFIX", prefix.absolutePath)
            put("TMPDIR", temp.absolutePath)
            put("USER", "xuser")
            put("PATH", "${File(rootfs, "opt/wine/bin").absolutePath}:${File(rootfs, "usr/local/bin").absolutePath}:${File(rootfs, "usr/bin").absolutePath}")
            put("LD_LIBRARY_PATH", File(rootfs, "usr/lib").absolutePath)
            put("BOX64_LD_LIBRARY_PATH", "${File(rootfs, "lib/x86_64-linux-gnu").absolutePath}:${File(rootfs, "usr/lib/x86_64-linux-gnu").absolutePath}")
            put("BOX64_DYNAREC", "1")
            put("BOX64_NOBANNER", "1")
            put("WINEDEBUG", "-all")
            put("DISPLAY", ":0")
        }
        return try {
            process.start()
        } catch (error: Exception) {
            throw IOException("Não foi possível iniciar Box64/Wine: ${error.message ?: "erro desconhecido"}", error)
        }
    }
}
