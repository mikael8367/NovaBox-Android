package com.mikael.novabox.ui

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun DeviceDiagnosticsCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val report = remember(refresh) { buildDeviceReport(context) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(NovaPanel).border(1.dp, NovaBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Diagnóstico do aparelho", color = NovaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "Atualizar",
                color = NovaGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovaPanelRaised)
                    .clickable { refresh++ }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            )
        }
        report.lines().forEach { line ->
            if (line.isNotBlank()) Text(line, color = NovaMuted, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Text(
            "Copiar relatório",
            color = NovaGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF203326))
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Diagnóstico NovaBox", report))
                    Toast.makeText(context, "Relatório copiado.", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

private fun buildDeviceReport(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memory = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memory)
    val totalRam = memory.totalMem / 1_048_576L
    val availableRam = memory.availMem / 1_048_576L
    val storage = runCatching { StatFs(context.filesDir.absolutePath).availableBytes / 1_048_576L }.getOrDefault(0L)
    val vulkanFeature = context.packageManager.getSystemAvailableFeatures()
        .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
    val glVersion = activityManager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0
    val glText = if (glVersion > 0) "${glVersion shr 16}.${glVersion and 0xffff}" else "não informado"
    val versionName = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "desconhecida"
    }.getOrDefault("desconhecida")

    return listOf(
        "Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}",
        "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Hardware/board: ${Build.HARDWARE} / ${Build.BOARD}",
        "ABIs: ${Build.SUPPORTED_ABIS.joinToString()}",
        "Núcleos lógicos: ${Runtime.getRuntime().availableProcessors()}",
        "RAM: ${totalRam} MiB total • ${availableRam} MiB disponível",
        "Armazenamento livre no app: ${storage} MiB",
        "OpenGL ES informado pelo sistema: $glText",
        "Vulkan feature: ${if (vulkanFeature != null) "presente (versão ${vulkanFeature.version})" else "não anunciado"}",
        "NovaBox: $versionName"
    ).joinToString("\n")
}
