package com.pe.cobrorutapp.feature.collection.manual

import com.pe.cobrorutapp.core.domain.matching.CustomerReference
import com.pe.cobrorutapp.core.domain.matching.MatchableCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualAssistantViewModelTest {
    @Test
    fun `single suggestion stays unselected until human review`() {
        val viewModel = ManualAssistantViewModel()

        viewModel.onInputChanged("pablito1 20")
        viewModel.saveDraft()

        val draft = viewModel.uiState.value.drafts.single()
        assertEquals(1, draft.candidates.size)
        assertNull(draft.selectedCustomer)
        assertEquals(2_000L, draft.amount.minorUnits)
    }

    @Test
    fun `ambiguous name keeps every candidate and no automatic selection`() {
        val viewModel = ManualAssistantViewModel()

        viewModel.onInputChanged("juan mototaxi 150.50")
        viewModel.saveDraft()

        val draft = viewModel.uiState.value.drafts.single()
        assertEquals(2, draft.candidates.size)
        assertNull(draft.selectedCustomer)
        assertEquals(15_050L, draft.amount.minorUnits)
    }

    @Test
    fun `invalid command does not create a draft`() {
        val viewModel = ManualAssistantViewModel()

        viewModel.onInputChanged("pablito1")
        viewModel.saveDraft()

        assertTrue(viewModel.uiState.value.drafts.isEmpty())
        assertEquals(ManualAssistantFeedback.InvalidCommand, viewModel.uiState.value.feedback)
    }

    @Test
    fun `simulation validation requires explicit customer and debt selection`() {
        val viewModel = ManualAssistantViewModel()
        viewModel.onInputChanged("pablito1 20")
        viewModel.saveDraft()
        val draftId = viewModel.uiState.value.drafts.single().id

        viewModel.validateSimulation(draftId)
        assertEquals(ManualDraftStatus.PENDING_REVIEW, viewModel.review(draftId)?.draft?.status)

        viewModel.selectCustomer(draftId, CustomerReference("synthetic-customer-1"))
        viewModel.selectDebt(draftId, "synthetic-debt-1")
        viewModel.validateSimulation(draftId)

        assertEquals(ManualDraftStatus.VALIDATED_SIMULATION, viewModel.review(draftId)?.draft?.status)
        assertEquals(0, viewModel.uiState.value.pendingCount)
    }

    @Test
    fun `draft without match can be assigned from synthetic directory`() {
        val viewModel = ManualAssistantViewModel()
        viewModel.onInputChanged("cliente desconocido 10")
        viewModel.saveDraft()
        val draftId = viewModel.uiState.value.drafts.single().id

        assertTrue(viewModel.review(draftId)?.draft?.candidates.orEmpty().isEmpty())
        assertEquals(4, viewModel.review(draftId)?.availableCustomers?.size)

        viewModel.selectCustomer(draftId, CustomerReference("synthetic-customer-4"))

        assertEquals("Rosa Elena Díaz", viewModel.review(draftId)?.selectedCustomerName)
        assertEquals(1, viewModel.review(draftId)?.availableDebts?.size)
    }

    @Test
    fun `imported directory invalidates prior synthetic validation`() {
        val viewModel = ManualAssistantViewModel()
        viewModel.onInputChanged("pablito1 25")
        viewModel.saveDraft()
        val draftId = viewModel.uiState.value.drafts.single().id
        viewModel.selectCustomer(draftId, CustomerReference("synthetic-customer-1"))
        viewModel.selectDebt(draftId, "synthetic-debt-1")
        viewModel.validateSimulation(draftId)

        viewModel.replaceDirectory(
            listOf(
                ManualDirectoryCustomer(
                    customer = MatchableCustomer(
                        reference = CustomerReference("import-row-9"),
                        displayName = "Cliente Importado",
                    ),
                    debts = listOf(SyntheticDebt("import-credit-9", "Cuota 2/20")),
                ),
            ),
        )

        val draft = viewModel.uiState.value.drafts.single()
        assertTrue(draft.candidates.isEmpty())
        assertNull(draft.selectedCustomer)
        assertNull(draft.selectedDebtReference)
        assertEquals(ManualDraftStatus.PENDING_REVIEW, draft.status)
        assertEquals(1, viewModel.review(draft.id)?.availableCustomers?.size)
    }
}
