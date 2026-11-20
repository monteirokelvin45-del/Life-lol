package com.lifeos.app

import android.app.Application
import com.lifeos.core.system.notification.LifeOsNotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LifeOsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LifeOsNotificationChannels.ensureCreated(this)
    }
}
