package com.cointask.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val type: TransactionType,
    val amount: Int,
    val taskId: Int? = null,
    val campaignId: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val referenceId: String? = null
)

enum class TransactionType {
    EARNED_FROM_TASK, WITHDRAWAL, CAMPAIGN_PAYMENT, REFUND, BONUS
}

enum class TransactionStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}
