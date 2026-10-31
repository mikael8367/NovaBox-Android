package com.mikael.novabox.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.mikael.novabox.data.ProgramEntry
import com.mikael.novabox.data.ProgramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.novabox.data.ContainerRepository
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.EngineType
import com.mikael.novabox.model.OsCatalog
import com.mikael.novabox.model.OsFamily
import com.mikael.novabox.model.RuntimeId
import com.mikael.novabox.runtime.EngineAvailability
import com.mikael.novabox.runtime.RuntimeEngine
import com.mikael.novabox.runtime.WineProcessSupervisor
import com.mikael.novabox.runtime.WineLogReader
import com.mikael.novabox.runtime.WineRuntimeLauncher
import com.mikael.novabox.runtime.WineWorkspaceManager
import com.mikael.novabox.runtime.WineWorkspaceStatus

private enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Início", Icons.Default.Home),
    CONTAINERS("Ambientes", Icons.Default.Computer),
    RUNTIMES("Motores", Icons.Default.Memory),
    PROGRAMS("Programas", Icons.Default.FolderOpen),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

@Composable
fun NovaBoxApp() {
    val context = LocalContext.current
    val repository = remember { ContainerRepository(context) }
    val runtimeEngine = remember { RuntimeEngine(context) }
    val wineWorkspaceManager = remember { WineWorkspaceManager(context) }
    val programRepository = remember { ProgramRepository(context) }
    val programs = remember { mutableStateListOf<ProgramEntry>() }
    LaunchedEffect(programRepository) {
        val loadedPrograms = withContext(Dispatchers.IO) { programRepository.load() }
        programs.clear()
        programs.addAll(loadedPrograms)
    }
    val scope = rememberCoroutineScope()
    var pendingProgramContainer by remember { mutableStateOf<ContainerConfig?>(null) }
    val programPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = pendingProgramContainer
        pendingProgramContainer = null
        if (uri != null && target != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        programRepository.importProgram(target.id, uri)
                    }
                }.onSuccess { entry ->
                    programs.removeAll { it.id == entry.id }
                    programs.add(0, entry)
                    Toast.makeText(context, "Programa importado e verificado.", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Não foi possível importar o programa.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    val containers = remember {
        mutableStateListOf<ContainerConfig>().apply { addAll(repository.load()) }
    }
    val workspaceStatuses = remember {
        mutableStateMapOf<String, WineWorkspaceStatus>().apply {
            containers.forEach { config -> put(config.id, wineWorkspaceManager.inspect(config)) }
        }
    }
    val runningWine = remember {
        mutableStateMapOf<String, Boolean>().apply {
            containers.forEach { config -> put(config.id, WineProcessSupervisor.isRunning(config.id)) }
        }
    }
    val wineLogs = remember { mutableStateMapOf<String, String>() }


    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    LaunchedEffect(selectedTab, containers.map { it.id }) {
        while (selectedTab == MainTab.PROGRAMS) {
            val snapshot = withContext(Dispatchers.IO) {
                containers.associate { config -> config.id to WineProcessSupervisor.isRunning(config.id) }
            }
            runningWine.clear()
            runningWine.putAll(snapshot)
            delay(900)
        }
    }
    var activePreview by remember { mutableStateOf<ContainerConfig?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ContainerConfig?>(null) }

    val colorScheme = darkColorScheme(
        primary = NovaGreen,
        onPrimary = NovaBackground,
        background = NovaBackground,
        onBackground = NovaText,
        surface = NovaPanel,
        onSurface = NovaText,
        secondary = NovaGreen,
        onSecondary = NovaBackground
    )

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = NovaBackground) {
            if (activePreview != null) {
                DesktopPreviewScreen(
                    config = activePreview!!,
                    onBack = { activePreview = null }
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 22 }) togetherWith fadeOut()
                        },
                        label = "bottom_tab_transition",
                        modifier = Modifier.weight(1f)
                    ) { tab ->
                        when (tab) {
                            MainTab.HOME -> HomeScreen(
                                containers = containers,
                                runtimeEngine = runtimeEngine,
                                onCreate = {
                                    editingConfig = null
                                    showEditor = true
                                },
                                onOpenContainers = { selectedTab = MainTab.CONTAINERS },
                                onOpenPreview = { activePreview = it }
                            )
                            MainTab.CONTAINERS -> ContainersScreen(
                                containers = containers,
                                runtimeEngine = runtimeEngine,
                                onCreate = {
                                    editingConfig = null
                                    showEditor = true
                                },
                                onEdit = {
                                    editingConfig = it
                                    showEditor = true
                                },
                                onOpenPreview = { activePreview = it },
                                onDelete = { config ->
                                    val attachedPrograms = programs.filter { it.containerId == config.id }
                                    scope.launch {
                                        val processStopped = withContext(Dispatchers.IO) {
                                            !WineProcessSupervisor.isRunning(config.id) ||
                                                WineProcessSupervisor.stop(config.id)
                                        }
                                        runningWine[config.id] = WineProcessSupervisor.isRunning(config.id)
                                        if (!processStopped) {
                                            Toast.makeText(
                                                context,
                                                "O processo Wine não encerrou. O ambiente foi preservado para evitar apagar arquivos em uso.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                        val deletionResults = withContext(Dispatchers.IO) {
                                            attachedPrograms.map { entry -> entry.id to programRepository.delete(entry) }
                                        }
                                        val deletedIds = deletionResults.filter { it.second }.map { it.first }.toSet()
                                        programs.removeAll { it.id in deletedIds }
                                        if (deletionResults.all { it.second }) {
                                            val workspaceRemoved = withContext(Dispatchers.IO) {
                                                wineWorkspaceManager.delete(config.id)
                                            }
                                            if (workspaceRemoved) {
                                                workspaceStatuses.remove(config.id)
                                                containers.removeAll { item -> item.id == config.id }
                                                repository.save(containers)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "A estrutura privada não pôde ser removida. O ambiente foi preservado.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Alguns programas não puderam ser removidos. O ambiente foi preservado.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                            MainTab.RUNTIMES -> RuntimesScreen(runtimeEngine)
                            MainTab.PROGRAMS -> ProgramsScreen(
                                containers = containers,
                                programs = programs,
                                workspaceStatuses = workspaceStatuses,
                                runningEnvironmentIds = runningWine.filterValues { it }.keys,
                                onLaunchProgram = { config, entry ->
                                    scope.launch {
                                        try {
                                            val process = withContext(Dispatchers.IO) {
                                                WineRuntimeLauncher(context).launchProgram(config, entry)
                                            }
                                            runningWine[config.id] = true
                                            Toast.makeText(
                                                context,
                                                "Processo Box64/Wine iniciado. A janela gráfica ainda não está integrada.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            while (process.isAlive) delay(400)
                                            runningWine[config.id] = false
                                            Toast.makeText(
                                                context,
                                                "Processo Wine encerrado (código ${process.exitValue()}). Consulte wine-session.log.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (error: Exception) {
                                            runningWine[config.id] = false
                                            Toast.makeText(
                                                context,
                                                error.message ?: "Não foi possível iniciar o processo Wine.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onStopWine = { config ->
                                    scope.launch {
                                        val stopped = withContext(Dispatchers.IO) {
                                            WineProcessSupervisor.stop(config.id)
                                        }
                                        runningWine[config.id] = WineProcessSupervisor.isRunning(config.id)
                                        Toast.makeText(
                                            context,
                                            if (stopped) "Processo Wine encerrado." else "Não foi possível encerrar o processo Wine.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onPrepareWorkspace = { config ->
                                    scope.launch {
                                        val status = withContext(Dispatchers.IO) {
                                            wineWorkspaceManager.prepare(config)
                                        }
                                        workspaceStatuses[config.id] = status
                                        Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                onInitializePrefix = { config ->
                                    scope.launch {
                                        try {
                                            val exitCode = withContext(Dispatchers.IO) {
                                                WineRuntimeLauncher(context).initializePrefix(config).waitFor()
                                            }
                                            val status = withContext(Dispatchers.IO) {
                                                if (exitCode == 0) wineWorkspaceManager.markWineBootCompleted(config.id)
                                                wineWorkspaceManager.inspect(config)
                                            }
                                            workspaceStatuses[config.id] = status
                                            Toast.makeText(
                                                context,
                                                if (exitCode == 0 && status.state.name == "WINE_INITIALIZED") "Prefixo Wine inicializado."
                                                else "wineboot terminou com código $exitCode. Confira o log do ambiente.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (error: Exception) {
                                            Toast.makeText(context, error.message ?: "Não foi possível inicializar o Wine.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onImport = { config ->
                                    pendingProgramContainer = config
                                    programPicker.launch(arrayOf("*/*"))
                                },
                                onDelete = { entry ->
                                    scope.launch {
                                        val removed = withContext(Dispatchers.IO) {
                                            programRepository.delete(entry)
                                        }
                                        if (removed) programs.removeAll { it.id == entry.id }
                                        else Toast.makeText(context, "Não foi possível remover o arquivo.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onVerify = { entry ->
                                    scope.launch {
                                        val valid = withContext(Dispatchers.IO) {
                                            programRepository.verifyIntegrity(entry)
                                        }
                                        Toast.makeText(
                                            context,
                                            if (valid) "Integridade confirmada: SHA-256 corresponde ao arquivo."
                                            else "Falha na integridade: o arquivo foi alterado, está incompleto ou não está acessível.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                logContents = wineLogs,
                                onViewLog = { config ->
                                    scope.launch {
                                        wineLogs[config.id] = withContext(Dispatchers.IO) {
                                            runCatching {
                                                WineLogReader.readTail(
                                                    java.io.File(context.filesDir, "wine-workspaces"),
                                                    config.id
                                                )
                                            }.getOrElse { error ->
                                                error.message ?: "Não foi possível ler o log."
                                            }
                                        }
                                    }
                                }
                            )
                            MainTab.SETTINGS -> SettingsScreen(
                                containerCount = containers.size,
                                onOpenRuntimes = { selectedTab = MainTab.RUNTIMES }
                            )
                        }
                    }
                    BottomNavigation(
                        selected = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                }
            }
        }
    }

    if (showEditor) {
        CreateEnvironmentDialog(
            initialConfig = editingConfig,
            onDismiss = {
                showEditor = false
                editingConfig = null
            },
            onSave = { config ->
                val index = containers.indexOfFirst { it.id == config.id }
                if (index >= 0) containers[index] = config else containers.add(0, config)
                repository.save(containers)
                showEditor = false
                editingConfig = null
                selectedTab = MainTab.CONTAINERS
            }
        )
    }
}

@Composable
private fun BottomNavigation(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF0D140F))
            .border(1.dp, NovaBorder)
            .navigationBarsPadding()
            .padding(horizontal = 9.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val background by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF203326) else Color.Transparent,
                label = "nav_item_background"
            )
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(background).clickable { onSelect(tab) }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.title,
                    tint = if (isSelected) NovaGreen else NovaMuted,
                    modifier = Modifier.size(21.dp)
                )
                Text(
                    tab.title,
                    color = if (isSelected) NovaGreen else NovaMuted,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Box(
                    Modifier.padding(top = 1.dp).size(width = if (isSelected) 18.dp else 5.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) NovaGreen else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    containers: List<ContainerConfig>,
    runtimeEngine: RuntimeEngine,
    onCreate: () -> Unit,
    onOpenContainers: () -> Unit,
    onOpenPreview: (ContainerConfig) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(NovaBackground).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(19.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NOVABOX", color = NovaGreen, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    SmallTag("BETA")
                }
                Text("Seu espaço.\nDo seu jeito.", color = NovaText, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Seus ambientes e sistemas em um só lugar.", color = NovaMuted, fontSize = 12.sp)
            }
            Box(
                Modifier.size(68.dp).clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1C3222)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(37.dp))
            }
        }

        Surface(
            color = Color(0xFF142219),
            shape = RoundedCornerShape(23.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, NovaBorder, RoundedCornerShape(23.dp))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("COMECE POR AQUI", color = NovaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Add, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(17.dp))
                }
                Text("Crie seu próximo ambiente.", color = NovaText, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                Text(
                    "Escolha uma família de sistema e personalize memória, CPU, tela, áudio, rede, gráficos e controles.",
                    color = NovaMuted, fontSize = 12.sp, lineHeight = 18.sp
                )
                PrimaryAction("Criar ambiente", Icons.Default.Add, onClick = onCreate)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DashboardMetric(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Storage,
                value = containers.size.toString(),
                label = "Ambientes"
            )
            DashboardMetric(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Memory,
                value = OsCatalog.profiles.size.toString(),
                label = "Perfis de sistema"
            )
            DashboardMetric(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Terminal,
                value = RuntimeId.entries.size.toString(),
                label = "Motores"
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Seus ambientes", color = NovaText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text("Ver todos", color = NovaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpenContainers))
        }

        if (containers.isEmpty()) {
            EmptyCard(
                icon = Icons.Default.FolderOpen,
                title = "Nenhum ambiente criado",
                subtitle = "Crie um perfil para começar a personalizar seu espaço."
            )
        } else {
            containers.take(2).forEach { config ->
                ContainerCard(
                    config = config,
                    runtimeEngine = runtimeEngine,
                    onOpen = { onOpenPreview(config) },
                    onEdit = null,
                    onDelete = null
                )
            }
        }

        InfoNotice(
            title = "Importante sobre compatibilidade",
            body = "Os perfis organizam as configurações. Para iniciar sistemas completos, ainda são necessários motores e imagens compatíveis. O app informa quando um componente não está integrado.",
            warning = true
        )
    }
}

@Composable
private fun DashboardMetric(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Surface(modifier = modifier, color = NovaPanel, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(17.dp))
            Text(value, color = NovaText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = NovaMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ContainersScreen(
    containers: List<ContainerConfig>,
    runtimeEngine: RuntimeEngine,
    onCreate: () -> Unit,
    onEdit: (ContainerConfig) -> Unit,
    onOpenPreview: (ContainerConfig) -> Unit,
    onDelete: (ContainerConfig) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(NovaBackground).statusBarsPadding()
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        PageHeader(
            eyebrow = "Biblioteca",
            title = "Ambientes",
            subtitle = "Configure e organize seus sistemas.",
            trailing = {
                Box(
                    Modifier.size(43.dp).clip(RoundedCornerShape(14.dp))
                        .background(NovaGreen).clickable(onClick = onCreate),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Criar ambiente", tint = NovaBackground)
                }
            }
        )
        if (containers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyCard(
                    Icons.Default.FolderOpen,
                    "Sua biblioteca está vazia",
                    "Toque no botão verde para criar o primeiro ambiente."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(containers, key = { it.id }) { config ->
                    ContainerCard(
                        config = config,
                        runtimeEngine = runtimeEngine,
                        onOpen = { onOpenPreview(config) },
                        onEdit = { onEdit(config) },
                        onDelete = { onDelete(config) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContainerCard(
    config: ContainerConfig,
    runtimeEngine: RuntimeEngine,
    onOpen: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val profile = config.profile
    val availability = runtimeEngine.inspect(config)
    val familyColor = when (profile?.family) {
        OsFamily.LINUX, OsFamily.BSD -> Color(0xFF85C7FF)
        OsFamily.ANDROID -> Color(0xFF9BFF62)
        else -> NovaGreen
    }
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, NovaBorder, RoundedCornerShape(18.dp)),
        color = NovaPanel,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF203326)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (profile?.family) {
                            OsFamily.LINUX, OsFamily.BSD -> Icons.Default.Terminal
                            OsFamily.ANDROID -> Icons.Default.Android
                            else -> Icons.Default.DesktopWindows
                        },
                        contentDescription = null,
                        tint = familyColor
                    )
                }
                Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(config.name, color = NovaText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile?.name ?: "Sistema desconhecido", color = NovaMuted, fontSize = 10.sp)
                }
                if (onDelete != null) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Excluir ambiente",
                        tint = NovaMuted,
                        modifier = Modifier.size(21.dp).clickable(onClick = onDelete).padding(1.dp)
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallTag(config.architecture.label)
                SmallTag(config.resolution)
                SmallTag("${config.ramMb} MB RAM")
                SmallTag("${config.cpuCores} CPU")
            }
            val (status, isReady) = when (availability) {
                is EngineAvailability.Ready -> "Motores encontrados" to true
                is EngineAvailability.Missing -> "Faltam: " + availability.components.joinToString { it.title } to false
                is EngineAvailability.Unsupported -> availability.reason to false
            }
            StatusLabel(status, isReady)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction("Abrir prévia", Icons.Default.DesktopWindows, Modifier.weight(1f), onOpen)
                if (onEdit != null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(13.dp)).background(NovaPanelRaised)
                            .clickable(onClick = onEdit).padding(horizontal = 15.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Editar configurações", tint = NovaGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RuntimesScreen(runtimeEngine: RuntimeEngine) {
    var refreshCounter by remember { mutableStateOf(0) }
    val statusRows = remember(runtimeEngine, refreshCounter) {
        RuntimeId.entries.map { id -> id to runtimeEngine.componentStatus(id) }
    }
    Column(
        Modifier.fillMaxSize().background(NovaBackground).statusBarsPadding()
            .padding(horizontal = 17.dp, vertical = 15.dp)
    ) {
        PageHeader("Compatibilidade", "Motores", "13 componentes • estado verificado no armazenamento do app.")
        Spacer(Modifier.height(12.dp))
        InfoNotice(
            title = "Gerenciador de componentes",
            body = "O NovaBox verifica arquivos reais, permissões de execução, dependências e compatibilidade. Só pacotes com origem e SHA-256 validados recebem botão de instalação.",
            warning = true
        )
        Spacer(Modifier.height(10.dp))
        XServerCard()
        Spacer(Modifier.height(10.dp))
        RuntimePackageInstallCard(
            runtimeEngine = runtimeEngine,
            onInstallChanged = { refreshCounter += 1 }
        )
        Spacer(Modifier.height(10.dp))
        LinuxRootfsInstallCard(
            onInstallChanged = { refreshCounter += 1 }
        )
        Spacer(Modifier.height(10.dp))
        ProotInstallCard(onInstallChanged = { refreshCounter += 1 })
        Spacer(Modifier.height(10.dp))
        LinuxTerminalCard()
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(statusRows, key = { it.first.name }) { (runtime, status) ->
                val (label, tint) = when (status.state) {
                    com.mikael.novabox.runtime.RuntimeComponentState.INSTALLED -> "Instalado" to NovaGreen
                    com.mikael.novabox.runtime.RuntimeComponentState.NOT_INSTALLED -> "Não instalado" to NovaWarning
                    com.mikael.novabox.runtime.RuntimeComponentState.INCOMPLETE -> "Incompleto" to NovaWarning
                    com.mikael.novabox.runtime.RuntimeComponentState.UNSUPPORTED_DEVICE -> "Incompatível" to Color(0xFFFF7777)
                    com.mikael.novabox.runtime.RuntimeComponentState.BLOCKED -> "Bloqueado" to NovaWarning
                }
                Surface(
                    color = NovaPanel,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, NovaBorder, RoundedCornerShape(15.dp))
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF203326)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(18.dp))
                            }
                            Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(runtime.title, color = NovaText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(runtime.purpose, color = NovaGreen, fontSize = 10.sp)
                            }
                            Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(runtime.detail, color = NovaMuted, fontSize = 10.sp, lineHeight = 14.sp)
                        Text(status.message, color = tint, fontSize = 10.sp, lineHeight = 14.sp)
                        if (runtime != RuntimeId.LINUX_ROOTFS && com.mikael.novabox.runtime.RuntimePackageCatalog.packageFor(runtime) == null) {
                            Text(
                                com.mikael.novabox.runtime.RuntimePackageCatalog.unavailableReason(runtime),
                                color = NovaMuted,
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                        if (status.dependencies.isNotEmpty()) {
                            Text(
                                "Relacionados: " + status.dependencies.joinToString { it.title },
                                color = NovaMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(containerCount: Int, onOpenRuntimes: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(NovaBackground).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(19.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        PageHeader("NovaBox", "Configurações", "Informações gerais do aplicativo.")
        SettingTile(Icons.Default.Storage, "Ambientes salvos", "$containerCount perfil(is) neste aparelho")
        SettingTile(Icons.Default.Tune, "Personalização", "CPU, memória, gráficos, rede e tela são ajustados em cada ambiente")
        SettingTile(Icons.Default.Terminal, "Motores e runtimes", "Consultar componentes necessários", onClick = onOpenRuntimes)
        SettingTile(Icons.Default.Computer, "Mouse virtual", "Controle por toque disponível na prévia da área de trabalho")
        DeviceDiagnosticsCard()
        InfoNotice(
            title = "Sobre o NovaBox",
            body = "Um launcher de ambientes experimentais para Android. Criar um perfil não fornece uma licença nem uma imagem de Windows."
        )
        Text("NovaBox • versão de desenvolvimento 0.3.0", color = NovaMuted, fontSize = 10.sp)
    }
}

@Composable
private fun SettingTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(NovaPanel)
            .clickable(enabled = onClick != null) { onClick?.invoke() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(21.dp))
        Column(Modifier.weight(1f).padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = NovaText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(subtitle, color = NovaMuted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}
