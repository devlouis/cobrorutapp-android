package com.pe.cobrorutapp.integration.excel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pe.cobrorutapp.core.domain.importing.CustomerDirectoryImportResult
import com.pe.cobrorutapp.core.domain.importing.WorkbookSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XlsxAndroidCompatibilityTest {
    @Test
    fun importsXlsxUsingAndroidXmlParser() = runBlocking {
        val result = XlsxCustomerDirectoryImporter().import(
            WorkbookSource { ByteArrayInputStream(syntheticWorkbook()) },
        )

        assertTrue(result is CustomerDirectoryImportResult.Success)
        result as CustomerDirectoryImportResult.Success
        assertEquals(1, result.summary.includedRows)
        assertEquals("Cliente Sintético", result.rows.single().name)
        assertEquals(1_000L, result.rows.single().balance.money?.minorUnits)
    }

    private fun syntheticWorkbook(): ByteArray {
        val entries = mapOf(
            "xl/workbook.xml" to """
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Plan1" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
            """.trimIndent(),
            "xl/_rels/workbook.xml.rels" to """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
            """.trimIndent(),
            "xl/worksheets/sheet1.xml" to """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="D1" t="inlineStr"><is><t>NOMBRE</t></is></c>
                      <c r="E1" t="inlineStr"><is><t>DNI</t></is></c>
                      <c r="K1" t="inlineStr"><is><t>SALDO</t></is></c>
                    </row>
                    <row r="2">
                      <c r="D2" t="inlineStr"><is><t>Cliente Sintético</t></is></c>
                      <c r="E2" t="inlineStr"><is><t>12345678</t></is></c>
                      <c r="K2"><v>10.00</v></c>
                    </row>
                  </sheetData>
                </worksheet>
            """.trimIndent(),
        )
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
}
