package com.pe.cobrorutapp.feature.collection.manual

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pe.cobrorutapp.core.domain.matching.CustomerReference
import com.pe.cobrorutapp.feature.collection.R

@Composable
fun ManualDraftReviewRoute(
    viewModel: ManualAssistantViewModel,
    draftId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ManualDraftReviewScreen(
        review = state.drafts.firstOrNull { it.id == draftId }?.let { viewModel.review(draftId) },
        onSelectCustomer = { viewModel.selectCustomer(draftId, it) },
        onSelectDebt = { viewModel.selectDebt(draftId, it) },
        onValidateSimulation = { viewModel.validateSimulation(draftId) },
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualDraftReviewScreen(
    review: ManualDraftReview?,
    onSelectCustomer: (CustomerReference) -> Unit,
    onSelectDebt: (String) -> Unit,
    onValidateSimulation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_review_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        if (review == null) {
            Text(
                text = stringResource(R.string.manual_review_not_found),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ReviewSummary(review)
            }
            item {
                Text(
                    text = stringResource(R.string.manual_review_choose_customer),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (review.draft.candidates.isEmpty()) {
                item { Text(stringResource(R.string.manual_review_no_match_help)) }
            }
            items(review.availableCustomers, key = { it.reference.value }) { customer ->
                    SelectionRow(
                        selected = customer.reference == review.draft.selectedCustomer,
                        text = if (customer.isSuggested) {
                            stringResource(R.string.manual_review_suggested_customer, customer.displayName)
                        } else {
                            customer.displayName
                        },
                        onClick = { onSelectCustomer(customer.reference) },
                    )
            }
            if (review.draft.selectedCustomer != null) {
                item {
                    Text(
                        text = stringResource(R.string.manual_review_choose_debt),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(review.availableDebts, key = SyntheticDebt::reference) { debt ->
                    SelectionRow(
                        selected = debt.reference == review.draft.selectedDebtReference,
                        text = debt.label,
                        onClick = { onSelectDebt(debt.reference) },
                    )
                }
            }
            item {
                Button(
                    onClick = { showConfirmation = true },
                    enabled = review.canRequestConfirmation,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (review.draft.status == ManualDraftStatus.VALIDATED_SIMULATION) {
                            stringResource(R.string.manual_review_already_validated)
                        } else {
                            stringResource(R.string.manual_review_confirm)
                        },
                    )
                }
            }
        }

        if (showConfirmation) {
            val debtLabel = review.availableDebts
                .firstOrNull { it.reference == review.draft.selectedDebtReference }
                ?.label
                .orEmpty()
            AlertDialog(
                onDismissRequest = { showConfirmation = false },
                title = { Text(stringResource(R.string.manual_confirmation_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.manual_confirmation_customer, review.selectedCustomerName.orEmpty()))
                        Text(stringResource(R.string.manual_confirmation_debt, debtLabel))
                        Text(stringResource(R.string.manual_confirmation_method))
                        Text(stringResource(R.string.manual_confirmation_currency))
                        Text(stringResource(R.string.manual_confirmation_amount, review.draft.amount.asPenText()))
                        Text(
                            text = stringResource(R.string.manual_confirmation_simulation_warning),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onValidateSimulation()
                            showConfirmation = false
                        },
                    ) {
                        Text(stringResource(R.string.manual_confirmation_apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmation = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun ReviewSummary(review: ManualDraftReview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(review.draft.customerQuery, fontWeight = FontWeight.SemiBold)
            Text(review.draft.amount.asPenText(), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.manual_confirmation_method))
        }
    }
}

@Composable
private fun SelectionRow(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
        )
    }
}
