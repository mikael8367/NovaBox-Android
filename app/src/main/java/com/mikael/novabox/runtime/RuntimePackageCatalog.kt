package com.mikael.novabox.runtime

import com.mikael.novabox.model.RuntimeId

enum class RuntimeArchiveFormat {
    TAR_GZ
}

data class RuntimePackageSpec(
    val runtimeId: RuntimeId,
    val version: String,
    val fileName: String,
    val downloadUrl: String,
    val sha256: String,
    val expectedSizeBytes: Long,
    val archiveFormat: RuntimeArchiveFormat,
    val archiveRootDirectory: String,
    val outputDirectory: String,
    val allowedArchiveFiles: Set<String>,
    val requiredArchiveFiles: Set<String>,
    val maxExpandedBytes: Long = 256L * 1024L * 1024L
)

object RuntimePackageCatalog {
    private val dxvk311 = RuntimePackageSpec(
        runtimeId = RuntimeId.DXVK,
        version = "3.1.1",
        fileName = "dxvk-3.1.1.tar.gz",
        downloadUrl = "https://github.com/doitsujin/dxvk/releases/download/v3.1.1/dxvk-3.1.1.tar.gz",
        sha256 = "40565b4a724aadc4433fa4e010b4b23916d9b1f1baeee64e17186db94f54e608",
        expectedSizeBytes = 18_041_512L,
        archiveFormat = RuntimeArchiveFormat.TAR_GZ,
        archiveRootDirectory = "dxvk-3.1.1",
        outputDirectory = "lib/dxvk",
        allowedArchiveFiles = setOf(
            "LICENSE",
            "x32/d3d9.dll",
            "x32/d3d10core.dll",
            "x32/d3d11.dll",
            "x32/dxgi.dll",
            "x64/d3d9.dll",
            "x64/d3d10core.dll",
            "x64/d3d11.dll",
            "x64/dxgi.dll"
        ),
        requiredArchiveFiles = setOf(
            "x32/d3d9.dll",
            "x32/d3d10core.dll",
            "x32/d3d11.dll",
            "x32/dxgi.dll",
            "x64/d3d9.dll",
            "x64/d3d10core.dll",
            "x64/d3d11.dll",
            "x64/dxgi.dll"
        )
    )

    private val verifiedPackages = listOf(dxvk311).associateBy { it.runtimeId }

    fun packageFor(id: RuntimeId): RuntimePackageSpec? = verifiedPackages[id]

    fun verifiedPackages(): List<RuntimePackageSpec> = verifiedPackages.values.toList()

    fun unavailableReason(id: RuntimeId): String =
        when (id) {
            RuntimeId.DXVK -> "O pacote está disponível, mas depende de Wine e de uma pilha Vulkan compatível."
            RuntimeId.TURNIP -> "Turnip exige GPU Qualcomm Adreno. Não é compatível com GPU PowerVR."
            RuntimeId.WINE -> "Ainda não há um pacote Android ARM64 do Wine validado neste projeto."
            RuntimeId.BOX64 -> "Os arquivos de apoio upstream não substituem um executável Box64 Android ARM64 validado."
            RuntimeId.BOX86 -> "Ainda não há um binário Android ARM64 validado com dependências completas."
            RuntimeId.PROOT -> "Ainda falta integrar o pacote Android do PRoot com as dependências do ambiente."
            RuntimeId.LINUX_ROOTFS -> "O rootfs Alpine ARM64 tem instalador próprio; ainda depende do PRoot para iniciar o Linux."
            RuntimeId.DOSBOX -> "Os pacotes binários encontrados não correspondem ao Android ARM64."
            RuntimeId.QEMU -> "O gerenciador de discos e processos QEMU está preparado, mas ainda falta o binário Android ARM64 e a interface de console."
            RuntimeId.MESA -> "Ainda falta uma pilha Mesa Android compatível com a GPU e com o sandbox do app."
            RuntimeId.VIRGL -> "Ainda falta um pacote VirGL Android ARM64 e a integração do backend gráfico."
            RuntimeId.ZINK -> "Zink depende de Mesa e Vulkan compatíveis; o pacote Android ainda não foi validado."
            RuntimeId.VKD3D -> "O pacote upstream atual usa um formato de arquivo ainda não integrado e requer Wine/Vulkan."
        }
}
