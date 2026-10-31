package com.mikael.novabox.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.novabox.model.RuntimeId
import com.mikael.novabox.runtime.RuntimeComponentState
import com.mikael.novabox.runtime.RuntimeEngine

private const val SETUP_PREFS = "novabox_first_run"
private const val SETUP_COMPLETED = "runtime_setup_reviewed"

@Composable
fun FirstRunSetupGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var completed by remember {
        mutableStateOf(
            context.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE)
                .getBoolean(SETUP_COMPLETED, false)
        )
    }

    if (completed) {
        content()
    } else {
        FirstRunSetupScreen(
            runtimeEngine = remember { RuntimeEngine(context) },
            onContinue = {
                context.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(SETUP_COMPLETED, true)
                    .apply()
                completed = true
            }
        )
    }
}

@Composable
private fun FirstRunSetupScreen(
    runtimeEngine: RuntimeEngine,
    onContinue: () -> Unit
) {
    val statuses = remember(runtimeEngine) {
        RuntimeId.entries.map { id -> id to runtimeEngine.componentStatus(id) }
    }
    val readyCount = statuses.count { it.second.state == RuntimeComponentState.INSTALLED }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF080D09)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    "CONFIGURAÇÃO INICIAL",
                    color = Color(0xFF78E08F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "Preparando o NovaBox",
                    color = Color(0xFFF0F5F1),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Antes de entrar, verificamos os 12 componentes e a compatibilidade deste aparelho.",
                    color = Color(0xFFABB8AE),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = Color(0xFF78E08F),
                    trackColor = Color(0xFF243329)
                )
                Text(
                    "Verificação concluída • 100% • $readyCount de 12 componentes já instalados",
                    color = Color(0xFF78E08F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    color = Color(0xFF19251D),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Downloads ainda não disponíveis",
                            color = Color(0xFFFFD27A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Os pacotes nativos ainda não têm fontes e hashes SHA-256 validados no projeto. Por segurança, o NovaBox não baixa arquivos desconhecidos nem mostra progresso ou tempo falso.",
                            color = Color(0xFFD0D8D1),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Text(
                            "Tempo de download: indisponível até os pacotes serem publicados.",
                            color = Color(0xFFABB8AE),
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    "ESTADO DOS COMPONENTES",
                    color = Color(0xFF78E08F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(statuses, key = { it.first.name }) { (id, status) ->
                        val (label, tint) = when (status.state) {
                            RuntimeComponentState.INSTALLED -> "Instalado" to Color(0xFF78E08F)
                            RuntimeComponentState.UNSUPPORTED_DEVICE -> "Incompatível" to Color(0xFFFF8585)
                            RuntimeComponentState.BLOCKED -> "Dependência ausente" to Color(0xFFFFD27A)
                            RuntimeComponentState.INCOMPLETE -> "Incompleto" to Color(0xFFFFD27A)
                            RuntimeComponentState.NOT_INSTALLED -> "Pendente" to Color(0xFFABB8AE)
                        }
                        Surface(
                            color = Color(0xFF121B15),
                            shape = RoundedCornerShape(11.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(id.title, color = Color(0xFFF0F5F1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(status.message, color = Color(0xFFABB8AE), fontSize = 9.sp, lineHeight = 13.sp)
                                }
                                Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78E08F),
                        contentColor = Color(0xFF08110A)
                    )
                ) {
                    Text("Concluir verificação e entrar", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Você poderá consultar os motores na aba correspondente. A instalação automática será liberada quando houver pacotes verificados.",
                    color = Color(0xFF7F8D82),
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
