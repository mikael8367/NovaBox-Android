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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.novabox.runtime.XServerController
import com.mikael.novabox.runtime.XServerLaunchResult

@Composable
fun XServerCard() {
    val context = LocalContext.current
    val controller = remember(context) { XServerController(context) }
    var message by remember { mutableStateOf("") }
    val installed = remember(context) { controller.isInstalled() }

    Surface(Modifier.fillMaxWidth(), color = NovaPanel, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Servidor gráfico X11", color = NovaText, fontSize = 13.sp)
            Text(
                if (installed) "Termux:X11 detectado. Abra-o antes de iniciar uma sessão Wine com DISPLAY=:0."
                else "O servidor gráfico não vem embutido. Instale Termux:X11 separadamente para habilitar uma tela X11.",
                color = NovaMuted, fontSize = 10.sp, lineHeight = 14.sp
            )
            Row {
                Button(
                    onClick = {
                        message = when (controller.launch()) {
                            XServerLaunchResult.OPENED -> "Termux:X11 aberto. Inicie o display :0 no ambiente Termux, se necessário."
                            XServerLaunchResult.NOT_INSTALLED -> "Termux:X11 não está instalado."
                            XServerLaunchResult.NO_LAUNCH_ACTIVITY -> "O pacote existe, mas não oferece uma tela inicial."
                            XServerLaunchResult.FAILED -> "O Android não conseguiu abrir Termux:X11."
                        }
                    },
                    enabled = installed,
                    colors = ButtonDefaults.buttonColors(containerColor = NovaGreen)
                ) { Text("Abrir XServer", color = NovaBackground, fontSize = 11.sp) }
            }
            if (message.isNotBlank()) Text(message, color = NovaWarning, fontSize = 10.sp)
        }
    }
}
