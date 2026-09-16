package com.pe.cobrorutapp.core.data.probe

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pe.cobrorutapp.core.domain.probe.NotificationSample
import com.pe.cobrorutapp.core.domain.probe.ProbeSource
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalNotificationProbeRepositoryTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(base.noBackupFilesDir, "probe-test-${UUID.randomUUID()}").apply { mkdirs() }
    private val context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory
    }
    private val scopes = mutableListOf<CoroutineScope>()

    private suspend fun repository(sessionDurationMillis: Long = 600_000): LocalNotificationProbeRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes.add(it) }
        return LocalNotificationProbeRepository(context, scope, sessionDurationMillis).also { repository ->
            withTimeout(10_000) { repository.state.first { it.ready || it.storageError } }
        }
    }

    private fun sample(id: String) = NotificationSample(
        id, ProbeSource.YAPE, 1000, 2000, "Yape", "Persona sintética te yapeó S/ 1.00", "", emptyList(), "test",
    )

    @After fun cleanup() {
        scopes.forEach { it.cancel() }
        directory.deleteRecursively()
    }

    @Test fun captureRequiresActiveConnectedSessionAndStopsExplicitly() = runBlocking {
        val repository = repository()
        repository.record(sample("before"))
        repository.startSession()
        assertFalse(repository.isCaptureActive())
        repository.setListenerConnected(true)
        repository.startSession()
        repository.record(sample("during"))
        repository.stopSession()
        repository.record(sample("after"))
        assertEquals(listOf("during"), repository.state.value.samples.map { it.id })
    }

    @Test fun sessionExpiresWithoutAnActiveScreen() = runBlocking {
        val repository = repository(sessionDurationMillis = 50)
        repository.setListenerConnected(true)
        repository.startSession()
        withTimeout(10_000) { repository.state.first { !it.captureEnabled } }
        repository.record(sample("expired"))
        assertFalse(repository.isCaptureActive())
        assertTrue(repository.state.value.samples.isEmpty())
    }

    @Test fun disconnectStopsCaptureAndReconnectRequiresANewSession() = runBlocking {
        val repository = repository()
        repository.setListenerConnected(true)
        repository.startSession()
        repository.setListenerConnected(false)
        repository.setListenerConnected(true)
        repository.record(sample("after-reconnect"))
        assertFalse(repository.isCaptureActive())
        assertTrue(repository.state.value.samples.isEmpty())
    }

    @Test fun exactReplayIsIgnoredButSeparateEventsWithEqualContentSurvive() = runBlocking {
        val repository = repository()
        repository.setListenerConnected(true)
        repository.startSession()
        repository.record(sample("event-1"))
        repository.record(sample("event-1"))
        repository.record(sample("event-2"))
        assertEquals(2, repository.state.value.samples.size)
    }

    @Test fun samplesSurviveRecreationButCaptureDoesNotResumeAndClearIsDurable() = runBlocking {
        val first = repository()
        first.setListenerConnected(true)
        first.startSession()
        first.record(sample("persistent"))
        first.stopSession()
        scopes.forEach { it.cancel() }
        val second = repository()
        assertEquals("persistent", second.state.value.samples.single().id)
        assertFalse(second.isCaptureActive())
        assertFalse(second.state.value.listenerConnected)
        second.clear()
        scopes.forEach { it.cancel() }
        assertTrue(repository().state.value.samples.isEmpty())
    }

    @Test fun boundedRetentionKeepsNewestTwentySamples() = runBlocking {
        val repository = repository()
        repository.setListenerConnected(true)
        repository.startSession()
        repeat(22) { repository.record(sample("event-$it")) }
        assertEquals(20, repository.state.value.samples.size)
        assertEquals("event-21", repository.state.value.samples.first().id)
        assertEquals("event-2", repository.state.value.samples.last().id)
    }

    @Test fun corruptedFileDoesNotStartCaptureAndCanBeExplicitlyCleared() = runBlocking {
        File(directory, "notification_probe.json").writeText("invalid-json")
        val repository = repository()
        assertTrue(repository.state.value.storageError)
        assertFalse(repository.state.value.ready)
        repository.setListenerConnected(true)
        repository.startSession()
        assertFalse(repository.isCaptureActive())
        repository.clear()
        assertTrue(repository.state.value.ready)
        assertFalse(repository.state.value.storageError)
        assertTrue(repository.state.value.samples.isEmpty())
    }
}
