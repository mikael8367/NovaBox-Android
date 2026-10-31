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
    var installed by remember(context) { mutableStateOf(controller.isInstalled()) }
    var termuxInstalled by remember(context) { mutableStateOf(controller.isTermuxInstalled()) }

    Surface(Modifier.fillMaxWidth(), color = NovaPanel, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Servidor gráfico X11", color = NovaText, fontSize = 13.sp)
            Text(
                when {
                    !termuxInstalled -> "Instale o Termux e o pacote complementar Termux:X11. O NovaBox usa o Termux para iniciar o display."
                    !installed -> "Termux detectado. Falta instalar o aplicativo Termux:X11 e o pacote termux-x11-nightly no Termux."
                    else -> "Termux e Termux:X11 detectados. Inicie o display :1 e depois a ponte local."
                },
                color = NovaMuted, fontSize = 10.sp, lineHeight = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        installed = controller.isInstalled()
                        termuxInstalled = controller.isTermuxInstalled()
                        message = "Termux: " + (if (termuxInstalled) "detectado" else "não encontrado") +
                            " • Termux:X11: " + (if (installed) "detectado" else "não encontrado")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaPanelRaised)
                ) { Text("Verificar", color = NovaText, fontSize = 11.sp) }
                Button(
                    onClick = {
                        installed = controller.isInstalled()
                        termuxInstalled = controller.isTermuxInstalled()
                        message = when (controller.startDisplay()) {
                            XServerLaunchResult.START_REQUESTED ->
                                "Pedido enviado e Termux:X11 aberto. Aguarde o display ficar pronto."
                            XServerLaunchResult.DISPLAY_COMMAND_SENT ->
                                "Comando enviado ao Termux. Abra Termux:X11 manualmente e confira o log."
                            XServerLaunchResult.TERMUX_NOT_INSTALLED -> "Termux não está instalado."
                            XServerLaunchResult.NOT_INSTALLED -> "Instale o aplicativo Termux:X11."
                            XServerLaunchResult.PERMISSION_REQUIRED ->
                                "Permissão necessária: permita 'Executar comandos no ambiente Termux' nas permissões do NovaBox."
                            XServerLaunchResult.SERVICE_START_FAILED ->
                                "Termux recusou o comando. Confira allow-external-apps=true em ~/.termux/termux.properties."
                            else -> "Não foi possível solicitar a inicialização do display."
                        }
                    },
                    enabled = installed && termuxInstalled,
                    colors = ButtonDefaults.buttonColors(containerColor = NovaGreen)
                ) { Text("Iniciar display :1", color = NovaBackground, fontSize = 11.sp) }
            }
            Button(
                onClick = {
                    message = when (controller.startSocketBridge()) {
                        XServerLaunchResult.BRIDGE_REQUESTED ->
                            "Ponte solicitada. Ela aguarda o socket X1 por até 45 s e escuta somente em 127.0.0.1:6001. Isso não confirma conexão."
                        XServerLaunchResult.TERMUX_NOT_INSTALLED -> "Termux não está instalado."
                        XServerLaunchResult.NOT_INSTALLED -> "Instale o aplicativo Termux:X11."
                        XServerLaunchResult.PERMISSION_REQUIRED ->
                            "Permita 'Executar comandos no ambiente Termux' nas permissões do NovaBox."
                        XServerLaunchResult.SERVICE_START_FAILED ->
                            "Não foi possível enviar o comando. Confira allow-external-apps=true e a permissão RUN_COMMAND."
                        else -> "Não foi possível solicitar a ponte X11."
                    }
                },
                enabled = installed && termuxInstalled,
                colors = ButtonDefaults.buttonColors(containerColor = NovaPanelRaised)
            ) { Text("Iniciar ponte X11 local", color = NovaText, fontSize = 11.sp) }
            if (!controller.hasRunCommandPermission() && termuxInstalled) {
                Button(
                    onClick = {
                        controller.openNovaBoxPermissionSettings()
                        message = "Nas permissões do NovaBox, habilite 'Executar comandos no ambiente Termux'."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaPanelRaised)
                ) { Text("Abrir permissões", color = NovaText, fontSize = 11.sp) }
            }
            if (message.isNotBlank()) Text(message, color = NovaWarning, fontSize = 10.sp)
            Text(
                "No Termux, habilite allow-external-apps=true em ~/.termux/termux.properties e reinicie o Termux. A ponte usa TCP apenas em 127.0.0.1; não fica exposta à rede Wi-Fi. Wine usa DISPLAY=127.0.0.1:1.",
                color = NovaMuted, fontSize = 9.sp, lineHeight = 13.sp
            )
        }
    }
}
