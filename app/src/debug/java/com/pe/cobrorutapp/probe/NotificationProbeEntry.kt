package com.pe.cobrorutapp.probe

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun NotificationProbeEntry() {
    val context = LocalContext.current
    TextButton(onClick = { context.startActivity(Intent(context, NotificationProbeActivity::class.java)) }) {
        Text("Prueba de notificaciones (debug)")
    }
}
