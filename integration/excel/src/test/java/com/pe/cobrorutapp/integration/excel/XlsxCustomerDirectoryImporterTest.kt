package com.pe.cobrorutapp.integration.excel

import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportFailure
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportResult
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportWarningReason
import com.pe.cobrorutapp.core.domain.importing.WorkbookSource
import com.pe.cobrorutapp.core.domain.money.Currency
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxCustomerDirectoryImporterTest {
    private val importer = XlsxCustomerDirectoryImporter()

    @Test
    fun `imports only rows with name and dni and maps cached values`() = runBlocking {
        val workbook = workbook(
            sheetXml = """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    ${headerRow()}
                    <row r="2">
                      <c r="C2" t="inlineStr"><is><t>Calle Uno</t></is></c>
                      <c r="D2" t="s"><v>0</v></c>
                      <c r="E2" t="inlineStr"><is><t>12345678</t></is></c>
                      <c r="F2" t="inlineStr"><is><t>2026-09-24</t></is></c>
                      <c r="G2"><v>100</v></c>
                      <c r="H2" t="inlineStr"><is><t>20</t></is></c>
                      <c r="I2"><v>120.50</v></c>
                      <c r="J2"><v>6.02</v></c>
                      <c r="K2"><f>I2-L2</f><v>95.25</v></c>
                      <c r="L2"><v>25.25</v></c>
                      <c r="M2" t="inlineStr"><is><t>4/20</t></is></c>
                    </row>
                    <row r="3">
                      <c r="D3" t="inlineStr"><is><t>Sin documento</t></is></c>
                    </row>
                    <row r="4">
                      <c r="E4" t="inlineStr"><is><t>87654321</t></is></c>
                    </row>
                  </sheetData>
                </worksheet>
            """.trimIndent(),
            sharedStrings = listOf("Cliente Sintético"),
        )

        val result = importer.import(WorkbookSource { ByteArrayInputStream(workbook) })

        assertTrue(result is CustomerDirectoryImportResult.Success)
        result as CustomerDirectoryImportResult.Success
        assertEquals(3, result.summary.scannedRows)
        assertEquals(1, result.summary.includedRows)
        assertEquals(2, result.summary.excludedRows)
        assertEquals(1, result.summary.excludedWithoutName)
        assertEquals(1, result.summary.excludedWithoutDni)
        assertEquals(0, result.summary.warningCount)
        val row = result.rows.single()
        assertEquals(2, row.sourceRowNumber)
        assertEquals("Cliente Sintético", row.name)
        assertEquals("Calle Uno", row.address)
        assertEquals("12345678", row.dni)
        assertEquals("2026-09-24", row.dateRaw)
        assertEquals("20", row.serviceRaw)
        assertEquals("4/20", row.installmentNumberRaw)
        assertEquals(10_000L, row.sale.money?.minorUnits)
        assertEquals(Currency.PEN, row.sale.money?.currency)
        assertEquals(12_050L, row.total.money?.minorUnits)
        assertEquals(602L, row.installmentValue.money?.minorUnits)
        assertEquals("6.02", row.installmentValue.rawValue)
        assertEquals(9_525L, row.balance.money?.minorUnits)
        assertEquals(2_525L, row.dailyPayment.money?.minorUnits)
    }

    @Test
    fun `keeps eligible row and reports non blocking validation warnings`() = runBlocking {
        val workbook = workbook(
            sheetXml = """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    ${headerRow()}
                    <row r="7">
                      <c r="D7" t="inlineStr"><is><t>Cliente Dos</t></is></c>
                      <c r="E7" t="inlineStr"><is><t>42</t></is></c>
                      <c r="G7" t="inlineStr"><is><t>MONTO</t></is></c>
                      <c r="K7"><f>I7-L7</f></c>
                    </row>
                  </sheetData>
                </worksheet>
            """.trimIndent(),
        )

        val result = importer.import(WorkbookSource { ByteArrayInputStream(workbook) })
            as CustomerDirectoryImportResult.Success

        assertEquals(1, result.summary.includedRows)
        assertEquals(3, result.summary.warningCount)
        assertEquals(
            listOf(
                CustomerDirectoryImportWarningReason.DNI_FORMAT_NOT_VALIDATED,
                CustomerDirectoryImportWarningReason.INVALID_MONEY,
                CustomerDirectoryImportWarningReason.FORMULA_WITHOUT_CACHED_VALUE,
            ),
            result.rows.single().warnings.map { it.reason },
        )
        assertEquals("MONTO", result.rows.single().sale.rawValue)
        assertNull(result.rows.single().balance.money)
    }

    @Test
    fun `returns missing sheet without exposing workbook contents`() = runBlocking {
        val result = importer.import(
            WorkbookSource {
                ByteArrayInputStream(workbook(sheetName = "Otra", sheetXml = emptySheet()))
            },
        )

        assertEquals(
            CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.PLAN1_NOT_FOUND),
            result,
        )
    }

    @Test
    fun `returns missing required headers`() = runBlocking {
        val result = importer.import(
            WorkbookSource {
                ByteArrayInputStream(
                    workbook(
                        sheetXml = """
                            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                              <sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>OTRO</t></is></c></row></sheetData>
                            </worksheet>
                        """.trimIndent(),
                    ),
                )
            },
        )

        assertEquals(
            CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.REQUIRED_HEADERS_NOT_FOUND),
            result,
        )
    }

    @Test
    fun `returns invalid xlsx for arbitrary bytes`() = runBlocking {
        val result = importer.import(WorkbookSource { ByteArrayInputStream("not-xlsx".toByteArray()) })

        assertEquals(
            CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.INVALID_XLSX),
            result,
        )
    }

    @Test
    fun `rejects doctype declarations before parsing xml`() = runBlocking {
        val sheet = """
            <?xml version="1.0"?>
            <!DOCTYPE worksheet [<!ENTITY external SYSTEM "file:///private-data">]>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>${headerRow()}</sheetData>
            </worksheet>
        """.trimIndent()

        val result = importer.import(
            WorkbookSource { ByteArrayInputStream(workbook(sheetXml = sheet)) },
        )

        assertEquals(
            CustomerDirectoryImportResult.Failure(CustomerDirectoryImportFailure.INVALID_XLSX),
            result,
        )
    }

    private fun workbook(
        sheetName: String = "Plan1",
        sheetXml: String,
        sharedStrings: List<String> = emptyList(),
    ): ByteArray {
        val entries = linkedMapOf(
            "xl/workbook.xml" to """
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="$sheetName" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
            """.trimIndent(),
            "xl/_rels/workbook.xml.rels" to """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
            """.trimIndent(),
            "xl/worksheets/sheet1.xml" to sheetXml,
        )
        if (sharedStrings.isNotEmpty()) {
            entries["xl/sharedStrings.xml"] = buildString {
                append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                sharedStrings.forEach { append("<si><t>").append(it).append("</t></si>") }
                append("</sst>")
            }
        }
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (path, contents) ->
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(contents.toByteArray())
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    private fun headerRow(): String = """
        <row r="1">
          <c r="C1" t="inlineStr"><is><t>DIRECCION</t></is></c>
          <c r="D1" t="inlineStr"><is><t>NOMBRE</t></is></c>
          <c r="E1" t="inlineStr"><is><t>DNI</t></is></c>
          <c r="F1" t="inlineStr"><is><t>FECHA</t></is></c>
          <c r="G1" t="inlineStr"><is><t>VENTA</t></is></c>
          <c r="H1" t="inlineStr"><is><t>SERVICIO</t></is></c>
          <c r="I1" t="inlineStr"><is><t>TOTAL</t></is></c>
          <c r="J1" t="inlineStr"><is><t>V.CUOTA</t></is></c>
          <c r="K1" t="inlineStr"><is><t>SALDO</t></is></c>
          <c r="L1" t="inlineStr"><is><t>ABONO</t></is></c>
          <c r="M1" t="inlineStr"><is><t>No.CUOTA</t></is></c>
        </row>
    """.trimIndent()

    private fun emptySheet(): String = """
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData/></worksheet>
    """.trimIndent()
}
