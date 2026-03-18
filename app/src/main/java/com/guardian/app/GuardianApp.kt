package com.guardian.app

import android.app.Application

class GuardianApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: GuardianApp
            private set
    }
}
