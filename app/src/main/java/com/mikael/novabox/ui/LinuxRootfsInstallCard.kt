package com.mikael.novabox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.mikael.novabox.runtime.AlpineRootfsInstaller
import com.mikael.novabox.runtime.LinuxRootfsInstallResult
import com.mikael.novabox.runtime.RuntimeDownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LinuxRootfsInstallCard(onInstallChanged: () -> Unit) {
    val context = LocalContext.current
    val installer = remember(context) { AlpineRootfsInstaller(context) }
    val scope = rememberCoroutineScope()
    var installed by remember(context) { mutableStateOf(installer.isInstalled()) }
    var installing by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(RuntimeDownloadProgress(0L, 4_029_999L)) }
    var message by remember { mutableStateOf<String?>(null) }
    var succeeded by remember { mutableStateOf(false) }

    Surface(color = Color(0xFF142219), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LINUX USERLAND", color = Color(0xFF78E08F), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Alpine Linux 3.24.3 • ARM64", color = Color(0xFFF0F5F1), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Rootfs oficial mínimo de 4 MB, com SHA-256 fixado e extração protegida. Requer PRoot ARM64 para iniciar o shell; o instalador do PRoot ainda está pendente.",
                color = Color(0xFFD0D8D1), fontSize = 10.sp, lineHeight = 15.sp)
            if (installing) {
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF78E08F), trackColor = Color(0xFF243329))
                Text((progress.fraction * 100).toInt().toString() + "% • " + formatRootfsBytes(progress.downloadedBytes) +
                    " / " + formatRootfsBytes(progress.totalBytes), color = Color(0xFF78E08F), fontSize = 10.sp)
            }
            message?.let { Text(it, color = if (succeeded) Color(0xFF78E08F) else Color(0xFFFFD27A), fontSize = 10.sp, lineHeight = 15.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    message = null
                    succeeded = false
                    progress = RuntimeDownloadProgress(0L, 4_029_999L)
                    installing = true
                    job = scope.launch {
                        try {
                            when (val result = withContext(Dispatchers.IO) {
                                installer.install { current -> withContext(Dispatchers.Main) { progress = current } }
                            }) {
                                LinuxRootfsInstallResult.Installed -> {
                                    installed = true
                                    succeeded = true
                                    message = "Alpine rootfs instalado e verificado. PRoot ainda precisa ser instalado para iniciar o Linux."
                                    onInstallChanged()
                                }
                                is LinuxRootfsInstallResult.Failed -> message = result.reason
                            }
                        } catch (cancelled: CancellationException) {
                            message = "Instalação cancelada; arquivos temporários descartados."
                            throw cancelled
                        } finally {
                            installing = false
                            job = null
                        }
                    }
                }, enabled = !installing && !installed, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78E08F), contentColor = Color(0xFF08110A),
                        disabledContainerColor = Color(0xFF334238), disabledContentColor = Color(0xFF8A988D))) {
                    Text(if (installed) "Rootfs instalado" else "Instalar Alpine Linux", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (installing) Button(onClick = { job?.cancel() }, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A3025), contentColor = Color(0xFFFFD5C2))) {
                    Text("Cancelar", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatRootfsBytes(bytes: Long): String {
    if (bytes < 1024L) return bytes.toString() + " B"
    return String.format(java.util.Locale.ROOT, "%.1f MiB", bytes / (1024f * 1024f))
}