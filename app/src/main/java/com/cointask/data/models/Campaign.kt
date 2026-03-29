package com.cointask.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class Campaign(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val advertiserId: Int,
    val name: String,
    val description: String,
    val budget: Int,
    val spentAmount: Int = 0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val status: CampaignStatus = CampaignStatus.PENDING,
    val startDate: Long,
    val endDate: Long,
    val targetAudience: String? = null,
    val dailyBudget: Int? = null,
    val costPerTask: Int
)

enum class CampaignStatus {
    PENDING, ACTIVE, PAUSED, ENDED, CANCELLED
}
