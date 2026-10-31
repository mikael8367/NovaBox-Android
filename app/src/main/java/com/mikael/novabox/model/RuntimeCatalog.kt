package com.mikael.novabox.model

enum class RuntimeId(val title: String, val purpose: String, val detail: String) {
    WINE("Windows (Wine)", "Compatibilidade com APIs Windows", "Executa aplicativos Windows sobre um ambiente Unix; não é uma instalação completa do Windows."),
    BOX64("Box64", "Tradução x86-64", "Executa programas Linux x86-64 em hosts ARM64 compatíveis."),
    BOX86("Box86", "Tradução x86", "Executa programas Linux x86 de 32 bits em hosts ARM compatíveis."),
    PROOT("PRoot", "Ambientes Linux", "Prepara ambientes Linux de usuário sem exigir root; não é uma máquina virtual completa."),
    LINUX_ROOTFS("Alpine Linux ARM64", "Sistema de arquivos Linux", "Rootfs Alpine 3.24.3 ARM64; requer PRoot para inicializar o ambiente."),
    DOSBOX("DOSBox", "Compatibilidade DOS", "Emulação separada para software e jogos DOS."),
    QEMU("QEMU", "Virtualização/emulação de sistema", "Necessário para sistemas completos; aceleração depende do suporte do aparelho. Ainda não integrado."),
    MESA("Mesa", "Pilha gráfica", "Drivers e bibliotecas gráficas; suporte depende da GPU e do backend."),
    VIRGL("VirGL", "GPU virtual", "Renderização virtualizada para pilhas compatíveis."),
    ZINK("Zink", "OpenGL sobre Vulkan", "Camada OpenGL sobre Vulkan quando a pilha gráfica oferece suporte."),
    TURNIP("Turnip", "Driver Adreno", "Driver Vulkan voltado a GPUs Qualcomm Adreno compatíveis."),
    DXVK("DXVK", "Direct3D 9/10/11", "Traduz chamadas Direct3D para Vulkan em configurações compatíveis."),
    VKD3D("VKD3D-Proton", "Direct3D 12", "Traduz Direct3D 12 para Vulkan em configurações compatíveis.")
}

object RuntimePlanner {
    fun requiredFor(config: ContainerConfig): List<RuntimeId> {
        val profile = config.profile ?: return emptyList()
        return when (profile.engine) {
            EngineType.WINE_COMPATIBILITY -> buildList {
                add(RuntimeId.WINE)
                add(RuntimeId.BOX64)
                if (config.graphics == GraphicsProfile.VIRGL) add(RuntimeId.VIRGL)
                if (config.graphics == GraphicsProfile.ZINK) add(RuntimeId.ZINK)
                if (config.graphics == GraphicsProfile.TURNIP) add(RuntimeId.TURNIP)
                if (config.dxvkEnabled) add(RuntimeId.DXVK)
                if (config.vkd3dEnabled) add(RuntimeId.VKD3D)
            }
            EngineType.LINUX_USERLAND -> buildList {
                add(RuntimeId.PROOT)
                add(RuntimeId.LINUX_ROOTFS)
                when (config.architecture) {
                    CpuArchitecture.X86_64 -> add(RuntimeId.BOX64)
                    CpuArchitecture.X86 -> add(RuntimeId.BOX86)
                    CpuArchitecture.ARM64 -> Unit
                }
                if (config.graphics == GraphicsProfile.VIRGL) add(RuntimeId.VIRGL)
                if (config.graphics == GraphicsProfile.ZINK) add(RuntimeId.ZINK)
                if (config.graphics == GraphicsProfile.TURNIP) add(RuntimeId.TURNIP)
            }
            EngineType.DOS_EMULATION -> listOf(RuntimeId.DOSBOX)
            EngineType.ANDROID_VIRTUALIZATION -> listOf(RuntimeId.QEMU)
            EngineType.FULL_SYSTEM_VIRTUALIZATION -> listOf(RuntimeId.QEMU)
            EngineType.UNSUPPORTED -> emptyList()
        }
    }
}