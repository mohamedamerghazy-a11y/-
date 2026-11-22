package com.example.core.printing

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

data class PairedBluetoothPrinter(
  val name: String,
  val address: String,
)

object BluetoothThermalPrinterManager {

  private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

  fun requiredBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
      )
    } else {
      emptyArray()
    }
  }

  fun hasBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  @SuppressLint("MissingPermission")
  fun getPairedPrinters(context: Context): Result<List<PairedBluetoothPrinter>> {
    if (!hasBluetoothPermissions(context)) {
      return Result.failure(SecurityException("يرجى منح صلاحية البلوتوث أولًا لعرض الطابعات المقترنة."))
    }
    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      ?: return Result.failure(IllegalStateException("جهازك لا يدعم البلوتوث."))
    val adapter = btManager.adapter
      ?: return Result.failure(IllegalStateException("محوّل البلوتوث غير متاح."))
    if (!adapter.isEnabled) {
      return Result.failure(IllegalStateException("البلوتوث مغلق حاليًا. يرجى تشغيل البلوتوث من إعدادات الجهاز."))
    }

    val pairedDevices: Set<BluetoothDevice> = adapter.bondedDevices.orEmpty()
    val printers = pairedDevices.map { dev ->
      PairedBluetoothPrinter(
        name = dev.name?.takeIf { it.isNotBlank() } ?: "طابعة بلوتوث",
        address = dev.address.orEmpty(),
      )
    }.filter { it.address.isNotBlank() }
      .sortedBy { it.name.lowercase() }

    return Result.success(printers)
  }

  @SuppressLint("MissingPermission")
  suspend fun printEscPosBytes(
    context: Context,
    deviceAddress: String,
    payload: ByteArray,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      if (!hasBluetoothPermissions(context)) {
        throw SecurityException("يرجى السماح بصلاحية الاتصال بالبلوتوث أولًا.")
      }
      val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        ?: throw IllegalStateException("البلوتوث غير مدعوم على هذا الجهاز.")
      val adapter = btManager.adapter
        ?: throw IllegalStateException("محوّل البلوتوث غير متاح.")
      if (!adapter.isEnabled) {
        throw IllegalStateException("يرجى تفعيل البلوتوث ثم المحاولة مرة أخرى.")
      }

      val device = adapter.getRemoteDevice(deviceAddress)
        ?: throw IllegalStateException("تعذر العثور على الطابعة المحددة.")

      runCatching { adapter.cancelDiscovery() }

      val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
      socket.use { sock ->
        sock.connect()
        val output = sock.outputStream
        val chunkSize = 1024
        var offset = 0
        while (offset < payload.size) {
          val count = minOf(chunkSize, payload.size - offset)
          output.write(payload, offset, count)
          output.flush()
          offset += count
          delay(20L)
        }
        delay(250L)
      }
    }
  }
}
