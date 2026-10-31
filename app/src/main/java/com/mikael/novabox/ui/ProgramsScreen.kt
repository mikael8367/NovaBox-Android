package com.mikael.novabox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.novabox.data.ProgramEntry
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.EngineType
import com.mikael.novabox.runtime.WineWorkspaceStatus
import com.mikael.novabox.runtime.WineWorkspaceState
import java.util.Locale

@Composable
fun ProgramsScreen(
    containers: List<ContainerConfig>,
    programs: List<ProgramEntry>,
    workspaceStatuses: Map<String, WineWorkspaceStatus>,
    runningEnvironmentIds: Set<String>,
    onLaunchProgram: (ContainerConfig, ProgramEntry) -> Unit,
    onStopWine: (ContainerConfig) -> Unit,
    onPrepareWorkspace: (ContainerConfig) -> Unit,
    onInitializePrefix: (ContainerConfig) -> Unit,
    onImport: (ContainerConfig) -> Unit,
    onDelete: (ProgramEntry) -> Unit,
    onVerify: (ProgramEntry) -> Unit,
    logContents: Map<String, String>,
    onViewLog: (ContainerConfig) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<ProgramEntry?>(null) }
    var selectedLog by remember { mutableStateOf<ContainerConfig?>(null) }
    Column(
        Modifier.fillMaxSize().background(NovaBackground).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        PageHeader("Biblioteca", "Programas", "Arquivos importados e organizados por ambiente.")
        InfoNotice(
            title = "Importação segura",
            body = "O NovaBox copia EXE, MSI e COM para o armazenamento privado do aplicativo, calcula SHA-256 e limita cada arquivo a 512 MiB. Os arquivos originais não são alterados."
        )
        InfoNotice(
            title = "Execução experimental",
            body = "O NovaBox pode iniciar o processo Box64/Wine e registrar a saída no log. A janela gráfica ainda não aparece: o servidor de tela, áudio e entrada não está integrado. Não trate um processo iniciado como um Windows funcional.",
            warning = true
        )
        if (containers.isEmpty()) {
            EmptyCard(
                Icons.Default.FolderOpen,
                "Crie um ambiente primeiro",
                "Depois você poderá importar programas e associá-los a um ambiente."
            )
        } else {
            containers.forEach { config ->
                val entries = programs.filter { it.containerId == config.id }
                Surface(
                    color = NovaPanel,
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, NovaBorder, RoundedCornerShape(17.dp))
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(config.name, color = NovaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(config.profile?.name ?: "Perfil desconhecido", color = NovaMuted, fontSize = 10.sp)
                            }
                            SmallTag("${entries.size} arquivo(s)")
                        }
                        if (config.profile?.engine == EngineType.WINE_COMPATIBILITY) {
                            val isRunning = config.id in runningEnvironmentIds
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                    .background(if (isRunning) Color(0xFF3A2522) else NovaPanelRaised)
                                    .clickable(enabled = isRunning) { onStopWine(config) }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    if (isRunning) Icons.Default.Stop else Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = if (isRunning) Color(0xFFFF9A8C) else NovaMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    if (isRunning) "Encerrar processo Wine" else "Wine parado",
                                    color = if (isRunning) Color(0xFFFF9A8C) else NovaMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val workspace = workspaceStatuses[config.id]
                            val initialized = workspace?.state == WineWorkspaceState.WINE_INITIALIZED
                            val prepared = workspace?.state == WineWorkspaceState.WORKSPACE_PREPARED
                            InfoNotice(
                                title = if (initialized) "Prefixo Wine inicializado" else if (prepared) "Estrutura Wine preparada" else "Ambiente Wine",
                                body = workspace?.message ?: "Prepare a estrutura privada deste ambiente e inicialize o prefixo com o runtime instalado.",
                                warning = !initialized
                            )
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                    .background(NovaPanelRaised).clickable { selectedLog = config; onViewLog(config) }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Abrir log do Wine", color = NovaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                    .background(NovaPanelRaised).clickable { onPrepareWorkspace(config) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    if (prepared || initialized) "Verificar estrutura" else "Preparar estrutura Wine",
                                    color = NovaGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (prepared && !initialized) {
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                        .background(Color(0xFF203326)).clickable { onInitializePrefix(config) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("Inicializar prefixo Wine", color = NovaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF203326)).clickable { onImport(config) }
                                .padding(horizontal = 13.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(7.dp))
                            Text("Importar EXE / MSI / COM", color = NovaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (entries.isEmpty()) {
                            Text("Nenhum programa importado para este ambiente.", color = NovaMuted, fontSize = 10.sp)
                        } else {
                            entries.forEach { entry ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                        .background(NovaPanelRaised).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(19.dp))
                                    Column(Modifier.weight(1f).padding(start = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(entry.displayName, color = NovaText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(
                                            "${formatProgramBytes(entry.sizeBytes)} • SHA-256 ${entry.sha256.take(12)}…",
                                            color = NovaMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Iniciar processo Wine (experimental)",
                                        tint = if (config.id in runningEnvironmentIds) NovaMuted else NovaGreen,
                                        modifier = Modifier.size(22.dp)
                                            .clickable(enabled = config.id !in runningEnvironmentIds) {
                                                onLaunchProgram(config, entry)
                                            }
                                            .padding(1.dp)
                                    )
                                    Spacer(Modifier.size(7.dp))
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = "Verificar integridade",
                                        tint = NovaGreen,
                                        modifier = Modifier.size(19.dp).clickable { onVerify(entry) }.padding(1.dp)
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remover programa",
                                        tint = Color(0xFFFF9A8C),
                                        modifier = Modifier.size(20.dp).clickable { pendingDelete = entry }.padding(1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            "O botão de iniciar executa apenas o processo experimental do Wine. Sem display integrado, programas com interface gráfica não terão janela. Os arquivos ficam no armazenamento privado do NovaBox.",
            color = NovaMuted,
            fontSize = 9.sp,
            lineHeight = 13.sp
        )
    }

    selectedLog?.let { config ->
        AlertDialog(
            onDismissRequest = { selectedLog = null },
            title = { Text("Log Wine • ${config.name}") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(logContents[config.id] ?: "Carregando log…", color = NovaMuted, fontSize = 9.sp, lineHeight = 13.sp)
                }
            },
            confirmButton = { TextButton(onClick = { selectedLog = null }) { Text("Fechar") } },
            containerColor = NovaPanel,
            titleContentColor = NovaText,
            textContentColor = NovaMuted
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remover programa?") },
            text = { Text("O arquivo privado ${entry.displayName} será excluído do NovaBox.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(entry)
                }) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
            containerColor = NovaPanel,
            titleContentColor = NovaText,
            textContentColor = NovaMuted
        )
    }
}

private fun formatProgramBytes(bytes: Long): String {
    if (bytes < 1024L * 1024L) return "${bytes / 1024L} KiB"
    return String.format(Locale.ROOT, "%.1f MiB", bytes / (1024f * 1024f))
}
