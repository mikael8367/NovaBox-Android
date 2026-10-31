package com.mikael.novabox.model

import java.util.UUID

enum class OsFamily(val label: String) {
    WINDOWS("Windows"),
    LINUX("Linux"),
    OTHER("Outros")
}

data class OsProfile(
    val id: String,
    val name: String,
    val family: OsFamily,
    val release: String,
    val description: String,
    val maturity: String
)

object OsCatalog {
    val profiles = listOf(
        OsProfile("windows-11", "Windows 11", OsFamily.WINDOWS, "11", "Perfil de compatibilidade para aplicativos modernos.", "Experimental"),
        OsProfile("windows-10", "Windows 10", OsFamily.WINDOWS, "10", "Perfil de compatibilidade para aplicativos atuais.", "Experimental"),
        OsProfile("windows-8-1", "Windows 8.1", OsFamily.WINDOWS, "8.1", "Perfil legado para aplicativos dessa geração.", "Experimental"),
        OsProfile("windows-8", "Windows 8", OsFamily.WINDOWS, "8", "Perfil legado.", "Experimental"),
        OsProfile("windows-7", "Windows 7", OsFamily.WINDOWS, "7", "Perfil legado para programas antigos.", "Experimental"),
        OsProfile("windows-vista", "Windows Vista", OsFamily.WINDOWS, "Vista", "Perfil legado.", "Experimental"),
        OsProfile("windows-xp", "Windows XP", OsFamily.WINDOWS, "XP", "Perfil legado para programas antigos.", "Experimental"),
        OsProfile("windows-2000", "Windows 2000", OsFamily.WINDOWS, "2000", "Perfil legado.", "Experimental"),

        OsProfile("ubuntu-24", "Ubuntu 24.04 LTS", OsFamily.LINUX, "24.04 LTS", "Distribuição Linux baseada em Debian.", "Perfil"),
        OsProfile("debian-12", "Debian 12", OsFamily.LINUX, "12", "Distribuição Linux estável.", "Perfil"),
        OsProfile("fedora-41", "Fedora 41", OsFamily.LINUX, "41", "Distribuição Linux com pacotes recentes.", "Perfil"),
        OsProfile("arch", "Arch Linux", OsFamily.LINUX, "Rolling", "Distribuição rolling release.", "Perfil"),
        OsProfile("mint-22", "Linux Mint 22", OsFamily.LINUX, "22", "Distribuição desktop baseada em Ubuntu.", "Perfil"),
        OsProfile("alpine-3", "Alpine Linux", OsFamily.LINUX, "3.x", "Distribuição compacta.", "Perfil"),
        OsProfile("opensuse", "openSUSE Tumbleweed", OsFamily.LINUX, "Rolling", "Distribuição rolling release.", "Perfil"),
        OsProfile("manjaro", "Manjaro", OsFamily.LINUX, "Rolling", "Distribuição baseada em Arch.", "Perfil"),

        OsProfile("freedos", "FreeDOS", OsFamily.OTHER, "1.x", "Sistema compatível com DOS; requer emulador próprio.", "Não integrado"),
        OsProfile("reactos", "ReactOS", OsFamily.OTHER, "0.4.x", "Sistema experimental; não possui motor de inicialização neste app.", "Não integrado"),
        OsProfile("android-x86", "Android-x86", OsFamily.OTHER, "9.x", "Imagem de outro sistema Android; exige virtualização própria.", "Não integrado")
    )

    fun byFamily(family: OsFamily): List<OsProfile> = profiles.filter { it.family == family }
    fun find(id: String): OsProfile? = profiles.firstOrNull { it.id == id }
}

enum class CpuArchitecture(val label: String) {
    X86_64("x86-64"),
    X86("x86 (32-bit)"),
    ARM64("ARM64")
}

enum class GraphicsProfile(val label: String) {
    AUTO("Automático"),
    VIRGL("VirGL"),
    ZINK("Zink"),
    SOFTWARE("Renderização por software"),
    TURNIP("Turnip (Adreno)")
}

data class ContainerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val osProfileId: String,
    val architecture: CpuArchitecture = CpuArchitecture.X86_64,
    val graphics: GraphicsProfile = GraphicsProfile.AUTO,
    val resolution: String = "1280x720",
    val ramMb: Int = 1024
) {
    val profile: OsProfile?
        get() = OsCatalog.find(osProfileId)

    fun validate(): String? {
        if (name.trim().isEmpty()) return "Digite um nome para o ambiente."
        if (name.length > 32) return "O nome deve ter no máximo 32 caracteres."
        if (profile == null) return "Selecione um sistema válido."
        if (ramMb !in 512..2048) return "A memória precisa ficar entre 512 MB e 2048 MB."
        if (resolution !in supportedResolutions) return "Selecione uma resolução válida."
        return null
    }

    companion object {
        val supportedResolutions = listOf("800x600", "960x540", "1280x720", "1366x768", "1920x1080")
    }
}
