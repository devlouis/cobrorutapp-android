package com.pe.cobrorutapp.probe

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pe.cobrorutapp.core.designsystem.theme.CobrorutappTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
class NotificationProbeActivity : ComponentActivity() {
    private val viewModel: NotificationProbeViewModel by viewModels()
    private var accessGranted by mutableStateOf(false)
    private var apps by mutableStateOf("")
    private var settingsUnavailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Diagnostic samples may contain names and amounts: no screenshots/recents previews.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            CobrorutappTheme {
                Scaffold { insets ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            TextButton(onClick = { finish() }) { Text("Volver a Ingresos") }
                            Text("Prueba de notificaciones", style = MaterialTheme.typography.headlineSmall)
                            Text("Solo diagnóstico. No confirma pagos ni modifica saldos.")
                        }
                        item {
                            Text("Android permite acceso amplio a notificaciones. Esta prueba solo examina Yape e Interbank y guarda textos que parecen ingresos; el filtro aún debe validarse. Puede omitir avisos o confundirlos.")
                            Text("Las muestras pueden incluir nombres y montos. Se guardan solo en este teléfono, fuera del respaldo, sin enviarlas ni registrarlas en logs. Conserva hasta 20; puedes borrarlas. No uses datos reales en reportes o capturas.")
                        }
                        item {
                            Text(apps)
                            Text(if (accessGranted) "Acceso del sistema: concedido" else "Acceso del sistema: pendiente")
                            Text(if (state.listenerConnected) "Servicio conectado" else "Servicio desconectado")
                            Text(if (state.captureEnabled) "Sesión activa: máximo 10 minutos" else "Captura detenida")
                            if (state.storageError) Text("No se pudo leer o guardar el diagnóstico. Captura detenida; borra las muestras para reiniciar.", color = MaterialTheme.colorScheme.error)
                            if (settingsUnavailable) Text("Este dispositivo no ofrece la pantalla de acceso a notificaciones.")
                        }
                        item {
                            Button(onClick = {
                                try {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (_: ActivityNotFoundException) {
                                    settingsUnavailable = true
                                }
                            }) { Text("Revisar acceso en Ajustes") }
                            Button(
                                enabled = accessGranted && state.listenerConnected && state.ready && !state.captureEnabled,
                                onClick = viewModel::startCapture,
                            ) { Text("Iniciar prueba de 10 minutos") }
                            TextButton(enabled = state.captureEnabled, onClick = viewModel::stopCapture) { Text("Detener captura") }
                            TextButton(onClick = viewModel::clearSamples) { Text("Borrar muestras y detener") }
                        }
                        item {
                            Text("Muestras: ${state.samples.size}/20", style = MaterialTheme.typography.titleMedium)
                            Text("Activa una sesión y recibe un ingreso de prueba. No se recuperan avisos anteriores. Si no aparece, anota versión de Android/app y si el banco mostró la notificación; no asumas que no hubo pago.")
                        }
                        items(state.samples, key = { it.id }) { sample ->
                            Card {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${sample.source} · ${DateFormat.getDateTimeInstance().format(Date(sample.postedAtMillis))}")
                                    Text("Título: ${sample.title.ifEmpty { "(vacío)" }}")
                                    Text("Texto: ${sample.text.ifEmpty { "(vacío)" }}")
                                    Text("Texto ampliado: ${sample.bigText.ifEmpty { "(vacío)" }}")
                                    sample.textLines.forEach { Text("Línea: $it") }
                                    Text("Canal: ${sample.channelId ?: "(no disponible)"}")
                                    Text("Muestra técnica; confirmar el ingreso en la app bancaria.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessGranted = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
        apps = "Yape: ${versionOf("com.bcp.innovacxion.yapeapp")}\nInterbank: ${versionOf("pe.com.interbank.mobilebanking")}"
        if (!accessGranted) viewModel.stopCapture()
    }

    @Suppress("DEPRECATION")
    private fun versionOf(packageId: String): String = try {
        packageManager.getPackageInfo(packageId, 0).versionName ?: "instalada"
    } catch (_: PackageManager.NameNotFoundException) {
        "no instalada en este perfil"
    }
}
