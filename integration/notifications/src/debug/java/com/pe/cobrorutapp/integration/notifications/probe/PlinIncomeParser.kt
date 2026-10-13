package com.pe.cobrorutapp.integration.notifications.probe

import com.pe.cobrorutapp.core.domain.money.Currency
import com.pe.cobrorutapp.core.domain.money.Money

internal data class ParsedPlinIncome(
    val senderDisplayName: String,
    val amount: Money,
)

internal object PlinIncomeParser {
    fun parse(snapshot: NotificationSnapshot): ParsedPlinIncome? {
        val parsed = snapshot.fields()
            .mapNotNull(::parseField)
            .distinct()

        return parsed.singleOrNull()
    }

    private fun parseField(value: String): ParsedPlinIncome? {
        val match = OBSERVED_FORMAT.matchEntire(value.trim()) ?: return null
        val sender = match.groupValues[1].trim().replace(REPEATED_SPACES, " ")
        if (sender.isEmpty()) return null

        val whole = match.groupValues[2].toLongOrNull() ?: return null
        val fraction = match.groupValues[3].toLong()
        if (whole > (Long.MAX_VALUE - fraction) / 100L) return null
        val minorUnits = whole * 100L + fraction
        if (minorUnits == 0L) return null

        return ParsedPlinIncome(
            senderDisplayName = sender,
            amount = Money(minorUnits = minorUnits, currency = Currency.PEN),
        )
    }

    private fun NotificationSnapshot.fields(): Sequence<String> = sequence {
        yield(title)
        yield(text)
        yield(bigText)
        yieldAll(textLines)
    }.filter(String::isNotBlank)

    private val OBSERVED_FORMAT = Regex(
        pattern = "^(.+?)\\s+te\\s+ha\\s+plineado\\s+S/\\s*(\\d+)\\.(\\d{2})$",
        option = RegexOption.IGNORE_CASE,
    )
    private val REPEATED_SPACES = "\\s+".toRegex()
}
