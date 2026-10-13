package com.pe.cobrorutapp.feature.workday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

enum class ExcelImportStage {
    EMPTY,
    READING,
    PREVIEW,
    IMPORTED,
    ERROR,
}

data class ExcelImportSummaryUi(
    val fileName: String,
    val sheetName: String,
    val scannedRows: Int,
    val eligibleRows: Int,
    val excludedWithoutDni: Int,
    val warningCount: Int,
)

data class ExcelImportUiState(
    val stage: ExcelImportStage = ExcelImportStage.EMPTY,
    val summary: ExcelImportSummaryUi? = null,
    val errorMessage: String? = null,
)

@Composable
fun ImportExcelScreen(
    state: ExcelImportUiState,
    onChooseFile: () -> Unit,
    onConfirmImport: () -> Unit,
    onOpenDirectory: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.import_excel_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            text = stringResource(R.string.import_excel_description),
            style = MaterialTheme.typography.bodyLarge,
        )

        when (state.stage) {
            ExcelImportStage.EMPTY -> EmptyImport(onChooseFile)
            ExcelImportStage.READING -> ReadingImport()
            ExcelImportStage.PREVIEW -> PreviewImport(
                summary = requireNotNull(state.summary),
                onChooseFile = onChooseFile,
                onConfirmImport = onConfirmImport,
            )
            ExcelImportStage.IMPORTED -> ImportedExcel(
                summary = requireNotNull(state.summary),
                onChooseFile = onChooseFile,
                onOpenDirectory = onOpenDirectory,
            )
            ExcelImportStage.ERROR -> ImportError(
                message = state.errorMessage.orEmpty(),
                onChooseFile = onChooseFile,
            )
        }
    }
}

@Composable
private fun EmptyImport(onChooseFile: () -> Unit) {
    ImportNotice()
    Button(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.choose_excel_file))
    }
}

@Composable
private fun ReadingImport() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.reading_excel_file),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.reading_excel_wait),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewImport(
    summary: ExcelImportSummaryUi,
    onChooseFile: () -> Unit,
    onConfirmImport: () -> Unit,
) {
    ImportSummaryCard(summary)
    ImportNotice()
    Button(
        onClick = onConfirmImport,
        enabled = summary.eligibleRows > 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.import_for_test))
    }
    OutlinedButton(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.choose_another_file))
    }
}

@Composable
private fun ImportedExcel(
    summary: ExcelImportSummaryUi,
    onChooseFile: () -> Unit,
    onOpenDirectory: () -> Unit,
) {
    ImportSummaryCard(summary)
    Text(
        text = stringResource(R.string.import_test_ready),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyLarge,
    )
    Button(onClick = onOpenDirectory, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.open_imported_directory))
    }
    OutlinedButton(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.choose_another_file))
    }
}

@Composable
private fun ImportError(message: String, onChooseFile: () -> Unit) {
    Text(
        text = message.ifBlank { stringResource(R.string.import_excel_generic_error) },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
    )
    Button(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.choose_excel_file))
    }
}

@Composable
private fun ImportSummaryCard(summary: ExcelImportSummaryUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(summary.fileName, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.import_sheet_value, summary.sheetName))
            Text(stringResource(R.string.import_scanned_rows, summary.scannedRows))
            Text(
                text = stringResource(R.string.import_eligible_rows, summary.eligibleRows),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.import_excluded_rows, summary.excludedWithoutDni))
            if (summary.warningCount > 0) {
                Text(
                    text = stringResource(R.string.import_warning_count, summary.warningCount),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun ImportNotice() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.import_filter_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(stringResource(R.string.import_filter_description))
            Text(stringResource(R.string.import_read_only_notice))
        }
    }
}
