package com.mikael.novabox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF090D0B)
private val Panel = Color(0xFF121A15)
private val Neon = Color(0xFF8BFF54)
private val Muted = Color(0xFF89958D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NovaBoxApp() }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Início", Icons.Default.Home),
    Containers("Containers", Icons.Default.Computer),
    Downloads("Downloads", Icons.Default.CloudDownload),
    Settings("Config.", Icons.Default.Settings)
}

@Composable
private fun NovaBoxApp() {
    var selectedTab by remember { mutableStateOf(Tab.Home) }
    val containers = remember { mutableStateListOf<String>() }

    val colors = darkColorScheme(
        primary = Neon,
        onPrimary = Background,
        background = Background,
        onBackground = Color.White,
        surface = Panel,
        onSurface = Color.White,
        secondary = Neon,
        onSecondary = Background
    )

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = Panel,
                    contentColor = Color.White
                ) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Neon,
                                selectedTextColor = Neon,
                                indicatorColor = Color(0xFF263B26),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition",
                modifier = Modifier.padding(padding)
            ) { tab ->
                when (tab) {
                    Tab.Home -> HomeScreen(onOpenContainers = { selectedTab = Tab.Containers })
                    Tab.Containers -> ContainersScreen(
                        containers = containers,
                        onAdd = { containers.add(it) }
                    )
                    Tab.Downloads -> InfoScreen(
                        title = "Downloads",
                        subtitle = "Seus componentes e arquivos aparecerão aqui."
                    )
                    Tab.Settings -> InfoScreen(
                        title = "Configurações",
                        subtitle = "Preferências do aplicativo e do ambiente."
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpenContainers: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("NOVABOX", color = Neon, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("Seu espaço para\napps de PC.", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
        Text("Ambientes organizados no Android.", color = Muted, fontSize = 14.sp)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Panel,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Color(0xFF263B26), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Computer, contentDescription = null, tint = Neon)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Meus containers", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Gerencie seus ambientes", color = Muted, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = onOpenContainers,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Background),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Abrir containers", fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(color = Color(0xFF211D12), shape = RoundedCornerShape(16.dp)) {
            Text(
                "Prévia inicial: a execução de programas Windows ainda não está integrada.",
                modifier = Modifier.padding(16.dp),
                color = Color(0xFFFFD58A),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ContainersScreen(containers: List<String>, onAdd: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(Background).statusBarsPadding().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Containers", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Organize seus ambientes em um só lugar.", color = Muted, fontSize = 13.sp)
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Background),
            shape = RoundedCornerShape(14.dp)
        ) { Text("+ Novo container", fontWeight = FontWeight.Bold) }

        if (containers.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = Muted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhum container ainda", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Crie um para começar a organizar.", color = Muted, fontSize = 12.sp)
                }
            }
        } else {
            containers.forEach { item ->
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Computer, contentDescription = null, tint = Neon)
                        Text(item, color = Color.White, modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Panel,
            title = { Text("Novo container", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    label = { Text("Nome do ambiente") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isNotEmpty()) onAdd(cleanName)
                    name = ""
                    showDialog = false
                }) { Text("Criar", color = Neon) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar", color = Muted) }
            }
        )
    }
}

@Composable
private fun InfoScreen(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().background(Background).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 14.sp)
    }
}
