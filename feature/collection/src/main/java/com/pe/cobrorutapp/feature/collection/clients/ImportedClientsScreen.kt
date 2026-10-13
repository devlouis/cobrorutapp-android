package com.pe.cobrorutapp.feature.collection.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pe.cobrorutapp.feature.collection.R

data class ImportedClientUi(
    val id: String,
    val name: String,
    val maskedDni: String,
    val address: String?,
    val credits: List<ImportedCreditUi>,
    val sameNameRowCount: Int = 1,
)

data class ImportedCreditUi(
    val id: String,
    val date: String,
    val sale: String,
    val service: String?,
    val total: String,
    val installmentValue: String,
    val balance: String,
    val importedPayment: String,
    val installmentNumber: String,
)

data class ImportedClientsUiState(
    val query: String = "",
    val clients: List<ImportedClientUi> = emptyList(),
    val importedCount: Int = 0,
    val excludedWithoutDniCount: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportedClientsScreen(
    state: ImportedClientsUiState,
    onQueryChanged: (String) -> Unit,
    onClientSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.imported_clients_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
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
                text = stringResource(
                    R.string.imported_clients_summary,
                    state.importedCount,
                    state.excludedWithoutDniCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.imported_clients_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.clients.isEmpty()) {
                Text(
                    text = stringResource(R.string.imported_clients_empty),
                    modifier = Modifier.padding(vertical = 32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.clients, key = ImportedClientUi::id) { client ->
                        ImportedClientCard(client = client, onClick = { onClientSelected(client.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportedClientCard(
    client: ImportedClientUi,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(client.name, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.imported_client_dni, client.maskedDni))
            client.address?.takeIf(String::isNotBlank)?.let { Text(it) }
            if (client.sameNameRowCount > 1) {
                Text(
                    text = stringResource(R.string.imported_client_repeated_name, client.sameNameRowCount),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            client.credits.firstOrNull()?.let { credit ->
                Text(
                    text = stringResource(
                        R.string.imported_client_credit_summary,
                        credit.date,
                        credit.balance,
                        credit.installmentNumber,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.imported_client_credit_count, client.credits.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportedClientDetailScreen(
    client: ImportedClientUi?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.imported_client_detail_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        if (client == null) {
            Text(
                text = stringResource(R.string.imported_client_not_found),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(client.name, style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.imported_client_dni, client.maskedDni))
                        client.address?.takeIf(String::isNotBlank)?.let { Text(it) }
                    }
                }
                items(client.credits, key = ImportedCreditUi::id) { credit ->
                    ImportedCreditCard(credit)
                }
            }
        }
    }
}

@Composable
private fun ImportedCreditCard(credit: ImportedCreditUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(R.string.imported_credit_title, credit.installmentNumber),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CreditValueRow(R.string.imported_credit_date, credit.date)
            CreditValueRow(R.string.imported_credit_sale, credit.sale)
            credit.service?.takeIf(String::isNotBlank)?.let {
                CreditValueRow(R.string.imported_credit_service, it)
            }
            CreditValueRow(R.string.imported_credit_total, credit.total)
            CreditValueRow(R.string.imported_credit_installment, credit.installmentValue)
            CreditValueRow(R.string.imported_credit_balance, credit.balance)
            CreditValueRow(R.string.imported_credit_payment, credit.importedPayment)
        }
    }
}

@Composable
private fun CreditValueRow(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, fontWeight = FontWeight.Medium)
    }
}
