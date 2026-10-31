package com.mikael.novabox.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import com.mikael.novabox.model.ContainerConfig
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

data class QemuLaunchSpec(
    val executable: String,
    val kernel: String,
    val initrd: String?,
    val disk: String,
    val ramMb: Int,
    val cpuCores: Int,
    val append: String,
    val networkEnabled: Boolean
)

object QemuCommandBuilder {
    fun build(spec: QemuLaunchSpec): List<String> {
        require(spec.executable.isNotBlank() && spec.kernel.isNotBlank() && spec.disk.isNotBlank()) {
            "Caminhos obrigatórios do QEMU ausentes."
        }
        require(spec.ramMb in 256..8192) { "RAM da VM fora do limite suportado." }
        require(spec.cpuCores in 1..8) { "Quantidade de CPUs da VM inválida." }
        require(spec.append.length <= 2048 && !spec.append.contains('\u0000')) { "Parâmetros de boot inválidos." }
        val args = mutableListOf(
            spec.executable,
            "-machine", "virt,accel=tcg",
            "-cpu", "cortex-a53",
            "-m", spec.ramMb.toString(),
            "-smp", spec.cpuCores.toString(),
            "-drive", "file=" + spec.disk + ",if=virtio,format=raw",
            "-kernel", spec.kernel
        )
        if (!spec.initrd.isNullOrBlank()) {
            args += listOf("-initrd", spec.initrd)
        }
        if (spec.append.isNotBlank()) args += listOf("-append", spec.append)
        args += listOf("-nographic", "-serial", "mon:stdio", "-monitor", "none", "-no-reboot")
        if (spec.networkEnabled) {
            args += listOf("-netdev", "user,id=net0", "-device", "virtio-net-device,netdev=net0")
        }
        return args
    }
}

data class QemuSession(val process: Process, val logFile: File, val diskFile: File)

class QemuVirtualMachineManager(context: Context) {
    private val app = context.applicationContext
    private val runtimeDirectory = File(app.filesDir, "runtimes/qemu")
    private val vmRoot = File(app.filesDir, "virtual-machines")

    fun prepareDisk(config: ContainerConfig): File {
        validateVmConfig(config)
        val directory = vmDirectory(config.id)
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Não foi possível criar a pasta da máquina virtual.")
        val disk = File(directory, "disk.raw").canonicalFile
        if (!disk.toPath().startsWith(directory.canonicalFile.toPath())) throw IOException("Caminho de disco inválido.")
        val requiredBytes = config.diskGb.toLong() * 1024L * 1024L * 1024L
        if (disk.exists()) {
            if (!disk.isFile || disk.length() != requiredBytes) throw IOException("O disco existente não corresponde ao tamanho configurado.")
            return disk
        }
        if (directory.usableSpace < requiredBytes + 256L * 1024L * 1024L) {
            throw IOException("Espaço livre insuficiente para criar o disco virtual.")
        }
        try {
            RandomAccessFile(disk, "rw").use { it.setLength(requiredBytes) }
        } catch (error: Exception) {
            disk.delete()
            throw IOException("Não foi possível criar o disco virtual.", error)
        }
        if (!disk.isFile || disk.length() != requiredBytes) {
            disk.delete()
            throw IOException("O disco virtual não passou na verificação de tamanho.")
        }
        return disk
    }

    fun launch(config: ContainerConfig, kernelFile: File, initrdFile: File? = null, bootArguments: String = ""): QemuSession {
        validateVmConfig(config)
        val qemu = File(runtimeDirectory, "bin/qemu-system-aarch64").canonicalFile
        if (!qemu.toPath().startsWith(runtimeDirectory.canonicalFile.toPath()) || !qemu.isFile || !qemu.canExecute() ||
            !ElfAbiValidator.isCompatible(qemu, setOf("arm64-v8a"))) {
            throw IOException("QEMU ARM64 não está instalado ou o binário não é compatível com Android.")
        }
        val directory = vmDirectory(config.id)
        val kernel = validateVmFile(directory, kernelFile, "kernel")
        val initrd = initrdFile?.let { validateVmFile(directory, it, "initrd") }
        val disk = File(directory, "disk.raw").canonicalFile
        val expectedDiskSize = config.diskGb.toLong() * 1024L * 1024L * 1024L
        if (!disk.isFile || disk.length() != expectedDiskSize) throw IOException("Prepare o disco virtual antes de iniciar a VM.")
        val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val safeRamMb = (memoryInfo.availMem / 2L / (1024L * 1024L)).toInt().coerceAtLeast(256)
        if (config.ramMb > safeRamMb) throw IOException("A RAM solicitada excede metade da memória disponível do Android. Reduza a RAM da VM.")
        val maxCores = Runtime.getRuntime().availableProcessors().coerceAtMost(8)
        if (config.cpuCores > maxCores) throw IOException("A VM solicita mais CPUs que o aparelho disponibiliza.")
        val spec = QemuLaunchSpec(qemu.absolutePath, kernel.absolutePath, initrd?.absolutePath, disk.absolutePath,
            config.ramMb, config.cpuCores, bootArguments, config.network == com.mikael.novabox.model.NetworkMode.NAT)
        val command = try { QemuCommandBuilder.build(spec) } catch (error: IllegalArgumentException) {
            throw IOException(error.message ?: "Configuração QEMU inválida.", error)
        }
        val log = File(directory, "qemu-session.log")
        val builder = ProcessBuilder(command).directory(directory).redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        return try {
            QemuSession(builder.start(), log, disk)
        } catch (error: Exception) {
            throw IOException("Não foi possível iniciar QEMU: " + (error.message ?: "erro desconhecido"), error)
        }
    }

    fun stop(session: QemuSession) {
        if (session.process.isAlive) {
            session.process.destroy()
            if (session.process.isAlive) session.process.destroyForcibly()
        }
    }

    private fun validateVmConfig(config: ContainerConfig) {
        config.validate()?.let { throw IOException(it) }
        if (config.architecture != com.mikael.novabox.model.CpuArchitecture.ARM64) {
            throw IOException("Este backend QEMU está configurado somente para convidados ARM64.")
        }
        if (config.profile?.engine != com.mikael.novabox.model.EngineType.FULL_SYSTEM_VIRTUALIZATION &&
            config.profile?.engine != com.mikael.novabox.model.EngineType.ANDROID_VIRTUALIZATION) {
            throw IOException("O perfil selecionado não utiliza virtualização de sistema.")
        }
        if (!config.id.matches(Regex("^[A-Za-z0-9_-]{1,80}$"))) throw IOException("Identificador da VM inválido.")
    }

    private fun vmDirectory(id: String): File {
        val root = vmRoot.canonicalFile
        val directory = File(root, id).canonicalFile
        if (!directory.toPath().startsWith(root.toPath())) throw IOException("Diretório da VM inválido.")
        return directory
    }

    private fun validateVmFile(directory: File, file: File, label: String): File {
        val canonicalDirectory = directory.canonicalFile
        val canonicalFile = file.canonicalFile
        if (!canonicalFile.toPath().startsWith(canonicalDirectory.toPath()) || !canonicalFile.isFile || canonicalFile.length() == 0L) {
            throw IOException("Arquivo de " + label + " ausente ou fora da pasta privada da VM.")
        }
        return canonicalFile
    }
}