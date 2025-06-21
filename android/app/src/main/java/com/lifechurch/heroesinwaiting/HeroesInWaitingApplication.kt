package com.lifechurch.heroesinwaiting

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Heroes in Waiting Application Class
 * 
 * Main application class that initializes Hilt dependency injection
 * and sets up global application configuration for the Heroes in Waiting
 * anti-bullying educational platform.
 */
@HiltAndroidApp
class HeroesInWaitingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configuration here
        // Log application startup for debugging
        if (BuildConfig.DEBUG) {
            println("Heroes in Waiting Application Started - Debug Mode")
        }
    }
}