package com.mikael.novabox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
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
import com.mikael.novabox.runtime.LinuxCommandResult
import com.mikael.novabox.runtime.LinuxSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LinuxTerminalCard() {
    val context = LocalContext.current
    val manager = remember(context) { LinuxSessionManager(context) }
    val scope = rememberCoroutineScope()
    var command by remember { mutableStateOf("uname -a") }
    var output by remember { mutableStateOf("Terminal Alpine ARM64. Instale o rootfs e o PRoot ARM64 validado para habilitar a execução.") }
    var running by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF101A14),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("TERMINAL LINUX", color = Color(0xFF78E08F), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Text(
                if (manager.isReady()) "Alpine ARM64 pronto" else "Aguardando PRoot ARM64",
                color = Color(0xFFF0F5F1), fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Comando Linux") },
                singleLine = false,
                minLines = 1,
                maxLines = 3,
                enabled = !running,
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    running = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { manager.runCommand(command) }
                        output = when (result) {
                            is LinuxCommandResult.Completed -> "[exit ${result.exitCode}]\n${result.output}"
                            is LinuxCommandResult.Failed -> result.reason
                        }
                        running = false
                    }
                },
                enabled = !running && manager.isReady() && command.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF78E08F),
                    contentColor = Color(0xFF08110A)
                ),
                shape = RoundedCornerShape(11.dp)
            ) {
                Text(if (running) "Executando..." else "Executar comando", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Text(
                output,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF080D0A), RoundedCornerShape(10.dp)).padding(11.dp),
                color = Color(0xFFB9DCC2), fontSize = 10.sp, lineHeight = 15.sp
            )
        }
    }
}
