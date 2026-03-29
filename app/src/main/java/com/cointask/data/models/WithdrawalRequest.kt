package com.cointask.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "withdrawal_requests")
data class WithdrawalRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val amount: Int, // Amount in coins
    val dollarAmount: Float, // Converted dollar amount
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val bankName: String,
    val accountNumber: String,
    val accountName: String,
    val requestDate: Long = System.currentTimeMillis(),
    val processedDate: Long? = null,
    val processedBy: Int? = null, // Admin user ID who processed
    val rejectionReason: String? = null,
    val transactionReference: String? = null,
    val notes: String? = null
)

enum class WithdrawalStatus {
    PENDING,        // Waiting for admin approval
    PROCESSING,     // Being processed
    COMPLETED,      // Successfully transferred
    REJECTED,       // Rejected by admin
    CANCELLED       // Cancelled by user
}
