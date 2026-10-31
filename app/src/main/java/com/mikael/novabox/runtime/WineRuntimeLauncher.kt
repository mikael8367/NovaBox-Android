package com.mikael.novabox.runtime

import android.content.Context
import com.mikael.novabox.data.ProgramEntry
import com.mikael.novabox.data.ProgramIntegrityVerifier
import com.mikael.novabox.model.ContainerConfig
import java.io.File
import java.io.IOException

/** Builds the host ARM64 -> guest x86_64 Wine process chain used by Winlator. */
object WineCommandBuilder {
    fun build(box64Executable: String, wineExecutable: String, guestArguments: List<String>): List<String> {
        require(box64Executable.isNotBlank() && wineExecutable.isNotBlank()) { "Caminho do runtime vazio." }
        require(guestArguments.isNotEmpty() && guestArguments.none { it.isBlank() }) { "Argumentos do Wine vazios." }
        return listOf(box64Executable, wineExecutable) + guestArguments
    }

    fun programArguments(displayName: String, storedPath: String): List<String> {
        require(displayName.isNotBlank() && storedPath.isNotBlank()) { "Programa inválido." }
        return when (displayName.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)) {
            "msi" -> listOf("msiexec", "/i", storedPath)
            "exe", "com" -> listOf(storedPath)
            else -> throw IllegalArgumentException("Formato de programa não suportado.")
        }
    }
}

/** Environment parity with Winlator's guest launcher, while keeping each prefix isolated. */
object WineEnvironmentBuilder {
    fun build(rootfs: File, workspace: File, config: ContainerConfig): Map<String, String> {
        val wineBin = File(rootfs, "opt/wine/bin").absolutePath
        val box64Bin = File(rootfs, "usr/local/bin").absolutePath
        val usrBin = File(rootfs, "usr/bin").absolutePath
        val x64Libraries = File(rootfs, "lib/x86_64-linux-gnu").absolutePath
        val x64UsrLibraries = File(rootfs, "usr/lib/x86_64-linux-gnu").absolutePath
        val env = linkedMapOf(
            "HOME" to File(workspace, "home").absolutePath,
            "USER" to "xuser",
            "TMPDIR" to File(workspace, "tmp").absolutePath,
            "WINEPREFIX" to File(workspace, "prefix").absolutePath,
            // The runtime ships a 64-bit Wine build; WoW64 handles 32-bit apps inside this prefix.
            "WINEARCH" to "win64",
            "PATH" to listOf(wineBin, box64Bin, usrBin).joinToString(File.pathSeparator),
            "LD_LIBRARY_PATH" to File(rootfs, "usr/lib").absolutePath,
            "BOX64_LD_LIBRARY_PATH" to listOf(x64Libraries, x64UsrLibraries).joinToString(File.pathSeparator),
            "BOX64_DYNAREC" to "1",
            "BOX64_DYNACACHE" to "0",
            "BOX64_UNITYPLAYER" to "0",
            "BOX64_NOBANNER" to "1",
            "WINEDEBUG" to "-all",
            // Termux socat forwards this loopback-only X11 endpoint to Termux:X11 display :1.
            "DISPLAY" to XServerCommand.WINE_DISPLAY,
        )
        if (config.esyncEnabled) env["WINEESYNC"] = "1"
        if (config.fsyncEnabled) env["WINEFSYNC"] = "1"
        val rcFile = File(rootfs, "etc/config.box64rc")
        if (rcFile.isFile) env["BOX64_RCFILE"] = rcFile.absolutePath
        return env
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
        if (entry.sizeBytes <= 0L || !entry.sha256.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            throw IOException("Metadados de integridade do programa inválidos.")
        }
        if (!entry.storedFileName.matches(Regex("^[A-Za-z0-9-]+[.]program$"))) {
            throw IOException("Nome interno do programa inválido.")
        }
        val programDirectory = File(programsRoot, config.id).canonicalFile
        val stored = File(programDirectory, entry.storedFileName).canonicalFile
        if (!stored.toPath().startsWith(programDirectory.toPath()) || !stored.isFile || stored.length() != entry.sizeBytes) {
            throw IOException("Arquivo do programa ausente ou inválido.")
        }
        val actualHash = try {
            stored.inputStream().use { ProgramIntegrityVerifier.sha256(it) }
        } catch (error: Exception) {
            throw IOException("Não foi possível verificar o arquivo do programa.", error)
        }
        if (!actualHash.equals(entry.sha256, ignoreCase = true)) {
            throw IOException("A integridade do programa falhou. Importe o arquivo novamente.")
        }
        val workspace = validateWorkspace(config)
        if (WineWorkspaceManager(appContext).inspect(config).state != WineWorkspaceState.WINE_INITIALIZED) {
            throw IOException("Inicialize o prefixo Wine antes de executar programas.")
        }
        val guestArguments = try {
            WineCommandBuilder.programArguments(entry.displayName, stored.absolutePath)
        } catch (error: IllegalArgumentException) {
            throw IOException(error.message ?: "Formato de programa inválido.", error)
        }
        return start(config, workspace, guestArguments)
    }

    private fun validateWorkspace(config: ContainerConfig): File {
        if (!WineWorkspacePolicy.isWindowsWineProfile(config)) throw IOException("O perfil selecionado não é compatível com Wine.")
        if (!WineWorkspacePolicy.isSafeEnvironmentId(config.id)) throw IOException("Identificador do ambiente inválido.")
        val workspace = File(workspaces, config.id).canonicalFile
        if (!workspace.toPath().startsWith(workspaces.canonicalFile.toPath()) || !File(workspace, "workspace.json").isFile) {
            throw IOException("Prepare a estrutura do ambiente antes de iniciar o Wine.")
        }
        return workspace
    }

    @Throws(IOException::class)
    private fun start(config: ContainerConfig, workspace: File, guestArguments: List<String>): Process {
        config.validate()?.let { throw IOException(it) }
        val box64 = File(rootfs, "usr/local/bin/box64")
        val wine = File(rootfs, "opt/wine/bin/wine")
        val wineboot = File(rootfs, "opt/wine/bin/wineboot")
        if (!WinlatorRuntimeBundleInstaller(appContext).isInstalled() || !box64.isFile || !box64.canExecute() ||
            !wine.isFile || !wine.canExecute() || !wineboot.isFile) {
            throw IOException("Runtime real Wine/Box64 incompleto. Instale o bundle oficial antes de executar.")
        }
        val executable = if (guestArguments == listOf("--init")) wineboot else wine
        val command = WineCommandBuilder.build(box64.absolutePath, executable.absolutePath, guestArguments)
        val home = File(workspace, "home")
        val prefix = File(workspace, "prefix")
        val temp = File(workspace, "tmp")
        listOf(home, prefix, temp).forEach {
            if (!it.exists() && !it.mkdirs()) throw IOException("Não foi possível preparar " + it.name + ".")
        }
        val logDirectory = File(workspace, "logs")
        if (!logDirectory.exists() && !logDirectory.mkdirs()) throw IOException("Não foi possível criar os logs.")
        val logFile = File(logDirectory, "wine-session.log")
        val root = rootfs.canonicalFile
        val builder = ProcessBuilder(command).directory(root).redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        builder.environment().putAll(WineEnvironmentBuilder.build(root, workspace, config))
        return try {
            WineProcessSupervisor.start(config.id) { builder.start() }
        } catch (error: Exception) {
            throw IOException("Não foi possível iniciar Box64/Wine: " + (error.message ?: "erro desconhecido"), error)
        }
    }
}