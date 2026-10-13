package com.pe.cobrorutapp.integration.excel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportFailure
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportResult
import com.pe.cobrorutapp.core.domain.importing.WorkbookSource
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WorkbookDocumentImport(
    val fileName: String,
    val result: CustomerDirectoryImportResult,
)

class AndroidWorkbookImportGateway(
    private val context: Context,
    private val importer: XlsxCustomerDirectoryImporter,
) {
    suspend fun import(uriValue: String): WorkbookDocumentImport = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriValue)
        val fileName = resolveDisplayName(uri) ?: "archivo.xlsx"
        val result = try {
            importer.import(
                WorkbookSource {
                    context.contentResolver.openInputStream(uri)
                        ?: throw FileNotFoundException("Selected workbook cannot be opened")
                },
            )
        } catch (_: SecurityException) {
            CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.FILE_UNAVAILABLE)
        } catch (_: FileNotFoundException) {
            CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.FILE_UNAVAILABLE)
        }
        WorkbookDocumentImport(fileName = fileName, result = result)
    }

    private fun resolveDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}
