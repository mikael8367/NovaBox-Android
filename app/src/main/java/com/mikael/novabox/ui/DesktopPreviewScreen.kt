package com.mikael.novabox.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mikael.novabox.input.MouseButton
import com.mikael.novabox.input.PreviewInputBridge
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.MouseMode
import com.mikael.novabox.model.OsFamily

@Composable
fun DesktopPreviewScreen(config: ContainerConfig, onBack: () -> Unit) {
    var cursorX by remember(config.id) { mutableFloatStateOf(0.46f) }
    var cursorY by remember(config.id) { mutableFloatStateOf(0.48f) }
    var canvasSize by remember(config.id) { mutableStateOf(IntSize.Zero) }
    var lastInput by remember(config.id) { mutableStateOf("Toque para clicar • pressione e segure para o botão direito") }

    val inputBridge = remember(config.id) {
        PreviewInputBridge { message -> lastInput = message }
    }
    var currentContext: Context = LocalContext.current
    while (currentContext is ContextWrapper && currentContext !is Activity) {
        currentContext = currentContext.baseContext
    }
    val activity = currentContext as? Activity
    val view = LocalView.current

    DisposableEffect(activity, view) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = activity?.window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (previousOrientation != null) {
                activity?.requestedOrientation = previousOrientation
            }
        }
    }

    val isAndroidGuest = config.profile?.family == OsFamily.ANDROID

    Box(
        Modifier.fillMaxSize().background(Color(0xFF111820))
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Color(0xFF202B37))
                .onGloballyPositioned { canvasSize = it.size }
                .pointerInput(config.id, canvasSize) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (config.mouseMode == MouseMode.DIRECT && canvasSize.width > 0 && canvasSize.height > 0) {
                                cursorX = (offset.x / canvasSize.width).coerceIn(0f, 1f)
                                cursorY = (offset.y / canvasSize.height).coerceIn(0f, 1f)
                            }
                            inputBridge.click(MouseButton.LEFT)
                        },
                        onLongPress = { offset ->
                            if (config.mouseMode == MouseMode.DIRECT && canvasSize.width > 0 && canvasSize.height > 0) {
                                cursorX = (offset.x / canvasSize.width).coerceIn(0f, 1f)
                                cursorY = (offset.y / canvasSize.height).coerceIn(0f, 1f)
                            }
                            inputBridge.click(MouseButton.RIGHT)
                        }
                    )
                }
                .pointerInput(config.id, canvasSize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            if (config.mouseMode == MouseMode.DIRECT) {
                                cursorX = (change.position.x / canvasSize.width).coerceIn(0f, 1f)
                                cursorY = (change.position.y / canvasSize.height).coerceIn(0f, 1f)
                            } else {
                                val sensitivity = config.mouseSensitivity / 5f
                                cursorX = (cursorX + dragAmount.x / canvasSize.width * sensitivity).coerceIn(0f, 1f)
                                cursorY = (cursorY + dragAmount.y / canvasSize.height * sensitivity).coerceIn(0f, 1f)
                            }
                        }
                        inputBridge.movePointer(cursorX, cursorY)
                    }
                }
        ) {
            Column(
                    Modifier.align(Alignment.Center).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF293746))
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isAndroidGuest) Icons.Default.Android else Icons.Default.Mouse,
                        contentDescription = null,
                        tint = NovaGreen,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        if (isAndroidGuest) "Prévia Android" else "Prévia da área de trabalho",
                        color = Color.White,
                        fontSize = 17.sp
                    )
                    Text(
                        if (isAndroidGuest) "A máquina virtual ainda não foi iniciada" else "O motor do Windows ainda não está conectado",
                        color = Color(0xFFB5C1CB),
                        fontSize = 11.sp
                    )
                }

            Canvas(Modifier.fillMaxSize()) {
                val x = size.width * cursorX
                val y = size.height * cursorY
                if (isAndroidGuest) {
                    drawCircle(Color.White.copy(alpha = 0.25f), 22f, Offset(x, y))
                    drawCircle(NovaGreen, 8f, Offset(x, y))
                    drawCircle(Color.White, 8f, Offset(x, y), style = Stroke(width = 2f))
                } else {
                    val pointer = Path().apply {
                        moveTo(x, y)
                        lineTo(x, y + 25f)
                        lineTo(x + 6f, y + 19f)
                        lineTo(x + 11f, y + 30f)
                        lineTo(x + 15f, y + 28f)
                        lineTo(x + 10f, y + 17f)
                        lineTo(x + 19f, y + 17f)
                        close()
                    }
                    drawPath(pointer, Color.White)
                    drawPath(pointer, Color.Black, style = Stroke(width = 2f))
                }
            }
        }

        Row(
            Modifier.align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xDD101820))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .background(NovaPanelRaised)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = NovaText)
            }
            Column(Modifier.padding(start = 9.dp)) {
                Text(config.name, color = NovaText, fontSize = 12.sp)
                Text("Tela cheia • paisagem", color = NovaGreen, fontSize = 9.sp)
            }
        }

        if (config.showFpsOverlay) {
            PerformanceOverlay(Modifier.align(Alignment.TopEnd).padding(10.dp))
        }

        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xDD101820))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                if (isAndroidGuest) "Prévia visual: QEMU e imagem Android não conectados."
                else "Windows não iniciou: o motor de virtualização ainda não foi integrado.",
                color = NovaWarning,
                fontSize = 10.sp
            )
            Text(lastInput, color = NovaMuted, fontSize = 9.sp)
        }
    }
}
