package com.pe.cobrorutapp.integration.notifications.probe

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pe.cobrorutapp.core.domain.probe.NotificationProbeRepository
import com.pe.cobrorutapp.core.domain.probe.NotificationSample
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomeProbeListener : NotificationListenerService() {
    @Inject lateinit var repository: NotificationProbeRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clock: Clock = Clock.systemUTC()

    override fun onListenerConnected() {
        super.onListenerConnected()
        repository.setListenerConnected(true)
    }

    override fun onListenerDisconnected() {
        repository.setListenerConnected(false)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val source = NotificationProbePolicy.sourceFor(statusBarNotification.packageName) ?: return
        if (!repository.isCaptureActive()) return

        val notification = statusBarNotification.notification
        val isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        val isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0
        if (isGroupSummary || isOngoing) return

        val snapshot = notification.toProbeSnapshot()
        if (!NotificationProbePolicy.shouldRecord(
                source = source,
                isGroupSummary = isGroupSummary,
                isOngoing = isOngoing,
                snapshot = snapshot,
            )
        ) return

        val sample = NotificationSample(
            id = NotificationFingerprint.create(
                packageName = statusBarNotification.packageName,
                notificationKey = statusBarNotification.key,
                postedAtMillis = statusBarNotification.postTime,
                snapshot = snapshot,
            ),
            source = source,
            postedAtMillis = statusBarNotification.postTime,
            capturedAtMillis = clock.millis(),
            title = snapshot.title,
            text = snapshot.text,
            bigText = snapshot.bigText,
            textLines = snapshot.textLines,
            channelId = snapshot.channelId,
        )
        serviceScope.launch { repository.record(sample) }
    }

    override fun onDestroy() {
        repository.setListenerConnected(false)
        serviceScope.cancel()
        super.onDestroy()
    }
}

private fun Notification.toProbeSnapshot(): NotificationSnapshot =
    NotificationSnapshotExtractor.create(
        title = extras.getCharSequence(Notification.EXTRA_TITLE),
        text = extras.getCharSequence(Notification.EXTRA_TEXT),
        bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
        textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES),
        channelId = channelId,
    )
