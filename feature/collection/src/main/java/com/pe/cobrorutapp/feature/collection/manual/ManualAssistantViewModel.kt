package com.pe.cobrorutapp.feature.collection.manual

import androidx.lifecycle.ViewModel
import com.pe.cobrorutapp.core.domain.matching.CustomerNameMatcher
import com.pe.cobrorutapp.core.domain.matching.CustomerReference
import com.pe.cobrorutapp.core.domain.payment.ManualPaymentCommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ManualAssistantViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ManualAssistantUiState())
    val uiState: StateFlow<ManualAssistantUiState> = _uiState.asStateFlow()

    private var nextDraftId = 1L
    private var directory = SyntheticCollectionDirectory.entries

    fun replaceDirectory(entries: List<ManualDirectoryCustomer>) {
        directory = entries.ifEmpty { SyntheticCollectionDirectory.entries }
        _uiState.update { state ->
            state.copy(
                drafts = state.drafts.map { draft ->
                    draft.copy(
                        candidates = CustomerNameMatcher.match(
                            query = draft.customerQuery,
                            directory = directory.map(ManualDirectoryCustomer::customer),
                        ),
                        selectedCustomer = null,
                        selectedDebtReference = null,
                        status = ManualDraftStatus.PENDING_REVIEW,
                    )
                },
            )
        }
    }

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value, feedback = null) }
    }

    fun saveDraft() {
        val command = ManualPaymentCommandParser.parse(_uiState.value.input)
        if (command == null) {
            _uiState.update { it.copy(feedback = ManualAssistantFeedback.InvalidCommand) }
            return
        }

        val candidates = CustomerNameMatcher.match(
            query = command.customerQuery,
            directory = directory.map(ManualDirectoryCustomer::customer),
        )
        val draft = ManualPaymentDraft(
            id = nextDraftId++,
            customerQuery = command.customerQuery,
            amount = command.amount,
            candidates = candidates,
        )
        _uiState.update {
            it.copy(
                input = "",
                drafts = listOf(draft) + it.drafts,
                feedback = ManualAssistantFeedback.DraftSaved(candidates.size),
            )
        }
    }

    fun review(draftId: Long): ManualDraftReview? {
        val draft = _uiState.value.drafts.firstOrNull { it.id == draftId } ?: return null
        val suggestedReferences = draft.candidates.mapTo(mutableSetOf()) { it.reference }
        return ManualDraftReview(
            draft = draft,
            availableCustomers = directory.map(ManualDirectoryCustomer::customer).map { customer ->
                ManualCustomerOption(
                    reference = customer.reference,
                    displayName = customer.displayName,
                    isSuggested = customer.reference in suggestedReferences,
                )
            },
            availableDebts = debts(draft.selectedCustomer),
            selectedCustomerName = customerName(draft.selectedCustomer),
        )
    }

    fun selectCustomer(draftId: Long, customer: CustomerReference) {
        updateDraft(draftId) { draft ->
            if (directory.none { it.customer.reference == customer }) {
                return@updateDraft draft
            }
            draft.copy(
                selectedCustomer = customer,
                selectedDebtReference = null,
            )
        }
    }

    fun selectDebt(draftId: Long, debtReference: String) {
        updateDraft(draftId) { draft ->
            val isAvailable = debts(draft.selectedCustomer)
                .any { it.reference == debtReference }
            if (isAvailable) draft.copy(selectedDebtReference = debtReference) else draft
        }
    }

    fun validateSimulation(draftId: Long) {
        updateDraft(draftId) { draft ->
            val review = ManualDraftReview(
                draft = draft,
                availableCustomers = emptyList(),
                availableDebts = debts(draft.selectedCustomer),
                selectedCustomerName = customerName(draft.selectedCustomer),
            )
            if (review.canRequestConfirmation) {
                draft.copy(status = ManualDraftStatus.VALIDATED_SIMULATION)
            } else {
                draft
            }
        }
    }

    private fun updateDraft(
        draftId: Long,
        transform: (ManualPaymentDraft) -> ManualPaymentDraft,
    ) {
        _uiState.update { state ->
            state.copy(
                drafts = state.drafts.map { draft ->
                    if (draft.id == draftId) transform(draft) else draft
                },
            )
        }
    }

    private fun customerName(reference: CustomerReference?): String? = directory
        .firstOrNull { it.customer.reference == reference }
        ?.customer
        ?.displayName

    private fun debts(reference: CustomerReference?): List<SyntheticDebt> = directory
        .firstOrNull { it.customer.reference == reference }
        ?.debts
        .orEmpty()
}
