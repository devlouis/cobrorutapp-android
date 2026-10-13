package com.pe.cobrorutapp.core.domain.money

enum class Currency {
    PEN,
}

data class Money(
    val minorUnits: Long,
    val currency: Currency,
) {
    init {
        require(minorUnits >= 0) { "Money cannot have negative minor units" }
    }
}
