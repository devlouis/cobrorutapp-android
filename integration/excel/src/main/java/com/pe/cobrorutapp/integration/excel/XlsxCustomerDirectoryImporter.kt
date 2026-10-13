package com.pe.cobrorutapp.integration.excel

import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportFailure
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportResult
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportSummary
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportWarning
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportWarningReason
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImporter
import com.pe.cobrorutapp.core.domain.importing.ImportedCustomerField
import com.pe.cobrorutapp.core.domain.importing.ImportedCustomerRow
import com.pe.cobrorutapp.core.domain.importing.ImportedMoneyValue
import com.pe.cobrorutapp.core.domain.importing.WorkbookSource
import com.pe.cobrorutapp.core.domain.money.Currency
import com.pe.cobrorutapp.core.domain.money.Money
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.Locale
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException
import org.xml.sax.helpers.DefaultHandler

class XlsxCustomerDirectoryImporter : CustomerDirectoryImporter {
    override suspend fun import(source: WorkbookSource): CustomerDirectoryImportResult =
        withContext(Dispatchers.IO) {
            try {
                parse(source)
            } catch (_: WorkbookTooLargeException) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.FILE_TOO_LARGE)
            } catch (_: Plan1NotFoundException) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.PLAN1_NOT_FOUND)
            } catch (_: RequiredHeadersNotFoundException) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.REQUIRED_HEADERS_NOT_FOUND)
            } catch (_: InvalidWorkbookException) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.INVALID_XLSX)
            } catch (_: SAXNotRecognizedException) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.READ_ERROR)
            } catch (_: SAXNotSupportedException) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.READ_ERROR)
            } catch (_: SAXException) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.INVALID_XLSX)
            } catch (_: Exception) {
                CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.READ_ERROR)
            }
        }

    private fun parse(source: WorkbookSource): CustomerDirectoryImportResult.Success {
        val entries = source.openStream().use(::readRelevantEntries)
        val workbook = entries[WORKBOOK_PATH] ?: throw InvalidWorkbookException()
        val relationships = entries[WORKBOOK_RELATIONSHIPS_PATH] ?: throw InvalidWorkbookException()
        val plan1RelationshipId = findPlan1RelationshipId(workbook) ?: throw Plan1NotFoundException()
        val sheetTarget = findRelationshipTarget(relationships, plan1RelationshipId)
            ?: throw Plan1NotFoundException()
        val sheetPath = resolveWorkbookTarget(sheetTarget)
        val sheetXml = entries[sheetPath] ?: throw Plan1NotFoundException()
        val sharedStrings = entries[SHARED_STRINGS_PATH]?.let(::readSharedStrings).orEmpty()
        val rows = readSheetRows(sheetXml, sharedStrings)
        return mapRows(rows)
    }

    private fun mapRows(rows: List<SheetRow>): CustomerDirectoryImportResult.Success {
        val headerIndex = rows.indexOfFirst { row ->
            val headers = row.cells.values.map { normalizeHeader(it.value) }.toSet()
            REQUIRED_HEADERS.all(headers::contains)
        }
        if (headerIndex < 0) throw RequiredHeadersNotFoundException()

        val headerColumns = rows[headerIndex].cells.mapNotNull { (column, value) ->
            Header.from(normalizeHeader(value.value))?.let { it to column }
        }.toMap()
        if (!REQUIRED_HEADER_TYPES.all(headerColumns::containsKey)) {
            throw RequiredHeadersNotFoundException()
        }

        var scannedRows = 0
        var excludedWithoutName = 0
        var excludedWithoutDni = 0
        val importedRows = mutableListOf<ImportedCustomerRow>()

        rows.drop(headerIndex + 1).forEach { row ->
            if (row.cells.isEmpty()) return@forEach
            scannedRows += 1
            val name = row.text(headerColumns.getValue(Header.NAME))
            val dni = row.text(headerColumns.getValue(Header.DNI))
            if (name.isNullOrBlank()) {
                excludedWithoutName += 1
                return@forEach
            }
            if (dni.isNullOrBlank()) {
                excludedWithoutDni += 1
                return@forEach
            }

            val warnings = mutableListOf<CustomerDirectoryImportWarning>()
            if (!DNI_PATTERN.matches(dni)) {
                warnings += CustomerDirectoryImportWarning(
                    ImportedCustomerField.DNI,
                    CustomerDirectoryImportWarningReason.DNI_FORMAT_NOT_VALIDATED,
                )
            }
            fun money(header: Header, field: ImportedCustomerField): ImportedMoneyValue {
                val cell = headerColumns[header]?.let(row.cells::get)
                val raw = cell?.value?.trim()?.takeIf(String::isNotEmpty)
                if (cell?.hasFormula == true && raw == null) {
                    warnings += CustomerDirectoryImportWarning(
                        field,
                        CustomerDirectoryImportWarningReason.FORMULA_WITHOUT_CACHED_VALUE,
                    )
                }
                val parsed = raw?.toMoneyOrNull()
                if (raw != null && parsed == null) {
                    warnings += CustomerDirectoryImportWarning(
                        field,
                        CustomerDirectoryImportWarningReason.INVALID_MONEY,
                    )
                }
                return ImportedMoneyValue(rawValue = raw, money = parsed)
            }

            importedRows += ImportedCustomerRow(
                sourceRowNumber = row.number,
                name = name,
                address = headerColumns[Header.ADDRESS]?.let(row::text),
                dni = dni,
                dateRaw = headerColumns[Header.DATE]?.let(row::text),
                sale = money(Header.SALE, ImportedCustomerField.SALE),
                serviceRaw = headerColumns[Header.SERVICE]?.let(row::text),
                total = money(Header.TOTAL, ImportedCustomerField.TOTAL),
                installmentValue = money(Header.INSTALLMENT_VALUE, ImportedCustomerField.INSTALLMENT_VALUE),
                balance = money(Header.BALANCE, ImportedCustomerField.BALANCE),
                dailyPayment = money(Header.DAILY_PAYMENT, ImportedCustomerField.DAILY_PAYMENT),
                installmentNumberRaw = headerColumns[Header.INSTALLMENT_NUMBER]?.let(row::text),
                warnings = warnings,
            )
        }

        val warningCount = importedRows.sumOf { it.warnings.size }
        return CustomerDirectoryImportResult.Success(
            rows = importedRows,
            summary = CustomerDirectoryImportSummary(
                scannedRows = scannedRows,
                includedRows = importedRows.size,
                excludedRows = excludedWithoutName + excludedWithoutDni,
                excludedWithoutName = excludedWithoutName,
                excludedWithoutDni = excludedWithoutDni,
                warningCount = warningCount,
            ),
        )
    }

    private fun readRelevantEntries(input: java.io.InputStream): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        var entryCount = 0
        var totalBytes = 0L
        try {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount += 1
                    if (entryCount > MAX_ENTRY_COUNT) throw WorkbookTooLargeException()
                    val path = entry.name.normalizedZipPath()
                    if (!entry.isDirectory && isRelevantXml(path)) {
                        val bytes = zip.readLimited(MAX_ENTRY_BYTES)
                        totalBytes += bytes.size
                        if (totalBytes > MAX_TOTAL_XML_BYTES) throw WorkbookTooLargeException()
                        result[path] = bytes
                    }
                    zip.closeEntry()
                }
            }
        } catch (error: ZipException) {
            throw InvalidWorkbookException(error)
        }
        return result
    }

    private fun isRelevantXml(path: String): Boolean =
        path == WORKBOOK_PATH ||
            path == WORKBOOK_RELATIONSHIPS_PATH ||
            path == SHARED_STRINGS_PATH ||
            (path.startsWith("xl/worksheets/") && path.endsWith(".xml"))

    private fun findPlan1RelationshipId(xml: ByteArray): String? {
        var relationshipId: String? = null
        parseXml(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                if (elementName(localName, qName) == "sheet" &&
                    attributes.value("name")?.equals("Plan1", ignoreCase = true) == true
                ) {
                    relationshipId = attributes.getValue(RELATIONSHIP_ID_NAMESPACE, "id")
                        ?: attributes.value("r:id")
                }
            }
        })
        return relationshipId
    }

    private fun findRelationshipTarget(xml: ByteArray, id: String): String? {
        var target: String? = null
        parseXml(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                if (elementName(localName, qName) == "Relationship" && attributes.value("Id") == id) {
                    target = attributes.value("Target")
                }
            }
        })
        return target
    }

    private fun readSharedStrings(xml: ByteArray): List<String> {
        val values = mutableListOf<String>()
        var insideItem = false
        var insideText = false
        val current = StringBuilder()
        parseXml(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                when (elementName(localName, qName)) {
                    "si" -> {
                        insideItem = true
                        current.clear()
                    }
                    "t" -> if (insideItem) insideText = true
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (insideText) current.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (elementName(localName, qName)) {
                    "t" -> insideText = false
                    "si" -> {
                        values += current.toString()
                        insideItem = false
                    }
                }
            }
        })
        return values
    }

    private fun readSheetRows(xml: ByteArray, sharedStrings: List<String>): List<SheetRow> {
        val rows = mutableListOf<SheetRow>()
        var rowNumber = 0
        var cells = mutableMapOf<Int, SheetCell>()
        var cellReference: String? = null
        var cellType: String? = null
        var hasFormula = false
        var collectingValue = false
        var collectingInlineText = false
        val cellValue = StringBuilder()
        val inlineValue = StringBuilder()

        parseXml(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                when (elementName(localName, qName)) {
                    "row" -> {
                        rowNumber = attributes.value("r")?.toIntOrNull() ?: (rows.lastOrNull()?.number ?: 0) + 1
                        cells = mutableMapOf()
                    }
                    "c" -> {
                        cellReference = attributes.value("r")
                        cellType = attributes.value("t")
                        hasFormula = false
                        cellValue.clear()
                        inlineValue.clear()
                    }
                    "f" -> hasFormula = true
                    "v" -> collectingValue = true
                    "t" -> if (cellType == "inlineStr") collectingInlineText = true
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (collectingValue) cellValue.append(ch, start, length)
                if (collectingInlineText) inlineValue.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (elementName(localName, qName)) {
                    "v" -> collectingValue = false
                    "t" -> collectingInlineText = false
                    "c" -> {
                        val column = cellReference?.columnIndex() ?: return
                        val raw = if (cellType == "inlineStr") inlineValue.toString() else cellValue.toString()
                        val resolved = when (cellType) {
                            "s" -> raw.toIntOrNull()?.let(sharedStrings::getOrNull).orEmpty()
                            "b" -> if (raw == "1") "TRUE" else "FALSE"
                            else -> raw
                        }
                        cells[column] = SheetCell(resolved, hasFormula)
                    }
                    "row" -> rows += SheetRow(rowNumber, cells.toMap())
                }
            }
        })
        return rows
    }

    private fun parseXml(xml: ByteArray, handler: DefaultHandler) {
        if (xml.containsDoctypeDeclaration()) throw InvalidWorkbookException()
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        factory.newSAXParser().xmlReader.apply {
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            entityResolver = org.xml.sax.EntityResolver { _, _ -> InputSource(ByteArrayInputStream(ByteArray(0))) }
            contentHandler = handler
            parse(InputSource(ByteArrayInputStream(xml)))
        }
    }

    private fun ByteArray.containsDoctypeDeclaration(): Boolean =
        containsAsciiSequence(DOCTYPE_ASCII) ||
            containsAsciiSequence(DOCTYPE_UTF16_LE) ||
            containsAsciiSequence(DOCTYPE_UTF16_BE)

    private fun ByteArray.containsAsciiSequence(pattern: ByteArray): Boolean {
        if (size < pattern.size) return false
        for (start in 0..size - pattern.size) {
            var matches = true
            for (offset in pattern.indices) {
                val actual = this[start + offset]
                val expected = pattern[offset]
                if (actual.asciiUppercase() != expected.asciiUppercase()) {
                    matches = false
                    break
                }
            }
            if (matches) return true
        }
        return false
    }

    private fun Byte.asciiUppercase(): Byte = if (this in 'a'.code.toByte()..'z'.code.toByte()) {
        (this - ASCII_CASE_OFFSET).toByte()
    } else {
        this
    }

    private fun java.io.InputStream.readLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw WorkbookTooLargeException()
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun String.toMoneyOrNull(): Money? {
        return try {
            val decimal = BigDecimal(this)
            if (decimal.signum() < 0 || decimal.scale() > 2) {
                null
            } else {
                val minorUnits = decimal.movePointRight(2).longValueExact()
                Money(minorUnits = minorUnits, currency = Currency.PEN)
            }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun String.normalizedZipPath(): String = removePrefix("/").replace('\\', '/')

    private fun String.columnIndex(): Int? {
        var result = 0
        var found = false
        for (character in this) {
            if (!character.isLetter()) break
            found = true
            result = result * 26 + (character.uppercaseChar() - 'A' + 1)
        }
        return if (found) result - 1 else null
    }

    private fun resolveWorkbookTarget(target: String): String {
        val segments = mutableListOf<String>()
        ("xl/$target").replace('\\', '/').split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
        return segments.joinToString("/")
    }

    private fun normalizeHeader(value: String): String = value
        .uppercase(Locale.ROOT)
        .replace(HEADER_SEPARATOR, " ")
        .trim()
        .replace(MULTIPLE_SPACES, " ")

    private fun elementName(localName: String?, qName: String?): String =
        localName?.takeIf(String::isNotEmpty) ?: qName.orEmpty().substringAfter(':')

    private fun Attributes.value(name: String): String? = getValue(name)

    private data class SheetRow(val number: Int, val cells: Map<Int, SheetCell>) {
        fun text(column: Int): String? = cells[column]?.value?.trim()?.takeIf(String::isNotEmpty)
    }

    private data class SheetCell(val value: String, val hasFormula: Boolean)

    private enum class Header {
        ADDRESS,
        NAME,
        DNI,
        DATE,
        SALE,
        SERVICE,
        TOTAL,
        INSTALLMENT_VALUE,
        BALANCE,
        DAILY_PAYMENT,
        INSTALLMENT_NUMBER;

        companion object {
            fun from(value: String): Header? = when (value) {
                "DIRECCION" -> ADDRESS
                "NOMBRE" -> NAME
                "DNI" -> DNI
                "FECHA" -> DATE
                "VENTA" -> SALE
                "SERVICIO" -> SERVICE
                "TOTAL" -> TOTAL
                "V CUOTA", "VCUOTA", "VALOR CUOTA" -> INSTALLMENT_VALUE
                "SALDO" -> BALANCE
                "ABONO" -> DAILY_PAYMENT
                "NO CUOTA", "N CUOTA", "NRO CUOTA", "NUMERO CUOTA" -> INSTALLMENT_NUMBER
                else -> null
            }
        }
    }

    private class InvalidWorkbookException(cause: Throwable? = null) : Exception(cause)
    private class WorkbookTooLargeException : Exception()
    private class Plan1NotFoundException : Exception()
    private class RequiredHeadersNotFoundException : Exception()

    private companion object {
        const val WORKBOOK_PATH = "xl/workbook.xml"
        const val WORKBOOK_RELATIONSHIPS_PATH = "xl/_rels/workbook.xml.rels"
        const val SHARED_STRINGS_PATH = "xl/sharedStrings.xml"
        const val RELATIONSHIP_ID_NAMESPACE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        const val MAX_ENTRY_COUNT = 256
        const val MAX_ENTRY_BYTES = 16 * 1024 * 1024
        const val MAX_TOTAL_XML_BYTES = 32L * 1024L * 1024L
        const val ASCII_CASE_OFFSET = 32
        val DOCTYPE_ASCII = "<!DOCTYPE".toByteArray(Charsets.US_ASCII)
        val DOCTYPE_UTF16_LE = DOCTYPE_ASCII.flatMap { listOf(it, 0) }.toByteArray()
        val DOCTYPE_UTF16_BE = DOCTYPE_ASCII.flatMap { listOf(0, it) }.toByteArray()
        val DNI_PATTERN = Regex("\\d{8}")
        val HEADER_SEPARATOR = Regex("[^A-Z0-9]+")
        val MULTIPLE_SPACES = Regex("\\s+")
        val REQUIRED_HEADERS = setOf("NOMBRE", "DNI")
        val REQUIRED_HEADER_TYPES = setOf(Header.NAME, Header.DNI)
    }
}
