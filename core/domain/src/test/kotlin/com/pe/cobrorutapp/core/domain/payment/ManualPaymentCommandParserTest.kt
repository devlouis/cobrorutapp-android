package com.pe.cobrorutapp.core.domain.payment

import com.pe.cobrorutapp.core.domain.money.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualPaymentCommandParserTest {
    @Test
    fun `parses alias and integer amount without currency symbol`() {
        val result = ManualPaymentCommandParser.parse("pablito1 20")

        assertEquals("pablito1", result?.customerQuery)
        assertEquals(2_000L, result?.amount?.minorUnits)
        assertEquals(Currency.PEN, result?.amount?.currency)
    }

    @Test
    fun `uses the last token as amount and keeps a multiword query`() {
        val result = ManualPaymentCommandParser.parse("juan mototaxi 150.50")

        assertEquals("juan mototaxi", result?.customerQuery)
        assertEquals(15_050L, result?.amount?.minorUnits)
    }

    @Test
    fun `accepts one decimal and optional soles symbol`() {
        assertEquals(2_050L, ManualPaymentCommandParser.parse("Pablito S/20.5")?.amount?.minorUnits)
        assertEquals(2_000L, ManualPaymentCommandParser.parse("Pablito S/ 20")?.amount?.minorUnits)
    }

    @Test
    fun `normalizes only repeated spaces in the captured query`() {
        val result = ManualPaymentCommandParser.parse("  Juan   Mototaxi   10  ")

        assertEquals("Juan Mototaxi", result?.customerQuery)
        assertEquals(1_000L, result?.amount?.minorUnits)
    }

    @Test
    fun `rejects incomplete ambiguous or unsupported formats`() {
        val rejected = listOf(
            "",
            "pablito1",
            "20",
            "pablito1 veinte",
            "pablito1 20.123",
            "pablito1 20,50",
            "pablito1 -20",
            "pablito1 0",
            "pablito1 ${'$'}20",
        )

        rejected.forEach { input -> assertNull(ManualPaymentCommandParser.parse(input)) }
    }

    @Test
    fun `rejects amounts that overflow cents`() {
        assertNull(ManualPaymentCommandParser.parse("pablito1 92233720368547759.99"))
    }
}
