package com.pe.cobrorutapp.core.domain.payment

import com.pe.cobrorutapp.core.domain.money.Currency
import com.pe.cobrorutapp.core.domain.money.Money

data class ManualPaymentCommand(
    val customerQuery: String,
    val amount: Money,
)

object ManualPaymentCommandParser {
    fun parse(input: String): ManualPaymentCommand? {
        val match = COMMAND_FORMAT.matchEntire(input.trim()) ?: return null
        val customerQuery = match.groupValues[1]
            .trim()
            .replace(REPEATED_SPACES, " ")
        if (customerQuery.isEmpty()) return null

        val whole = match.groupValues[2].toLongOrNull() ?: return null
        val fraction = match.groupValues[3]
            .padEnd(2, '0')
            .ifEmpty { "00" }
            .toLong()
        if (whole > (Long.MAX_VALUE - fraction) / 100L) return null
        val minorUnits = whole * 100L + fraction
        if (minorUnits == 0L) return null

        return ManualPaymentCommand(
            customerQuery = customerQuery,
            amount = Money(minorUnits = minorUnits, currency = Currency.PEN),
        )
    }

    private val COMMAND_FORMAT = Regex(
        pattern = "^(.+?)\\s+(?:S/\\s*)?(\\d+)(?:\\.(\\d{1,2}))?$",
        option = RegexOption.IGNORE_CASE,
    )
    private val REPEATED_SPACES = "\\s+".toRegex()
}
