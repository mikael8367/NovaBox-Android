package com.mikael.novabox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.novabox.runtime.ProotInstallResult
import com.mikael.novabox.runtime.ProotInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProotInstallCard(onInstallChanged: () -> Unit) {
    val context = LocalContext.current
    val installer = remember(context) { ProotInstaller(context) }
    val scope = rememberCoroutineScope()
    var installed by remember(context) { mutableStateOf(installer.isInstalled()) }
    var installing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Pacote Android ARM64 fixado em commit upstream, com verificação do objeto Git e teste --version.") }

    Surface(color = Color(0xFF142219), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PRoot ANDROID ARM64", color = Color(0xFF78E08F), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Text(if (installed) "PRoot instalado e testado" else "Instalar PRoot para Alpine", color = Color(0xFFF0F5F1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color(0xFFD0D8D1), fontSize = 10.sp, lineHeight = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        installing = true
                        message = "Baixando, verificando integridade e testando o executável ARM64..."
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { installer.install() }
                            when (result) {
                                ProotInstallResult.Installed -> {
                                    installed = true
                                    message = "PRoot ARM64 instalado. Teste --version passou."
                                    onInstallChanged()
                                }
                                is ProotInstallResult.Failed -> message = result.reason
                            }
                            installing = false
                        }
                    },
                    enabled = !installing && !installed,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78E08F), contentColor = Color(0xFF08110A))
                ) {
                    Text(if (installing) "Instalando..." else if (installed) "Instalado" else "Instalar PRoot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
