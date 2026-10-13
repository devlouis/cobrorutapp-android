package com.pe.cobrorutapp.core.domain.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerNameMatcherTest {
    @Test
    fun `normalizes case accents punctuation and spaces for exact names`() {
        val customer = customer("customer-1", "María del Pilar")

        val result = CustomerNameMatcher.match("  MARIA,  DEL PILAR ", listOf(customer))

        assertEquals(
            listOf(candidate(customer, CustomerMatchReason.EXACT_NORMALIZED_NAME)),
            result,
        )
    }

    @Test
    fun `returns every homonym without choosing one`() {
        val first = customer("customer-1", "Alex Vega")
        val second = customer("customer-2", "Alex Vega")

        val result = CustomerNameMatcher.match("Alex Vega", listOf(first, second))

        assertEquals(2, result.size)
        assertEquals(
            setOf(first.reference, second.reference),
            result.map(CustomerCandidate::reference).toSet(),
        )
    }

    @Test
    fun `matches only aliases explicitly attached to customers`() {
        val customer = customer("customer-1", "Rosa Campos", aliases = setOf("Rosita"))
        val unrelated = customer("customer-2", "Rosita Calderón")

        val result = CustomerNameMatcher.match("Rosita", listOf(customer, unrelated))

        assertEquals(listOf(candidate(customer, CustomerMatchReason.CONFIRMED_ALIAS)), result)
    }

    @Test
    fun `returns all customers sharing an ambiguous confirmed alias`() {
        val first = customer("customer-1", "Rosa Campos", aliases = setOf("Vecina"))
        val second = customer("customer-2", "Ana Torres", aliases = setOf("Vecina"))

        val result = CustomerNameMatcher.match("Vecina", listOf(first, second))

        assertEquals(2, result.size)
        assertTrue(result.all { it.reason == CustomerMatchReason.CONFIRMED_ALIAS })
    }

    @Test
    fun `suggests same complete name terms in another order`() {
        val customer = customer("customer-1", "Elena Cruz Soto")

        val result = CustomerNameMatcher.match("Soto Elena Cruz", listOf(customer))

        assertEquals(listOf(candidate(customer, CustomerMatchReason.SAME_NAME_TERMS)), result)
    }

    @Test
    fun `does not suggest partial or fuzzy names`() {
        val directory = listOf(customer("customer-1", "María Elena Cruz"))

        assertTrue(CustomerNameMatcher.match("María Cruz", directory).isEmpty())
        assertTrue(CustomerNameMatcher.match("Maria", directory).isEmpty())
        assertTrue(CustomerNameMatcher.match("Maria Elna Cruz", directory).isEmpty())
    }

    @Test
    fun `exact name takes precedence over an alias on another customer`() {
        val exact = customer("customer-1", "Luz Marina")
        val alias = customer("customer-2", "Lucía Soto", aliases = setOf("Luz Marina"))

        val result = CustomerNameMatcher.match("Luz Marina", listOf(exact, alias))

        assertEquals(listOf(candidate(exact, CustomerMatchReason.EXACT_NORMALIZED_NAME)), result)
    }

    @Test
    fun `preserves original display name and deduplicates repeated references`() {
        val customer = customer("customer-1", "José Núñez")

        val result = CustomerNameMatcher.match("jose nunez", listOf(customer, customer))

        assertEquals(1, result.size)
        assertEquals("José Núñez", result.single().displayName)
    }

    @Test
    fun `blank query and empty directory have no candidates`() {
        assertTrue(CustomerNameMatcher.match("  ", listOf(customer("customer-1", "Ana Paz"))).isEmpty())
        assertTrue(CustomerNameMatcher.match("Ana Paz", emptyList()).isEmpty())
    }

    private fun customer(
        reference: String,
        displayName: String,
        aliases: Set<String> = emptySet(),
    ) = MatchableCustomer(CustomerReference(reference), displayName, aliases)

    private fun candidate(
        customer: MatchableCustomer,
        reason: CustomerMatchReason,
    ) = CustomerCandidate(customer.reference, customer.displayName, reason)
}
