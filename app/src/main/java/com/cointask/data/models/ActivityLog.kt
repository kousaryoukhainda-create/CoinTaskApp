package com.cointask.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val action: String,
    val details: String,
    val ipAddress: String? = null,
    val deviceInfo: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuspicious: Boolean = false,
    val fraudScore: Float = 0f
)
