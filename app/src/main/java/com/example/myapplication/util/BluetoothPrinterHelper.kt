package com.example.myapplication.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
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

    @SuppressLint("MissingPermission")
    suspend fun printTestReceipt(
        device: BluetoothDevice?,
        companyName: String,
        cashierName: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        if (device == null) {
            // Virtual / Simulation print test
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
