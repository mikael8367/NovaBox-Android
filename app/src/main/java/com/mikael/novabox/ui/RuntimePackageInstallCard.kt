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
import com.mikael.novabox.model.RuntimeId
import com.mikael.novabox.runtime.RuntimeComponentState
import com.mikael.novabox.runtime.RuntimeDownloadProgress
import com.mikael.novabox.runtime.RuntimeInstallResult
import com.mikael.novabox.runtime.RuntimePackageCatalog
import com.mikael.novabox.runtime.RuntimePackageInstaller
import com.mikael.novabox.runtime.RuntimeEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun RuntimePackageInstallCard(
    runtimeEngine: RuntimeEngine,
    onInstallChanged: () -> Unit
) {
    val context = LocalContext.current
    val spec = remember { RuntimePackageCatalog.packageFor(RuntimeId.DXVK) }
    val installer = remember(context) { RuntimePackageInstaller(context) }
    val scope = rememberCoroutineScope()

    var isInstalling by remember { mutableStateOf(false) }
    var installJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(RuntimeDownloadProgress(0L, spec?.expectedSizeBytes ?: 0L)) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    val componentStatus = runtimeEngine.componentStatus(RuntimeId.DXVK)
    val deviceUnsupported = componentStatus.state == RuntimeComponentState.UNSUPPORTED_DEVICE

    Surface(
        color = Color(0xFF142219),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "PACOTES VERIFICADOS",
                color = Color(0xFF78E08F),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            if (spec != null) {
                Text("DXVK ${spec.version}", color = Color(0xFFF0F5F1), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Pacote oficial: 17,2 MiB. O NovaBox confere o tamanho e o SHA-256 antes de extrair somente as DLLs esperadas.",
                    color = Color(0xFFD0D8D1),
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text(
                    "Requer Wine e Vulkan funcional. O projeto upstream não declara suporte oficial ao Android; a instalação apenas prepara os arquivos DXVK.",
                    color = Color(0xFFFFD27A),
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                if (deviceUnsupported) {
                    Text(
                        componentStatus.message,
                        color = Color(0xFFFF8585),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
                if (isInstalling) {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF78E08F),
                        trackColor = Color(0xFF243329)
                    )
                    Text(
                        "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)} • ${(progress.fraction * 100).toInt()}%",
                        color = Color(0xFF78E08F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                statusMessage?.let {
                    Text(
                        it,
                        color = if (success) Color(0xFF78E08F) else Color(0xFFFFD27A),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            statusMessage = null
                            success = false
                            progress = RuntimeDownloadProgress(0L, spec.expectedSizeBytes)
                            isInstalling = true
                            installJob = scope.launch {
                                try {
                                    when (val result = installer.install(spec) { current ->
                                        progress = current
                                    }) {
                                        is RuntimeInstallResult.Installed -> {
                                            success = true
                                            statusMessage = "DXVK ${result.version} instalado e verificado."
                                            onInstallChanged()
                                        }
                                        is RuntimeInstallResult.Failed -> {
                                            statusMessage = result.reason
                                        }
                                    }
                                } catch (cancelled: CancellationException) {
                                    statusMessage = "Instalação cancelada. Os arquivos temporários foram descartados."
                                    throw cancelled
                                } finally {
                                    isInstalling = false
                                    installJob = null
                                }
                            }
                        },
                        enabled = !isInstalling && !deviceUnsupported,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF78E08F),
                            contentColor = Color(0xFF08110A),
                            disabledContainerColor = Color(0xFF334238),
                            disabledContentColor = Color(0xFF8A988D)
                        )
                    ) {
                        Text("Baixar e instalar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (isInstalling) {
                        Button(
                            onClick = { installJob?.cancel() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A3025),
                                contentColor = Color(0xFFFFD5C2)
                            )
                        ) {
                            Text("Cancelar", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Text("Nenhum pacote com origem, arquitetura e SHA-256 validados.", color = Color(0xFFFFD27A), fontSize = 11.sp)
            }
            Text(
                "Os outros 11 componentes aparecem no catálogo, mas ainda não possuem instaladores Android ARM64 verificados.",
                color = Color(0xFFABB8AE),
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val mebibytes = bytes / (1024f * 1024f)
    return String.format(java.util.Locale.ROOT, "%.1f MiB", mebibytes)
}
