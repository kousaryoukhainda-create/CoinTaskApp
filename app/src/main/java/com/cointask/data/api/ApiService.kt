package com.cointask.data.api

import com.cointask.data.models.Task
import com.cointask.data.models.Transaction
import com.cointask.data.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Authentication endpoints
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Query("userId") userId: Int): Response<BaseResponse>

    // User endpoints
    @GET("api/users/{userId}")
    suspend fun getUser(@Path("userId") userId: Int): Response<User>

    @GET("api/users/{userId}/tasks")
    suspend fun getUserTasks(@Path("userId") userId: Int): Response<List<Task>>

    @GET("api/users/{userId}/transactions")
    suspend fun getUserTransactions(@Path("userId") userId: Int): Response<List<Transaction>>

    @GET("api/users/{userId}/balance")
    suspend fun getUserBalance(@Path("userId") userId: Int): Response<BalanceResponse>

    // Task endpoints
    @GET("api/tasks")
    suspend fun getAvailableTasks(@Query("userId") userId: Int): Response<List<Task>>

    @POST("api/tasks/{taskId}/complete")
    suspend fun completeTask(
        @Path("taskId") taskId: Int,
        @Body request: CompleteTaskRequest
    ): Response<TaskCompletionResponse>

    // Campaign endpoints (for advertisers)
    @GET("api/advertisers/{advertiserId}/campaigns")
    suspend fun getAdvertiserCampaigns(@Path("advertiserId") advertiserId: Int): Response<List<CampaignResponse>>

    @POST("api/campaigns")
    suspend fun createCampaign(@Body request: CreateCampaignRequest): Response<CampaignResponse>

    // Admin endpoints
    @GET("api/admin/users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("api/admin/statistics")
    suspend fun getPlatformStatistics(): Response<PlatformStatisticsResponse>
}

// Request/Response DTOs

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: User?,
    val token: String?
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)

data class BalanceResponse(
    val userId: Int,
    val balance: Int,
    val pendingBalance: Int
)

data class CompleteTaskRequest(
    val userId: Int,
    val proofUrl: String?
)

data class TaskCompletionResponse(
    val success: Boolean,
    val message: String,
    val coinsEarned: Int
)

data class CampaignResponse(
    val id: Int,
    val title: String,
    val budget: Int,
    val spent: Int,
    val impressions: Int,
    val status: String
)

data class CreateCampaignRequest(
    val advertiserId: Int,
    val title: String,
    val description: String,
    val budget: Int,
    val targetImpressions: Int,
    val startDate: Long,
    val endDate: Long
)

data class PlatformStatisticsResponse(
    val totalUsers: Int,
    val totalAdvertisers: Int,
    val totalTasks: Int,
    val totalTransactions: Int,
    val revenue: Double
)
