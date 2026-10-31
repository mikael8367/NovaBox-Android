package com.mikael.novabox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mikael.novabox.model.AudioMode
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.CpuArchitecture
import com.mikael.novabox.model.EngineType
import com.mikael.novabox.model.GraphicsProfile
import com.mikael.novabox.model.MouseMode
import com.mikael.novabox.model.NetworkMode
import com.mikael.novabox.model.OsCatalog
import com.mikael.novabox.model.OsFamily
import com.mikael.novabox.model.WinePreset

@Composable
fun CreateEnvironmentDialog(
    initialConfig: ContainerConfig?,
    onDismiss: () -> Unit,
    onSave: (ContainerConfig) -> Unit
) {
    val key = initialConfig?.id ?: "new"
    var name by rememberSaveable(key) { mutableStateOf(initialConfig?.name.orEmpty()) }
    var family by rememberSaveable(key) { mutableStateOf(initialConfig?.profile?.family ?: OsFamily.WINDOWS) }
    var profileId by rememberSaveable(key) { mutableStateOf(initialConfig?.osProfileId ?: "windows-10") }
    var architecture by rememberSaveable(key) { mutableStateOf(initialConfig?.architecture ?: CpuArchitecture.X86_64) }
    var graphics by rememberSaveable(key) { mutableStateOf(initialConfig?.graphics ?: GraphicsProfile.AUTO) }
    var resolution by rememberSaveable(key) { mutableStateOf(initialConfig?.resolution ?: "1280x720") }
    var ramMb by rememberSaveable(key) { mutableStateOf(initialConfig?.ramMb ?: 1024) }
    var cpuCores by rememberSaveable(key) { mutableStateOf(initialConfig?.cpuCores ?: 2) }
    var diskGb by rememberSaveable(key) { mutableStateOf(initialConfig?.diskGb ?: 8) }
    var dpi by rememberSaveable(key) { mutableStateOf(initialConfig?.dpi ?: 120) }
    var fullscreen by rememberSaveable(key) { mutableStateOf(initialConfig?.fullscreen ?: true) }
    var virtualDesktop by rememberSaveable(key) { mutableStateOf(initialConfig?.virtualDesktop ?: true) }
    var audio by rememberSaveable(key) { mutableStateOf(initialConfig?.audio ?: AudioMode.AUTO) }
    var network by rememberSaveable(key) { mutableStateOf(initialConfig?.network ?: NetworkMode.NAT) }
    var mouseMode by rememberSaveable(key) { mutableStateOf(initialConfig?.mouseMode ?: MouseMode.TOUCHPAD) }
    var vsync by rememberSaveable(key) { mutableStateOf(initialConfig?.vsync ?: true) }
    var dxvkEnabled by rememberSaveable(key) { mutableStateOf(initialConfig?.dxvkEnabled ?: false) }
    var vkd3dEnabled by rememberSaveable(key) { mutableStateOf(initialConfig?.vkd3dEnabled ?: false) }
    var esyncEnabled by rememberSaveable(key) { mutableStateOf(initialConfig?.esyncEnabled ?: true) }
    var fsyncEnabled by rememberSaveable(key) { mutableStateOf(initialConfig?.fsyncEnabled ?: false) }
    var wow64Enabled by rememberSaveable(key) { mutableStateOf(initialConfig?.wow64Enabled ?: true) }
    var winePreset by rememberSaveable(key) { mutableStateOf(initialConfig?.winePreset ?: WinePreset.STABLE) }
    var locale by rememberSaveable(key) { mutableStateOf(initialConfig?.locale ?: "pt_BR") }
    var environmentVariables by rememberSaveable(key) { mutableStateOf(initialConfig?.environmentVariables.orEmpty()) }
    var sharedFolderPath by rememberSaveable(key) { mutableStateOf(initialConfig?.sharedFolderPath.orEmpty()) }
    var advanced by rememberSaveable(key) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val profiles = OsCatalog.byFamily(family)
    val selectedProfile = profiles.firstOrNull { it.id == profileId } ?: profiles.first()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.96f).heightIn(max = 740.dp)
                .clip(RoundedCornerShape(25.dp)).background(NovaPanel)
                .border(1.dp, NovaBorder, RoundedCornerShape(25.dp))
                .verticalScroll(rememberScrollState()).padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (initialConfig == null) "NOVO AMBIENTE" else "EDITAR AMBIENTE", color = NovaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(if (initialConfig == null) "Monte seu sistema" else "Personalize seu sistema", color = NovaText, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("Fechar", color = NovaMuted, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onDismiss).padding(8.dp))
            }

            FormLabel("Nome do ambiente")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(32); error = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex.: Windows para jogos", color = NovaMuted, fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = NovaPanelRaised,
                    unfocusedContainerColor = NovaPanelRaised,
                    focusedTextColor = NovaText,
                    unfocusedTextColor = NovaText,
                    focusedIndicatorColor = NovaGreen,
                    unfocusedIndicatorColor = NovaBorder,
                    cursorColor = NovaGreen
                )
            )

            FormLabel("Família do sistema")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OsFamily.entries.forEach { option ->
                    SelectableChip(
                        text = option.label,
                        selected = family == option,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            family = option
                            profileId = OsCatalog.byFamily(option).first().id
                            architecture = if (option == OsFamily.LINUX) CpuArchitecture.ARM64 else CpuArchitecture.X86_64
                            error = null
                        }
                    )
                }
            }

            FormLabel("Versão ou distribuição")
            profiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEach { profile ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (profile.id == selectedProfile.id) Color(0xFF203326) else NovaPanelRaised)
                                .border(1.dp, if (profile.id == selectedProfile.id) Color(0xFF4F9F35) else NovaBorder, RoundedCornerShape(12.dp))
                                .clickable { profileId = profile.id }.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.name, color = if (profile.id == selectedProfile.id) NovaGreen else NovaText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (profile.id == selectedProfile.id) Icon(Icons.Default.Check, contentDescription = null, tint = NovaGreen, modifier = Modifier.padding(start = 3.dp))
                            }
                            Text(profile.maturity, color = NovaMuted, fontSize = 9.sp)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            InfoNotice(
                title = when (selectedProfile.engine) {
                    EngineType.WINE_COMPATIBILITY -> "Perfil de compatibilidade Windows"
                    EngineType.LINUX_USERLAND -> "Ambiente Linux de usuário"
                    EngineType.DOS_EMULATION -> "Emulação DOS"
                    EngineType.FULL_SYSTEM_VIRTUALIZATION -> "Virtualização completa necessária"
                    EngineType.UNSUPPORTED -> "Motor não disponível"
                },
                body = when (selectedProfile.engine) {
                    EngineType.WINE_COMPATIBILITY -> "Este perfil configura o Wine para aplicativos Windows. Não instala nem inicializa uma cópia completa do Windows."
                    EngineType.LINUX_USERLAND -> "Este perfil foi pensado para um ambiente Linux de usuário via PRoot. Não inicia um kernel ou desktop Linux completo."
                    EngineType.DOS_EMULATION -> "Este sistema depende de um emulador DOS. O motor ainda não foi integrado."
                    EngineType.FULL_SYSTEM_VIRTUALIZATION -> "Este sistema exige QEMU ou outro virtualizador de máquina completa. Essa integração ainda não existe no app."
                    EngineType.UNSUPPORTED -> "Não há motor de execução configurado para este sistema."
                },
                warning = selectedProfile.engine == EngineType.FULL_SYSTEM_VIRTUALIZATION || selectedProfile.engine == EngineType.UNSUPPORTED
            )

            FormLabel("Arquitetura do programa")
            ChoiceGrid(
                options = CpuArchitecture.entries.map { it.label to it },
                selected = architecture,
                onSelect = { architecture = it }
            )

            FormLabel("Resolução")
            HorizontalChoices(
                options = ContainerConfig.supportedResolutions,
                selected = resolution,
                onSelect = { resolution = it }
            )

            FormLabel("Memória RAM: $ramMb MB")
            Slider(
                value = ramMb.toFloat(),
                onValueChange = { ramMb = it.toInt().coerceIn(256, 8192) },
                valueRange = 256f..8192f,
                steps = 30
            )

            FormLabel("Núcleos de CPU: $cpuCores")
            Slider(
                value = cpuCores.toFloat(),
                onValueChange = { cpuCores = it.toInt().coerceIn(1, 8) },
                valueRange = 1f..8f,
                steps = 6
            )

            FormLabel("Armazenamento virtual: $diskGb GB")
            Slider(
                value = diskGb.toFloat(),
                onValueChange = { diskGb = it.toInt().coerceIn(2, 128) },
                valueRange = 2f..128f,
                steps = 24
            )

            FormLabel("Densidade da tela: $dpi DPI")
            ChoiceGrid(
                options = listOf(80, 96, 120, 160, 200, 240).map { "$it DPI" to it },
                selected = dpi,
                onSelect = { dpi = it }
            )

            FormLabel("Renderização gráfica")
            ChoiceGrid(
                options = GraphicsProfile.entries.map { it.label to it },
                selected = graphics,
                onSelect = { graphics = it }
            )
            if (graphics == GraphicsProfile.TURNIP) {
                InfoNotice("Atenção ao driver", "Turnip foi feito para GPUs Qualcomm Adreno compatíveis. Em outras GPUs, escolha Automático ou outro backend.", warning = true)
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(NovaPanelRaised)
                    .clickable { advanced = !advanced }.padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Configurações avançadas", color = NovaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Áudio, rede, mouse, DXVK, Wine e variáveis", color = NovaMuted, fontSize = 10.sp)
                }
                Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = NovaGreen)
            }

            if (advanced) {
                FormLabel("Áudio")
                ChoiceGrid(AudioMode.entries.map { it.label to it }, audio, onSelect = { audio = it })

                FormLabel("Rede")
                ChoiceGrid(NetworkMode.entries.map { it.label to it }, network, onSelect = { network = it })

                FormLabel("Controle do mouse")
                ChoiceGrid(MouseMode.entries.map { it.label to it }, mouseMode, onSelect = { mouseMode = it })

                FormLabel("Idioma do ambiente")
                ChoiceGrid(
                    listOf("Português" to "pt_BR", "Inglês" to "en_US", "Espanhol" to "es_ES", "Francês" to "fr_FR", "Alemão" to "de_DE"),
                    locale,
                    onSelect = { locale = it }
                )

                if (selectedProfile.engine == EngineType.WINE_COMPATIBILITY) {
                    FormLabel("Versão do Wine desejada")
                    ChoiceGrid(WinePreset.entries.map { it.label to it }, winePreset, onSelect = { winePreset = it })
                }

                FormLabel("Opções de vídeo e compatibilidade")
                SwitchSetting("Tela cheia", fullscreen) { fullscreen = it }
                SwitchSetting("Desktop virtual do Wine", virtualDesktop) { virtualDesktop = it }
                SwitchSetting("VSync", vsync) { vsync = it }
                if (selectedProfile.engine == EngineType.WINE_COMPATIBILITY) {
                    SwitchSetting("DXVK (Direct3D 9/10/11)", dxvkEnabled) { dxvkEnabled = it }
                    SwitchSetting("VKD3D-Proton (Direct3D 12)", vkd3dEnabled) { vkd3dEnabled = it }
                    SwitchSetting("ESync", esyncEnabled) { esyncEnabled = it }
                    SwitchSetting("FSync", fsyncEnabled) { fsyncEnabled = it }
                    SwitchSetting("WoW64", wow64Enabled) { wow64Enabled = it }
                }

                FormLabel("Variáveis de ambiente (opcional)")
                OutlinedTextField(
                    value = environmentVariables,
                    onValueChange = { environmentVariables = it.take(2048) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex.: MESA_GL_VERSION_OVERRIDE=4.5", color = NovaMuted, fontSize = 10.sp) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NovaPanelRaised,
                        unfocusedContainerColor = NovaPanelRaised,
                        focusedTextColor = NovaText,
                        unfocusedTextColor = NovaText,
                        focusedIndicatorColor = NovaGreen,
                        unfocusedIndicatorColor = NovaBorder,
                        cursorColor = NovaGreen
                    )
                )

                FormLabel("Pasta compartilhada (caminho opcional)")
                OutlinedTextField(
                    value = sharedFolderPath,
                    onValueChange = { sharedFolderPath = it.take(512) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Será configurada quando o motor estiver integrado", color = NovaMuted, fontSize = 10.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NovaPanelRaised,
                        unfocusedContainerColor = NovaPanelRaised,
                        focusedTextColor = NovaText,
                        unfocusedTextColor = NovaText,
                        focusedIndicatorColor = NovaGreen,
                        unfocusedIndicatorColor = NovaBorder,
                        cursorColor = NovaGreen
                    )
                )
            }

            error?.let { Text(it, color = Color(0xFFFF8A80), fontSize = 11.sp) }

            PrimaryAction(
                text = if (initialConfig == null) "Salvar ambiente" else "Salvar alterações",
                icon = Icons.Default.Check,
                onClick = {
                    val config = ContainerConfig(
                        id = initialConfig?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        osProfileId = selectedProfile.id,
                        architecture = architecture,
                        graphics = graphics,
                        resolution = resolution,
                        ramMb = ramMb,
                        cpuCores = cpuCores,
                        diskGb = diskGb,
                        dpi = dpi,
                        fullscreen = fullscreen,
                        virtualDesktop = virtualDesktop,
                        audio = audio,
                        network = network,
                        mouseMode = mouseMode,
                        vsync = vsync,
                        dxvkEnabled = dxvkEnabled,
                        vkd3dEnabled = vkd3dEnabled,
                        esyncEnabled = esyncEnabled,
                        fsyncEnabled = fsyncEnabled,
                        wow64Enabled = wow64Enabled,
                        winePreset = winePreset,
                        locale = locale,
                        environmentVariables = environmentVariables,
                        sharedFolderPath = sharedFolderPath
                    )
                    error = config.validate()
                    if (error == null) onSave(config)
                }
            )
            Text("Perfis não baixam imagens do sistema e não instalam motores automaticamente.", color = NovaMuted, fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, color = NovaMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun <T> ChoiceGrid(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    options.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { (label, value) ->
                SelectableChip(
                    text = label,
                    selected = selected == value,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(value) }
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun <T> HorizontalChoices(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            SelectableChip(
                text = option.toString(),
                selected = selected == option,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NovaPanelRaised)
            .clickable { onChange(!checked) }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NovaText, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
