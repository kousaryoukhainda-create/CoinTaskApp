package com.cointask.data.database.dao

import androidx.room.*
import com.cointask.data.models.User
import com.cointask.data.models.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<User>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdSuspend(userId: Int): User?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?
    
    @Insert
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET coins = coins + :amount WHERE id = :userId")
    suspend fun addCoins(userId: Int, amount: Int)
    
    @Query("UPDATE users SET coins = coins - :amount WHERE id = :userId")
    suspend fun deductCoins(userId: Int, amount: Int)
    
    @Query("UPDATE users SET lastLogin = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE users SET isVerified = true WHERE id = :userId")
    suspend fun verifyUser(userId: Int)
    
    @Query("UPDATE users SET isActive = false WHERE id = :userId")
    suspend fun blockUser(userId: Int)
    
    @Query("UPDATE users SET isSuspended = true WHERE id = :userId")
    suspend fun autoSuspendUser(userId: Int)
    
    @Query("UPDATE users SET suspensionCount = suspensionCount + 1 WHERE id = :userId")
    suspend fun incrementSuspensionCount(userId: Int)
}
