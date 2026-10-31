package com.mikael.novabox.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.novabox.input.MouseButton
import com.mikael.novabox.input.PreviewInputBridge
import com.mikael.novabox.model.ContainerConfig

@Composable
fun DesktopPreviewScreen(config: ContainerConfig, onBack: () -> Unit) {
    var cursorX by remember(config.id) { mutableFloatStateOf(0.46f) }
    var cursorY by remember(config.id) { mutableFloatStateOf(0.48f) }
    var canvasSize by remember(config.id) { mutableStateOf(IntSize.Zero) }
    var lastInput by remember(config.id) { mutableStateOf("Arraste o dedo na tela para mover o mouse.") }
    var showKeyboard by remember(config.id) { mutableStateOf(false) }
    var keyboardText by remember(config.id) { mutableStateOf("") }

    val inputBridge = remember(config.id) {
        PreviewInputBridge { message -> lastInput = message }
    }

    Column(
        Modifier.fillMaxSize().background(NovaBackground)
            .statusBarsPadding().navigationBarsPadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(NovaPanelRaised)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = NovaText)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(config.name, color = NovaText, fontSize = 15.sp)
                Text("${config.profile?.name ?: "Ambiente"} • prévia de desktop", color = NovaMuted, fontSize = 10.sp)
            }
            Icon(Icons.Default.Mouse, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(23.dp))
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color(0xFF2A2417)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = NovaWarning, modifier = Modifier.size(16.dp))
            Text(
                "Modo de prévia: cursor controlado pelo toque. O motor Windows/Linux ainda não está conectado.",
                color = NovaWarning,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                modifier = Modifier.padding(start = 7.dp)
            )
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF151D27))
                .onGloballyPositioned { canvasSize = it.size }
                .pointerInput(config.id) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                cursorX = (offset.x / canvasSize.width).coerceIn(0f, 1f)
                                cursorY = (offset.y / canvasSize.height).coerceIn(0f, 1f)
                            }
                            inputBridge.click(MouseButton.LEFT)
                        },
                        onLongPress = { offset ->
                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                cursorX = (offset.x / canvasSize.width).coerceIn(0f, 1f)
                                cursorY = (offset.y / canvasSize.height).coerceIn(0f, 1f)
                            }
                            inputBridge.click(MouseButton.RIGHT)
                        }
                    )
                }
                .pointerInput(config.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            cursorX = (cursorX + dragAmount.x / canvasSize.width).coerceIn(0f, 1f)
                            cursorY = (cursorY + dragAmount.y / canvasSize.height).coerceIn(0f, 1f)
                        }
                        inputBridge.movePointer(cursorX, cursorY)
                    }
                }
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF26352B)).padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) {
                            Box(Modifier.size(6.dp).background(Color(0xFF8BFF62), CircleShape))
                        }
                    }
                    Text("  NovaBox Desktop", color = Color.White, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Text(config.resolution, color = Color(0xFFB8C8BB), fontSize = 9.sp)
                }

                Box(Modifier.fillMaxSize().background(Color(0xFF202B37))) {
                    Column(
                        Modifier.align(Alignment.Center).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF293746)).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(Icons.Default.Mouse, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(26.dp))
                        Text("Área de trabalho", color = Color.White, fontSize = 15.sp)
                        Text("O cursor acompanha seu dedo", color = Color(0xFFB5C1CB), fontSize = 10.sp)
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val x = size.width * cursorX
                        val y = size.height * cursorY
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
        }

        Text(lastInput, color = NovaMuted, fontSize = 10.sp, maxLines = 1)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton("Clique esquerdo", Modifier.weight(1f)) {
                inputBridge.click(MouseButton.LEFT)
            }
            ControlButton("Clique direito", Modifier.weight(1f)) {
                inputBridge.click(MouseButton.RIGHT)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton("Rolar para cima", Modifier.weight(1f)) {
                inputBridge.scroll(-1)
            }
            ControlButton("Rolar para baixo", Modifier.weight(1f)) {
                inputBridge.scroll(1)
            }
            ControlButton("Teclado", Modifier.weight(1f)) {
                showKeyboard = !showKeyboard
            }
        }
        if (showKeyboard) {
            androidx.compose.material3.OutlinedTextField(
                value = keyboardText,
                onValueChange = { keyboardText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Prévia do teclado virtual") },
                singleLine = true
            )
        }
    }
}

@Composable
private fun ControlButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(NovaPanelRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = NovaText, fontSize = 9.sp, maxLines = 1)
    }
}
