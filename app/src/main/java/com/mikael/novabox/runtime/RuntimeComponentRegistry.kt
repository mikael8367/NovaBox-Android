package com.mikael.novabox.runtime

import com.mikael.novabox.model.RuntimeId

enum class RuntimeComponentState {
    INSTALLED,
    NOT_INSTALLED,
    INCOMPLETE,
    UNSUPPORTED_DEVICE,
    BLOCKED
}

data class RuntimeComponentStatus(
    val id: RuntimeId,
    val state: RuntimeComponentState,
    val message: String,
    val dependencies: List<RuntimeId>
)

data class RuntimeComponentDefinition(
    val id: RuntimeId,
    val purpose: String,
    val dependencies: List<RuntimeId>,
    /** Each group requires at least one matching file. All groups must be satisfied. */
    val requiredFileGroups: List<List<String>>,
    val executablePaths: Set<String> = emptySet(),
    val requiredAbi: Set<String> = emptySet(),
    val requiresAdreno: Boolean = false,
    val requiresVulkan: Boolean = false
)

object RuntimeComponentRegistry {
    private val arm64 = setOf("arm64-v8a")
    private val arm32 = setOf("armeabi-v7a", "armeabi")

    val all: List<RuntimeComponentDefinition> = listOf(
        RuntimeComponentDefinition(
            RuntimeId.WINE, "Compatibilidade com aplicativos Windows",
            emptyList(),
            listOf(listOf("bin/wine")), setOf("bin/wine")
        ),
        RuntimeComponentDefinition(
            RuntimeId.BOX64, "Tradução de aplicativos Linux x86-64 para ARM64",
            emptyList(), listOf(listOf("bin/box64")), setOf("bin/box64"), arm64
        ),
        RuntimeComponentDefinition(
            RuntimeId.BOX86, "Tradução de aplicativos Linux x86 de 32 bits",
            emptyList(), listOf(listOf("bin/box86")), setOf("bin/box86"), arm64 + arm32
        ),
        RuntimeComponentDefinition(
            RuntimeId.PROOT, "Ambiente Linux de usuário sem root",
            emptyList(), listOf(listOf("bin/proot")), setOf("bin/proot"), arm64
        ),
        RuntimeComponentDefinition(
            RuntimeId.DOSBOX, "Emulação de programas DOS",
            emptyList(), listOf(listOf("bin/dosbox")), setOf("bin/dosbox"), arm64
        ),
        RuntimeComponentDefinition(
            RuntimeId.QEMU, "Virtualização/emulação de sistemas completos",
            emptyList(),
            listOf(listOf("bin/qemu-system-aarch64", "bin/qemu-system-x86_64", "bin/qemu-system")),
            setOf("bin/qemu-system-aarch64", "bin/qemu-system-x86_64", "bin/qemu-system"),
            arm64
        ),
        RuntimeComponentDefinition(
            RuntimeId.MESA, "Bibliotecas e drivers gráficos Mesa",
            emptyList(),
            listOf(listOf("lib/libEGL.so", "lib/libGLESv2.so", "lib/libvulkan.so"))
        ),
        RuntimeComponentDefinition(
            RuntimeId.VIRGL, "Renderizador de GPU virtual VirGL",
            listOf(RuntimeId.MESA),
            listOf(listOf("lib/libvirglrenderer.so", "lib/dri/virgl_dri.so"))
        ),
        RuntimeComponentDefinition(
            RuntimeId.ZINK, "OpenGL implementado sobre Vulkan",
            listOf(RuntimeId.MESA),
            listOf(listOf("lib/dri/zink_dri.so", "lib/libvulkan_zink.so")),
            requiresVulkan = true
        ),
        RuntimeComponentDefinition(
            RuntimeId.TURNIP, "Driver Vulkan Mesa para GPUs Qualcomm Adreno",
            listOf(RuntimeId.MESA),
            listOf(listOf("lib/libvulkan_freedreno.so", "lib/libvulkan_freedreno.so.1")),
            requiresAdreno = true,
            requiresVulkan = true
        ),
        RuntimeComponentDefinition(
            RuntimeId.DXVK, "Tradução Direct3D 9/10/11 para Vulkan",
            listOf(RuntimeId.WINE),
            listOf(
                listOf("lib/dxvk/x32/d3d9.dll", "lib/dxvk/x64/d3d9.dll"),
                listOf("lib/dxvk/x32/d3d10core.dll", "lib/dxvk/x64/d3d10core.dll"),
                listOf("lib/dxvk/x32/d3d11.dll", "lib/dxvk/x64/d3d11.dll"),
                listOf("lib/dxvk/x32/dxgi.dll", "lib/dxvk/x64/dxgi.dll")
            ),
            requiresVulkan = true
        ),
        RuntimeComponentDefinition(
            RuntimeId.VKD3D, "Tradução Direct3D 12 para Vulkan",
            listOf(RuntimeId.WINE),
            listOf(listOf("lib/vkd3d/d3d12.dll", "lib/d3d12.dll")),
            requiresVulkan = true
        )
    )

    fun get(id: RuntimeId): RuntimeComponentDefinition =
        all.first { it.id == id }
}
