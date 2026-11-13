package com.example.business.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.business.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportHelper {

    fun generateAndSharePdf(
        context: Context,
        companyName: String,
        voen: String,
        reportType: ReportType,
        period: ReportPeriod,
        periodReport: PeriodReport,
        pnlReport: ProfitAndLossReport,
        balanceReport: BalanceSheetReport,
        cashFlowReport: CashFlowReport,
        taxReport: TaxLiabilityReport,
        sendToWhatsApp: Boolean = false
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 points (approx 8.27 x 11.69 inches)
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            var yPos = 40f

            // 1. Header Banner
            paint.color = Color.parseColor("#059669") // EmeraldPrimary
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 75f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 20f
            canvas.drawText("e-Manager ERP Maliyyə Hesabatı", 35f, 42f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Rəsmi Elektron Hesabat Sənədi • Sahibkar Portalı", 35f, 60f, paint)

            yPos = 100f

            // 2. Company Info Box
            paint.color = Color.parseColor("#F3F4F6")
            canvas.drawRoundRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 65f, 10f, 10f, paint)

            paint.color = Color.parseColor("#111827")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("Müəssisə: $companyName", 45f, yPos + 24f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 11f
            paint.color = Color.parseColor("#4B5563")
            val voenText = if (voen.isNotBlank()) "VÖEN: $voen" else "VÖEN: Qeyd edilməyib"
            canvas.drawText(voenText, 45f, yPos + 42f, paint)

            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Tarix: $dateStr  |  Dövr: ${period.title}", 45f, yPos + 56f, paint)

            yPos += 85f

            // 3. Report Specific Section
            paint.color = Color.parseColor("#059669")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 15f
            canvas.drawText("Hesabat Növü: ${reportType.title}", 35f, yPos, paint)

            paint.color = Color.parseColor("#E5E7EB")
            canvas.drawLine(35f, yPos + 8f, (pageWidth - 35).toFloat(), yPos + 8f, paint)

            yPos += 25f

            when (reportType) {
                ReportType.TURNOVER -> {
                    drawKeyValueRow(canvas, paint, "Cəmi Satış Dövriyyəsi:", "₼ ${formatMoney(periodReport.totalSales)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Cəmi Alış / Maya:", "₼ ${formatMoney(periodReport.totalPurchases)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Ümumi Mənfəət:", "₼ ${formatMoney(periodReport.grossProfit)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Orta Çek:", "₼ ${formatMoney(periodReport.averageCheck)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Çek Sayı:", "${periodReport.checkCount} ədəd", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Nağd Satışlar:", "₼ ${formatMoney(periodReport.cashSales)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Kartla Satışlar:", "₼ ${formatMoney(periodReport.cardSales)}", yPos)
                }

                ReportType.STOCK_MOVEMENT -> {
                    paint.color = Color.parseColor("#059669")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("MALLARIN DÖVRİYYƏ VƏ HƏRƏKƏT İCMALI", 40f, yPos, paint)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Cəmi Satış Hərəkəti (Dövriyyə):", "₼ ${formatMoney(periodReport.totalSales)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Cəmi Qəbul / Alış Hərəkəti:", "₼ ${formatMoney(periodReport.totalPurchases)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Faktiki Anbar Qalıq Dəyəri:", "₼ ${formatMoney(balanceReport.inventory)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Ümumi Əməliyyat / Çek Sayı:", "${periodReport.checkCount} əməliyyat", yPos)
                    yPos += 24f
                    drawHighlightRow(canvas, paint, "HƏRƏKƏT NƏTİCƏSİ (DÖVRİYYƏ):", "₼ ${formatMoney(periodReport.totalSales)} (Anbar: ₼ ${formatMoney(balanceReport.inventory)})", yPos, pageWidth)
                }

                ReportType.MZH -> {
                    drawKeyValueRow(canvas, paint, "1. Ümumi Satış Gəlirləri:", "₼ ${formatMoney(pnlReport.revenue)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "2. Satılan Malların Maya Dəyəri (COGS):", "- ₼ ${formatMoney(pnlReport.cogs)}", yPos)
                    yPos += 22f
                    drawHighlightRow(canvas, paint, "3. ÜMUMİ MƏNFƏƏT (Qross Mənfəət):", "₼ ${formatMoney(pnlReport.grossProfit)}", yPos, pageWidth)
                    yPos += 26f
                    drawKeyValueRow(canvas, paint, "   • Əməkhaqqı Xərcləri:", "₼ ${formatMoney(pnlReport.salaries)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "   • İcarə Xərci:", "₼ ${formatMoney(pnlReport.rent)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "   • Kommunal & Xidmət Xərcləri:", "₼ ${formatMoney(pnlReport.utilities)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "   • Logistika və Digər Xərclər:", "₼ ${formatMoney(pnlReport.logistics + pnlReport.otherExpenses)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "4. Cəmi Əməliyyat Xərcləri:", "- ₼ ${formatMoney(pnlReport.totalOperatingExpenses)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "5. Əməliyyat Mənfəəti (EBITDA):", "₼ ${formatMoney(pnlReport.operatingProfit)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "6. Hesablanmış Vergi Xərci:", "- ₼ ${formatMoney(pnlReport.taxExpense)}", yPos)
                    yPos += 26f
                    drawHighlightRow(canvas, paint, "7. XALİS MƏNFƏƏT:", "₼ ${formatMoney(pnlReport.netProfit)} (Rentabellik: ${String.format(Locale.US, "%.1f", pnlReport.netMarginPercent)}%)", yPos, pageWidth)
                }

                ReportType.BALANCE -> {
                    paint.color = Color.parseColor("#059669")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("I. AKTİVLƏR", 40f, yPos, paint)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Kassa və Bank Hesabları:", "₼ ${formatMoney(balanceReport.cashAndBank)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Debitor Borcları (Müştəri alacaqları):", "₼ ${formatMoney(balanceReport.receivables)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Anbar Mal Qalıqları Dəyəri:", "₼ ${formatMoney(balanceReport.inventory)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Əsas Vəsaitlər və Avadanlıqlar:", "₼ ${formatMoney(balanceReport.fixedAssets)}", yPos)
                    yPos += 22f
                    drawHighlightRow(canvas, paint, "CƏMİ AKTİVLƏR:", "₼ ${formatMoney(balanceReport.totalAssets)}", yPos, pageWidth)
                    yPos += 28f

                    paint.color = Color.parseColor("#DC2626")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("II. ÖHDƏLİKLƏR VƏ KAPİTAL", 40f, yPos, paint)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Təchizatçılara Kreditor Borcları:", "₼ ${formatMoney(balanceReport.payables)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Cari Vergi Borcları:", "₼ ${formatMoney(balanceReport.taxDebt)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Qısamüddətli Kreditlər:", "₼ ${formatMoney(balanceReport.shortTermLoans)}", yPos)
                    yPos += 20f
                    drawKeyValueRow(canvas, paint, "Bölüşdürülməmiş Xalis Mənfəət:", "₼ ${formatMoney(balanceReport.retainedEarnings)}", yPos)
                    yPos += 22f
                    drawHighlightRow(canvas, paint, "CƏMİ ÖHDƏLİKLƏR VƏ KAPİTAL:", "₼ ${formatMoney(balanceReport.totalLiabilitiesAndEquity)}", yPos, pageWidth)
                }

                ReportType.CASH_FLOW -> {
                    drawKeyValueRow(canvas, paint, "Dövrün Əvvəlinə Qalıq:", "₼ ${formatMoney(cashFlowReport.openingCash)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Müştərilərdən Satış Daxilolmaları (+):", "₼ ${formatMoney(cashFlowReport.inflowSales)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Təchizatçılara Mallar Üzrə Ödəniş (-):", "- ₼ ${formatMoney(cashFlowReport.outflowSuppliers)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Əməkhaqqı Ödənişləri (-):", "- ₼ ${formatMoney(cashFlowReport.outflowSalaries)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "İcarə və Əməliyyat Xərcləri (-):", "- ₼ ${formatMoney(cashFlowReport.outflowExpenses)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Büdcəyə Vergi Ödənişləri (-):", "- ₼ ${formatMoney(cashFlowReport.outflowTaxes)}", yPos)
                    yPos += 24f
                    drawHighlightRow(canvas, paint, "Xalis Əməliyyat Pul Axını:", "₼ ${formatMoney(cashFlowReport.netOperatingCashFlow)}", yPos, pageWidth)
                    yPos += 26f
                    drawHighlightRow(canvas, paint, "DÖVRÜN SONUNA NAĞD PUL QALIĞI:", "₼ ${formatMoney(cashFlowReport.closingCash)}", yPos, pageWidth)
                }

                ReportType.TAX -> {
                    drawKeyValueRow(canvas, paint, "ƏDV (18%) Hesablanmış:", "₼ ${formatMoney(taxReport.vatDue)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Sadələşdirilmiş Vergi (2%):", "₼ ${formatMoney(taxReport.simplifiedTax)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Mənfəət / Gəlir Vergisi:", "₼ ${formatMoney(taxReport.incomeTax)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "DSMF Ayırmaları:", "₼ ${formatMoney(taxReport.dsmf)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "İcbari Tibbi Sığorta (İTS):", "₼ ${formatMoney(taxReport.its)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Cəmi Hesablanmış Vergi Məbləği:", "₼ ${formatMoney(taxReport.totalCalculated)}", yPos)
                    yPos += 22f
                    drawKeyValueRow(canvas, paint, "Büdcəyə Ödənilmiş Məbləğ:", "₼ ${formatMoney(taxReport.totalPaid)}", yPos)
                    yPos += 24f
                    drawHighlightRow(canvas, paint, "QALIQ VERGİ BORCU:", "₼ ${formatMoney(taxReport.taxDebt)} (${taxReport.status})", yPos, pageWidth)
                    yPos += 24f
                    drawKeyValueRow(canvas, paint, "Növbəti Son Ödəniş Tarixi:", taxReport.deadline, yPos)
                }
            }

            // 4. Footer & Signature
            yPos = 730f
            paint.color = Color.parseColor("#E5E7EB")
            canvas.drawLine(35f, yPos, (pageWidth - 35).toFloat(), yPos, paint)

            yPos += 25f
            paint.color = Color.parseColor("#6B7280")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            canvas.drawText("Məsul Şəxs / Sahibkar: _____________________", 45f, yPos, paint)
            canvas.drawText("Möhür / İmza yeri: (M.Y.)", (pageWidth - 200).toFloat(), yPos, paint)

            yPos += 30f
            canvas.drawText("e-Manager ERP sistemindən avtomatik tərtib edilmişdir. Sənədin doğruluğu sistem bazasındadır.", 45f, yPos, paint)

            pdfDocument.finishPage(page)

            // Save PDF to cache directory
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            val fileName = "emanager_${reportType.shortName.lowercase()}_${System.currentTimeMillis()}.pdf"
            val file = File(reportsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            sharePdfFile(context, file, "${companyName} - ${reportType.title}", sendToWhatsApp)

        } catch (e: Exception) {
            Log.e("PdfReportHelper", "Error generating PDF: ${e.message}", e)
            Toast.makeText(context, "PDF yaradılarkən xəta baş verdi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdfFile(context: Context, file: File, title: String, sendToWhatsApp: Boolean) {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("PdfReportHelper", "FileProvider error: ${e.message}")
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\ne-Manager ERP sistemi tərəfindən hazırlanmış rəsmi hesabat sənədi.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (sendToWhatsApp) {
            shareIntent.setPackage("com.whatsapp")
            try {
                context.startActivity(shareIntent)
                return
            } catch (e: Exception) {
                // Fall back to chooser if WhatsApp is not directly matched
            }
        }

        val chooser = Intent.createChooser(shareIntent, "Hesabat PDF-i Paylaş / WhatsApp")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun drawKeyValueRow(canvas: Canvas, paint: Paint, key: String, value: String, y: Float) {
        paint.color = Color.parseColor("#374151")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        canvas.drawText(key, 40f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#111827")
        val textWidth = paint.measureText(value)
        canvas.drawText(value, 555f - textWidth, y, paint)
    }

    private fun drawHighlightRow(canvas: Canvas, paint: Paint, key: String, value: String, y: Float, pageWidth: Int) {
        paint.color = Color.parseColor("#ECFDF5") // Emerald light
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(35f, y - 14f, (pageWidth - 35).toFloat(), y + 8f, 6f, 6f, paint)

        paint.color = Color.parseColor("#065F46")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText(key, 42f, y, paint)

        val textWidth = paint.measureText(value)
        canvas.drawText(value, 550f - textWidth, y, paint)
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "%,.2f", amount)
    }
}
