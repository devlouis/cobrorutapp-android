package com.pe.cobrorutapp.integration.notifications.probe

import com.pe.cobrorutapp.core.domain.probe.ProbeSource
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal data class NotificationSnapshot(
    val title: String,
    val text: String,
    val bigText: String,
    val textLines: List<String>,
    val channelId: String?,
) {
    fun searchableText(): String = buildList {
        add(title)
        add(text)
        add(bigText)
        addAll(textLines)
    }.joinToString(separator = " ")
}

internal object NotificationSnapshotExtractor {
    private const val MAX_FIELD_LENGTH = 500
    private const val MAX_TEXT_LINES = 5

    fun create(
        title: CharSequence?,
        text: CharSequence?,
        bigText: CharSequence?,
        textLines: Array<out CharSequence>?,
        channelId: String?,
    ): NotificationSnapshot {
        fun CharSequence?.limited(): String = this?.toString()?.take(MAX_FIELD_LENGTH).orEmpty()

        return NotificationSnapshot(
            title = title.limited(),
            text = text.limited(),
            bigText = bigText.limited(),
            textLines = textLines
                ?.asSequence()
                ?.take(MAX_TEXT_LINES)
                ?.map { it.limited() }
                ?.toList()
                .orEmpty(),
            channelId = channelId?.take(MAX_FIELD_LENGTH),
        )
    }
}

internal object NotificationProbePolicy {
    const val YAPE_PACKAGE = "com.bcp.innovacxion.yapeapp"
    const val INTERBANK_PACKAGE = "pe.com.interbank.mobilebanking"

    private val rejectedTokens = listOf(
        "enviaste",
        "has enviado",
        "envío realizado",
        "envio realizado",
        "pagaste",
        "pago realizado",
        "promoción",
        "promocion",
        "promo",
        "código",
        "codigo",
        "clave",
        "otp",
    )

    fun sourceFor(packageName: String): ProbeSource? = when (packageName) {
        YAPE_PACKAGE -> ProbeSource.YAPE
        INTERBANK_PACKAGE -> ProbeSource.INTERBANK
        else -> null
    }

    fun isPossibleIncome(source: ProbeSource, snapshot: NotificationSnapshot): Boolean {
        val content = Normalizer.normalize(
            snapshot.searchableText().lowercase(Locale.ROOT),
            Normalizer.Form.NFD,
        ).replace("\\p{M}+".toRegex(), "")
        if (rejectedTokens.any(content::contains)) return false

        return when (source) {
            ProbeSource.YAPE ->
                content.contains("te yapeo") ||
                    content.contains("te yapearon") ||
                    ((content.contains("recibiste") || content.contains("has recibido")) &&
                        content.contains("yape"))

            ProbeSource.INTERBANK ->
                content.contains("plin") &&
                    (content.contains("recibiste") ||
                        content.contains("has recibido") ||
                        content.contains("te plineo") ||
                        content.contains("te ha plineado"))
        }
    }

    fun shouldRecord(
        source: ProbeSource,
        isGroupSummary: Boolean,
        isOngoing: Boolean,
        snapshot: NotificationSnapshot,
    ): Boolean = !isGroupSummary && !isOngoing && isPossibleIncome(source, snapshot)
}

internal object NotificationFingerprint {
    fun create(
        packageName: String,
        notificationKey: String,
        postedAtMillis: Long,
        snapshot: NotificationSnapshot,
    ): String {
        val fields = buildList {
            add(packageName)
            add(notificationKey)
            add(postedAtMillis.toString())
            add(snapshot.title)
            add(snapshot.text)
            add(snapshot.bigText)
            addAll(snapshot.textLines)
            add(snapshot.channelId.orEmpty())
        }
        val canonical = fields.joinToString(separator = "") { value -> "${value.length}:$value" }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
