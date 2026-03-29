package com.cointask

import android.app.Application
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.ActivityLog
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
import com.cointask.utils.toUser
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

        // Initialize sample data from JSON assets
        CoroutineScope(Dispatchers.IO).launch {
            initializeSampleData()
        }
    }

    private suspend fun initializeSampleData() {
        val database = AppDatabase.getDatabase(this@CoinTaskApplication)

        // Check if we need to insert sample data
        val existingUser = database.userDao().getUserByEmail("user@example.com")

        if (existingUser == null) {
            val currentTime = System.currentTimeMillis()

            // Load sample data from JSON assets
            val sampleUsers = SampleDataLoader.loadUsers(this@CoinTaskApplication)
            val sampleTasks = SampleDataLoader.loadTasks(this@CoinTaskApplication)
            val sampleCampaigns = SampleDataLoader.loadCampaigns(this@CoinTaskApplication)
            val sampleTransactions = SampleDataLoader.loadTransactions(this@CoinTaskApplication)
            val sampleActivityLogs = SampleDataLoader.loadActivityLogs(this@CoinTaskApplication)

            // Insert users with hashed passwords
            val insertedUserIds = mutableListOf<Int>()
            sampleUsers.forEach { sampleUser ->
                val hashedPassword = PasswordUtils.hashPassword(sampleUser.password)
                val user = sampleUser.toUser(hashedPassword)
                val userId = database.userDao().insertUser(user).toInt()
                insertedUserIds.add(userId)
            }

            // Insert tasks (assign to first user)
            val firstUserId = insertedUserIds.firstOrNull() ?: 1
            sampleTasks.forEach { sampleTask ->
                val task = sampleTask.toTask(currentTime, firstUserId)
                database.taskDao().insertTask(task)
            }

            // Insert campaigns (assign to second user - advertiser)
            val advertiserId = insertedUserIds.getOrNull(1) ?: 2
            sampleCampaigns.forEach { sampleCampaign ->
                val campaign = sampleCampaign.toCampaign(currentTime).copy(advertiserId = advertiserId)
                database.campaignDao().insertCampaign(campaign)
            }

            // Insert transactions
            sampleTransactions.forEach { sampleTransaction ->
                val transaction = sampleTransaction.toTransaction()
                database.transactionDao().insertTransaction(transaction)
            }

            // Insert activity logs
            sampleActivityLogs.forEach { sampleActivityLog ->
                val activityLog = sampleActivityLog.toActivityLog()
                database.activityLogDao().insertLog(activityLog)
            }
        }
    }
}
