package com.mikael.novabox.model

enum class RuntimeId(val title: String, val purpose: String, val detail: String) {
    WINE("Wine", "Compatibilidade com aplicativos Windows", "Implementa APIs do Windows em ambiente Unix. Não é uma máquina virtual do Windows."),
    BOX64("Box64", "Compatibilidade x86-64", "Executa programas Linux x86-64 em hosts ARM64 compatíveis."),
    BOX86("Box86", "Compatibilidade x86", "Executa programas Linux x86 de 32 bits em hosts compatíveis."),
    PROOT("PRoot", "Ambientes Linux isolados", "Permite preparar ambientes de usuário Linux sem root; não substitui virtualização completa."),
    DOSBOX("DOSBox", "Aplicativos DOS", "Emulador separado para programas e jogos DOS."),
    MESA("Mesa", "Camada gráfica", "Conjunto de drivers gráficos; o driver correto depende da GPU do aparelho."),
    VIRGL("VirGL", "Renderização virtual", "Caminho gráfico virtualizado para ambientes compatíveis."),
    ZINK("Zink", "OpenGL sobre Vulkan", "Camada OpenGL baseada em Vulkan quando o dispositivo e a pilha suportam."),
    TURNIP("Turnip", "Driver Vulkan para Adreno", "Driver Mesa voltado a GPUs Qualcomm Adreno compatíveis."),
    DXVK("DXVK", "Direct3D 9/10/11", "Traduz chamadas Direct3D para Vulkan em configurações compatíveis."),
    VKD3D("VKD3D-Proton", "Direct3D 12", "Camada de tradução Direct3D 12 para Vulkan.")
}

data class RuntimeDescriptor(
    val id: RuntimeId,
    val status: String,
    val description: String
)

object RuntimePlanner {
    fun requiredFor(config: ContainerConfig): List<RuntimeId> {
        val profile = config.profile ?: return emptyList()
        return when (profile.family) {
            OsFamily.WINDOWS -> buildList {
                add(RuntimeId.WINE)
                when (config.architecture) {
                    CpuArchitecture.X86_64 -> add(RuntimeId.BOX64)
                    CpuArchitecture.X86 -> add(RuntimeId.BOX86)
                    CpuArchitecture.ARM64 -> Unit
                }
                if (config.graphics == GraphicsProfile.AUTO || config.graphics == GraphicsProfile.VIRGL) {
                    add(RuntimeId.VIRGL)
                }
                if (config.graphics == GraphicsProfile.ZINK) add(RuntimeId.ZINK)
            }
            OsFamily.LINUX -> buildList {
                add(RuntimeId.PROOT)
                when (config.architecture) {
                    CpuArchitecture.X86_64 -> add(RuntimeId.BOX64)
                    CpuArchitecture.X86 -> add(RuntimeId.BOX86)
                    CpuArchitecture.ARM64 -> Unit
                }
                if (config.graphics == GraphicsProfile.AUTO || config.graphics == GraphicsProfile.VIRGL) {
                    add(RuntimeId.VIRGL)
                }
                if (config.graphics == GraphicsProfile.ZINK) add(RuntimeId.ZINK)
            }
            OsFamily.OTHER -> when (profile.id) {
                "freedos" -> listOf(RuntimeId.DOSBOX)
                else -> emptyList()
            }
        }
    }
}
