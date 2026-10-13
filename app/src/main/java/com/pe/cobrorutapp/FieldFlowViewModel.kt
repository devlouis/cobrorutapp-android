package com.pe.cobrorutapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportFailure
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportResult
import com.pe.cobrorutapp.core.domain.importing.ImportedCustomerRow
import com.pe.cobrorutapp.feature.workday.ExcelImportStage
import com.pe.cobrorutapp.feature.workday.ExcelImportSummaryUi
import com.pe.cobrorutapp.feature.workday.ExcelImportUiState
import com.pe.cobrorutapp.integration.excel.AndroidWorkbookImportGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FieldFlowUiState(
    val excelImport: ExcelImportUiState = ExcelImportUiState(),
    val previewRows: List<ImportedCustomerRow> = emptyList(),
    val activeRows: List<ImportedCustomerRow> = emptyList(),
    val excludedWithoutDni: Int = 0,
    val customerQuery: String = "",
)

@HiltViewModel
class FieldFlowViewModel @Inject constructor(
    private val workbookGateway: AndroidWorkbookImportGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FieldFlowUiState())
    val uiState = _uiState.asStateFlow()
    private var importJob: Job? = null

    fun selectWorkbook(uri: String) {
        _uiState.update {
            it.copy(
                excelImport = ExcelImportUiState(stage = ExcelImportStage.READING),
                previewRows = emptyList(),
            )
        }
        importJob?.cancel()
        importJob = viewModelScope.launch {
            val startedAtNanos = System.nanoTime()
            val document = workbookGateway.import(uri)
            val elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L
            delay((MINIMUM_LOADING_MILLIS - elapsedMillis).coerceAtLeast(0L))
            when (val result = document.result) {
                is CustomerDirectoryImportResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            excelImport = ExcelImportUiState(
                                stage = ExcelImportStage.PREVIEW,
                                summary = ExcelImportSummaryUi(
                                    fileName = document.fileName,
                                    sheetName = "Plan1",
                                    scannedRows = result.summary.scannedRows,
                                    eligibleRows = result.summary.includedRows,
                                    excludedWithoutDni = result.summary.excludedWithoutDni,
                                    warningCount = result.summary.warningCount,
                                ),
                            ),
                            previewRows = result.rows,
                        )
                    }
                }

                is CustomerDirectoryImportResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            excelImport = ExcelImportUiState(
                                stage = ExcelImportStage.ERROR,
                                errorMessage = result.reason.message(),
                            ),
                            previewRows = emptyList(),
                        )
                    }
                }
            }
        }
    }

    fun confirmImport() {
        _uiState.update { state ->
            val summary = state.excelImport.summary ?: return@update state
            if (state.previewRows.isEmpty()) return@update state
            state.copy(
                excelImport = ExcelImportUiState(
                    stage = ExcelImportStage.IMPORTED,
                    summary = summary,
                ),
                activeRows = state.previewRows,
                excludedWithoutDni = summary.excludedWithoutDni,
                customerQuery = "",
            )
        }
    }

    fun updateCustomerQuery(query: String) {
        _uiState.update { it.copy(customerQuery = query) }
    }
}

private fun CustomerDirectoryImportFailure.message(): String = when (this) {
    CustomerDirectoryImportFailure.INVALID_XLSX ->
        "No se pudo leer el archivo. Selecciona un XLSX válido."
    CustomerDirectoryImportFailure.FILE_UNAVAILABLE ->
        "No se pudo abrir el archivo seleccionado. Vuelve a elegirlo desde Descargas."
    CustomerDirectoryImportFailure.READ_ERROR ->
        "Ocurrió un problema técnico al leer el XLSX. Intenta seleccionarlo nuevamente."
    CustomerDirectoryImportFailure.PLAN1_NOT_FOUND ->
        "El archivo no contiene la hoja Plan1."
    CustomerDirectoryImportFailure.REQUIRED_HEADERS_NOT_FOUND ->
        "Plan1 no contiene las columnas requeridas para esta prueba."
    CustomerDirectoryImportFailure.FILE_TOO_LARGE ->
        "El archivo supera el límite seguro de lectura para esta prueba."
}

private const val MINIMUM_LOADING_MILLIS = 700L
