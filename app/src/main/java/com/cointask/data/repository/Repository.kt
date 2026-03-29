package com.cointask.data.repository

import com.cointask.data.api.ApiClient
import com.cointask.data.api.AuthResponse
import com.cointask.data.api.BaseResponse
import com.cointask.data.api.BalanceResponse
import com.cointask.data.api.CampaignResponse
import com.cointask.data.api.CompleteTaskRequest
import com.cointask.data.api.CreateCampaignRequest
import com.cointask.data.api.LoginRequest
import com.cointask.data.api.PlatformStatisticsResponse
import com.cointask.data.api.RegisterRequest
import com.cointask.data.api.TaskCompletionResponse
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.Task
import com.cointask.data.models.Transaction
import com.cointask.data.models.User

class AuthRepository(
    private val database: AppDatabase
) {
    private val apiService = ApiClient.apiService

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            // Fallback to local authentication if API fails
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, fullName: String): Result<AuthResponse> {
        return try {
            val response = apiService.register(RegisterRequest(email, password, fullName))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(userId: Int): Result<BaseResponse> {
        return try {
            val response = apiService.logout(userId)
            if (response.isSuccessful) {
                Result.success(BaseResponse(true, "Logged out successfully"))
            } else {
                Result.failure(Exception("Logout failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UserRepository(
    private val database: AppDatabase
) {
    private val apiService = ApiClient.apiService

    suspend fun getUser(userId: Int): Result<User> {
        return try {
            val response = apiService.getUser(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Fallback to local database
                val localUser = database.userDao().getUserByIdSuspend(userId)
                if (localUser != null) {
                    Result.success(localUser)
                } else {
                    Result.failure(Exception("User not found"))
                }
            }
        } catch (e: Exception) {
            // Fallback to local database
            val localUser = database.userDao().getUserByIdSuspend(userId)
            if (localUser != null) {
                Result.success(localUser)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getUserBalance(userId: Int): Result<BalanceResponse> {
        return try {
            val response = apiService.getUserBalance(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch balance"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class TaskRepository(
    private val database: AppDatabase
) {
    private val apiService = ApiClient.apiService

    suspend fun getAvailableTasks(userId: Int): Result<List<Task>> {
        return try {
            val response = apiService.getAvailableTasks(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                // Fallback to local database
                Result.success(database.taskDao().getAvailableTasks())
            }
        } catch (e: Exception) {
            // Fallback to local database
            Result.success(database.taskDao().getAvailableTasks())
        }
    }

    suspend fun completeTask(taskId: Int, userId: Int, proofUrl: String?): Result<TaskCompletionResponse> {
        return try {
            val response = apiService.completeTask(taskId, CompleteTaskRequest(userId, proofUrl))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to complete task"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserTasks(userId: Int): Result<List<Task>> {
        return try {
            val response = apiService.getUserTasks(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.success(database.taskDao().getTasksByUser(userId))
            }
        } catch (e: Exception) {
            Result.success(database.taskDao().getTasksByUser(userId))
        }
    }
}

class TransactionRepository(
    private val database: AppDatabase
) {
    private val apiService = ApiClient.apiService

    suspend fun getUserTransactions(userId: Int): Result<List<Transaction>> {
        return try {
            val response = apiService.getUserTransactions(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.success(database.transactionDao().getTransactionsByUserSuspend(userId))
            }
        } catch (e: Exception) {
            Result.success(database.transactionDao().getTransactionsByUserSuspend(userId))
        }
    }
}

class CampaignRepository(
    private val database: AppDatabase
) {
    private val apiService = ApiClient.apiService

    suspend fun getAdvertiserCampaigns(advertiserId: Int): Result<List<CampaignResponse>> {
        return try {
            val response = apiService.getAdvertiserCampaigns(advertiserId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch campaigns"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCampaign(request: CreateCampaignRequest): Result<CampaignResponse> {
        return try {
            val response = apiService.createCampaign(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create campaign"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AdminRepository(
    private val database: AppDatabase
) {
    private val apiService = ApiClient.apiService

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val response = apiService.getAllUsers()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.success(database.userDao().getAllUsersList())
            }
        } catch (e: Exception) {
            Result.success(database.userDao().getAllUsersList())
        }
    }

    suspend fun getPlatformStatistics(): Result<PlatformStatisticsResponse> {
        return try {
            val response = apiService.getPlatformStatistics()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch statistics"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
