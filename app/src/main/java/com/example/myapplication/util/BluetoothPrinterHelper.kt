package com.example.myapplication.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object BluetoothPrinterHelper {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatReceiptText(receipt: ReceiptData): String {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(receipt.timestamp))
        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("       ${receipt.companyName}")
        if (!receipt.voen.isNullOrBlank()) {
            sb.appendLine("       VÖEN: ${receipt.voen}")
        }
        sb.appendLine("   e-Manager Satış Qəbzi")
        sb.appendLine("================================")
        sb.appendLine("Qəbz №: ${receipt.invoiceNumber}")
        sb.appendLine("Tarix: $dateStr")
        sb.appendLine("Kassir: ${receipt.cashierName}")
        sb.appendLine("Müştəri: ${receipt.customerName}")
        sb.appendLine("Terminal: Android POS-01")
        sb.appendLine("--------------------------------")
        sb.appendLine("MƏHSULLAR:")

        receipt.items.forEach { item ->
            val name = if (item.product.name.length > 18) item.product.name.take(17) + "…" else item.product.name
            val qtyStr = "${item.quantity} ${item.product.unit}"
            val priceStr = String.format("%.2f ₼", item.totalPrice)
            sb.appendLine(String.format("%-18s %5s %7s", name, qtyStr, priceStr))
            if (item.discountPercent > 0.0) {
                sb.appendLine("  (Endirim: ${item.discountPercent.toInt()}%)")
            }
        }

        sb.appendLine("--------------------------------")
        sb.appendLine(String.format("%-20s %10s", "İlkin Cəmi:", String.format("%.2f ₼", receipt.subtotal)))
        if (receipt.discountAmount > 0.0) {
            val discLabel = if (receipt.discountPercent > 0.0) "Endirim (${receipt.discountPercent.toInt()}%):" else "Endirim:"
            sb.appendLine(String.format("%-20s -%9s", discLabel, String.format("%.2f ₼", receipt.discountAmount)))
        }
        sb.appendLine(String.format("%-20s %10s", "YEKUN ÖDƏNİŞ:", String.format("%.2f ₼", receipt.totalAmount)))
        val vatAmount = receipt.totalAmount * 0.18 / 1.18
        sb.appendLine(String.format("%-20s %10s", "ƏDV (18% daxil):", String.format("%.2f ₼", vatAmount)))
        sb.appendLine("--------------------------------")
        sb.appendLine("Ödəniş növü: ${receipt.paymentType}")
        if (receipt.paymentType.contains("Nağd", ignoreCase = true) || receipt.paymentType.contains("Qarışıq", ignoreCase = true)) {
            if (receipt.cashPaid > 0.0) {
                sb.appendLine(String.format("%-20s %10s", "Müştəri verdi:", String.format("%.2f ₼", receipt.cashPaid)))
            }
            if (receipt.changeGiven > 0.0) {
                sb.appendLine(String.format("%-20s %10s", "Qaytarılan qalıq:", String.format("%.2f ₼", receipt.changeGiven)))
            }
        }
        if (receipt.cardPaid > 0.0) {
            sb.appendLine(String.format("%-20s %10s", "Kartla ödənildi:", String.format("%.2f ₼", receipt.cardPaid)))
        }
        sb.appendLine("================================")
        sb.appendLine("      Bizi seçdiyiniz üçün")
        sb.appendLine("        təşəkkür edirik!")
        sb.appendLine("        www.e-manager.az")
        sb.appendLine("================================")
        return sb.toString()
    }

    fun shareReceiptText(context: Context, receipt: ReceiptData) {
        val text = formatReceiptText(receipt)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Qəbzi Paylaş")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    @SuppressLint("MissingPermission")
    suspend fun printSaleReceipt(
        device: BluetoothDevice?,
        receipt: ReceiptData
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val text = formatReceiptText(receipt)

        if (device == null) {
            return@withContext Pair(true, text)
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            // ESC/POS Commands
            val initPrinter = byteArrayOf(0x1B, 0x40) // Initialize
            val centerAlign = byteArrayOf(0x1B, 0x61, 0x01) // Center
            val leftAlign = byteArrayOf(0x1B, 0x61, 0x00) // Left
            val boldOn = byteArrayOf(0x1B, 0x45, 0x01) // Bold
            val boldOff = byteArrayOf(0x1B, 0x45, 0x00) // Bold off
            val feedAndCut = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // Cut

            outputStream.write(initPrinter)
            outputStream.write(centerAlign)
            outputStream.write(boldOn)
            outputStream.write("${receipt.companyName}\n".toByteArray(Charsets.UTF_8))
            outputStream.write("e-Manager Satış Qəbzi\n".toByteArray(Charsets.UTF_8))
            outputStream.write(boldOff)
            outputStream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))
            outputStream.write(leftAlign)
            outputStream.write("Qəbz: ${receipt.invoiceNumber}\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Kassir: ${receipt.cashierName}\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Müştəri: ${receipt.customerName}\n".toByteArray(Charsets.UTF_8))
            outputStream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))

            receipt.items.forEach { item ->
                val line = "${item.quantity}x ${item.product.name} = ${String.format("%.2f ₼", item.totalPrice)}\n"
                outputStream.write(line.toByteArray(Charsets.UTF_8))
            }

            outputStream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))
            outputStream.write(boldOn)
            outputStream.write("YEKUN: ${String.format("%.2f ₼", receipt.totalAmount)}\n".toByteArray(Charsets.UTF_8))
            outputStream.write(boldOff)
            outputStream.write("Ödəniş: ${receipt.paymentType}\n".toByteArray(Charsets.UTF_8))
            if (receipt.changeGiven > 0.0) {
                outputStream.write("Qalıq: ${String.format("%.2f ₼", receipt.changeGiven)}\n".toByteArray(Charsets.UTF_8))
            }
            outputStream.write(centerAlign)
            outputStream.write("Təşəkkür edirik!\n".toByteArray(Charsets.UTF_8))
            outputStream.write("www.e-manager.az\n\n\n".toByteArray(Charsets.UTF_8))
            outputStream.write(feedAndCut)
            outputStream.flush()

            Pair(true, "${device.name ?: "Printer"} ilə çap tamamlandı!")
        } catch (e: Exception) {
            Pair(false, "Printerə qoşulma xətası: ${e.localizedMessage}")
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printTestReceipt(
        device: BluetoothDevice?,
        companyName: String,
        cashierName: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        if (device == null) {
            return@withContext Pair(
                true,
                "Test Çapı Uğurlu (Simulyasiya):\n" +
                "================================\n" +
                "       $companyName\n" +
                "   e-Manager Mobile POS Test\n" +
                "================================\n" +
                "Tarix: $dateStr\n" +
                "Kassir: $cashierName\n" +
                "Terminal: Android POS-01\n" +
                "--------------------------------\n" +
                "Test Məhsul 1         1x 2.50 ₼\n" +
                "Test Məhsul 2         2x 3.00 ₼\n" +
                "--------------------------------\n" +
                "CƏMİ:                    8.50 ₼\n" +
                "ÖDƏNİŞ: Nağd\n" +
                "================================\n" +
                "   Sistemlə əlaqə quruldu!\n" +
                "      www.e-manager.az\n" +
                "================================\n"
            )
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            val initPrinter = byteArrayOf(0x1B, 0x40)
            val centerAlign = byteArrayOf(0x1B, 0x61, 0x01)
            val leftAlign = byteArrayOf(0x1B, 0x61, 0x00)
            val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
            val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
            val feedAndCut = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

            outputStream.write(initPrinter)
            outputStream.write(centerAlign)
            outputStream.write(boldOn)
            outputStream.write("$companyName\n".toByteArray(Charsets.UTF_8))
            outputStream.write("e-Manager Mobile POS\n".toByteArray(Charsets.UTF_8))
            outputStream.write(boldOff)
            outputStream.write("================================\n".toByteArray(Charsets.UTF_8))
            outputStream.write(leftAlign)
            outputStream.write("Tarix: $dateStr\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Kassir: $cashierName\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Terminal: Android POS-01\n".toByteArray(Charsets.UTF_8))
            outputStream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))
            outputStream.write("TEST ÇAP SƏNƏDİ\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Printer əlaqəsi aktivdir!\n".toByteArray(Charsets.UTF_8))
            outputStream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))
            outputStream.write(centerAlign)
            outputStream.write("www.e-manager.az\n\n\n".toByteArray(Charsets.UTF_8))
            outputStream.write(feedAndCut)
            outputStream.flush()

            Pair(true, "${device.name ?: "Printer"} ilə test çapı icra olundu!")
        } catch (e: Exception) {
            Pair(false, "Printerə qoşulma xətası: ${e.localizedMessage}")
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
