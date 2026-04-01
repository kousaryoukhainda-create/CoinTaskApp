package com.cointask.data

import android.content.Context
import android.util.Log
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.*
import kotlinx.coroutines.runBlocking
import org.mindrot.jbcrypt.BCrypt

/**
 * Utility class to seed the database with sample data for testing
 * Call SeedDataUtils.seedDatabase(context) to populate sample data
 */
object SeedDataUtils {

    private const val TAG = "SeedDataUtils"

    /**
     * Seed the database with sample data for testing
     * This creates sample advertisers, users, and tasks
     */
    fun seedDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        
        runBlocking {
            // Check if data already exists
            val existingUsers = db.userDao().getAllUsersList()
            if (existingUsers.isNotEmpty()) {
                Log.d(TAG, "Database already has data, skipping seed")
                return@runBlocking
            }

            Log.d(TAG, "Seeding database with sample data...")

            // Create sample advertiser
            val advertiserEmail = "advertiser@cointask.com"
            val advertiserPassword = BCrypt.hashpw("password123", BCrypt.gensalt())
            
            val advertiser = User(
                email = advertiserEmail,
                password = advertiserPassword,
                fullName = "Test Advertiser",
                role = UserRole.ADVERTISER,
                coins = 100000, // Starting bonus
                isVerified = true,
                isActive = true,
                isSuspended = false,
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )
            
            val advertiserId = db.userDao().insertUser(advertiser).toInt()
            Log.d(TAG, "Created advertiser with ID: $advertiserId")

            // Create sample test user
            val userPassword = BCrypt.hashpw("password123", BCrypt.gensalt())
            val testUser = User(
                email = "user@cointask.com",
                password = userPassword,
                fullName = "Test User",
                role = UserRole.USER,
                coins = 500, // Starting bonus
                isVerified = true,
                isActive = true,
                isSuspended = false,
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )
            
            val userId = db.userDao().insertUser(testUser).toInt()
            Log.d(TAG, "Created test user with ID: $userId")

            // Create admin user
            val adminPassword = BCrypt.hashpw("admin123", BCrypt.gensalt())
            val admin = User(
                email = "admin@cointask.com",
                password = adminPassword,
                fullName = "System Admin",
                role = UserRole.ADMIN,
                coins = 0,
                isVerified = true,
                isActive = true,
                isSuspended = false,
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )
            
            db.userDao().insertUser(admin)
            Log.d(TAG, "Created admin user")

            // Create sample tasks for all task types
            val tasks = createSampleTasks(advertiserId)
            
            for (task in tasks) {
                val taskId = db.taskDao().insertTask(task)
                Log.d(TAG, "Created task: ${task.title} (ID: $taskId)")
            }

            // Create a sample campaign
            val campaign = Campaign(
                advertiserId = advertiserId,
                name = "Brand Awareness Campaign",
                description = "Promote our new product launch across multiple platforms",
                budget = 50000,
                spentAmount = 0,
                totalTasks = 10,
                completedTasks = 0,
                status = CampaignStatus.ACTIVE,
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days
                costPerTask = 50,
                targetAudience = "18-35 age group",
                dailyBudget = 7000
            )
            
            db.campaignDao().insertCampaign(campaign)
            Log.d(TAG, "Created campaign: ${campaign.name}")

            Log.d(TAG, "Database seeding completed successfully!")
            Log.d(TAG, "===========================================")
            Log.d(TAG, "Test Credentials:")
            Log.d(TAG, "  Advertiser: advertiser@cointask.com / password123")
            Log.d(TAG, "  User: user@cointask.com / password123")
            Log.d(TAG, "  Admin: admin@cointask.com / admin123")
            Log.d(TAG, "===========================================")
        }
    }

    private fun createSampleTasks(advertiserId: Int): List<Task> {
        val now = System.currentTimeMillis()
        val expiresAt = now + (30 * 24 * 60 * 60 * 1000) // 30 days

        return listOf(
            // WATCH_VIDEO tasks with different video types
            Task(
                advertiserId = advertiserId,
                title = "Watch YouTube Tutorial",
                description = "Watch our complete tutorial on how to use our platform effectively",
                taskType = TaskType.WATCH_VIDEO,
                rewardCoins = 50,
                totalCapacity = 1000,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                completionTimeSeconds = 15,
                targetViews = 1000
            ),
            Task(
                advertiserId = advertiserId,
                title = "Watch Product Demo Video",
                description = "Learn about our amazing features in this quick demo",
                taskType = TaskType.WATCH_VIDEO,
                rewardCoins = 30,
                totalCapacity = 500,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                videoUrl = "https://www.youtube.com/watch?v=jNQXAC9IVRw",
                completionTimeSeconds = 10,
                targetViews = 500
            ),
            Task(
                advertiserId = advertiserId,
                title = "Watch Vimeo Showcase",
                description = "View our creative showcase on Vimeo",
                taskType = TaskType.WATCH_VIDEO,
                rewardCoins = 40,
                totalCapacity = 300,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                videoUrl = "https://vimeo.com/148751763",
                completionTimeSeconds = 12,
                targetViews = 300
            ),
            Task(
                advertiserId = advertiserId,
                title = "Watch Short Clip",
                description = "Quick 5-second clip for instant rewards",
                taskType = TaskType.WATCH_VIDEO,
                rewardCoins = 10,
                totalCapacity = 2000,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                videoUrl = "https://www.youtube.com/shorts/jfKfPfyJRdk",
                completionTimeSeconds = 5,
                targetViews = 2000
            ),
            
            // VISIT_SITE tasks
            Task(
                advertiserId = advertiserId,
                title = "Visit Company Website",
                description = "Explore our official website and learn about our services",
                taskType = TaskType.VISIT_SITE,
                rewardCoins = 25,
                totalCapacity = 800,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                targetUrl = "https://www.example.com",
                completionTimeSeconds = 10,
                targetClicks = 800
            ),
            Task(
                advertiserId = advertiserId,
                title = "Browse Product Catalog",
                description = "Check out our latest product offerings",
                taskType = TaskType.VISIT_SITE,
                rewardCoins = 35,
                totalCapacity = 600,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                targetUrl = "https://www.wikipedia.org",
                completionTimeSeconds = 15,
                targetClicks = 600
            ),

            // SURVEY tasks
            Task(
                advertiserId = advertiserId,
                title = "Complete Customer Survey",
                description = "Help us improve by completing this quick survey about your experience",
                taskType = TaskType.SURVEY,
                rewardCoins = 100,
                totalCapacity = 400,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                targetUrl = "https://www.google.com",
                completionTimeSeconds = 30,
                targetClicks = 400
            ),
            Task(
                advertiserId = advertiserId,
                title = "Product Feedback Form",
                description = "Share your thoughts on our new product line",
                taskType = TaskType.SURVEY,
                rewardCoins = 75,
                totalCapacity = 300,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                targetUrl = "https://www.bing.com",
                completionTimeSeconds = 20,
                targetClicks = 300
            ),

            // LIKE_CONTENT tasks
            Task(
                advertiserId = advertiserId,
                title = "Like Our Facebook Post",
                description = "Show your support by liking our latest Facebook post",
                taskType = TaskType.LIKE_CONTENT,
                rewardCoins = 20,
                totalCapacity = 1500,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                socialMediaLink = "https://www.facebook.com",
                completionTimeSeconds = 5,
                targetLikes = 1500
            ),
            Task(
                advertiserId = advertiserId,
                title = "Like Instagram Photo",
                description = "Double tap to like our featured Instagram photo",
                taskType = TaskType.LIKE_CONTENT,
                rewardCoins = 15,
                totalCapacity = 2000,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                socialMediaLink = "https://www.instagram.com",
                completionTimeSeconds = 3,
                targetLikes = 2000
            ),

            // FOLLOW_ACCOUNT tasks
            Task(
                advertiserId = advertiserId,
                title = "Follow on Twitter",
                description = "Follow our official Twitter account for updates",
                taskType = TaskType.FOLLOW_ACCOUNT,
                rewardCoins = 30,
                totalCapacity = 1000,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                socialMediaLink = "https://www.twitter.com",
                completionTimeSeconds = 5,
                targetLikes = 1000
            ),
            Task(
                advertiserId = advertiserId,
                title = "Follow YouTube Channel",
                description = "Subscribe to our YouTube channel for more content",
                taskType = TaskType.FOLLOW_ACCOUNT,
                rewardCoins = 35,
                totalCapacity = 800,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                socialMediaLink = "https://www.youtube.com",
                completionTimeSeconds = 5,
                targetLikes = 800
            ),

            // SHARE_POST tasks
            Task(
                advertiserId = advertiserId,
                title = "Share on WhatsApp",
                description = "Share our promotional post with your WhatsApp contacts",
                taskType = TaskType.SHARE_POST,
                rewardCoins = 40,
                totalCapacity = 700,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                socialMediaLink = "https://www.whatsapp.com",
                completionTimeSeconds = 5,
                targetShares = 700
            ),
            Task(
                advertiserId = advertiserId,
                title = "Share on Telegram",
                description = "Spread the word by sharing on Telegram",
                taskType = TaskType.SHARE_POST,
                rewardCoins = 35,
                totalCapacity = 500,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                socialMediaLink = "https://www.telegram.org",
                completionTimeSeconds = 5,
                targetShares = 500
            ),

            // COMMENT tasks
            Task(
                advertiserId = advertiserId,
                title = "Comment on Blog Post",
                description = "Leave a thoughtful comment on our latest blog post",
                taskType = TaskType.COMMENT,
                rewardCoins = 50,
                totalCapacity = 400,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                targetUrl = "https://www.reddit.com",
                completionTimeSeconds = 10,
                targetClicks = 400
            ),
            Task(
                advertiserId = advertiserId,
                title = "Comment on YouTube Video",
                description = "Share your thoughts in the comments section",
                taskType = TaskType.COMMENT,
                rewardCoins = 45,
                totalCapacity = 600,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
                videoUrl = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
                completionTimeSeconds = 10,
                targetClicks = 600
            )
        )
    }

    /**
     * Clear all seeded data (for testing purposes)
     */
    fun clearDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        
        runBlocking {
            db.clearAllTables()
            Log.d(TAG, "Database cleared")
        }
    }
}
