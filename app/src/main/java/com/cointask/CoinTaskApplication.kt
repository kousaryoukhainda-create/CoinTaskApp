package com.cointask

import android.app.Application
import com.cointask.data.SeedDataUtils
import com.cointask.services.FraudDetectionService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CoinTaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start fraud detection
        FraudDetectionService(this).startMonitoring()

        // Seed database with sample test data
        SeedDataUtils.seedDatabase(this)
    }
}
