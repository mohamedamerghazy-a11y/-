package com.example.core.printing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.util.ArabicFormatter
import com.example.data.model.Order
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class ThermalPaperWidth(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
  val widthPx: Int,
) {
  MM_58("58mm", "58 مم", "رول صغير محمول (384 نقطة)", 384),
  MM_80("80mm", "80 مم", "رول قياسي للكاشير والمخزن (576 نقطة)", 576);

  companion object {
    fun fromCode(code: String?): ThermalPaperWidth =
      entries.firstOrNull { it.code == code } ?: MM_80
  }
}

object ThermalReceiptRenderer {

  private const val COLOR_CHARCOAL = 0xFF1E1E1E.toInt()
  private const val COLOR_PRIMARY_YELLOW = 0xFFFFD400.toInt()
  private const val COLOR_YELLOW_SOFT = 0xFFFFF8DC.toInt()
  private const val COLOR_ROYAL_BLUE = 0xFF1565C0.toInt()
  private const val COLOR_SURFACE_GRAY = 0xFFF7F7F9.toInt()
  private const val COLOR_BORDER = 0xFFD9DCE2.toInt()

  /**
   * يرسم فاتورة عربية منسقة بخط Tajawal كصورة Bitmap جاهزة للطباعة الحرارية أو المشاركة كصورة احترافية.
   */
  fun renderInvoiceBitmap(
    context: Context,
    order: Order,
    storeName: String,
    storePhone: String,
    widthPx: Int,
    brandedShareStyle: Boolean = false,
  ): Bitmap {
    val regularTf = ResourcesCompat.getFont(context, R.font.tajawal_regular) ?: Typeface.DEFAULT
    val boldTf = ResourcesCompat.getFont(context, R.font.tajawal_bold) ?: Typeface.DEFAULT_BOLD

    // تمريرة أولى لحساب الارتفاع بدقة ثم التمريرة الثانية للرسم الفعلي
    val measuredHeight = drawReceipt(
      canvas = null,
      order = order,
      storeName = storeName.ifBlank { "الإيمان للتجارة" },
      storePhone = storePhone,
      widthPx = widthPx,
      branded = brandedShareStyle,
      regularTf = regularTf,
      boldTf = boldTf,
    )

    val bitmap = Bitmap.createBitmap(widthPx, measuredHeight.coerceAtLeast(320), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    drawReceipt(
      canvas = canvas,
      order = order,
      storeName = storeName.ifBlank { "الإيمان للتجارة" },
      storePhone = storePhone,
      widthPx = widthPx,
      branded = brandedShareStyle,
      regularTf = regularTf,
      boldTf = boldTf,
    )
    return bitmap
  }

  private fun drawReceipt(
    canvas: Canvas?,
    order: Order,
    storeName: String,
    storePhone: String,
    widthPx: Int,
    branded: Boolean,
    regularTf: Typeface,
    boldTf: Typeface,
  ): Int {
    val scale = widthPx / 576f
    val pad = (18f * scale).roundToInt().coerceAtLeast(10)
    val contentW = (widthPx - pad * 2).coerceAtLeast(180)
    val gapSm = 8f * scale
    val gapMd = 14f * scale

    val paintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = (1.8f * scale).coerceAtLeast(1f)
      color = if (branded) COLOR_BORDER else Color.BLACK
    }

    fun makePaint(sizeSp: Float, bold: Boolean, color: Int): TextPaint {
      return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (bold) boldTf else regularTf
        textSize = (sizeSp * scale).coerceAtLeast(15f)
        this.color = color
      }
    }

    val titlePaint = makePaint(30f, bold = true, color = if (branded) COLOR_PRIMARY_YELLOW else Color.WHITE)
    val subTitlePaint = makePaint(19f, bold = false, color = Color.WHITE)
    val sectionPaint = makePaint(22f, bold = true, color = if (branded) COLOR_CHARCOAL else Color.BLACK)
    val bodyPaint = makePaint(20f, bold = false, color = if (branded) COLOR_CHARCOAL else Color.BLACK)
    val bodyBoldPaint = makePaint(20f, bold = true, color = if (branded) COLOR_CHARCOAL else Color.BLACK)
    val mutedPaint = makePaint(18f, bold = false, color = if (branded) 0xFF555555.toInt() else Color.BLACK)
    val badgeTextPaint = makePaint(18f, bold = true, color = if (branded) COLOR_CHARCOAL else Color.BLACK)
    val totalLabelPaint = makePaint(24f, bold = true, color = if (branded) COLOR_PRIMARY_YELLOW else Color.WHITE)

    var y = pad.toFloat()

    // --- 1. ترويسة المتجر ---
    val headerStoreLayout = buildRtlLayout(storeName, titlePaint, contentW - pad, Layout.Alignment.ALIGN_CENTER)
    val headerSubLayout = buildRtlLayout("فاتورة طلب وتجهيز • عروض الجملة والقطاعي", subTitlePaint, contentW - pad, Layout.Alignment.ALIGN_CENTER)
    val headerHeight = pad + headerStoreLayout.height + gapSm + headerSubLayout.height + pad
    if (canvas != null) {
      paintFill.color = if (branded) COLOR_CHARCOAL else Color.BLACK
      val rect = RectF(pad.toFloat(), y, (widthPx - pad).toFloat(), y + headerHeight)
      if (branded) {
        canvas.drawRoundRect(rect, 16f * scale, 16f * scale, paintFill)
      } else {
        canvas.drawRect(rect, paintFill)
      }
      drawLayoutAt(canvas, headerStoreLayout, pad + (pad / 2f), y + pad * 0.7f)
      drawLayoutAt(canvas, headerSubLayout, pad + (pad / 2f), y + pad * 0.7f + headerStoreLayout.height + gapSm * 0.6f)
    }
    y += headerHeight + gapMd

    // --- 2. صندوق رقم الطلب والتاريخ ---
    val orderNumText = "رقم الطلب: #${ArabicFormatter.formatDigits(order.orderNumber.ifBlank { order.id.take(8) })}"
    val sdf = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.Builder().setLanguage("ar").setRegion("EG").build())
    val dateText = order.createdAt?.toDate()?.let { ArabicFormatter.formatDigits(sdf.format(it)) } ?: "حديث"
    val channelText = if (order.accountTypeAtOrder == "wholesale") "طلب جملة" else "طلب متجر"

    val fulfillmentText = if (order.isStorePickup) "الاستلام من المتجر" else "توصيل للعنوان"
    val statusLabel = order.statusEnum.labelFor(order.isStorePickup)

    val ordNumLayout = buildRtlLayout(orderNumText, sectionPaint, contentW - pad)
    val ordDateLayout = buildRtlLayout("التاريخ: $dateText", mutedPaint, contentW - pad)
    val ordStatusLayout = buildRtlLayout("الحالة: $statusLabel  |  $fulfillmentText", bodyBoldPaint, contentW - pad)
    val metaBoxHeight = pad * 0.7f + ordNumLayout.height + gapSm * 0.5f + ordDateLayout.height + gapSm * 0.5f + ordStatusLayout.height + pad * 0.7f

    if (canvas != null) {
      val metaRect = RectF(pad.toFloat(), y, (widthPx - pad).toFloat(), y + metaBoxHeight)
      paintFill.color = if (branded) COLOR_YELLOW_SOFT else Color.WHITE
      canvas.drawRoundRect(metaRect, 12f * scale, 12f * scale, paintFill)
      paintStroke.color = if (branded) COLOR_PRIMARY_YELLOW else Color.BLACK
      canvas.drawRoundRect(metaRect, 12f * scale, 12f * scale, paintStroke)
      var my = y + pad * 0.6f
      drawLayoutAt(canvas, ordNumLayout, pad + pad * 0.5f, my)
      my += ordNumLayout.height + gapSm * 0.5f
      drawLayoutAt(canvas, ordDateLayout, pad + pad * 0.5f, my)
      my += ordDateLayout.height + gapSm * 0.5f
      drawLayoutAt(canvas, ordStatusLayout, pad + pad * 0.5f, my)
    }
    y += metaBoxHeight + gapMd

    // --- 3. بيانات العميل والاستلام/العنوان ---
    val recipient = order.recipientName.ifBlank { order.customerName }.ifBlank { "عميل المتجر" }
    val addressLine = if (order.isStorePickup) {
      order.streetAddress.takeIf { it.isNotBlank() } ?: "الفرع الرئيسي - $storeName"
    } else {
      listOfNotNull(
        order.governorate.takeIf { it.isNotBlank() },
        order.city.takeIf { it.isNotBlank() },
        order.streetAddress.takeIf { it.isNotBlank() },
      ).joinToString(" - ")
    }
    val customerLines = buildString {
      append("العميل / المستلم: $recipient  ($channelText)\n")
      if (order.phone.isNotBlank()) append("الهاتف: ${ArabicFormatter.formatDigits(order.phone)}\n")
      if (order.isStorePickup) {
        append("مقر الاستلام: $addressLine\n")
      } else {
        if (addressLine.isNotBlank()) append("العنوان: $addressLine\n")
        if (!order.landmark.isNullOrBlank()) append("علامة مميزة: ${order.landmark}\n")
      }
      append("الدفع: ${order.paymentMethodEnum.arabicLabel} (${order.paymentStatusEnum.arabicLabel})")
      if (!order.displaySenderPhone.isNullOrBlank()) {
        append("\nالرقم المحوَّل منه: ${ArabicFormatter.formatDigits(order.displaySenderPhone.orEmpty())}")
      }
      if (!order.notes.isNullOrBlank()) append("\nملاحظات: ${order.notes}")
    }
    val custHeaderLayout = buildRtlLayout(
      if (order.isStorePickup) "بيانات الاستلام من المتجر والدفع" else "بيانات التوصيل والدفع",
      sectionPaint,
      contentW - pad,
    )
    val custBodyLayout = buildRtlLayout(customerLines, bodyPaint, contentW - pad)
    val custBoxHeight = pad * 0.6f + custHeaderLayout.height + gapSm * 0.6f + custBodyLayout.height + pad * 0.7f

    if (canvas != null) {
      val custRect = RectF(pad.toFloat(), y, (widthPx - pad).toFloat(), y + custBoxHeight)
      paintFill.color = if (branded) COLOR_SURFACE_GRAY else Color.WHITE
      canvas.drawRoundRect(custRect, 12f * scale, 12f * scale, paintFill)
      paintStroke.color = if (branded) COLOR_BORDER else Color.BLACK
      canvas.drawRoundRect(custRect, 12f * scale, 12f * scale, paintStroke)
      drawLayoutAt(canvas, custHeaderLayout, pad + pad * 0.5f, y + pad * 0.55f)
      drawLayoutAt(canvas, custBodyLayout, pad + pad * 0.5f, y + pad * 0.55f + custHeaderLayout.height + gapSm * 0.6f)
    }
    y += custBoxHeight + gapMd

    // --- 4. جدول الأصناف ---
    val itemsTitleLayout = buildRtlLayout("تفاصيل الأصناف (${ArabicFormatter.formatNumber(order.items.size)})", sectionPaint, contentW)
    if (canvas != null) {
      drawLayoutAt(canvas, itemsTitleLayout, pad.toFloat(), y)
    }
    y += itemsTitleLayout.height + gapSm

    // خط فاصل قبل الأصناف
    if (canvas != null) {
      paintFill.color = if (branded) COLOR_CHARCOAL else Color.BLACK
      canvas.drawRect(pad.toFloat(), y, (widthPx - pad).toFloat(), y + (2f * scale).coerceAtLeast(1.5f), paintFill)
    }
    y += gapSm

    order.items.forEachIndexed { idx, item ->
      val indexNum = ArabicFormatter.formatNumber(idx + 1)
      val variantInfo = listOfNotNull(
        item.selectedColor?.takeIf { it.isNotBlank() }?.let { "لون: $it" },
        item.selectedSize?.takeIf { it.isNotBlank() }?.let { "مقاس: $it" },
      ).joinToString(" • ")
      val itemTitle = if (variantInfo.isNotBlank()) {
        "$indexNum. ${item.productName} ($variantInfo)"
      } else {
        "$indexNum. ${item.productName}"
      }
      val nameLayout = buildRtlLayout(itemTitle, bodyBoldPaint, contentW - (8f * scale).toInt())
      val qtyPriceStr = "${ArabicFormatter.formatNumber(item.quantity)} ${item.unit.ifBlank { "قطعة" }} × ${ArabicFormatter.formatCurrency(item.unitPrice)}"
      val lineTotalStr = ArabicFormatter.formatCurrency(item.lineTotal)

      val leftPriceColW = (contentW * 0.36f).roundToInt()
      val rightQtyColW = (contentW * 0.62f).roundToInt()
      val qtyLayout = buildRtlLayout(qtyPriceStr, mutedPaint, rightQtyColW)
      val priceLayout = buildRtlLayout(lineTotalStr, bodyBoldPaint, leftPriceColW, Layout.Alignment.ALIGN_OPPOSITE)
      val secondRowH = maxOf(qtyLayout.height, priceLayout.height)
      val rowH = nameLayout.height + (4f * scale) + secondRowH + gapSm

      if (canvas != null) {
        if (branded && idx % 2 == 1) {
          paintFill.color = COLOR_SURFACE_GRAY
          canvas.drawRoundRect(
            RectF(pad.toFloat(), y - 2f, (widthPx - pad).toFloat(), y + rowH - 2f),
            8f * scale,
            8f * scale,
            paintFill,
          )
        }
        drawLayoutAt(canvas, nameLayout, pad + 4f * scale, y)
        val row2Y = y + nameLayout.height + 4f * scale
        // في التخطيط العربي: الكمية يمين والعمود الإجمالي يسار
        drawLayoutAt(canvas, priceLayout, pad + 4f * scale, row2Y)
        drawLayoutAt(canvas, qtyLayout, (widthPx - pad - rightQtyColW - 4f * scale), row2Y)
        paintFill.color = if (branded) COLOR_BORDER else Color.BLACK
        canvas.drawRect(pad.toFloat(), y + rowH - 1f, (widthPx - pad).toFloat(), y + rowH, paintFill)
      }
      y += rowH + (4f * scale)
    }

    y += gapSm

    // --- 5. ملخص الحساب والإجمالي النهائي ---
    val summaryRows = mutableListOf<Pair<String, String>>()
    summaryRows.add("المجموع الفرعي" to ArabicFormatter.formatCurrency(order.subtotal))
    if (order.discountAmount > 0.0) {
      val cCode = order.couponCode?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
      summaryRows.add("الخصم$cCode" to "- ${ArabicFormatter.formatCurrency(order.discountAmount)}")
    }
    if (order.isStorePickup) {
      summaryRows.add("الاستلام من المتجر" to "مجانًا")
    } else {
      summaryRows.add("رسوم التوصيل" to ArabicFormatter.formatCurrency(order.deliveryFee))
    }

    summaryRows.forEach { (label, amount) ->
      val labelW = (contentW * 0.56f).roundToInt()
      val valW = (contentW * 0.42f).roundToInt()
      val lLayout = buildRtlLayout(label, bodyPaint, labelW)
      val vLayout = buildRtlLayout(amount, bodyBoldPaint, valW, Layout.Alignment.ALIGN_OPPOSITE)
      val rH = maxOf(lLayout.height, vLayout.height)
      if (canvas != null) {
        drawLayoutAt(canvas, vLayout, pad.toFloat(), y)
        drawLayoutAt(canvas, lLayout, (widthPx - pad - labelW).toFloat(), y)
      }
      y += rH + (5f * scale)
    }

    y += gapSm * 0.6f
    val totalTitleL = buildRtlLayout("الإجمالي المطلوب", totalLabelPaint, (contentW * 0.50f).roundToInt())
    val totalValL = buildRtlLayout(
      ArabicFormatter.formatCurrency(order.totalAmount),
      totalLabelPaint,
      (contentW * 0.46f).roundToInt(),
      Layout.Alignment.ALIGN_OPPOSITE,
    )
    val totalBoxH = pad * 0.7f + maxOf(totalTitleL.height, totalValL.height) + pad * 0.7f
    if (canvas != null) {
      paintFill.color = if (branded) COLOR_CHARCOAL else Color.BLACK
      val totalRect = RectF(pad.toFloat(), y, (widthPx - pad).toFloat(), y + totalBoxH)
      if (branded) {
        canvas.drawRoundRect(totalRect, 12f * scale, 12f * scale, paintFill)
      } else {
        canvas.drawRect(totalRect, paintFill)
      }
      val ty = y + pad * 0.6f
      drawLayoutAt(canvas, totalValL, pad + pad * 0.4f, ty)
      drawLayoutAt(canvas, totalTitleL, widthPx - pad - (contentW * 0.50f) - pad * 0.4f, ty)
    }
    y += totalBoxH + gapMd

    // --- 6. التذييل ---
    val footerMsg = buildString {
      append("شكرًا لتعاملكم مع $storeName")
      if (storePhone.isNotBlank()) {
        append("\nللاستفسار والتواصل: ${ArabicFormatter.formatDigits(storePhone)}")
      }
    }
    val footerLayout = buildRtlLayout(footerMsg, badgeTextPaint, contentW, Layout.Alignment.ALIGN_CENTER)
    if (canvas != null) {
      drawLayoutAt(canvas, footerLayout, pad.toFloat(), y)
    }
    y += footerLayout.height + pad * 1.4f

    return y.roundToInt()
  }

  private fun buildRtlLayout(
    text: String,
    paint: TextPaint,
    widthPx: Int,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
  ): StaticLayout {
    val safeW = widthPx.coerceAtLeast(40)
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, safeW)
      .setAlignment(alignment)
      .setTextDirection(TextDirectionHeuristics.RTL)
      .setIncludePad(false)
      .build()
  }

  private fun drawLayoutAt(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
  }

  /**
   * تحويل صورة الفاتورة إلى بايتات ESC/POS قياسية (GS v 0) مقسمة لشرائح لحماية ذاكرة الطابعات الحرارية.
   */
  fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
    val out = ByteArrayOutputStream()
    // ESC @ (Reset) + ESC a 1 (Center align)
    out.write(byteArrayOf(0x1B, 0x40, 0x1B, 0x61, 0x01))

    val width = bitmap.width
    val height = bitmap.height
    val bytesPerRow = (width + 7) / 8
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val sliceHeight = 200
    var startY = 0
    while (startY < height) {
      val chunkH = minOf(sliceHeight, height - startY)
      val xL = (bytesPerRow and 0xFF).toByte()
      val xH = ((bytesPerRow shr 8) and 0xFF).toByte()
      val yL = (chunkH and 0xFF).toByte()
      val yH = ((chunkH shr 8) and 0xFF).toByte()

      // GS v 0 m=0 xL xH yL yH
      out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

      for (y in startY until (startY + chunkH)) {
        val rowOffset = y * width
        for (byteX in 0 until bytesPerRow) {
          var sliceByte = 0
          for (bit in 0 until 8) {
            val pxX = byteX * 8 + bit
            if (pxX < width) {
              val c = pixels[rowOffset + pxX]
              val alpha = Color.alpha(c)
              val r = Color.red(c)
              val g = Color.green(c)
              val b = Color.blue(c)
              val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
              if (alpha > 128 && luminance < 168.0) {
                sliceByte = sliceByte or (1 shl (7 - bit))
              }
            }
          }
          out.write(sliceByte)
        }
      }
      startY += chunkH
    }

    // تغذية 4 سطور ثم قص جزئي (يدعم أغلب طابعات 58 و 80 مم)
    out.write(byteArrayOf(0x1B, 0x64, 0x04, 0x1D, 0x56, 0x42, 0x00))
    return out.toByteArray()
  }

  fun buildWhatsAppInvoiceText(
    order: Order,
    storeName: String,
    storePhone: String,
  ): String {
    val cleanStore = storeName.ifBlank { "الإيمان للتجارة" }
    val recipient = order.recipientName.ifBlank { order.customerName }
    val address = listOfNotNull(
      order.governorate.takeIf { it.isNotBlank() },
      order.city.takeIf { it.isNotBlank() },
      order.streetAddress.takeIf { it.isNotBlank() },
    ).joinToString(" - ")

    return buildString {
      appendLine("🧾 *فاتورة طلب من $cleanStore*")
      appendLine("رقم الطلب: *#${ArabicFormatter.formatDigits(order.orderNumber)}*")
      appendLine("طريقة الاستلام: ${if (order.isStorePickup) "الاستلام من المتجر" else "توصيل إلى العنوان"}")
      appendLine("العميل: $recipient")
      if (order.phone.isNotBlank()) {
        appendLine("الهاتف: ${ArabicFormatter.formatDigits(order.phone)}")
      }
      if (order.isStorePickup) {
        appendLine("مقر الاستلام: ${order.streetAddress.ifBlank { cleanStore }}")
      } else if (address.isNotBlank()) {
        appendLine("العنوان: $address")
      }
      appendLine("الحالة: ${order.statusEnum.labelFor(order.isStorePickup)}")
      if (!order.displaySenderPhone.isNullOrBlank()) {
        appendLine("الرقم المحوَّل منه: ${ArabicFormatter.formatDigits(order.displaySenderPhone.orEmpty())}")
      }
      appendLine("------------------")
      order.items.forEachIndexed { idx, item ->
        val variantInfo = listOfNotNull(
          item.selectedColor?.takeIf { it.isNotBlank() }?.let { "لون: $it" },
          item.selectedSize?.takeIf { it.isNotBlank() }?.let { "مقاس: $it" },
        ).joinToString(" • ")
        val variantSuffix = if (variantInfo.isNotBlank()) " [$variantInfo]" else ""
        appendLine(
          "${ArabicFormatter.formatNumber(idx + 1)}. ${item.productName}$variantSuffix (${ArabicFormatter.formatNumber(item.quantity)} ${item.unit}) = ${ArabicFormatter.formatCurrency(item.lineTotal)}",
        )
      }
      appendLine("------------------")
      appendLine("المجموع الفرعي: ${ArabicFormatter.formatCurrency(order.subtotal)}")
      if (order.discountAmount > 0.0) {
        appendLine("الخصم: - ${ArabicFormatter.formatCurrency(order.discountAmount)}")
      }
      if (order.isStorePickup) {
        appendLine("الاستلام من المتجر: مجانًا")
      } else {
        appendLine("التوصيل: ${ArabicFormatter.formatCurrency(order.deliveryFee)}")
      }
      appendLine("*الإجمالي النهائي: ${ArabicFormatter.formatCurrency(order.totalAmount)}*")
      if (storePhone.isNotBlank()) {
        appendLine("📞 للتواصل: ${ArabicFormatter.formatDigits(storePhone)}")
      }
    }.trim()
  }

  fun shareInvoiceImage(
    context: Context,
    order: Order,
    storeName: String,
    storePhone: String,
    targetWhatsAppDirectly: Boolean = false,
  ): Result<Unit> {
    return runCatching {
      val bitmap = renderInvoiceBitmap(
        context = context,
        order = order,
        storeName = storeName,
        storePhone = storePhone,
        widthPx = 760,
        brandedShareStyle = true,
      )
      val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
      val safeNum = order.orderNumber.ifBlank { order.id.take(8) }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val outFile = File(invoicesDir, "invoice_$safeNum.png")
      FileOutputStream(outFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
      }

      val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outFile,
      )
      val caption = buildWhatsAppInvoiceText(order, storeName, storePhone)
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        clipData = ClipData.newRawUri("فاتورة الطلب ${order.orderNumber}", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      if (targetWhatsAppDirectly) {
        val pm = context.packageManager
        val whatsAppPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
          runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
        if (whatsAppPkg != null) {
          sendIntent.`package` = whatsAppPkg
          sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(sendIntent)
          return@runCatching
        }
      }

      val chooser = Intent.createChooser(sendIntent, "مشاركة فاتورة الطلب #${order.orderNumber}").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(chooser)
    }
  }

  fun openWhatsAppDirectToPhone(
    context: Context,
    phone: String,
    message: String,
  ): Result<Unit> {
    return runCatching {
      val normalized = normalizeWhatsAppPhone(phone)
      require(normalized.length >= 10) { "رقم الهاتف غير صالح لفتح واتساب." }
      val uri = Uri.parse("https://wa.me/$normalized?text=${Uri.encode(message)}")
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  fun normalizeWhatsAppPhone(rawPhone: String): String {
    val digitsOnly = ArabicFormatter.formatDigits(rawPhone, com.example.data.model.NumeralFormat.ENGLISH)
      .filter { it.isDigit() }
    return when {
      digitsOnly.startsWith("00") -> digitsOnly.drop(2)
      digitsOnly.startsWith("01") && digitsOnly.length == 11 -> "2$digitsOnly"
      digitsOnly.startsWith("20") -> digitsOnly
      else -> digitsOnly
    }
  }

  fun printViaAndroidPrintManager(
    context: Context,
    order: Order,
    storeName: String,
    storePhone: String,
  ): Result<Unit> {
    return runCatching {
      val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        ?: throw IllegalStateException("خدمة الطباعة غير متاحة على هذا الجهاز.")
      val bitmap = renderInvoiceBitmap(
        context = context,
        order = order,
        storeName = storeName,
        storePhone = storePhone,
        widthPx = 760,
        brandedShareStyle = true,
      )
      val jobName = "فاتورة_${order.orderNumber.ifBlank { order.id }}"
      val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback,
          extras: Bundle?,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
          }
          val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
          callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback,
        ) {
          val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
          val pdfDoc = PrintedPdfDocument(context, attributes)
          try {
            val page = pdfDoc.startPage(0)
            val canvas = page.canvas
            val margin = 28f
            val availW = (canvas.width - margin * 2f).coerceAtLeast(100f)
            val availH = (canvas.height - margin * 2f).coerceAtLeast(100f)
            val ratio = minOf(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat()).coerceAtMost(1.2f)
            val drawW = bitmap.width * ratio
            val drawH = bitmap.height * ratio
            val left = (canvas.width - drawW) / 2f
            canvas.drawBitmap(bitmap, null, RectF(left, margin, left + drawW, margin + drawH), null)
            pdfDoc.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use { out ->
              pdfDoc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback.onWriteFailed(e.message)
          } finally {
            pdfDoc.close()
          }
        }
      }
      printManager.print(jobName, adapter, null)
    }
  }
}
