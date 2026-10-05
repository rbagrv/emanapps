package com.example.business

import android.app.Application

class BusinessApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        var instance: BusinessApp? = null
            private set
    }
}
