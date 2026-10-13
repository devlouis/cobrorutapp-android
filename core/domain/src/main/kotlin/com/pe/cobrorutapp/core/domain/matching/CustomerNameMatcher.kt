package com.pe.cobrorutapp.core.domain.matching

import java.text.Normalizer
import java.util.Locale

@JvmInline
value class CustomerReference(val value: String) {
    init {
        require(value.isNotBlank()) { "Customer reference cannot be blank" }
    }
}

data class MatchableCustomer(
    val reference: CustomerReference,
    val displayName: String,
    val confirmedAliases: Set<String> = emptySet(),
) {
    init {
        require(displayName.isNotBlank()) { "Customer display name cannot be blank" }
    }
}

enum class CustomerMatchReason {
    EXACT_NORMALIZED_NAME,
    CONFIRMED_ALIAS,
    SAME_NAME_TERMS,
}

data class CustomerCandidate(
    val reference: CustomerReference,
    val displayName: String,
    val reason: CustomerMatchReason,
)

object CustomerNameMatcher {
    fun match(
        query: String,
        directory: List<MatchableCustomer>,
    ): List<CustomerCandidate> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        return find(directory, CustomerMatchReason.EXACT_NORMALIZED_NAME) { customer ->
            normalize(customer.displayName) == normalizedQuery
        }.ifEmpty {
            find(directory, CustomerMatchReason.CONFIRMED_ALIAS) { customer ->
                customer.confirmedAliases.any { alias -> normalize(alias) == normalizedQuery }
            }
        }.ifEmpty {
            val queryTerms = normalizedQuery.terms()
            if (queryTerms.size < 2) {
                emptyList()
            } else {
                find(directory, CustomerMatchReason.SAME_NAME_TERMS) { customer ->
                    normalize(customer.displayName).terms() == queryTerms
                }
            }
        }
    }

    private fun find(
        directory: List<MatchableCustomer>,
        reason: CustomerMatchReason,
        predicate: (MatchableCustomer) -> Boolean,
    ): List<CustomerCandidate> = directory
        .asSequence()
        .filter(predicate)
        .distinctBy(MatchableCustomer::reference)
        .map { customer ->
            CustomerCandidate(
                reference = customer.reference,
                displayName = customer.displayName,
                reason = reason,
            )
        }
        .toList()

    private fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace(NON_ALPHANUMERIC, " ")
        .trim()
        .replace(REPEATED_SPACES, " ")

    private fun String.terms(): Map<String, Int> = split(' ')
        .filter(String::isNotEmpty)
        .groupingBy { it }
        .eachCount()

    private val COMBINING_MARKS = "\\p{M}+".toRegex()
    private val NON_ALPHANUMERIC = "[^\\p{L}\\p{N}]+".toRegex()
    private val REPEATED_SPACES = "\\s+".toRegex()
}
