package com.pe.cobrorutapp.core.data.probe

import android.content.Context
import android.os.SystemClock
import android.util.AtomicFile
import com.pe.cobrorutapp.core.domain.probe.NotificationProbeRepository
import com.pe.cobrorutapp.core.domain.probe.NotificationProbeState
import com.pe.cobrorutapp.core.domain.probe.NotificationSample
import com.pe.cobrorutapp.core.domain.probe.ProbeSource
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Debug-only, bounded diagnostics in noBackupFilesDir. Capture never resumes on process restart. */
class LocalNotificationProbeRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val sessionDurationMillis: Long = 10 * 60 * 1000L,
) : NotificationProbeRepository {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "notification_probe.json"))
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(NotificationProbeState())
    override val state = mutableState.asStateFlow()
    @Volatile private var expiresAtElapsed = 0L
    private var expiryJob: Job? = null

    init {
        scope.launch {
            mutex.withLock {
                try {
                    val rows = if (file.baseFile.exists()) {
                        val bytes = file.openRead().use {
                            if (it.channel.size() > MAX_FILE_BYTES) throw IOException("Diagnostic file too large")
                            it.readBytes()
                        }
                        val array = JSONArray(bytes.toString(Charsets.UTF_8))
                        (0 until minOf(array.length(), MAX_SAMPLES)).map { decode(array.getJSONObject(it)) }
                    } else emptyList()
                    mutableState.update { it.copy(ready = true, samples = rows) }
                } catch (_: IOException) {
                    mutableState.update { it.copy(storageError = true) }
                } catch (_: JSONException) {
                    mutableState.update { it.copy(storageError = true) }
                } catch (_: IllegalArgumentException) {
                    mutableState.update { it.copy(storageError = true) }
                }
            }
        }
    }

    override fun isCaptureActive(): Boolean =
        state.value.ready && state.value.captureEnabled && SystemClock.elapsedRealtime() < expiresAtElapsed

    override suspend fun startSession() = mutex.withLock {
        if (!state.value.ready || !state.value.listenerConnected) return@withLock
        expiryJob?.cancel()
        expiresAtElapsed = SystemClock.elapsedRealtime() + sessionDurationMillis
        mutableState.update { it.copy(captureEnabled = true, storageError = false) }
        expiryJob = scope.launch {
            delay(sessionDurationMillis)
            stopSession()
        }
    }

    override suspend fun stopSession() = mutex.withLock {
        expiresAtElapsed = 0L
        expiryJob?.cancel()
        expiryJob = null
        mutableState.update { it.copy(captureEnabled = false) }
    }

    override fun setListenerConnected(connected: Boolean) {
        if (!connected) expiresAtElapsed = 0L
        mutableState.update {
            it.copy(listenerConnected = connected, captureEnabled = it.captureEnabled && connected)
        }
    }

    override suspend fun clear() = mutex.withLock {
        expiresAtElapsed = 0L
        expiryJob?.cancel()
        expiryJob = null
        mutableState.update { it.copy(captureEnabled = false) }
        if (persist(emptyList())) {
            mutableState.update { it.copy(ready = true, samples = emptyList(), storageError = false) }
        }
    }

    override suspend fun record(sample: NotificationSample) = mutex.withLock {
        if (!isCaptureActive() || state.value.samples.any { it.id == sample.id }) return@withLock
        val bounded = sample.copy(
            title = sample.title.take(512), text = sample.text.take(1024),
            bigText = sample.bigText.take(2048), textLines = sample.textLines.take(6).map { it.take(512) },
            channelId = sample.channelId?.take(128), id = sample.id.take(128),
        )
        val rows = (listOf(bounded) + state.value.samples).take(MAX_SAMPLES)
        if (persist(rows)) mutableState.update { it.copy(samples = rows, storageError = false) }
    }

    private fun persist(rows: List<NotificationSample>): Boolean {
        var output: java.io.FileOutputStream? = null
        return try {
            val array = JSONArray()
            rows.forEach { row ->
                array.put(JSONObject().apply {
                    put("id", row.id); put("source", row.source.name)
                    put("posted", row.postedAtMillis); put("captured", row.capturedAtMillis)
                    put("title", row.title); put("text", row.text); put("bigText", row.bigText)
                    put("lines", JSONArray(row.textLines)); put("channel", row.channelId ?: JSONObject.NULL)
                })
            }
            output = file.startWrite()
            output.write(array.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
            true
        } catch (_: IOException) {
            file.failWrite(output)
            expiresAtElapsed = 0L
            mutableState.update { it.copy(captureEnabled = false, storageError = true) }
            false
        }
    }

    private fun decode(row: JSONObject): NotificationSample {
        val lines = row.getJSONArray("lines")
        return NotificationSample(
            id = row.getString("id").take(128), source = ProbeSource.valueOf(row.getString("source")),
            postedAtMillis = row.getLong("posted"), capturedAtMillis = row.getLong("captured"),
            title = row.getString("title").take(512), text = row.getString("text").take(1024),
            bigText = row.getString("bigText").take(2048),
            textLines = (0 until minOf(lines.length(), 6)).map { lines.getString(it).take(512) },
            channelId = if (row.isNull("channel")) null else row.getString("channel").take(128),
        )
    }

    private companion object {
        const val MAX_SAMPLES = 20
        const val MAX_FILE_BYTES = 1_048_576
    }
}
