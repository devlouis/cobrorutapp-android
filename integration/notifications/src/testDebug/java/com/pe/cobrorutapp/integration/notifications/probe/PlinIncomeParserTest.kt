package com.pe.cobrorutapp.integration.notifications.probe

import com.pe.cobrorutapp.core.domain.money.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlinIncomeParserTest {
    @Test
    fun `parses observed Plin wording into cents and preserves sender`() {
        val cases = listOf(
            "Ana Pérez te ha plineado S/0.01" to 1L,
            "Luis Torres te ha plineado S/1.00" to 100L,
            "María del Río te ha plineado S/123.45" to 12_345L,
        )

        cases.forEach { (text, expectedMinorUnits) ->
            val result = PlinIncomeParser.parse(snapshot(text = text))

            assertEquals(expectedMinorUnits, result?.amount?.minorUnits)
            assertEquals(Currency.PEN, result?.amount?.currency)
            assertEquals(text.substringBefore(" te ha plineado"), result?.senderDisplayName)
        }
    }

    @Test
    fun `accepts harmless whitespace and case differences`() {
        val result = PlinIncomeParser.parse(
            snapshot(text = "  Ana   Pérez TE HA PLINEADO S/ 2.50  "),
        )

        assertEquals("Ana Pérez", result?.senderDisplayName)
        assertEquals(250L, result?.amount?.minorUnits)
    }

    @Test
    fun `accepts identical repetitions across notification fields`() {
        val phrase = "Ana Pérez te ha plineado S/2.50"

        val result = PlinIncomeParser.parse(
            snapshot(text = phrase, bigText = phrase, textLines = listOf(phrase)),
        )

        assertEquals(250L, result?.amount?.minorUnits)
    }

    @Test
    fun `rejects conflicting parseable values in one notification`() {
        val result = PlinIncomeParser.parse(
            snapshot(
                text = "Ana Pérez te ha plineado S/2.50",
                bigText = "Ana Pérez te ha plineado S/3.50",
            ),
        )

        assertNull(result)
    }

    @Test
    fun `rejects wording and money formats not observed`() {
        val rejected = listOf(
            "Ana Pérez te ah plineado S/1.00",
            "Ana Pérez te plineó S/1.00",
            "Ana Pérez te ha plineado $1.00",
            "Ana Pérez te ha plineado S/1",
            "Ana Pérez te ha plineado S/1.0",
            "Ana Pérez te ha plineado S/1,00",
            "Ana Pérez te ha plineado S/-1.00",
            "Ana Pérez te ha plineado S/0.00",
        )

        rejected.forEach { text -> assertNull(PlinIncomeParser.parse(snapshot(text = text))) }
    }

    @Test
    fun `rejects overflow and unrelated notification text`() {
        assertNull(
            PlinIncomeParser.parse(
                snapshot(text = "Ana Pérez te ha plineado S/92233720368547759.99"),
            ),
        )
        assertNull(PlinIncomeParser.parse(snapshot(text = "Información de tu cuenta")))
        assertNull(PlinIncomeParser.parse(snapshot()))
    }

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
