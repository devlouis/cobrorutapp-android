package com.pe.cobrorutapp.core.domain.probe

import kotlinx.coroutines.flow.StateFlow

enum class ProbeSource { YAPE, INTERBANK }

data class NotificationSample(
    val id: String,
    val source: ProbeSource,
    val postedAtMillis: Long,
    val capturedAtMillis: Long,
    val title: String,
    val text: String,
    val bigText: String,
    val textLines: List<String>,
    val channelId: String?,
)

data class NotificationProbeState(
    val ready: Boolean = false,
    val captureEnabled: Boolean = false,
    val listenerConnected: Boolean = false,
    val samples: List<NotificationSample> = emptyList(),
    val storageError: Boolean = false,
)

/** Diagnostic samples only. Never a ledger, payment confirmation or bank integration. */
interface NotificationProbeRepository {
    val state: StateFlow<NotificationProbeState>
    fun isCaptureActive(): Boolean
    suspend fun startSession()
    suspend fun stopSession()
    suspend fun clear()
    suspend fun record(sample: NotificationSample)
    fun setListenerConnected(connected: Boolean)
}
