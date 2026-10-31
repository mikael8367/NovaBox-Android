package com.mikael.novabox.model

import java.util.UUID

enum class OsFamily(val label: String) {
    WINDOWS("Windows"),
    LINUX("Linux"),
    BSD("BSD"),
    OTHER("Outros")
}

enum class EngineType {
    WINE_COMPATIBILITY,
    LINUX_USERLAND,
    DOS_EMULATION,
    FULL_SYSTEM_VIRTUALIZATION,
    UNSUPPORTED
}

data class OsProfile(
    val id: String,
    val name: String,
    val family: OsFamily,
    val release: String,
    val description: String,
    val engine: EngineType,
    val maturity: String
)

object OsCatalog {
    val profiles = listOf(
        OsProfile("windows-11", "Windows 11", OsFamily.WINDOWS, "11", "Perfil de compatibilidade para aplicativos modernos.", EngineType.WINE_COMPATIBILITY, "Experimental"),
        OsProfile("windows-10", "Windows 10", OsFamily.WINDOWS, "10", "Perfil de compatibilidade para aplicativos atuais.", EngineType.WINE_COMPATIBILITY, "Experimental"),
        OsProfile("windows-8-1", "Windows 8.1", OsFamily.WINDOWS, "8.1", "Perfil legado para aplicativos dessa geração.", EngineType.WINE_COMPATIBILITY, "Experimental"),
        OsProfile("windows-8", "Windows 8", OsFamily.WINDOWS, "8", "Perfil legado.", EngineType.WINE_COMPATIBILITY, "Experimental"),
        OsProfile("windows-7", "Windows 7", OsFamily.WINDOWS, "7", "Perfil legado para programas antigos.", EngineType.WINE_COMPATIBILITY, "Experimental"),
        OsProfile("windows-vista", "Windows Vista", OsFamily.WINDOWS, "Vista", "Perfil legado.", EngineType.WINE_COMPATIBILITY, "Experimental"),
        OsProfile("windows-xp", "Windows XP", OsFamily.WINDOWS, "XP", "Perfil legado para programas antigos.", EngineType.WINE_COMPATIBILITY, "Experimental"),
        OsProfile("windows-2000", "Windows 2000", OsFamily.WINDOWS, "2000", "Perfil legado.", EngineType.WINE_COMPATIBILITY, "Experimental"),

        OsProfile("ubuntu-24", "Ubuntu 24.04 LTS", OsFamily.LINUX, "24.04 LTS", "Distribuição Linux baseada em Debian.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("ubuntu-22", "Ubuntu 22.04 LTS", OsFamily.LINUX, "22.04 LTS", "Distribuição Linux de suporte prolongado.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("debian-12", "Debian 12", OsFamily.LINUX, "12", "Distribuição Linux estável.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("debian-13", "Debian 13", OsFamily.LINUX, "13", "Distribuição Linux atual.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("mint-22", "Linux Mint 22", OsFamily.LINUX, "22", "Distribuição desktop baseada em Ubuntu.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("fedora-41", "Fedora 41", OsFamily.LINUX, "41", "Distribuição Linux com pacotes recentes.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("arch", "Arch Linux", OsFamily.LINUX, "Rolling", "Distribuição rolling release.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("manjaro", "Manjaro", OsFamily.LINUX, "Rolling", "Distribuição baseada em Arch.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("alpine", "Alpine Linux", OsFamily.LINUX, "3.x", "Distribuição leve.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("opensuse", "openSUSE Tumbleweed", OsFamily.LINUX, "Rolling", "Distribuição rolling release.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("kali", "Kali Linux", OsFamily.LINUX, "Rolling", "Distribuição Linux especializada.", EngineType.LINUX_USERLAND, "Perfil"),
        OsProfile("raspios", "Raspberry Pi OS", OsFamily.LINUX, "Bookworm", "Distribuição Linux baseada em Debian.", EngineType.LINUX_USERLAND, "Perfil"),

        OsProfile("freebsd", "FreeBSD", OsFamily.BSD, "14.x", "Sistema BSD; precisa de virtualização de sistema completa.", EngineType.FULL_SYSTEM_VIRTUALIZATION, "Não integrado"),
        OsProfile("openbsd", "OpenBSD", OsFamily.BSD, "7.x", "Sistema BSD; precisa de virtualização de sistema completa.", EngineType.FULL_SYSTEM_VIRTUALIZATION, "Não integrado"),
        OsProfile("netbsd", "NetBSD", OsFamily.BSD, "10.x", "Sistema BSD; precisa de virtualização de sistema completa.", EngineType.FULL_SYSTEM_VIRTUALIZATION, "Não integrado"),

        OsProfile("freedos", "FreeDOS", OsFamily.OTHER, "1.x", "Sistema compatível com DOS; requer emulador DOS.", EngineType.DOS_EMULATION, "Planejado"),
        OsProfile("reactos", "ReactOS", OsFamily.OTHER, "0.4.x", "Sistema experimental; exige virtualização completa.", EngineType.FULL_SYSTEM_VIRTUALIZATION, "Não integrado"),
        OsProfile("haiku", "Haiku", OsFamily.OTHER, "R1", "Sistema inspirado no BeOS; exige virtualização completa.", EngineType.FULL_SYSTEM_VIRTUALIZATION, "Não integrado"),
        OsProfile("android-x86", "Android-x86", OsFamily.OTHER, "9.x", "Imagem de outro sistema Android; exige virtualização completa.", EngineType.FULL_SYSTEM_VIRTUALIZATION, "Não integrado"),
        OsProfile("solaris", "Solaris / illumos", OsFamily.OTHER, "Compatível", "Sistemas Unix; exigem virtualização completa.", EngineType.FULL_SYSTEM_VIRTUALIZATION, "Não integrado")
    )

    fun byFamily(family: OsFamily): List<OsProfile> = profiles.filter { it.family == family }
    fun find(id: String): OsProfile? = profiles.firstOrNull { it.id == id }
}

enum class CpuArchitecture(val label: String) {
    ARM64("ARM64 nativo"),
    X86_64("x86-64"),
    X86("x86 32-bit")
}

enum class GraphicsProfile(val label: String) {
    AUTO("Automático"),
    VIRGL("VirGL"),
    ZINK("Zink"),
    SOFTWARE("Software"),
    TURNIP("Turnip / Adreno")
}

enum class MouseMode(val label: String) {
    TOUCHPAD("Touchpad"),
    DIRECT("Direto na tela")
}

enum class AudioMode(val label: String) {
    AUTO("Automático"),
    OPENSLES("OpenSL ES"),
    AAUDIO("AAudio"),
    DISABLED("Desativado")
}

enum class NetworkMode(val label: String) {
    NAT("NAT"),
    DISABLED("Desativada")
}

enum class WinePreset(val label: String) {
    STABLE("Wine Stable"),
    STAGING("Wine Staging"),
    GE("Wine-GE")
}

enum class Box64Preset(val label: String) {
    COMPATIBILITY("Compatibilidade"),
    BALANCED("Equilibrado"),
    PERFORMANCE("Desempenho")
}

data class ContainerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val osProfileId: String,
    val architecture: CpuArchitecture = CpuArchitecture.X86_64,
    val graphics: GraphicsProfile = GraphicsProfile.AUTO,
    val resolution: String = "1280x720",
    val ramMb: Int = 1024,
    val cpuCores: Int = 2,
    val diskGb: Int = 8,
    val dpi: Int = 120,
    val fullscreen: Boolean = true,
    val virtualDesktop: Boolean = true,
    val audio: AudioMode = AudioMode.AUTO,
    val network: NetworkMode = NetworkMode.NAT,
    val mouseMode: MouseMode = MouseMode.TOUCHPAD,
    val vsync: Boolean = true,
    val dxvkEnabled: Boolean = false,
    val vkd3dEnabled: Boolean = false,
    val esyncEnabled: Boolean = true,
    val fsyncEnabled: Boolean = false,
    val wow64Enabled: Boolean = true,
    val winePreset: WinePreset = WinePreset.STABLE,
    val box64Preset: Box64Preset = Box64Preset.BALANCED,
    val graphicsMemoryMb: Int = 512,
    val fpsLimit: Int = 60,
    val locale: String = "pt_BR",
    val environmentVariables: String = "",
    val sharedFolderPath: String = ""
) {
    val profile: OsProfile?
        get() = OsCatalog.find(osProfileId)

    fun validate(): String? {
        if (name.trim().isEmpty()) return "Digite um nome para o ambiente."
        if (name.length > 32) return "O nome deve ter no máximo 32 caracteres."
        if (profile == null) return "Selecione um sistema válido."
        if (ramMb !in 256..8192) return "A memória precisa ficar entre 256 MB e 8192 MB."
        if (cpuCores !in 1..8) return "A quantidade de núcleos deve ficar entre 1 e 8."
        if (diskGb !in 2..128) return "O armazenamento deve ficar entre 2 GB e 128 GB."
        if (dpi !in 80..240) return "O DPI deve ficar entre 80 e 240."
        if (graphicsMemoryMb !in 128..2048) return "A memória gráfica virtual deve ficar entre 128 MB e 2048 MB."
        if (fpsLimit !in supportedFpsLimits) return "Selecione um limite de FPS válido."
        if (resolution !in supportedResolutions) return "Selecione uma resolução válida."
        if (environmentVariables.length > 2048) return "As variáveis de ambiente ultrapassaram 2048 caracteres."
        if (sharedFolderPath.length > 512) return "O caminho da pasta compartilhada é muito longo."
        if (locale !in supportedLocales) return "Selecione um idioma válido."
        return null
    }

    companion object {
        val supportedResolutions = listOf("800x600", "960x540", "1024x768", "1280x720", "1366x768", "1600x900", "1920x1080")
        val supportedLocales = listOf("pt_BR", "en_US", "es_ES", "fr_FR", "de_DE")
        val supportedFpsLimits = listOf(30, 45, 60, 90, 120)
    }
}
