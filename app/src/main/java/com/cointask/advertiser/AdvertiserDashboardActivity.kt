package com.cointask.advertiser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cointask.auth.LoginActivity
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.*
import com.cointask.databinding.ActivityAdvertiserDashboardBinding
import com.cointask.utils.PasswordUtils
import com.cointask.utils.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdvertiserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdvertiserDashboardBinding
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private var currentAdvertiserId = -1
    private var totalBudget = 0
    private var spentAmount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvertiserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        currentAdvertiserId = preferencesManager.getUserId()

        setupClickListeners()
        loadAdvertiserData()
        loadCampaigns()
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.fabCreateCampaign.setOnClickListener {
            showCreateCampaignDialog()
        }

        binding.fabCreateTask.setOnClickListener {
            showCreateTaskDialog()
        }

        binding.btnAnalytics.setOnClickListener {
            showAnalyticsDialog()
        }

        binding.btnRefreshCampaigns.setOnClickListener {
            loadCampaigns()
        }

        binding.btnSettings.setOnClickListener {
            showAdvertiserSettingsDialog()
        }
    }

    private fun loadAdvertiserData() {
        lifecycleScope.launch {
            database.userDao().getUserById(currentAdvertiserId).collectLatest { user ->
                user?.let {
                    binding.tvAdvertiserName.text = "Welcome ${it.fullName}!"
                    totalBudget = it.coins
                    binding.tvTotalBudget.text = "🪙$totalBudget"
                    updateBudgetDisplay()
                }
            }

            // Calculate spent amount from campaigns
            database.campaignDao().getCampaignsByAdvertiser(currentAdvertiserId).collectLatest { campaigns ->
                spentAmount = campaigns.sumOf { it.spentAmount }
                val remaining = totalBudget - spentAmount
                binding.tvRemainingBudget.text = "🪙$remaining"

                val totalTasks = campaigns.sumOf { it.totalTasks }
                val completedTasks = campaigns.sumOf { it.completedTasks }

                binding.tvTotalTasks.text = totalTasks.toString()
                binding.tvCompletedTasks.text = completedTasks.toString()

                // Calculate ROI
                if (spentAmount > 0) {
                    val estimatedValue = completedTasks * 5 // Assume each completion is worth 5 coins
                    val roi = ((estimatedValue - spentAmount).toFloat() / spentAmount * 100).toInt()
                    binding.tvRoi.text = "${roi}%"
                } else {
                    binding.tvRoi.text = "0%"
                }
            }
        }
    }

    private fun updateBudgetDisplay() {
        binding.tvRemainingBudget.text = "🪙${totalBudget - spentAmount}"
    }

    private fun loadCampaigns() {
        lifecycleScope.launch {
            database.campaignDao().getCampaignsByAdvertiser(currentAdvertiserId).collectLatest { campaigns ->
                if (campaigns.isEmpty()) {
                    binding.rvCampaigns.visibility = View.GONE
                    binding.tvNoCampaigns.visibility = View.VISIBLE
                } else {
                    binding.rvCampaigns.visibility = View.VISIBLE
                    binding.tvNoCampaigns.visibility = View.GONE
                    
                    // For now, show campaigns in a simple list
                    showCampaignsInList(campaigns)
                }
            }
        }
    }

    private fun showCampaignsInList(campaigns: List<Campaign>) {
        // Campaigns are displayed in the RecyclerView via loadCampaigns()
        // This method is kept for compatibility but no longer needed
    }

    private fun showCreateCampaignDialog() {
        val nameInput = EditText(this).apply { hint = "Campaign Name" }
        val descriptionInput = EditText(this).apply {
            hint = "Description"
            setLines(3)
        }
        val budgetInput = EditText(this).apply {
            hint = "Budget (🪙)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val tasksInput = EditText(this).apply { 
            hint = "Number of Tasks"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(nameInput)
            addView(descriptionInput)
            addView(budgetInput)
            addView(tasksInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Create New Campaign")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val budget = budgetInput.text.toString().toIntOrNull() ?: 0
                val tasks = tasksInput.text.toString().toIntOrNull() ?: 0

                if (name.isNotEmpty() && budget > 0 && tasks > 0) {
                    createCampaign(name, description, budget, tasks)
                } else {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCampaign(name: String, description: String, budget: Int, taskCount: Int) {
        lifecycleScope.launch {
            val currentDate = System.currentTimeMillis()
            val campaign = Campaign(
                advertiserId = currentAdvertiserId,
                name = name,
                description = description,
                budget = budget,
                spentAmount = 0,
                totalTasks = taskCount,
                completedTasks = 0,
                status = CampaignStatus.ACTIVE,
                startDate = currentDate,
                endDate = currentDate + (7 * 24 * 60 * 60 * 1000), // 7 days
                costPerTask = budget / taskCount
            )

            val campaignId = database.campaignDao().insertCampaign(campaign).toInt()
            
            // Create associated tasks
            for (i in 1..taskCount) {
                val task = Task(
                    advertiserId = currentAdvertiserId,
                    title = "$name - Task $i",
                    description = description,
                    taskType = TaskType.WATCH_VIDEO,
                    rewardCoins = (budget.toFloat() / taskCount).toInt(),
                    totalCapacity = 1,
                    completedCount = 0,
                    status = TaskStatus.ACTIVE,
                    expiresAt = currentDate + (7 * 24 * 60 * 60 * 1000),
                    targetUrl = null,
                    videoUrl = null,
                    completionTimeSeconds = 5,
                    targetViews = 0,
                    targetLikes = 0,
                    targetShares = 0,
                    targetClicks = 0,
                    currentViews = 0,
                    currentLikes = 0,
                    currentShares = 0,
                    currentClicks = 0
                )
                database.taskDao().insertTask(task)
            }

            // Log activity
            database.activityLogDao().insertLog(
                ActivityLog(
                    userId = currentAdvertiserId,
                    action = "CAMPAIGN_CREATED",
                    details = "Created campaign: $name with $taskCount tasks and 🪙$budget budget"
                )
            )

            Toast.makeText(this@AdvertiserDashboardActivity,
                "Campaign created successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCreateTaskDialog() {
        val titleInput = EditText(this).apply { hint = "Task Title" }
        val descriptionInput = EditText(this).apply {
            hint = "Description"
            setLines(3)
        }
        val rewardInput = EditText(this).apply {
            hint = "Reward (coins)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val capacityInput = EditText(this).apply {
            hint = "Capacity (number of users)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val completionTimeInput = EditText(this).apply {
            hint = "Completion Time (seconds)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("5")
        }

        val taskTypes = arrayOf("WATCH_VIDEO", "VISIT_SITE", "LIKE_CONTENT", "SHARE_POST", "FOLLOW_ACCOUNT", "COMMENT", "SURVEY")
        var selectedType = 0

        // Target settings based on task type
        val targetViewsInput = EditText(this).apply {
            hint = "Target Views (0 to disable)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
        }
        val targetLikesInput = EditText(this).apply {
            hint = "Target Likes (0 to disable)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
        }
        val targetSharesInput = EditText(this).apply {
            hint = "Target Shares (0 to disable)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
        }
        val targetClicksInput = EditText(this).apply {
            hint = "Target Clicks (0 to disable)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(titleInput)
            addView(descriptionInput)
            addView(rewardInput)
            addView(capacityInput)
            addView(completionTimeInput)

            addView(TextView(this@AdvertiserDashboardActivity).apply {
                text = "Task Type"
                setPadding(0, 30, 0, 10)
            })

            addView(TextView(this@AdvertiserDashboardActivity).apply {
                text = "Target Settings (Set 0 to disable)"
                setPadding(0, 30, 0, 10)
                textSize = 12f
            })
            addView(targetViewsInput)
            addView(targetLikesInput)
            addView(targetSharesInput)
            addView(targetClicksInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Create New Task")
            .setView(layout)
            .setSingleChoiceItems(taskTypes, selectedType) { _, which ->
                selectedType = which
            }
            .setPositiveButton("Create") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val reward = rewardInput.text.toString().toIntOrNull() ?: 0
                val capacity = capacityInput.text.toString().toIntOrNull() ?: 0
                val completionTime = completionTimeInput.text.toString().toIntOrNull() ?: 5
                val targetViews = targetViewsInput.text.toString().toIntOrNull() ?: 0
                val targetLikes = targetLikesInput.text.toString().toIntOrNull() ?: 0
                val targetShares = targetSharesInput.text.toString().toIntOrNull() ?: 0
                val targetClicks = targetClicksInput.text.toString().toIntOrNull() ?: 0

                if (title.isNotEmpty() && reward > 0 && capacity > 0 && completionTime > 0) {
                    createTask(title, description, reward, capacity, TaskType.valueOf(taskTypes[selectedType]),
                        completionTime, targetViews, targetLikes, targetShares, targetClicks)
                } else {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Please fill all required fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createTask(
        title: String,
        description: String,
        reward: Int,
        capacity: Int,
        type: TaskType,
        completionTimeSeconds: Int = 5,
        targetViews: Int = 0,
        targetLikes: Int = 0,
        targetShares: Int = 0,
        targetClicks: Int = 0
    ) {
        lifecycleScope.launch {
            val task = Task(
                advertiserId = currentAdvertiserId,
                title = title,
                description = description,
                taskType = type,
                rewardCoins = reward,
                totalCapacity = capacity,
                completedCount = 0,
                status = TaskStatus.ACTIVE,
                expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
                completionTimeSeconds = completionTimeSeconds,
                targetViews = targetViews,
                targetLikes = targetLikes,
                targetShares = targetShares,
                targetClicks = targetClicks,
                currentViews = 0,
                currentLikes = 0,
                currentShares = 0,
                currentClicks = 0
            )

            database.taskDao().insertTask(task)

            // Log activity
            database.activityLogDao().insertLog(
                ActivityLog(
                    userId = currentAdvertiserId,
                    action = "TASK_CREATED",
                    details = "Created task: $title with $reward coins reward, completion time: ${completionTimeSeconds}s, " +
                            "targets: views=$targetViews, likes=$targetLikes, shares=$targetShares, clicks=$targetClicks"
                )
            )

            Toast.makeText(this@AdvertiserDashboardActivity,
                "Task created successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAnalyticsDialog() {
        lifecycleScope.launch {
            val campaigns = database.campaignDao().getCampaignsByAdvertiser(currentAdvertiserId)
            val tasks = database.taskDao().getTasksByAdvertiser(currentAdvertiserId)
            
            campaigns.collectLatest { campaignList ->
                tasks.collectLatest { taskList ->
                    val totalSpent = campaignList.sumOf { it.spentAmount }
                    val totalBudget = campaignList.sumOf { it.budget }
                    val totalTasks = taskList.size
                    val completedTasks = taskList.sumOf { it.completedCount }
                    val completionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100).toInt() else 0
                    
                    val analyticsText = """
                        Campaign Analytics Summary

                        💰 Budget Overview:
                        - Total Budget: 🪙$totalBudget
                        - Total Spent: 🪙$totalSpent
                        - Remaining: 🪙${totalBudget - totalSpent}

                        📊 Task Performance:
                        - Total Tasks: $totalTasks
                        - Completed: $completedTasks
                        - Completion Rate: $completionRate%

                        📈 Campaign Status:
                        ${campaignList.joinToString("\n") { "- ${it.name}: ${it.completedTasks}/${it.totalTasks} tasks" }}
                    """.trimIndent()

                    AlertDialog.Builder(this@AdvertiserDashboardActivity)
                        .setTitle("Analytics Dashboard")
                        .setMessage(analyticsText)
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Export") { _, _ ->
                            Toast.makeText(this@AdvertiserDashboardActivity,
                                "Export feature coming soon!", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
            }
        }
    }

    private fun showCampaignManagementDialog(campaign: Campaign) {
        val actions = arrayOf("View Details", "Pause/Resume Campaign", "Edit Budget", "Delete Campaign")

        AlertDialog.Builder(this)
            .setTitle("Manage: ${campaign.name}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showCampaignDetails(campaign)
                    1 -> toggleCampaignStatus(campaign)
                    2 -> showEditBudgetDialog(campaign)
                    3 -> deleteCampaign(campaign)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showCampaignDetails(campaign: Campaign) {
        val details = """
            Campaign: ${campaign.name}
            Description: ${campaign.description}

            Budget: 🪙${campaign.budget}
            Spent: 🪙${campaign.spentAmount}
            Remaining: 🪙${campaign.budget - campaign.spentAmount}

            Tasks: ${campaign.completedTasks}/${campaign.totalTasks}
            Cost per Task: 🪙${campaign.costPerTask}

            Status: ${campaign.status}
            Start: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(campaign.startDate))}
            End: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(campaign.endDate))}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Campaign Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleCampaignStatus(campaign: Campaign) {
        lifecycleScope.launch {
            val newStatus = when (campaign.status) {
                CampaignStatus.ACTIVE -> CampaignStatus.PAUSED
                CampaignStatus.PAUSED -> CampaignStatus.ACTIVE
                else -> campaign.status
            }
            
            if (newStatus != campaign.status) {
                database.campaignDao().updateCampaign(campaign.copy(status = newStatus))
                Toast.makeText(this@AdvertiserDashboardActivity,
                    "Campaign ${if (newStatus == CampaignStatus.ACTIVE) "resumed" else "paused"}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditBudgetDialog(campaign: Campaign) {
        val budgetInput = EditText(this).apply {
            setText(campaign.budget.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Budget")
            .setView(budgetInput)
            .setPositiveButton("Update") { _, _ ->
                val newBudget = budgetInput.text.toString().toIntOrNull() ?: return@setPositiveButton
                if (newBudget > campaign.spentAmount) {
                    lifecycleScope.launch {
                        database.campaignDao().updateCampaign(campaign.copy(budget = newBudget))
                        Toast.makeText(this@AdvertiserDashboardActivity,
                            "Budget updated!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Budget must be greater than spent amount!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCampaign(campaign: Campaign) {
        AlertDialog.Builder(this)
            .setTitle("Delete Campaign")
            .setMessage("Are you sure you want to delete '${campaign.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    // Mark campaign as cancelled
                    database.campaignDao().updateCampaign(campaign.copy(status = CampaignStatus.CANCELLED))
                    
                    // Mark associated tasks as completed
                    val tasks = database.taskDao().getTasksByUser(campaign.advertiserId)
                    tasks.forEach { task: Task ->
                        database.taskDao().updateTask(task.copy(status = TaskStatus.COMPLETED))
                    }
                    
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Campaign deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                preferencesManager.clearSession()
                startActivity(Intent(this@AdvertiserDashboardActivity, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show advertiser settings dialog with profile and payment options
     */
    private fun showAdvertiserSettingsDialog() {
        val settings = arrayOf(
            "👤 View Profile",
            "💳 Payment Details",
            "🔐 Change Password",
            "📊 Campaign Statistics",
            "⚙️ Task Settings",
            "ℹ️ About CoinTask"
        )

        AlertDialog.Builder(this)
            .setTitle("Advertiser Settings")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> showAdvertiserProfile()
                    1 -> showPaymentDetails()
                    2 -> showChangePasswordDialog()
                    3 -> showCampaignStatistics()
                    4 -> showTaskSettingsInfo()
                    5 -> showAboutDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAdvertiserProfile() {
        lifecycleScope.launch {
            val user = database.userDao().getUserByIdSuspend(currentAdvertiserId)
            user?.let { advertiser ->
                val profileDetails = """
                    📢 Advertiser Profile

                    Name: ${advertiser.fullName}
                    Email: ${advertiser.email}
                    Role: ${advertiser.role}

                    💰 Account Balance: 🪙${advertiser.coins}
                    💵 Total Budget: 🪙${totalBudget}
                    💸 Spent: 🪙${spentAmount}
                    💰 Remaining: 🪙${totalBudget - spentAmount}

                    ✅ Verified: ${if (advertiser.isVerified) "Yes" else "No"}
                    🔓 Active: ${if (advertiser.isActive) "Yes" else "No"}
                    💳 Payment Verified: ${if (advertiser.paymentVerified) "Yes" else "No"}

                    📅 Member Since: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(advertiser.createdAt))}
                    🕐 Last Login: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(advertiser.lastLogin))}
                """.trimIndent()

                AlertDialog.Builder(this@AdvertiserDashboardActivity)
                    .setTitle("My Profile")
                    .setMessage(profileDetails)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showPaymentDetails() {
        lifecycleScope.launch {
            val user = database.userDao().getUserByIdSuspend(currentAdvertiserId)
            user?.let { advertiser ->
                val paymentDetails = """
                    💳 Payment Information

                    Transaction ID: ${advertiser.transactionId ?: "Not provided"}
                    CNIC: ${advertiser.cnic ?: "Not provided"}
                    Account Title: ${advertiser.accountTitle ?: "Not provided"}
                    Account Number: ${advertiser.accountNumber ?: "Not provided"}

                    Status: ${if (advertiser.paymentVerified) "✅ Verified by Admin" else "⏳ Pending Verification"}

                    ${if (!advertiser.paymentVerified) """
                    Note: Your payment details are pending verification by admin.
                    Once verified, you'll receive your bonus coins.
                    """ else """
                    Your payment has been verified. You can receive coins from admin
                    as your campaigns perform well.
                    """}
                """.trimIndent()

                AlertDialog.Builder(this@AdvertiserDashboardActivity)
                    .setTitle("Payment Details")
                    .setMessage(paymentDetails)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Edit") { _, _ ->
                        showEditPaymentDetailsDialog(advertiser)
                    }
                    .show()
            }
        }
    }

    private fun showEditPaymentDetailsDialog(advertiser: com.cointask.data.models.User) {
        val transactionIdInput = EditText(this).apply {
            setText(advertiser.transactionId ?: "")
            hint = "Transaction ID"
        }
        val cnicInput = EditText(this).apply {
            setText(advertiser.cnic ?: "")
            hint = "CNIC (12345-1234567-1)"
        }
        val accountTitleInput = EditText(this).apply {
            setText(advertiser.accountTitle ?: "")
            hint = "Account Title"
        }
        val accountNumberInput = EditText(this).apply {
            setText(advertiser.accountNumber ?: "")
            hint = "Account Number"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(transactionIdInput)
            addView(cnicInput)
            addView(accountTitleInput)
            addView(accountNumberInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Payment Details")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val transactionId = transactionIdInput.text.toString().trim()
                val cnic = cnicInput.text.toString().trim()
                val accountTitle = accountTitleInput.text.toString().trim()
                val accountNumber = accountNumberInput.text.toString().trim()

                if (transactionId.isNotEmpty() && cnic.isNotEmpty() && 
                    accountTitle.isNotEmpty() && accountNumber.isNotEmpty()) {
                    lifecycleScope.launch {
                        val updatedUser = advertiser.copy(
                            transactionId = transactionId,
                            cnic = cnic,
                            accountTitle = accountTitle,
                            accountNumber = accountNumber,
                            paymentVerified = false // Reset verification on edit
                        )
                        database.userDao().updateUser(updatedUser)

                        database.activityLogDao().insertLog(
                            ActivityLog(
                                userId = advertiser.id,
                                action = "PAYMENT_DETAILS_UPDATED",
                                details = "Advertiser updated payment details"
                            )
                        )

                        Toast.makeText(this@AdvertiserDashboardActivity,
                            "Payment details updated! Pending re-verification.",
                            Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "All fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val currentPasswordInput = EditText(this).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val newPasswordInput = EditText(this).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmPasswordInput = EditText(this).apply {
            hint = "Confirm New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(currentPasswordInput)
            addView(newPasswordInput)
            addView(confirmPasswordInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 8) {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val hashedPassword = PasswordUtils.hashPassword(newPassword)
                    val user = database.userDao().getUserByIdSuspend(currentAdvertiserId)
                    user?.let {
                        val updatedUser = it.copy(password = hashedPassword)
                        database.userDao().updateUser(updatedUser)

                        database.activityLogDao().insertLog(
                            ActivityLog(
                                userId = it.id,
                                action = "PASSWORD_CHANGED",
                                details = "Advertiser changed password"
                            )
                        )

                        Toast.makeText(this@AdvertiserDashboardActivity,
                            "Password changed successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCampaignStatistics() {
        lifecycleScope.launch {
            val campaigns = database.campaignDao().getCampaignsByAdvertiser(currentAdvertiserId)
            val tasks = database.taskDao().getTasksByAdvertiser(currentAdvertiserId)

            campaigns.collectLatest { campaignList ->
                tasks.collectLatest { taskList ->
                    val totalBudget = campaignList.sumOf { it.budget }
                    val totalSpent = campaignList.sumOf { it.spentAmount }
                    val totalTasks = taskList.size
                    val completedTasks = taskList.sumOf { it.completedCount }
                    val completionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100).toInt() else 0

                    val stats = """
                        📊 Campaign Statistics

                        💰 Budget Overview:
                        - Total Budget: $${totalBudget}
                        - Total Spent: $${totalSpent}
                        - Remaining: $${totalBudget - totalSpent}

                        📋 Task Performance:
                        - Total Tasks: $totalTasks
                        - Completed: $completedTasks
                        - Completion Rate: $completionRate%

                        📈 Campaigns: ${campaignList.size}
                        - Active: ${campaignList.count { it.status == CampaignStatus.ACTIVE }}
                        - Paused: ${campaignList.count { it.status == CampaignStatus.PAUSED }}
                        - Completed: ${campaignList.count { it.status == CampaignStatus.ENDED }}
                    """.trimIndent()

                    AlertDialog.Builder(this@AdvertiserDashboardActivity)
                        .setTitle("Campaign Statistics")
                        .setMessage(stats)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun showTaskSettingsInfo() {
        val infoText = """
            ⚙️ Task Settings Information

            When creating tasks, you can set:

            🎯 Target Settings:
            • Target Views - Auto-pause when reached
            • Target Likes - Auto-pause when reached
            • Target Shares - Auto-pause when reached
            • Target Clicks - Auto-pause when reached

            ⏱️ Completion Time:
            • Set how long users need to complete the task
            • Default: 5 seconds
            • Range: 1-60 seconds

            💰 Reward Coins:
            • Set attractive rewards to get more completions
            • Higher rewards = faster completions

            📊 Capacity:
            • Set how many users can complete the task
            • Task auto-pauses when capacity is reached
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Task Settings Guide")
            .setMessage(infoText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAboutDialog() {
        val aboutText = """
            💰 CoinTask - Task Rewards Platform

            Version: 2.0.0

            Advertiser Features:
            • Create campaigns with custom budgets
            • Set task rewards and completion times
            • Define targets (views, likes, shares, clicks)
            • Auto-pause when targets achieved
            • Real-time analytics and ROI tracking

            © 2026-2031 CoinTask. All rights reserved.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About CoinTask")
            .setMessage(aboutText)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Stay", null)
            .show()
    }
}
