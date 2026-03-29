package com.cointask.utils

import android.content.Context
import com.cointask.data.models.ActivityLog
import com.cointask.data.models.Campaign
import com.cointask.data.models.CampaignStatus
import com.cointask.data.models.Task
import com.cointask.data.models.TaskStatus
import com.cointask.data.models.TaskType
import com.cointask.data.models.Transaction
import com.cointask.data.models.TransactionType
import com.cointask.data.models.User
import com.cointask.data.models.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

data class SampleUser(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String,
    val coins: Int,
    val isVerified: Boolean
)

data class SampleTask(
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val advertiserId: Int,
    val taskType: String,
    val totalCapacity: Int,
    val status: String,
    val expiresAtOffset: Long
)

data class SampleCampaign(
    val title: String,
    val description: String,
    val budget: Int,
    val spent: Int,
    val targetImpressions: Int,
    val currentImpressions: Int,
    val advertiserId: Int,
    val startDateOffset: Long,
    val endDateOffset: Long,
    val isActive: Boolean
)

data class SampleTransaction(
    val userId: Int,
    val amount: Int,
    val type: String,
    val description: String
)

data class SampleActivityLog(
    val userId: Int,
    val action: String,
    val description: String
)

object SampleDataLoader {

    private val gson = Gson()

    fun loadUsers(context: Context): List<SampleUser> {
        return loadFromAssets(context, "sample_users.json", object : TypeToken<List<SampleUser>>() {}.type)
    }

    fun loadTasks(context: Context): List<SampleTask> {
        return loadFromAssets(context, "sample_tasks.json", object : TypeToken<List<SampleTask>>() {}.type)
    }

    fun loadCampaigns(context: Context): List<SampleCampaign> {
        return loadFromAssets(context, "sample_campaigns.json", object : TypeToken<List<SampleCampaign>>() {}.type)
    }

    fun loadTransactions(context: Context): List<SampleTransaction> {
        return loadFromAssets(context, "sample_transactions.json", object : TypeToken<List<SampleTransaction>>() {}.type)
    }

    fun loadActivityLogs(context: Context): List<SampleActivityLog> {
        return loadFromAssets(context, "sample_activity_logs.json", object : TypeToken<List<SampleActivityLog>>() {}.type)
    }

    private fun <T> loadFromAssets(context: Context, fileName: String, type: java.lang.reflect.Type): List<T> {
        return try {
            context.assets.open(fileName).use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                gson.fromJson(json, type)
            }
        } catch (e: IOException) {
            emptyList()
        }
    }
}

// Extension functions to convert sample data to domain models

fun SampleUser.toUser(passwordHash: String): User {
    return User(
        email = this.email,
        password = passwordHash,
        fullName = this.fullName,
        role = UserRole.valueOf(this.role),
        coins = this.coins,
        isVerified = this.isVerified
    )
}

fun SampleTask.toTask(currentTime: Long): Task {
    return Task(
        title = this.title,
        description = this.description,
        rewardCoins = this.rewardCoins,
        advertiserId = this.advertiserId,
        taskType = TaskType.valueOf(this.taskType),
        totalCapacity = this.totalCapacity,
        status = TaskStatus.valueOf(this.status),
        expiresAt = currentTime + this.expiresAtOffset
    )
}

fun SampleCampaign.toCampaign(currentTime: Long): Campaign {
    return Campaign(
        advertiserId = this.advertiserId,
        name = this.title,
        description = this.description,
        budget = this.budget,
        spentAmount = this.spent,
        totalTasks = this.targetImpressions / 100,
        completedTasks = this.currentImpressions / 100,
        status = if (this.isActive) CampaignStatus.ACTIVE else CampaignStatus.PENDING,
        startDate = currentTime + this.startDateOffset,
        endDate = currentTime + this.endDateOffset,
        costPerTask = if (this.targetImpressions > 0) this.budget / (this.targetImpressions / 100) else 0
    )
}

fun SampleTransaction.toTransaction(): Transaction {
    return Transaction(
        userId = this.userId,
        amount = this.amount,
        type = when (this.type) {
            "DEPOSIT" -> TransactionType.BONUS
            "WITHDRAWAL" -> TransactionType.WITHDRAWAL
            "REWARD" -> TransactionType.EARNED_FROM_TASK
            "CAMPAIGN_SPEND" -> TransactionType.CAMPAIGN_PAYMENT
            else -> TransactionType.BONUS
        },
        description = this.description
    )
}

fun SampleActivityLog.toActivityLog(): ActivityLog {
    return ActivityLog(
        userId = this.userId,
        action = this.action,
        details = this.description
    )
}
