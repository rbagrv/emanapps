package com.example.business

import android.app.Application

class BusinessApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.business.util.NotificationHelper.createNotificationChannel(this)
        com.example.business.util.BackgroundAlertReceiver.schedulePeriodicAlerts(this)
    }

    companion object {
        var instance: BusinessApp? = null
            private set
    }
}
