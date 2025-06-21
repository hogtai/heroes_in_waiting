package com.lifechurch.heroesinwaiting

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HeroesInWaitingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any app-wide components here
        // For now, Hilt handles most of our initialization
    }
}