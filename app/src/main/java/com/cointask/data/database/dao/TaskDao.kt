package com.cointask.data.database.dao

import androidx.room.*
import com.cointask.data.models.Task
import com.cointask.data.models.TaskStatus
import com.cointask.data.models.TaskType
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE'")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<Task>

    @Query("SELECT * FROM tasks WHERE advertiserId = :advertiserId")
    fun getTasksByAdvertiser(advertiserId: Int): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks
        WHERE (:type IS NULL OR taskType = :type)
        AND rewardCoins >= :minReward
        AND status = :status
        AND completedCount < totalCapacity
    """)
    fun getFilteredTasks(type: TaskType?, minReward: Int, status: TaskStatus): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE'")
    suspend fun getAvailableTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE advertiserId = :advertiserId")
    suspend fun getTasksByUser(advertiserId: Int): List<Task>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Query("UPDATE tasks SET completedCount = completedCount + 1 WHERE id = :taskId")
    suspend fun incrementCompletion(taskId: Int)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?
}
