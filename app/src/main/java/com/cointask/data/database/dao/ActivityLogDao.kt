package com.cointask.data.database.dao

import androidx.room.*
import com.cointask.data.models.ActivityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insertLog(log: ActivityLog): Long
    
    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getLogsByUser(userId: Int): Flow<List<ActivityLog>>
    
    @Query("SELECT * FROM activity_logs WHERE isSuspicious = true ORDER BY timestamp DESC")
    fun getSuspiciousLogs(): Flow<List<ActivityLog>>
    
    @Query("SELECT * FROM activity_logs WHERE timestamp > :timeThreshold")
    fun getLogsLastHour(timeThreshold: Long): Flow<List<ActivityLog>>
    
    @Update
    suspend fun updateLog(log: ActivityLog)
    
    @Query("UPDATE activity_logs SET isSuspicious = true, fraudScore = :score WHERE id = :logId")
    suspend fun markAsSuspicious(logId: Int, score: Float = 0.5f)

    @Query("SELECT * FROM activity_logs")
    suspend fun getAllLogsList(): List<ActivityLog>

    @Delete
    suspend fun deleteLog(log: ActivityLog)
}
