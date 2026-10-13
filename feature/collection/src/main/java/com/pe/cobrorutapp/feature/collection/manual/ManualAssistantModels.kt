package com.pe.cobrorutapp.feature.collection.manual

import com.pe.cobrorutapp.core.domain.matching.CustomerCandidate
import com.pe.cobrorutapp.core.domain.matching.CustomerReference
import com.pe.cobrorutapp.core.domain.matching.MatchableCustomer
import com.pe.cobrorutapp.core.domain.money.Money

enum class ManualDraftStatus {
    PENDING_REVIEW,
    VALIDATED_SIMULATION,
}

data class SyntheticDebt(
    val reference: String,
    val label: String,
)

data class ManualDirectoryCustomer(
    val customer: MatchableCustomer,
    val debts: List<SyntheticDebt>,
)

data class ManualPaymentDraft(
    val id: Long,
    val customerQuery: String,
    val amount: Money,
    val candidates: List<CustomerCandidate>,
    val selectedCustomer: CustomerReference? = null,
    val selectedDebtReference: String? = null,
    val status: ManualDraftStatus = ManualDraftStatus.PENDING_REVIEW,
)

data class ManualAssistantUiState(
    val input: String = "",
    val drafts: List<ManualPaymentDraft> = emptyList(),
    val feedback: ManualAssistantFeedback? = null,
) {
    val pendingCount: Int
        get() = drafts.count { it.status == ManualDraftStatus.PENDING_REVIEW }
}

sealed interface ManualAssistantFeedback {
    data object InvalidCommand : ManualAssistantFeedback

    data class DraftSaved(
        val candidateCount: Int,
    ) : ManualAssistantFeedback
}

data class ManualDraftReview(
    val draft: ManualPaymentDraft,
    val availableCustomers: List<ManualCustomerOption>,
    val availableDebts: List<SyntheticDebt>,
    val selectedCustomerName: String?,
) {
    val canRequestConfirmation: Boolean
        get() = draft.status == ManualDraftStatus.PENDING_REVIEW &&
            draft.selectedCustomer != null &&
            draft.selectedDebtReference != null
}

data class ManualCustomerOption(
    val reference: CustomerReference,
    val displayName: String,
    val isSuggested: Boolean,
)

internal object SyntheticCollectionDirectory {
    val entries = listOf(
        ManualDirectoryCustomer(
            customer = MatchableCustomer(
                reference = CustomerReference("synthetic-customer-1"),
                displayName = "Pablo Ramírez",
                confirmedAliases = setOf("pablito1"),
            ),
            debts = listOf(
                SyntheticDebt("synthetic-debt-1", "Préstamo A · cuota diaria S/ 20.00"),
            ),
        ),
        ManualDirectoryCustomer(
            customer = MatchableCustomer(
                reference = CustomerReference("synthetic-customer-2"),
                displayName = "Juan Mototaxi",
            ),
            debts = listOf(
                SyntheticDebt("synthetic-debt-2", "Préstamo A · cuota diaria S/ 25.00"),
                SyntheticDebt("synthetic-debt-3", "Préstamo B · cuota diaria S/ 15.00"),
            ),
        ),
        ManualDirectoryCustomer(
            customer = MatchableCustomer(
                reference = CustomerReference("synthetic-customer-3"),
                displayName = "Juan Mototaxi",
            ),
            debts = listOf(
                SyntheticDebt("synthetic-debt-4", "Préstamo A · cuota diaria S/ 30.00"),
            ),
        ),
        ManualDirectoryCustomer(
            customer = MatchableCustomer(
                reference = CustomerReference("synthetic-customer-4"),
                displayName = "Rosa Elena Díaz",
                confirmedAliases = setOf("rosita"),
            ),
            debts = listOf(
                SyntheticDebt("synthetic-debt-5", "Préstamo A · cuota diaria S/ 18.00"),
            ),
        ),
    )
}
