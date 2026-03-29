package com.cointask.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val password: String,
    val fullName: String,
    val role: UserRole,
    val coins: Int = 0,
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val suspensionCount: Int = 0,
    val isSuspended: Boolean = false,
    val bankAccount: String? = null,
    val bankName: String? = null,
    val accountName: String? = null,
    val ipAddress: String? = null,
    val deviceId: String? = null
) : Parcelable
