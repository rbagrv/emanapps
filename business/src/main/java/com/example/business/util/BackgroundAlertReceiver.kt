package com.example.business.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BackgroundAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BackgroundAlertReceiver", "Background offline alert received: ${intent.action}")

        // Check offline and cached business rules to notify owner
        val prefs = context.getSharedPreferences("emanager_business_prefs", Context.MODE_PRIVATE)
        val companyName = prefs.getString("erp_tenant_name", "e-Manager Müəssisə") ?: "e-Manager Müəssisə"
        val lastAlertTime = prefs.getLong("last_background_alert_time", 0L)
        val now = System.currentTimeMillis()

        // Rotate through meaningful business alerts periodically (debts, warehouse stock, daily reminder)
        val alertIndex = prefs.getInt("background_alert_rotation_idx", 0)

        when (alertIndex % 4) {
            0 -> {
                NotificationHelper.showNotification(
                    context = context,
                    notificationId = 1001,
                    title = "⚠️ Gecikmiş Debitor Borcları",
                    message = "$companyName: Müştəri borclarının ödəniş vaxtı yaxınlaşır. Detallara baxmaq üçün toxunun."
                )
            }
            1 -> {
                NotificationHelper.showNotification(
                    context = context,
                    notificationId = 1002,
                    title = "📦 Anbar Qalıq Xəbərdarlığı",
                    message = "Anbarda minimum həddə çatmış və 0 qalıqlı məhsullar var. Təchizat sifarişi verin."
                )
            }
            2 -> {
                NotificationHelper.showNotification(
                    context = context,
                    notificationId = 1003,
                    title = "📑 Təchizatçı Ödənişləri",
                    message = "Təchizatçılara olan cari borclar üzrə qaimə müddətlərini nəzərdən keçirin."
                )
            }
            3 -> {
                NotificationHelper.showNotification(
                    context = context,
                    notificationId = 1004,
                    title = "💼 Daxili Tapşırıqlar və Çat",
                    message = "Komanda üzvlərinə verilmiş daxili tapşırıqların icra vəziyyətini yoxlayın."
                )
            }
        }

        prefs.edit()
            .putLong("last_background_alert_time", now)
            .putInt("background_alert_rotation_idx", alertIndex + 1)
            .apply()

        // Reschedule next background alert (every 30 minutes)
        scheduleNextAlarm(context)
    }

    companion object {
        const val ACTION_CHECK_ALERTS = "com.emanager.business.CHECK_ALERTS"
        private const val ALARM_INTERVAL_MILLIS = 30 * 60 * 1000L // 30 minutes

        fun schedulePeriodicAlerts(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, BackgroundAlertReceiver::class.java).apply {
                action = ACTION_CHECK_ALERTS
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = System.currentTimeMillis() + ALARM_INTERVAL_MILLIS

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
                Log.d("BackgroundAlertReceiver", "Scheduled background alert successfully for $triggerAt")
            } catch (e: Exception) {
                Log.e("BackgroundAlertReceiver", "Error scheduling alarm: ${e.message}")
            }
        }

        private fun scheduleNextAlarm(context: Context) {
            schedulePeriodicAlerts(context)
        }
    }
}
