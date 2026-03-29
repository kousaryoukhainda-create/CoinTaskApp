package com.cointask.data.database.dao

import androidx.room.*
import com.cointask.data.models.WithdrawalRequest
import com.cointask.data.models.WithdrawalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WithdrawalRequestDao {
    @Query("SELECT * FROM withdrawal_requests WHERE userId = :userId ORDER BY requestDate DESC")
    fun getWithdrawalsByUser(userId: Int): Flow<List<WithdrawalRequest>>

    @Query("SELECT * FROM withdrawal_requests WHERE userId = :userId ORDER BY requestDate DESC")
    suspend fun getWithdrawalsByUserSuspend(userId: Int): List<WithdrawalRequest>

    @Query("SELECT * FROM withdrawal_requests WHERE status = 'PENDING' ORDER BY requestDate ASC")
    fun getPendingWithdrawals(): Flow<List<WithdrawalRequest>>

    @Query("SELECT * FROM withdrawal_requests WHERE status = 'PENDING' ORDER BY requestDate ASC")
    suspend fun getPendingWithdrawalsList(): List<WithdrawalRequest>

    @Query("SELECT * FROM withdrawal_requests WHERE status = :status ORDER BY requestDate DESC")
    fun getWithdrawalsByStatus(status: WithdrawalStatus): Flow<List<WithdrawalRequest>>

    @Query("SELECT * FROM withdrawal_requests WHERE id = :id")
    suspend fun getWithdrawalById(id: Int): WithdrawalRequest?

    @Query("SELECT * FROM withdrawal_requests")
    suspend fun getAllWithdrawals(): List<WithdrawalRequest>

    @Insert
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequest): Long

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalRequest)

    @Query("UPDATE withdrawal_requests SET status = :status, processedDate = :processedDate, processedBy = :adminId WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Int, status: WithdrawalStatus, processedDate: Long, adminId: Int)

    @Query("UPDATE withdrawal_requests SET status = :status, rejectionReason = :reason, processedDate = :processedDate, processedBy = :adminId WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Int, status: WithdrawalStatus, processedDate: Long, adminId: Int, rejectionReason: String)

    @Query("UPDATE withdrawal_requests SET transactionReference = :ref, status = :status, processedDate = :processedDate, processedBy = :adminId WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Int, status: WithdrawalStatus, processedDate: Long, adminId: Int, transactionReference: String)

    @Delete
    suspend fun deleteWithdrawal(withdrawal: WithdrawalRequest)
}
