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
                    else -> "Termux e Termux:X11 detectados. O NovaBox pode solicitar a abertura do display :1."
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
                                "Comando enviado e Termux:X11 aberto. Isso ainda não confirma que o display iniciou nem conecta o Wine."
                            XServerLaunchResult.DISPLAY_COMMAND_SENT ->
                                "Comando enviado ao Termux, mas a tela X11 não abriu. Abra Termux:X11 manualmente e confira o log do Termux."
                            XServerLaunchResult.TERMUX_NOT_INSTALLED ->
                                "Termux não está instalado."
                            XServerLaunchResult.NOT_INSTALLED ->
                                "Instale o aplicativo Termux:X11."
                            XServerLaunchResult.PERMISSION_REQUIRED ->
                                "Permissão necessária: abra Ajustes do NovaBox > Permissões e permita 'Executar comandos no ambiente Termux'."
                            XServerLaunchResult.SERVICE_START_FAILED ->
                                "Termux recusou o comando. Confira se allow-external-apps=true está em ~/.termux/termux.properties e se a permissão foi concedida."
                            else -> "Não foi possível solicitar a inicialização do display."
                        }
                    },
                    enabled = installed && termuxInstalled,
                    colors = ButtonDefaults.buttonColors(containerColor = NovaGreen)
                ) { Text("Iniciar display :1", color = NovaBackground, fontSize = 11.sp) }
            }
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
                "Configuração obrigatória no Termux: em ~/.termux/termux.properties, habilite allow-external-apps=true. Depois reinicie o Termux. O Wine ainda precisa de um canal de conexão X11 separado.",
                color = NovaMuted, fontSize = 9.sp, lineHeight = 13.sp
            )
        }
    }
}
