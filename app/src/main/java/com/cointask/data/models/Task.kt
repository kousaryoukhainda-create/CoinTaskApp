package com.cointask.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val advertiserId: Int,
    val title: String,
    val description: String,
    val taskType: TaskType,
    val rewardCoins: Int,
    val totalCapacity: Int,
    val completedCount: Int = 0,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val targetUrl: String? = null,
    val videoUrl: String? = null,
    val socialMediaLink: String? = null,
    val requiredActions: String = "", // JSON string
    val verificationData: String = "", // JSON string
    // Advertiser-defined task settings
    val completionTimeSeconds: Int = 5, // Time required to complete task
    val targetViews: Int = 0, // For view-based tasks
    val targetLikes: Int = 0, // For like-based tasks
    val targetShares: Int = 0, // For share-based tasks
    val targetClicks: Int = 0, // For click-based tasks
    val currentViews: Int = 0, // Tracking current progress
    val currentLikes: Int = 0,
    val currentShares: Int = 0,
    val currentClicks: Int = 0
)

enum class TaskType {
    WATCH_VIDEO, VISIT_SITE, LIKE_CONTENT, SHARE_POST, FOLLOW_ACCOUNT, COMMENT, SURVEY
}

enum class TaskStatus {
    PENDING, ACTIVE, COMPLETED, EXPIRED, PAUSED
}
