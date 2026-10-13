package com.pe.cobrorutapp.feature.collection.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pe.cobrorutapp.core.domain.money.Money
import com.pe.cobrorutapp.feature.collection.R

@Composable
fun ManualAssistantRoute(
    viewModel: ManualAssistantViewModel,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ManualAssistantScreen(
        state = state,
        onInputChanged = viewModel::onInputChanged,
        onSaveDraft = viewModel::saveDraft,
        onBack = onBack,
        onOpenQueue = onOpenQueue,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAssistantScreen(
    state: ManualAssistantUiState,
    onInputChanged: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_assistant_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.manual_assistant_instructions),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.manual_assistant_examples),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.manual_assistant_cash_method),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChanged,
                label = { Text(stringResource(R.string.manual_assistant_input_label)) },
                placeholder = { Text(stringResource(R.string.manual_assistant_input_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSaveDraft() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSaveDraft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.manual_assistant_save))
            }
            FeedbackMessage(state.feedback)
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onOpenQueue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        R.string.manual_assistant_review_pending,
                        state.pendingCount,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeedbackMessage(feedback: ManualAssistantFeedback?) {
    val text = when (feedback) {
        null -> return
        ManualAssistantFeedback.InvalidCommand -> stringResource(R.string.manual_assistant_invalid)
        is ManualAssistantFeedback.DraftSaved -> when (feedback.candidateCount) {
            0 -> stringResource(R.string.manual_assistant_saved_no_match)
            1 -> stringResource(R.string.manual_assistant_saved_one_match)
            else -> stringResource(
                R.string.manual_assistant_saved_multiple_matches,
                feedback.candidateCount,
            )
        }
    }
    val isError = feedback == ManualAssistantFeedback.InvalidCommand
    Text(
        text = text,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun ManualDraftQueueRoute(
    viewModel: ManualAssistantViewModel,
    onBack: () -> Unit,
    onReviewDraft: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ManualDraftQueueScreen(
        drafts = state.drafts,
        onBack = onBack,
        onReviewDraft = onReviewDraft,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualDraftQueueScreen(
    drafts: List<ManualPaymentDraft>,
    onBack: () -> Unit,
    onReviewDraft: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_queue_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        if (drafts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.manual_queue_empty),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(drafts, key = ManualPaymentDraft::id) { draft ->
                    DraftCard(draft = draft, onReview = { onReviewDraft(draft.id) })
                }
            }
        }
    }
}

@Composable
private fun DraftCard(
    draft: ManualPaymentDraft,
    onReview: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (draft.status == ManualDraftStatus.VALIDATED_SIMULATION) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = draft.customerQuery,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = draft.amount.asPenText(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = when (draft.candidates.size) {
                    0 -> stringResource(R.string.manual_queue_no_candidates)
                    1 -> stringResource(R.string.manual_queue_one_candidate)
                    else -> stringResource(R.string.manual_queue_many_candidates, draft.candidates.size)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (draft.status == ManualDraftStatus.VALIDATED_SIMULATION) {
                    stringResource(R.string.manual_queue_validated)
                } else {
                    stringResource(R.string.manual_queue_pending)
                },
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(onClick = onReview) {
                Text(stringResource(R.string.manual_queue_review))
            }
        }
    }
}

internal fun Money.asPenText(): String {
    val whole = minorUnits / 100
    val fraction = (minorUnits % 100).toString().padStart(2, '0')
    return "S/ $whole.$fraction"
}
