package com.cointask.services

import android.content.Context
import androidx.work.*
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.ActivityLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FraudDetectionService(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    
    fun startMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<FraudDetectionWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "fraud_detection",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    class FraudDetectionWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {
        
        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val suspiciousActivities = analyzeUserBehavior(database)
                if (suspiciousActivities.isNotEmpty()) {
                    flagSuspiciousAccounts(database, suspiciousActivities)
                }
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
        
        private suspend fun analyzeUserBehavior(database: AppDatabase): List<ActivityLog> {
            val oneHourAgo = System.currentTimeMillis() - 3600000
            val recentLogs = database.activityLogDao().getLogsLastHour(oneHourAgo)
            val suspiciousLogs = mutableListOf<ActivityLog>()
            
            recentLogs.collect { logs ->
                logs.groupBy { it.userId }.forEach { (userId, userLogs) ->
                    var fraudScore = 0f
                    
                    // Check for rapid task completion
                    val tasksInMinute = userLogs.filter { 
                        it.action == "complete_task" && 
                        System.currentTimeMillis() - it.timestamp < 60000 
                    }.size
                    if (tasksInMinute > 10) fraudScore += 0.3f
                    
                    // Check for suspicious patterns
                    if (fraudScore >= 0.5f) {
                        suspiciousLogs.addAll(userLogs.map { 
                            it.copy(fraudScore = fraudScore, isSuspicious = true) 
                        })
                    }
                }
            }
            
            return suspiciousLogs
        }
        
        private suspend fun flagSuspiciousAccounts(
            database: AppDatabase,
            suspiciousLogs: List<ActivityLog>
        ) {
            suspiciousLogs.forEach { log ->
                database.activityLogDao().markAsSuspicious(log.id, log.fraudScore)
                
                if (log.fraudScore >= 0.8f) {
                    database.userDao().autoSuspendUser(log.userId)
                }
            }
        }
    }
}
