package com.mikael.novabox.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private data class PerformanceSnapshot(
    val fps: Int? = null,
    val systemRamUsedMb: Int? = null,
    val systemRamTotalMb: Int? = null,
    val appMemoryMb: Int? = null,
    val cpuPercent: Int? = null,
    val internetLatencyMs: Int? = null,
    val batteryPercent: Int? = null,
    val batteryTemperatureC: Float? = null,
    val storageFreeGb: Float? = null
)

@Composable
fun PerformanceOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(PerformanceSnapshot()) }

    LaunchedEffect(Unit) {
        var previousCpuMs = Process.getElapsedCpuTime()
        var previousWallMs = SystemClock.elapsedRealtime()
        var lastLatencyCheckMs = 0L

        while (isActive) {
            val nowWallMs = SystemClock.elapsedRealtime()
            val nowCpuMs = Process.getElapsedCpuTime()
            val wallDelta = (nowWallMs - previousWallMs).coerceAtLeast(1L)
            val cpuDelta = (nowCpuMs - previousCpuMs).coerceAtLeast(0L)
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val cpuPercent = ((cpuDelta.toDouble() / wallDelta) * 100.0 / cores)
                .roundToInt().coerceIn(0, 100)

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val totalRamMb = if (memoryInfo.totalMem > 0L) (memoryInfo.totalMem / 1_048_576L).toInt() else null
            val usedRamMb = if (memoryInfo.totalMem > 0L) {
                ((memoryInfo.totalMem - memoryInfo.availMem).coerceAtLeast(0L) / 1_048_576L).toInt()
            } else null

            val processMemory = Debug.MemoryInfo()
            Debug.getMemoryInfo(processMemory)
            val appMemoryMb = (processMemory.totalPss / 1024).coerceAtLeast(0)

            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPercent = if (batteryLevel >= 0 && batteryScale > 0) {
                (batteryLevel * 100f / batteryScale).roundToInt().coerceIn(0, 100)
            } else null
            val temperatureTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val batteryTemperature = if (temperatureTenths >= 0) temperatureTenths / 10f else null

            val storageFreeGb = runCatching {
                StatFs(context.filesDir.absolutePath).availableBytes / 1_073_741_824f
            }.getOrNull()

            val latency = if (nowWallMs - lastLatencyCheckMs >= 15_000L || lastLatencyCheckMs == 0L) {
                lastLatencyCheckMs = nowWallMs
                withContext(Dispatchers.IO) { measureInternetLatency() }
            } else {
                snapshot.internetLatencyMs
            }

            snapshot = snapshot.copy(
                systemRamUsedMb = usedRamMb,
                systemRamTotalMb = totalRamMb,
                appMemoryMb = appMemoryMb,
                cpuPercent = cpuPercent,
                internetLatencyMs = latency,
                batteryPercent = batteryPercent,
                batteryTemperatureC = batteryTemperature,
                storageFreeGb = storageFreeGb
            )

            previousCpuMs = nowCpuMs
            previousWallMs = nowWallMs
            delay(1_000L)
        }
    }

    LaunchedEffect(Unit) {
        var frames = 0
        var startFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (startFrameNanos == 0L) startFrameNanos = frameNanos
                frames++
                val elapsed = frameNanos - startFrameNanos
                if (elapsed >= 1_000_000_000L) {
                    snapshot = snapshot.copy(
                        fps = (frames * 1_000_000_000.0 / elapsed).roundToInt()
                    )
                    frames = 0
                    startFrameNanos = frameNanos
                }
            }
        }
    }

    Column(
        modifier = modifier
            .widthIn(max = 190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE817211C))
            .border(1.dp, Color(0xFF47734E), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("DESEMPENHO • PRÉVIA", color = Color(0xFF9BFF62), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        MetricLine("FPS da interface", snapshot.fps?.let { "$it FPS" } ?: "Aguardando")
        MetricLine(
            "RAM do celular",
            if (snapshot.systemRamUsedMb != null && snapshot.systemRamTotalMb != null) {
                "${snapshot.systemRamUsedMb} / ${snapshot.systemRamTotalMb} MB"
            } else "Indisponível"
        )
        MetricLine("RAM do NovaBox", snapshot.appMemoryMb?.let { "$it MB" } ?: "Indisponível")
        MetricLine("CPU do app", snapshot.cpuPercent?.let { "$it%" } ?: "Aguardando")
        MetricLine("Latência internet", snapshot.internetLatencyMs?.let { "$it ms" } ?: "Sem resposta")
        MetricLine("Bateria", snapshot.batteryPercent?.let { "$it%" } ?: "Indisponível")
        MetricLine("Temperatura bateria", snapshot.batteryTemperatureC?.let { "%.1f °C".format(it) } ?: "Indisponível")
        MetricLine("Armazenamento livre", snapshot.storageFreeGb?.let { "%.1f GB".format(it) } ?: "Indisponível")
        Text(
            "A latência é até um serviço de conectividade, não o ping de um servidor de jogo. GPU e métricas da VM dependem do motor.",
            color = Color(0xFFB5C1BB),
            fontSize = 8.sp,
            lineHeight = 11.sp
        )
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFB5C1BB), fontSize = 9.sp, maxLines = 1)
        Text(value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun measureInternetLatency(): Int? {
    var connection: HttpURLConnection? = null
    return try {
        val startedAt = SystemClock.elapsedRealtime()
        connection = (URL("https://connectivitycheck.gstatic.com/generate_204").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2_000
            readTimeout = 2_000
            instanceFollowRedirects = false
            useCaches = false
        }
        val responseCode = connection.responseCode
        val elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
        if (responseCode == 204 || responseCode in 200..299) elapsed.toInt() else null
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
