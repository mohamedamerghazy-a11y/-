package com.example.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.CashVerificationLog
import com.example.data.model.CashWithdrawalRecord
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class CashDeviceReportRow(
  val deviceName: String,
  val email: String,
  val phoneNumber: String,
  val periodOperationsCount: Int,
  val periodTotalAmount: Double,
  val currentUnsettledCount: Int,
  val currentUnsettledBalance: Double,
  val totalWithdrawnAmount: Double,
  val lastWithdrawalLabel: String,
)

/**
 * مولّد ملفات Excel حقيقية بصيغة .xlsx (Office Open XML) مع دعم كامل للغة العربية (RTL)
 * و٣ أوراق عمل منفصلة:
 * 1) ملخص الهواتف
 * 2) تفاصيل عمليات الكاش
 * 3) سجل عمليات السحب
 */
object CashExcelExporter {

  const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

  fun suggestedFileName(periodSlug: String): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    val safeSlug = periodSlug.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
    return "aleman_cash_${safeSlug}_$stamp.xlsx"
  }

  fun exportToUri(
    context: Context,
    uri: Uri,
    periodTitle: String,
    deviceSummaries: List<CashDeviceReportRow>,
    logs: List<CashVerificationLog>,
    withdrawals: List<CashWithdrawalRecord>,
  ): Result<Unit> = runCatching {
    context.contentResolver.openOutputStream(uri)?.use { output ->
      writeWorkbook(output, periodTitle, deviceSummaries, logs, withdrawals)
    } ?: error("تعذر فتح مسار الحفظ المختار.")
  }

  fun exportAndCreateShareIntent(
    context: Context,
    periodTitle: String,
    periodSlug: String,
    deviceSummaries: List<CashDeviceReportRow>,
    logs: List<CashVerificationLog>,
    withdrawals: List<CashWithdrawalRecord>,
  ): Result<Intent> = runCatching {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, suggestedFileName(periodSlug))
    outFile.outputStream().use { output ->
      writeWorkbook(output, periodTitle, deviceSummaries, logs, withdrawals)
    }
    val contentUri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      outFile,
    )
    Intent(Intent.ACTION_SEND).apply {
      type = XLSX_MIME_TYPE
      putExtra(Intent.EXTRA_STREAM, contentUri)
      putExtra(Intent.EXTRA_SUBJECT, "تقرير تحويلات الإيمان كاش - $periodTitle")
      putExtra(Intent.EXTRA_TEXT, "مرفق ملف إكسيل بتفاصيل تحويلات الكاش للفترة: $periodTitle")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  private sealed interface CellValue {
    data class Text(val value: String, val styleId: Int = 0) : CellValue
    data class Number(val value: Double, val styleId: Int = 2) : CellValue
  }

  private fun writeWorkbook(
    output: OutputStream,
    periodTitle: String,
    deviceSummaries: List<CashDeviceReportRow>,
    logs: List<CashVerificationLog>,
    withdrawals: List<CashWithdrawalRecord>,
  ) {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    // Sheet 1: ملخص الهواتف
    val summaryRows = mutableListOf<List<CellValue>>()
    summaryRows += listOf(
      CellValue.Text("تقرير متابعة تحويلات الإيمان كاش — $periodTitle", styleId = 3),
    )
    summaryRows += listOf(
      CellValue.Text("تاريخ التصدير: ${dateFmt.format(Date())}", styleId = 0),
    )
    summaryRows.add(emptyList())
    summaryRows += listOf(
      CellValue.Text("اسم الهاتف", 1),
      CellValue.Text("البريد الإلكتروني", 1),
      CellValue.Text("رقم المحفظة", 1),
      CellValue.Text("عدد العمليات (الفترة)", 1),
      CellValue.Text("إجمالي الفترة (ج.م)", 1),
      CellValue.Text("العمليات القائمة (غير المسحوبة)", 1),
      CellValue.Text("الرصيد القائم للسحب (ج.م)", 1),
      CellValue.Text("إجمالي المسحوب سابقًا (ج.م)", 1),
      CellValue.Text("آخر عملية سحب", 1),
    )
    deviceSummaries.forEach { d ->
      summaryRows += listOf(
        CellValue.Text(d.deviceName),
        CellValue.Text(d.email),
        CellValue.Text(d.phoneNumber.ifBlank { "-" }),
        CellValue.Number(d.periodOperationsCount.toDouble(), styleId = 4),
        CellValue.Number(d.periodTotalAmount, styleId = 2),
        CellValue.Number(d.currentUnsettledCount.toDouble(), styleId = 4),
        CellValue.Number(d.currentUnsettledBalance, styleId = 2),
        CellValue.Number(d.totalWithdrawnAmount, styleId = 2),
        CellValue.Text(d.lastWithdrawalLabel.ifBlank { "لم يتم السحب بعد" }),
      )
    }
    summaryRows += listOf(
      CellValue.Text("الإجمالي الكلي", 1),
      CellValue.Text("", 1),
      CellValue.Text("", 1),
      CellValue.Number(deviceSummaries.sumOf { it.periodOperationsCount }.toDouble(), styleId = 4),
      CellValue.Number(deviceSummaries.sumOf { it.periodTotalAmount }, styleId = 2),
      CellValue.Number(deviceSummaries.sumOf { it.currentUnsettledCount }.toDouble(), styleId = 4),
      CellValue.Number(deviceSummaries.sumOf { it.currentUnsettledBalance }, styleId = 2),
      CellValue.Number(deviceSummaries.sumOf { it.totalWithdrawnAmount }, styleId = 2),
      CellValue.Text("", 1),
    )

    // Sheet 2: تفاصيل عمليات الكاش
    val detailRows = mutableListOf<List<CellValue>>()
    detailRows += listOf(
      CellValue.Text("التاريخ والوقت", 1),
      CellValue.Text("اسم الهاتف المؤكِّد", 1),
      CellValue.Text("بريد الهاتف", 1),
      CellValue.Text("رقم الطلب", 1),
      CellValue.Text("المحفظة / المزود", 1),
      CellValue.Text("رقم المحوِّل", 1),
      CellValue.Text("القيمة (ج.م)", 1),
      CellValue.Text("رقم العملية المرجعي", 1),
      CellValue.Text("حالة التحقق", 1),
      CellValue.Text("حالة السحب", 1),
      CellValue.Text("تفاصيل / ملاحظات", 1),
    )
    logs.forEach { log ->
      val createdStr = log.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "-"
      val statusAr = when (log.status) {
        "verified", "matched" -> "تم التأكيد"
        "unmatched" -> "غير مطابق"
        "duplicate" -> "مكرر"
        else -> log.status
      }
      val settleAr = when {
        !log.isVerifiedOperation -> "-"
        log.isSettled -> "تم السحب"
        else -> "قائم (لم يُسحب)"
      }
      detailRows += listOf(
        CellValue.Text(createdStr),
        CellValue.Text(log.verifiedByDeviceName.ifBlank { "هاتف الإدارة" }),
        CellValue.Text(log.verifiedByEmail.ifBlank { "-" }),
        CellValue.Text(log.matchedOrderNumber.orEmpty().ifBlank { "-" }),
        CellValue.Text(log.providerName.ifBlank { "-" }),
        CellValue.Text(log.senderPhone.ifBlank { "-" }),
        CellValue.Number(log.amount, styleId = 2),
        CellValue.Text(log.transactionId.ifBlank { "-" }),
        CellValue.Text(statusAr),
        CellValue.Text(settleAr),
        CellValue.Text(log.reason.ifBlank { "-" }),
      )
    }

    // Sheet 3: سجل عمليات السحب
    val withdrawalRows = mutableListOf<List<CellValue>>()
    withdrawalRows += listOf(
      CellValue.Text("تاريخ ووقت السحب", 1),
      CellValue.Text("اسم الهاتف", 1),
      CellValue.Text("بريد الهاتف", 1),
      CellValue.Text("المبلغ المسحوب (ج.م)", 1),
      CellValue.Text("عدد العمليات المصفّاة", 1),
      CellValue.Text("المسؤول عن السحب", 1),
      CellValue.Text("بريد المسؤول", 1),
      CellValue.Text("ملاحظات", 1),
    )
    withdrawals.forEach { w ->
      val dateStr = w.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "-"
      withdrawalRows += listOf(
        CellValue.Text(dateStr),
        CellValue.Text(w.deviceName),
        CellValue.Text(w.deviceEmail.ifBlank { "-" }),
        CellValue.Number(w.withdrawnAmount, styleId = 2),
        CellValue.Number(w.operationsCount.toDouble(), styleId = 4),
        CellValue.Text(w.withdrawnByName),
        CellValue.Text(w.withdrawnByEmail.ifBlank { "-" }),
        CellValue.Text(w.note.orEmpty().ifBlank { "-" }),
      )
    }

    ZipOutputStream(output).use { zip ->
      writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
      writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
      writeZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
      writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
      writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
      writeZipEntry(
        zip,
        "xl/worksheets/sheet1.xml",
        buildWorksheetXml(summaryRows, listOf(24, 28, 16, 18, 18, 22, 20, 22, 24)),
      )
      writeZipEntry(
        zip,
        "xl/worksheets/sheet2.xml",
        buildWorksheetXml(detailRows, listOf(20, 24, 26, 14, 18, 16, 15, 22, 14, 18, 36)),
      )
      writeZipEntry(
        zip,
        "xl/worksheets/sheet3.xml",
        buildWorksheetXml(withdrawalRows, listOf(20, 24, 26, 20, 18, 22, 26, 32)),
      )
    }
  }

  private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(content.toByteArray(Charsets.UTF_8))
    zip.closeEntry()
  }

  private fun buildWorksheetXml(rows: List<List<CellValue>>, colWidths: List<Int>): String {
    val sb = StringBuilder(8192)
    sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
    sb.append("""<sheetViews><sheetView rightToLeft="1" workbookViewId="0"/></sheetViews>""")
    sb.append("""<cols>""")
    colWidths.forEachIndexed { idx, w ->
      val colIdx = idx + 1
      sb.append("""<col min="$colIdx" max="$colIdx" width="$w" customWidth="1"/>""")
    }
    sb.append("""</cols>""")
    sb.append("""<sheetData>""")
    rows.forEachIndexed { rowIdx, cells ->
      val r = rowIdx + 1
      sb.append("""<row r="$r">""")
      cells.forEachIndexed { colIdx, cell ->
        val ref = "${columnLetter(colIdx)}$r"
        when (cell) {
          is CellValue.Text -> {
            sb.append("""<c r="$ref" t="inlineStr" s="${cell.styleId}"><is><t xml:space="preserve">""")
            sb.append(escapeXml(cell.value))
            sb.append("""</t></is></c>""")
          }
          is CellValue.Number -> {
            val formatted = String.format(Locale.US, "%.2f", cell.value)
            sb.append("""<c r="$ref" s="${cell.styleId}"><v>$formatted</v></c>""")
          }
        }
      }
      sb.append("""</row>""")
    }
    sb.append("""</sheetData></worksheet>""")
    return sb.toString()
  }

  private fun columnLetter(index: Int): String {
    var n = index
    val sb = StringBuilder()
    while (n >= 0) {
      sb.insert(0, ('A'.code + (n % 26)).toChar())
      n = (n / 26) - 1
    }
    return sb.toString()
  }

  private fun escapeXml(raw: String): String = buildString(raw.length + 16) {
    raw.forEach { ch ->
      when (ch) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        '"' -> append("&quot;")
        '\'' -> append("&apos;")
        '\u0000' -> Unit
        else -> if (ch >= ' ' || ch == '\n' || ch == '\r' || ch == '\t') append(ch)
      }
    }
  }

  private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

  private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

  private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="ملخص الهواتف" sheetId="1" r:id="rId1"/>
    <sheet name="تفاصيل عمليات الكاش" sheetId="2" r:id="rId2"/>
    <sheet name="سجل عمليات السحب" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""

  private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

  private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><name val="Arial"/></font>
    <font><b/><sz val="11"/><color rgb="FF181A20"/><name val="Arial"/></font>
    <font><b/><sz val="14"/><color rgb="FF181A20"/><name val="Arial"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFD000"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFF6CC"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD0D5DD"/></left>
      <right style="thin"><color rgb="FFD0D5DD"/></right>
      <top style="thin"><color rgb="FFD0D5DD"/></top>
      <bottom style="thin"><color rgb="FFD0D5DD"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="5">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center" readingOrder="2"/></xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" readingOrder="2"/></xf>
    <xf numFmtId="4" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center" readingOrder="2"/></xf>
    <xf numFmtId="3" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
  </cellXfs>
</styleSheet>"""
}
