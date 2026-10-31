package com.mikael.novabox.runtime

import android.content.Context
import com.mikael.novabox.data.ProgramEntry
import com.mikael.novabox.model.ContainerConfig
import java.io.File
import java.io.IOException

/**
 * Starts Wine inside the bundled Linux rootfs through Android-native PRoot.
 *
 * This class deliberately refuses to run until all three real runtime pieces
 * (PRoot, Box64 and the Wine rootfs) exist. It never treats a prepared folder
 * or a copied EXE as an executable environment.
 */
object WineCommandBuilder {
    fun build(
        prootExecutable: String,
        rootfsDirectory: String,
        workspaceDirectory: String,
        sharedProgramsDirectory: String,
        guestCommand: List<String>
    ): List<String> {
        require(guestCommand.isNotEmpty() && guestCommand.none { it.isBlank() }) {
            "Comando Wine vazio."
        }
        return listOf(
            prootExecutable,
            "--kill-on-exit",
            "-0",
            "-r", rootfsDirectory,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "$workspaceDirectory:/workspace",
            "-b", "$sharedProgramsDirectory:/programs",
            "-w", "/workspace/home",
            "/usr/bin/box64"
        ) + guestCommand
    }
}

class WineRuntimeLauncher(context: Context) {
    private val appContext = context.applicationContext
    private val filesRoot = appContext.filesDir.canonicalFile
    private val runtimeRoot = File(filesRoot, "runtimes")
    private val rootfs = File(runtimeRoot, "rootfs")
    private val proot = File(runtimeRoot, "proot/bin/proot")
    private val workspaces = File(filesRoot, "wine-workspaces")
    private val programsRoot = File(filesRoot, "programs")

    @Throws(IOException::class)
    fun initializePrefix(config: ContainerConfig): Process =
        start(config, listOf("/usr/bin/wineboot", "--init"), null)

    @Throws(IOException::class)
    fun launchProgram(config: ContainerConfig, entry: ProgramEntry): Process {
        if (entry.containerId != config.id) throw IOException("O programa pertence a outro ambiente.")
        val stored = File(File(programsRoot, config.id), entry.storedFileName).canonicalFile
        val programDirectory = File(programsRoot, config.id).canonicalFile
        if (!stored.toPath().startsWith(programDirectory.toPath()) || !stored.isFile ||
            stored.length() != entry.sizeBytes
        ) {
            throw IOException("Arquivo do programa ausente ou inválido.")
        }
        if (!entry.storedFileName.matches(Regex("^[A-Za-z0-9-]+[.]program$"))) {
            throw IOException("Nome interno do programa inválido.")
        }
        val guestPath = "/programs/${stored.name}"
        return start(config, listOf("/usr/bin/wine", guestPath), programDirectory)
    }

    @Throws(IOException::class)
    private fun start(
        config: ContainerConfig,
        guestCommand: List<String>,
        sharedDirectory: File?
    ): Process {
        if (!WineWorkspacePolicy.isWindowsWineProfile(config)) {
            throw IOException("O perfil selecionado não é compatível com Wine.")
        }
        if (!WineWorkspacePolicy.isSafeEnvironmentId(config.id)) {
            throw IOException("Identificador do ambiente inválido.")
        }

        val workspace = File(workspaces, config.id).canonicalFile
        val canonicalWorkspaces = workspaces.canonicalFile
        if (!workspace.toPath().startsWith(canonicalWorkspaces.toPath()) ||
            !File(workspace, "workspace.json").isFile
        ) {
            throw IOException("Prepare a estrutura do ambiente antes de iniciar o Wine.")
        }
        if (!rootfs.isDirectory || !File(rootfs, "usr/bin/box64").isFile ||
            !File(rootfs, "usr/bin/wine").isFile ||
            !File(rootfs, "usr/bin/wineboot").isFile
        ) {
            throw IOException("Rootfs Wine/Box64 incompleto. Instale um pacote Android validado primeiro.")
        }
        if (!proot.isFile || !proot.canExecute()) {
            throw IOException("PRoot Android ARM64 não está instalado ou não pode ser executado.")
        }

        val shared = sharedDirectory ?: File(workspace, "shared")
        if (!shared.exists() && !shared.mkdirs()) {
            throw IOException("Não foi possível preparar a pasta compartilhada.")
        }
        val canonicalShared = shared.canonicalFile
        if (sharedDirectory == null &&
            !canonicalShared.toPath().startsWith(workspace.toPath())
        ) {
            throw IOException("Pasta compartilhada fora do ambiente.")
        }

        val command = WineCommandBuilder.build(
            prootExecutable = proot.absolutePath,
            rootfsDirectory = rootfs.canonicalPath,
            workspaceDirectory = workspace.absolutePath,
            sharedProgramsDirectory = canonicalShared.absolutePath,
            guestCommand = guestCommand
        )
        val logDirectory = File(workspace, "logs")
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            throw IOException("Não foi possível criar a pasta de logs.")
        }
        val logFile = File(logDirectory, "wine-session.log")
        val process = ProcessBuilder(command)
            .directory(workspace)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        process.environment().apply {
            put("PROOT_TMP_DIR", File(workspace, "tmp").absolutePath)
            put("WINEPREFIX", "/workspace/prefix")
            put("HOME", "/workspace/home")
            put("TMPDIR", "/workspace/tmp")
            put("USER", "novabox")
            put("DISPLAY", ":0")
            put("PULSE_SERVER", "127.0.0.1")
        }
        return try {
            process.start()
        } catch (error: Exception) {
            throw IOException("Não foi possível iniciar PRoot/Wine: ${error.message ?: "erro desconhecido"}", error)
        }
    }
}
