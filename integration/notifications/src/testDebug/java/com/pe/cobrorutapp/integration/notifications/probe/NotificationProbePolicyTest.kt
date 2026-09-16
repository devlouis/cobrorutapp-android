package com.pe.cobrorutapp.integration.notifications.probe

import com.pe.cobrorutapp.core.domain.probe.ProbeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationProbePolicyTest {
    @Test
    fun `allowlist accepts only exact supported package names`() {
        assertEquals(
            ProbeSource.YAPE,
            NotificationProbePolicy.sourceFor(NotificationProbePolicy.YAPE_PACKAGE),
        )
        assertEquals(
            ProbeSource.INTERBANK,
            NotificationProbePolicy.sourceFor(NotificationProbePolicy.INTERBANK_PACKAGE),
        )
        assertNull(NotificationProbePolicy.sourceFor("com.example.yapeapp"))
        assertNull(NotificationProbePolicy.sourceFor("${NotificationProbePolicy.YAPE_PACKAGE}.debug"))
    }

    @Test
    fun `accepts synthetic Yape income phrases including accents`() {
        assertTrue(
            NotificationProbePolicy.isPossibleIncome(
                ProbeSource.YAPE,
                snapshot(text = "María te yapeó"),
            ),
        )
        assertTrue(
            NotificationProbePolicy.isPossibleIncome(
                ProbeSource.YAPE,
                snapshot(title = "Yape", bigText = "Has recibido un abono"),
            ),
        )
        assertTrue(
            NotificationProbePolicy.isPossibleIncome(
                ProbeSource.YAPE,
                snapshot(text = "Maria te yapeo"),
            ),
        )
    }

    @Test
    fun `accepts synthetic Plin income phrases only for Interbank`() {
        val incomePhrases = listOf(
            "Plin: PERSONA te plineó S/2.00",
            "Plin: PERSONA te ha plineado S/2.00",
        )

        incomePhrases.forEach { text ->
            assertTrue(
                NotificationProbePolicy.isPossibleIncome(
                    ProbeSource.INTERBANK,
                    snapshot(text = text),
                ),
            )
            assertFalse(
                NotificationProbePolicy.isPossibleIncome(
                    ProbeSource.YAPE,
                    snapshot(text = text),
                ),
            )
        }
    }

    @Test
    fun `reported Plin wording supplies context without a separate Plin title`() {
        assertTrue(
            NotificationProbePolicy.isPossibleIncome(
                ProbeSource.INTERBANK,
                snapshot(text = "PERSONA te ha plineado S/2.00"),
            ),
        )
        assertFalse(
            NotificationProbePolicy.isPossibleIncome(
                ProbeSource.INTERBANK,
                snapshot(text = "Plin: PERSONA te ah plineado S/2.00"),
            ),
        )
        assertFalse(
            NotificationProbePolicy.isPossibleIncome(
                ProbeSource.INTERBANK,
                snapshot(text = "Has recibido una transferencia"),
            ),
        )
    }

    @Test
    fun `rejects outgoing promotional and security messages`() {
        val rejected = listOf(
            "Yape: enviaste dinero",
            "Plin: pago realizado",
            "Plin: PERSONA te ha plineado S/2.00 promoción",
            "Plin: PERSONA te ha plineado S/2.00 código OTP",
        )

        rejected.forEach { text ->
            assertFalse(
                NotificationProbePolicy.isPossibleIncome(ProbeSource.YAPE, snapshot(text = text)),
            )
            assertFalse(
                NotificationProbePolicy.isPossibleIncome(ProbeSource.INTERBANK, snapshot(text = text)),
            )
        }
    }

    @Test
    fun `rejects empty and unknown content`() {
        assertFalse(NotificationProbePolicy.isPossibleIncome(ProbeSource.YAPE, snapshot()))
        assertFalse(
            NotificationProbePolicy.isPossibleIncome(
                ProbeSource.INTERBANK,
                snapshot(title = "Interbank", text = "Información de tu cuenta"),
            ),
        )
    }

    @Test
    fun `rejects group summaries and ongoing notifications`() {
        val income = snapshot(text = "María te yapeó")

        assertFalse(NotificationProbePolicy.shouldRecord(ProbeSource.YAPE, true, false, income))
        assertFalse(NotificationProbePolicy.shouldRecord(ProbeSource.YAPE, false, true, income))
        assertTrue(NotificationProbePolicy.shouldRecord(ProbeSource.YAPE, false, false, income))
    }

    @Test
    fun `snapshot extractor bounds fields and text lines`() {
        val extracted = NotificationSnapshotExtractor.create(
            title = "a".repeat(600),
            text = "text",
            bigText = null,
            textLines = Array(8) { "line-$it" },
            channelId = "c".repeat(600),
        )

        assertEquals(500, extracted.title.length)
        assertEquals(5, extracted.textLines.size)
        assertEquals(500, extracted.channelId?.length)
    }

    @Test
    fun `same technical notification produces same fingerprint`() {
        val snapshot = snapshot(title = "Yape", text = "Ana te yapeó")

        val first = fingerprint(key = "key-1", postedAt = 100L, snapshot = snapshot)
        val second = fingerprint(key = "key-1", postedAt = 100L, snapshot = snapshot)

        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun `same visible content remains distinct for another notification key or update time`() {
        val snapshot = snapshot(title = "Yape", text = "Ana te yapeó")
        val original = fingerprint(key = "key-1", postedAt = 100L, snapshot = snapshot)

        assertNotEquals(original, fingerprint(key = "key-2", postedAt = 100L, snapshot = snapshot))
        assertNotEquals(original, fingerprint(key = "key-1", postedAt = 101L, snapshot = snapshot))
    }

    private fun fingerprint(
        key: String,
        postedAt: Long,
        snapshot: NotificationSnapshot,
    ): String = NotificationFingerprint.create(
        packageName = NotificationProbePolicy.YAPE_PACKAGE,
        notificationKey = key,
        postedAtMillis = postedAt,
        snapshot = snapshot,
    )

    private fun snapshot(
        title: String = "",
        text: String = "",
        bigText: String = "",
        textLines: List<String> = emptyList(),
    ) = NotificationSnapshot(
        title = title,
        text = text,
        bigText = bigText,
        textLines = textLines,
        channelId = null,
    )
}
