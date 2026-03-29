package com.cointask.data.database

import androidx.room.TypeConverter
import com.cointask.data.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromTaskType(type: TaskType): String = type.name
    
    @TypeConverter
    fun toTaskType(name: String): TaskType = TaskType.valueOf(name)
    
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name
    
    @TypeConverter
    fun toTaskStatus(name: String): TaskStatus = TaskStatus.valueOf(name)
    
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name
    
    @TypeConverter
    fun toUserRole(name: String): UserRole = UserRole.valueOf(name)
    
    @TypeConverter
    fun fromCampaignStatus(status: CampaignStatus): String = status.name
    
    @TypeConverter
    fun toCampaignStatus(name: String): CampaignStatus = CampaignStatus.valueOf(name)
    
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name
    
    @TypeConverter
    fun toTransactionType(name: String): TransactionType = TransactionType.valueOf(name)
    
    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus): String = status.name
    
    @TypeConverter
    fun toTransactionStatus(name: String): TransactionStatus = TransactionStatus.valueOf(name)
}
