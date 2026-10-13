package com.pe.cobrorutapp.core.domain.importing

import com.pe.cobrorutapp.core.domain.money.Money
import java.io.InputStream

fun interface WorkbookSource {
    fun openStream(): InputStream
}

interface CustomerDirectoryImporter {
    suspend fun import(source: WorkbookSource): CustomerDirectoryImportResult
}

sealed interface CustomerDirectoryImportResult {
    data class Success(
        val rows: List<ImportedCustomerRow>,
        val summary: CustomerDirectoryImportSummary,
    ) : CustomerDirectoryImportResult

    data class Failure(
        val reason: CustomerDirectoryImportFailure,
    ) : CustomerDirectoryImportResult
}

enum class CustomerDirectoryImportFailure {
    INVALID_XLSX,
    FILE_UNAVAILABLE,
    READ_ERROR,
    PLAN1_NOT_FOUND,
    REQUIRED_HEADERS_NOT_FOUND,
    FILE_TOO_LARGE,
}

data class CustomerDirectoryImportSummary(
    val scannedRows: Int,
    val includedRows: Int,
    val excludedRows: Int,
    val excludedWithoutName: Int,
    val excludedWithoutDni: Int,
    val warningCount: Int,
)

data class ImportedCustomerRow(
    val sourceRowNumber: Int,
    val name: String,
    val address: String?,
    val dni: String,
    val dateRaw: String?,
    val sale: ImportedMoneyValue,
    val serviceRaw: String?,
    val total: ImportedMoneyValue,
    val installmentValue: ImportedMoneyValue,
    val balance: ImportedMoneyValue,
    val dailyPayment: ImportedMoneyValue,
    val installmentNumberRaw: String?,
    val warnings: List<CustomerDirectoryImportWarning>,
)

data class ImportedMoneyValue(
    val rawValue: String?,
    val money: Money?,
)

data class CustomerDirectoryImportWarning(
    val field: ImportedCustomerField,
    val reason: CustomerDirectoryImportWarningReason,
)

enum class ImportedCustomerField {
    DNI,
    SALE,
    TOTAL,
    INSTALLMENT_VALUE,
    BALANCE,
    DAILY_PAYMENT,
}

enum class CustomerDirectoryImportWarningReason {
    DNI_FORMAT_NOT_VALIDATED,
    INVALID_MONEY,
    FORMULA_WITHOUT_CACHED_VALUE,
}
