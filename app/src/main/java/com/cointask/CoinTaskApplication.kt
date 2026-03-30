package com.cointask

import android.app.Application
import com.cointask.services.FraudDetectionService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CoinTaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start fraud detection
        FraudDetectionService(this).startMonitoring()
        
        // No sample/demo data - all data must be created by real users and advertisers
    }
}
