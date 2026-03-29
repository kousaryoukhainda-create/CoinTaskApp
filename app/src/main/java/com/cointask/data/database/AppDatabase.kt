package com.cointask.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.cointask.data.database.dao.*
import com.cointask.data.models.*

@Database(
    entities = [User::class, Task::class, Transaction::class, 
                Campaign::class, ActivityLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun campaignDao(): CampaignDao
    abstract fun activityLogDao(): ActivityLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coin_task_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
