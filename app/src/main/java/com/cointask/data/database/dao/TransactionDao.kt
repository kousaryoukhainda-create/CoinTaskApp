package com.cointask.data.database.dao

import androidx.room.*
import com.cointask.data.models.Transaction
import com.cointask.data.models.TransactionStatus
import com.cointask.data.models.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsByUser(userId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getTransactionsByUserSuspend(userId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND type = 'WITHDRAWAL'")
    suspend fun getPendingWithdrawals(): List<Transaction>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'EARNED_FROM_TASK' AND status = 'COMPLETED'")
    suspend fun getTotalEarnings(userId: Int): Int?

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'WITHDRAWAL' AND status = 'COMPLETED'")
    suspend fun getTotalWithdrawals(userId: Int): Int?

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId AND timestamp > :timeThreshold")
    suspend fun getRecentTasksCount(userId: Int, timeThreshold: Long): Int
}
