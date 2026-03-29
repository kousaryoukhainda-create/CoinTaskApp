package com.cointask.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import android.content.Context
import com.cointask.data.database.dao.*
import com.cointask.data.models.*

@Database(
    entities = [User::class, Task::class, Transaction::class,
                Campaign::class, ActivityLog::class, WithdrawalRequest::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun campaignDao(): CampaignDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun withdrawalRequestDao(): WithdrawalRequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coin_task_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 3 to 4: Add new columns to User and Task tables
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new columns to users table
                database.execSQL("ALTER TABLE users ADD COLUMN cnic TEXT")
                database.execSQL("ALTER TABLE users ADD COLUMN accountTitle TEXT")
                database.execSQL("ALTER TABLE users ADD COLUMN accountNumber TEXT")
                database.execSQL("ALTER TABLE users ADD COLUMN transactionId TEXT")
                database.execSQL("ALTER TABLE users ADD COLUMN paymentVerified INTEGER NOT NULL DEFAULT 0")

                // Add new columns to tasks table
                database.execSQL("ALTER TABLE tasks ADD COLUMN completionTimeSeconds INTEGER NOT NULL DEFAULT 5")
                database.execSQL("ALTER TABLE tasks ADD COLUMN targetViews INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN targetLikes INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN targetShares INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN targetClicks INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN currentViews INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN currentLikes INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN currentShares INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN currentClicks INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from version 4 to 5: Remove admin_secret_keys table
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS admin_secret_keys")
            }
        }
    }
}
