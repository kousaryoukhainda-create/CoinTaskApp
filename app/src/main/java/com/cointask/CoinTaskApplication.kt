package com.cointask

import android.app.Application
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.Campaign
import com.cointask.data.models.Task
import com.cointask.data.models.Transaction
import com.cointask.data.models.User
import com.cointask.data.models.UserRole
import com.cointask.services.FraudDetectionService
import com.cointask.utils.PasswordUtils
import com.cointask.utils.SampleDataLoader
import com.cointask.utils.toActivityLog
import com.cointask.utils.toCampaign
import com.cointask.utils.toTask
import com.cointask.utils.toTransaction
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class CoinTaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start fraud detection
        FraudDetectionService(this).startMonitoring()

        // Initialize sample data (tasks, campaigns only - no users)
        CoroutineScope(Dispatchers.IO).launch {
            initializeSampleData()
        }
    }

    private suspend fun initializeSampleData() {
        val database = AppDatabase.getDatabase(this@CoinTaskApplication)

        // Check if we need to insert sample data
        val existingTasks = database.taskDao().getAllTasksList()

        if (existingTasks.isEmpty()) {
            val currentTime = System.currentTimeMillis()

            // Load sample data from JSON assets (tasks, campaigns, transactions, logs only)
            val sampleTasks = SampleDataLoader.loadTasks(this@CoinTaskApplication)
            val sampleCampaigns = SampleDataLoader.loadCampaigns(this@CoinTaskApplication)
            val sampleTransactions = SampleDataLoader.loadTransactions(this@CoinTaskApplication)
            val sampleActivityLogs = SampleDataLoader.loadActivityLogs(this@CoinTaskApplication)

            // Insert sample tasks (assign to advertiserId = 0 as placeholder)
            sampleTasks.forEach { sampleTask ->
                val task = sampleTask.toTask(currentTime).copy(advertiserId = 0)
                database.taskDao().insertTask(task)
            }

            // Insert sample campaigns
            sampleCampaigns.forEach { sampleCampaign ->
                val campaign = sampleCampaign.toCampaign(currentTime).copy(advertiserId = 0)
                database.campaignDao().insertCampaign(campaign)
            }

            // Insert sample transactions
            sampleTransactions.forEach { sampleTransaction ->
                val transaction = sampleTransaction.toTransaction()
                database.transactionDao().insertTransaction(transaction)
            }

            // Insert sample activity logs
            sampleActivityLogs.forEach { sampleActivityLog ->
                val activityLog = sampleActivityLog.toActivityLog()
                database.activityLogDao().insertLog(activityLog)
            }
        }
    }
}
