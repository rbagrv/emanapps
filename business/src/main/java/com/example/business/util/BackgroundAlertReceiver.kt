package com.example.business.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BackgroundAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Periodic background alert spam has been completely disabled per user requirement.
        // Notifications are now ONLY triggered in real-time when new chat messages or
        // new ERP operations (sales, purchases, payments) are created.
        cancelPeriodicAlerts(context)
        Log.d("BackgroundAlertReceiver", "Periodic alerts disabled. Cancelled residual alarms.")
    }

    companion object {
        const val ACTION_CHECK_ALERTS = "com.emanager.business.CHECK_ALERTS"

        fun cancelPeriodicAlerts(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, BackgroundAlertReceiver::class.java).apply {
                    action = ACTION_CHECK_ALERTS
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    100,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d("BackgroundAlertReceiver", "Successfully cancelled periodic background alarm.")
                }
            } catch (e: Exception) {
                Log.w("BackgroundAlertReceiver", "Error cancelling alarm: ${e.message}")
            }
        }
    }
}
