package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.printing.BluetoothThermalPrinterManager
import com.example.core.printing.PairedBluetoothPrinter
import com.example.core.printing.ThermalPaperWidth
import com.example.core.printing.ThermalReceiptRenderer
import com.example.data.model.Order
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderReceiptPrintDialog(
  order: Order,
  storeName: String = "الإيمان للتجارة",
  storePhone: String = "",
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedPaper by remember { mutableStateOf(ThermalPaperWidth.MM_80) }
  var hasBtPermission by remember {
    mutableStateOf(BluetoothThermalPrinterManager.hasBluetoothPermissions(context))
  }
  var pairedPrinters by remember { mutableStateOf<List<PairedBluetoothPrinter>>(emptyList()) }
  var selectedPrinterAddress by remember { mutableStateOf<String?>(null) }
  var isPrinting by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var statusIsError by remember { mutableStateOf(false) }

  fun reloadPrinters() {
    BluetoothThermalPrinterManager.getPairedPrinters(context).fold(
      onSuccess = { list ->
        pairedPrinters = list
        if (selectedPrinterAddress == null && list.isNotEmpty()) {
          selectedPrinterAddress = list.first().address
        }
        if (list.isEmpty()) {
          statusMessage = "لا توجد طابعات مقترنة بالبلوتوث. قم بإقران الطابعة من إعدادات البلوتوث بالهاتف ثم اضغط تحديث."
          statusIsError = false
        } else {
          statusMessage = null
        }
      },
      onFailure = { err ->
        pairedPrinters = emptyList()
        statusMessage = err.message ?: "تعذر قراءة الطابعات المقترنة."
        statusIsError = true
      },
    )
  }

  val btPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    val granted = result.values.all { it }
    hasBtPermission = granted || BluetoothThermalPrinterManager.hasBluetoothPermissions(context)
    if (hasBtPermission) {
      reloadPrinters()
    } else {
      statusMessage = "تم رفض إذن البلوتوث. لا يمكن الاتصال بالطابعة الحرارية بدون الإذن."
      statusIsError = true
    }
  }

  LaunchedEffect(hasBtPermission) {
    if (hasBtPermission) {
      reloadPrinters()
    }
  }

  val previewBitmap = remember(order, selectedPaper, storeName, storePhone) {
    ThermalReceiptRenderer.renderInvoiceBitmap(
      context = context,
      order = order,
      storeName = storeName,
      storePhone = storePhone,
      widthPx = selectedPaper.widthPx,
      brandedShareStyle = true,
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("order_receipt_print_dialog"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // الترويسة
        Surface(color = CharcoalBlack) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Filled.Print, contentDescription = null, tint = PrimaryYellow)
              Column {
                Text(
                  text = "إيصال الطلب #${order.orderNumber.ifBlank { order.id.take(8) }}",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = PrimaryYellow,
                  maxLines = 1,
                )
                Text(
                  text = "تجهيز وطباعة حرارية أو مشاركة واتساب",
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite.copy(alpha = 0.85f),
                )
              }
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = PureWhite)
            }
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // 1. اختيار مقاس الورق الحراري
          Text(
            text = "مقاس ورق الطابعة الحرارية",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ThermalPaperWidth.entries.forEach { widthOpt ->
              val selected = selectedPaper == widthOpt
              FilterChip(
                selected = selected,
                onClick = { selectedPaper = widthOpt },
                modifier = Modifier
                  .weight(1f)
                  .testTag("paper_size_${widthOpt.code}"),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryYellow,
                  selectedLabelColor = CharcoalBlack,
                ),
                label = {
                  Text(
                    text = widthOpt.arabicLabel,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                  )
                },
              )
            }
          }
          Text(
            text = selectedPaper.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )

          // 2. معاينة الإيصال المصغر
          Text(
            text = "معاينة الفاتورة المنسقة",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = GeneralBackground,
            border = BorderStroke(1.dp, BorderNeutral),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 260.dp),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
              contentAlignment = Alignment.TopCenter,
            ) {
              Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "معاينة إيصال الطلب",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                  .fillMaxWidth(if (selectedPaper == ThermalPaperWidth.MM_58) 0.78f else 0.96f)
                  .clip(RoundedCornerShape(8.dp)),
              )
            }
          }

          HorizontalDivider(color = BorderNeutral)

          // 3. الطباعة بالبلوتوث على الطابعات الحرارية
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
              Text(
                text = "طابعة البلوتوث الحرارية",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
            }
            if (hasBtPermission) {
              OutlinedButton(
                onClick = { reloadPrinters() },
                border = BorderStroke(1.dp, BorderNeutral),
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = CharcoalBlack)
                Spacer(modifier = Modifier.width(4.dp))
                Text("تحديث", color = CharcoalBlack, maxLines = 1, softWrap = false)
              }
            }
          }

          if (!hasBtPermission) {
            OutlinedButton(
              onClick = {
                val perms = BluetoothThermalPrinterManager.requiredBluetoothPermissions()
                if (perms.isNotEmpty()) {
                  btPermissionLauncher.launch(perms)
                } else {
                  hasBtPermission = true
                }
              },
              border = BorderStroke(1.dp, RoyalBlue),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("السماح بالاتصال بالطابعات عبر البلوتوث", color = RoyalBlue, fontWeight = FontWeight.Bold)
            }
          } else {
            if (pairedPrinters.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pairedPrinters.forEach { printer ->
                  val isSel = selectedPrinterAddress == printer.address
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) PrimaryYellowSoft else GeneralBackground,
                    border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) CharcoalBlack else BorderNeutral),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { selectedPrinterAddress = printer.address },
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = printer.name,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = printer.address,
                          style = MaterialTheme.typography.labelSmall,
                          color = TextSecondary,
                        )
                      }
                      if (isSel) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "طابعة محددة", tint = CharcoalBlack)
                      }
                    }
                  }
                }
              }
            }

            Button(
              onClick = {
                val targetAddr = selectedPrinterAddress
                if (targetAddr.isNullOrBlank()) {
                  statusMessage = "اختر طابعة بلوتوث مقترنة أولًا."
                  statusIsError = true
                  return@Button
                }
                isPrinting = true
                statusMessage = "جاري إرسال الفاتورة (${selectedPaper.arabicLabel}) للطابعة..."
                statusIsError = false
                scope.launch {
                  val monoBitmap = ThermalReceiptRenderer.renderInvoiceBitmap(
                    context = context,
                    order = order,
                    storeName = storeName,
                    storePhone = storePhone,
                    widthPx = selectedPaper.widthPx,
                    brandedShareStyle = false,
                  )
                  val escPosBytes = ThermalReceiptRenderer.bitmapToEscPosRaster(monoBitmap)
                  BluetoothThermalPrinterManager.printEscPosBytes(context, targetAddr, escPosBytes).fold(
                    onSuccess = {
                      isPrinting = false
                      statusMessage = "تمت طباعة الإيصال بنجاح بمقاس ${selectedPaper.arabicLabel}."
                      statusIsError = false
                    },
                    onFailure = { err ->
                      isPrinting = false
                      statusMessage = err.message ?: "تعذر الاتصال بالطابعة. تأكد من تشغيلها واقترابها."
                      statusIsError = true
                    },
                  )
                }
              },
              enabled = !isPrinting && !selectedPrinterAddress.isNullOrBlank(),
              colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryYellow,
                contentColor = CharcoalBlack,
              ),
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("thermal_print_execute_button"),
            ) {
              if (isPrinting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CharcoalBlack, strokeWidth = 2.dp)
              } else {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "طباعة حرارية (${selectedPaper.arabicLabel})",
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }
          }

          if (!statusMessage.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (statusIsError) DiscountRedSoft else SuccessGreenSoft,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = statusMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (statusIsError) DiscountRed else SuccessGreen,
                modifier = Modifier.padding(10.dp),
              )
            }
          }

          HorizontalDivider(color = BorderNeutral)

          // 4. مشاركة الفاتورة على واتساب أو PDF
          Text(
            text = "مشاركة الفاتورة أو تصديرها",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (order.phone.isNotBlank()) {
              Button(
                onClick = {
                  val text = ThermalReceiptRenderer.buildWhatsAppInvoiceText(order, storeName, storePhone)
                  ThermalReceiptRenderer.openWhatsAppDirectToPhone(context, order.phone, text).onFailure {
                    statusMessage = "تعذر فتح واتساب على رقم العميل."
                    statusIsError = true
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = SuccessGreen,
                  contentColor = PureWhite,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("whatsapp_send_to_customer_button"),
              ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("واتساب لصاحب الطلب", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
              }
            }

            Button(
              onClick = {
                ThermalReceiptRenderer.shareInvoiceImage(
                  context = context,
                  order = order,
                  storeName = storeName,
                  storePhone = storePhone,
                  targetWhatsAppDirectly = true,
                ).onFailure {
                  statusMessage = "تعذر تجهيز صورة الفاتورة للمشاركة."
                  statusIsError = true
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = RoyalBlue,
                contentColor = PureWhite,
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("share_invoice_whatsapp_image_button"),
            ) {
              Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("مشاركة صورة واتساب", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                ThermalReceiptRenderer.shareInvoiceImage(
                  context = context,
                  order = order,
                  storeName = storeName,
                  storePhone = storePhone,
                  targetWhatsAppDirectly = false,
                ).onFailure {
                  statusMessage = "تعذر مشاركة صورة الفاتورة."
                  statusIsError = true
                }
              },
              border = BorderStroke(1.dp, CharcoalBlack),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Filled.Share, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("مشاركة لشخص آخر", color = CharcoalBlack, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
              onClick = {
                ThermalReceiptRenderer.printViaAndroidPrintManager(
                  context = context,
                  order = order,
                  storeName = storeName,
                  storePhone = storePhone,
                ).onFailure { err ->
                  statusMessage = err.message ?: "تعذر بدء الطباعة عبر النظام."
                  statusIsError = true
                }
              },
              border = BorderStroke(1.dp, BorderNeutral),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("طباعة PDF", color = CharcoalBlack, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}
