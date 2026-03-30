package com.cointask.advertiser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
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
        binding.btnLogout.setOnClickListener { logout() }
        binding.fabCreateCampaign.setOnClickListener { showCreateCampaignDialog() }
        binding.fabCreateTask.setOnClickListener { showCreateTaskDialog() }
        binding.btnAnalytics.setOnClickListener { showAnalyticsDialog() }
        binding.btnRefreshCampaigns.setOnClickListener { loadCampaigns() }
        binding.btnSettings.setOnClickListener { showAdvertiserSettingsDialog() }
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

            database.campaignDao().getCampaignsByAdvertiser(currentAdvertiserId).collectLatest { campaigns ->
                spentAmount = campaigns.sumOf { it.spentAmount }
                val remaining = totalBudget - spentAmount
                binding.tvRemainingBudget.text = "🪙$remaining"
                binding.tvTotalTasks.text = campaigns.sumOf { it.totalTasks }.toString()
                binding.tvCompletedTasks.text = campaigns.sumOf { it.completedTasks }.toString()

                if (spentAmount > 0) {
                    val estimatedValue = campaigns.sumOf { it.completedTasks } * 5
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
                    showCampaignsInList(campaigns)
                }
            }
        }
    }

    private fun showCampaignsInList(campaigns: List<Campaign>) {
        // Implementation for displaying campaigns
    }

    private fun showCreateCampaignDialog() {
        val scrollView = NestedScrollView(this).apply {
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val nameInput = EditText(this).apply {
            hint = "Campaign Name *"
            setSingleLine()
        }

        val descriptionInput = EditText(this).apply {
            hint = "Description *"
            setLines(3)
        }

        val budgetInput = EditText(this).apply {
            hint = "Budget (🪙) *"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
        }

        val tasksInput = EditText(this).apply {
            hint = "Number of Tasks *"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
        }

        val rewardPerTaskInput = EditText(this).apply {
            hint = "Reward per Task (🪙) *"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
        }

        val taskTypeLabel = TextView(this).apply {
            text = "Task Type *"
            setPadding(0, 30, 0, 10)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_primary))
        }

        val taskTypeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@AdvertiserDashboardActivity,
                android.R.layout.simple_spinner_item,
                arrayOf("WATCH_VIDEO", "VISIT_SITE", "LIKE_CONTENT", "SHARE_POST", "FOLLOW_ACCOUNT", "COMMENT", "SURVEY"))
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val linkLabel = TextView(this).apply {
            text = "Task Link (URL) *"
            setPadding(0, 20, 0, 10)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_primary))
        }

        val linkInput = EditText(this).apply {
            hint = "Video URL, Website URL, or Social Media Link"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
        }

        val linkInfoText = TextView(this).apply {
            text = "📌 Users will see a preview of this link before starting the task"
            textSize = 12f
            setPadding(0, 8, 0, 8)
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_secondary))
        }

        val targetLabel = TextView(this).apply {
            text = "Target Settings (Set 0 to disable)"
            setPadding(0, 20, 0, 10)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_primary))
        }

        val targetViewsInput = EditText(this).apply {
            hint = "Target Views"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }

        val targetLikesInput = EditText(this).apply {
            hint = "Target Likes"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }

        val targetSharesInput = EditText(this).apply {
            hint = "Target Shares"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }

        val targetClicksInput = EditText(this).apply {
            hint = "Target Clicks"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }

        val balanceInfoText = TextView(this).apply {
            text = "💰 Your Balance: 🪙$totalBudget"
            textSize = 14f
            setPadding(0, 20, 0, 10)
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.primary))
            textStyle = android.graphics.Typeface.BOLD
        }

        layout.addView(nameInput)
        layout.addView(descriptionInput)
        layout.addView(budgetInput)
        layout.addView(tasksInput)
        layout.addView(rewardPerTaskInput)
        layout.addView(taskTypeLabel)
        layout.addView(taskTypeSpinner)
        layout.addView(linkLabel)
        layout.addView(linkInput)
        layout.addView(linkInfoText)
        layout.addView(targetLabel)
        layout.addView(targetViewsInput)
        layout.addView(targetLikesInput)
        layout.addView(targetSharesInput)
        layout.addView(targetClicksInput)
        layout.addView(balanceInfoText)

        scrollView.addView(layout)

        AlertDialog.Builder(this)
            .setTitle("Create New Campaign")
            .setView(scrollView)
            .setPositiveButton("Save as Draft") { _, _ ->
                val name = nameInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val budget = budgetInput.text.toString().toIntOrNull() ?: 0
                val tasks = tasksInput.text.toString().toIntOrNull() ?: 0
                val rewardPerTask = rewardPerTaskInput.text.toString().toIntOrNull() ?: 0
                val linkUrl = linkInput.text.toString().trim()
                val selectedTaskType = taskTypeSpinner.selectedItem.toString()

                if (name.isNotEmpty() && description.isNotEmpty() && budget > 0 && tasks > 0 && rewardPerTask > 0 && linkUrl.isNotEmpty()) {
                    createCampaign(name, description, budget, tasks, rewardPerTask, selectedTaskType, linkUrl,
                        targetViewsInput.text.toString().toIntOrNull() ?: 0,
                        targetLikesInput.text.toString().toIntOrNull() ?: 0,
                        targetSharesInput.text.toString().toIntOrNull() ?: 0,
                        targetClicksInput.text.toString().toIntOrNull() ?: 0,
                        isDraft = true)
                } else {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Please fill all required fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Create & Pay") { _, _ ->
                val name = nameInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val budget = budgetInput.text.toString().toIntOrNull() ?: 0
                val tasks = tasksInput.text.toString().toIntOrNull() ?: 0
                val rewardPerTask = rewardPerTaskInput.text.toString().toIntOrNull() ?: 0
                val linkUrl = linkInput.text.toString().trim()
                val selectedTaskType = taskTypeSpinner.selectedItem.toString()

                if (name.isNotEmpty() && description.isNotEmpty() && budget > 0 && tasks > 0 && rewardPerTask > 0 && linkUrl.isNotEmpty()) {
                    val totalCost = budget + (rewardPerTask * tasks)
                    if (totalCost > totalBudget) {
                        Toast.makeText(this@AdvertiserDashboardActivity,
                            "Insufficient balance! Total cost: 🪙$totalCost, Your balance: 🪙$totalBudget\nPlease save as draft and add funds.",
                            Toast.LENGTH_LONG).show()
                    } else {
                        createCampaign(name, description, budget, tasks, rewardPerTask, selectedTaskType, linkUrl,
                            targetViewsInput.text.toString().toIntOrNull() ?: 0,
                            targetLikesInput.text.toString().toIntOrNull() ?: 0,
                            targetSharesInput.text.toString().toIntOrNull() ?: 0,
                            targetClicksInput.text.toString().toIntOrNull() ?: 0,
                            isDraft = false)
                    }
                } else {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Please fill all required fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCampaign(
        name: String,
        description: String,
        budget: Int,
        taskCount: Int,
        rewardPerTask: Int,
        taskTypeStr: String,
        linkUrl: String,
        targetViews: Int,
        targetLikes: Int,
        targetShares: Int,
        targetClicks: Int,
        isDraft: Boolean
    ) {
        lifecycleScope.launch {
            val currentDate = System.currentTimeMillis()
            val totalCost = budget + (rewardPerTask * taskCount)

            if (!isDraft && totalCost > totalBudget) {
                Toast.makeText(this@AdvertiserDashboardActivity,
                    "Insufficient balance! Please save as draft.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val campaignStatus = if (isDraft) CampaignStatus.PENDING else CampaignStatus.ACTIVE

            val campaign = Campaign(
                advertiserId = currentAdvertiserId,
                name = name,
                description = description,
                budget = budget,
                spentAmount = if (isDraft) 0 else budget,
                totalTasks = taskCount,
                completedTasks = 0,
                status = campaignStatus,
                startDate = currentDate,
                endDate = currentDate + (7 * 24 * 60 * 60 * 1000),
                costPerTask = rewardPerTask
            )

            val campaignId = database.campaignDao().insertCampaign(campaign).toInt()

            if (!isDraft) {
                // Deduct budget from advertiser's balance
                database.userDao().deductCoins(currentAdvertiserId, budget)
            }

            // Create associated tasks
            val taskType = TaskType.valueOf(taskTypeStr)
            val videoUrl = if (taskType == TaskType.WATCH_VIDEO) linkUrl else null
            val targetUrl = if (taskType == TaskType.VISIT_SITE || taskType == TaskType.SURVEY) linkUrl else null
            val socialMediaLink = if (taskType in listOf(TaskType.LIKE_CONTENT, TaskType.SHARE_POST,
                    TaskType.FOLLOW_ACCOUNT, TaskType.COMMENT)) linkUrl else null

            for (i in 1..taskCount) {
                val task = Task(
                    advertiserId = currentAdvertiserId,
                    title = "$name - Task $i",
                    description = description,
                    taskType = taskType,
                    rewardCoins = rewardPerTask,
                    totalCapacity = 1,
                    completedCount = 0,
                    status = campaignStatus,
                    expiresAt = currentDate + (7 * 24 * 60 * 60 * 1000),
                    videoUrl = videoUrl,
                    targetUrl = targetUrl,
                    socialMediaLink = socialMediaLink,
                    completionTimeSeconds = 5,
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
            }

            database.activityLogDao().insertLog(
                ActivityLog(
                    userId = currentAdvertiserId,
                    action = if (isDraft) "CAMPAIGN_SAVED_DRAFT" else "CAMPAIGN_CREATED",
                    details = "${if (isDraft) "Saved draft" else "Created"} campaign: $name with $taskCount tasks, 🪙$budget budget"
                )
            )

            Toast.makeText(this@AdvertiserDashboardActivity,
                if (isDraft) "Campaign saved as draft!" else "Campaign created successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCreateTaskDialog() {
        val scrollView = NestedScrollView(this).apply {
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val titleInput = EditText(this).apply { hint = "Task Title *"; setSingleLine() }
        val descriptionInput = EditText(this).apply { hint = "Description *"; setLines(3) }
        val rewardInput = EditText(this).apply {
            hint = "Reward (🪙) *"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
        }
        val capacityInput = EditText(this).apply {
            hint = "Capacity (number of users) *"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
        }
        val completionTimeInput = EditText(this).apply {
            hint = "Completion Time (seconds) *"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("5")
            setSingleLine()
        }

        val taskTypeLabel = TextView(this).apply {
            text = "Task Type *"
            setPadding(0, 20, 0, 10)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_primary))
        }

        val taskTypeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@AdvertiserDashboardActivity,
                android.R.layout.simple_spinner_item,
                arrayOf("WATCH_VIDEO", "VISIT_SITE", "LIKE_CONTENT", "SHARE_POST", "FOLLOW_ACCOUNT", "COMMENT", "SURVEY"))
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val linkLabel = TextView(this).apply {
            text = "Task Link (URL) *"
            setPadding(0, 20, 0, 10)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_primary))
        }

        val linkInput = EditText(this).apply {
            hint = "Video URL, Website URL, or Social Media Link"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
        }

        val linkInfoText = TextView(this).apply {
            text = "📌 Users will preview this link and complete real actions"
            textSize = 12f
            setPadding(0, 8, 0, 8)
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_secondary))
        }

        val targetLabel = TextView(this).apply {
            text = "Target Settings (Set 0 to disable)"
            setPadding(0, 20, 0, 10)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.text_primary))
        }

        val targetViewsInput = EditText(this).apply {
            hint = "Target Views"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }
        val targetLikesInput = EditText(this).apply {
            hint = "Target Likes"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }
        val targetSharesInput = EditText(this).apply {
            hint = "Target Shares"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }
        val targetClicksInput = EditText(this).apply {
            hint = "Target Clicks"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
            setSingleLine()
        }

        val balanceInfoText = TextView(this).apply {
            text = "💰 Your Balance: 🪙$totalBudget | Cost: 🪙${rewardInput.text.toString().toIntOrNull() ?: 0} × ${capacityInput.text.toString().toIntOrNull() ?: 0}"
            textSize = 14f
            setPadding(0, 20, 0, 10)
            setTextColor(ContextCompat.getColor(this@AdvertiserDashboardActivity, com.cointask.R.color.primary))
            textStyle = android.graphics.Typeface.BOLD
        }

        layout.addView(titleInput)
        layout.addView(descriptionInput)
        layout.addView(rewardInput)
        layout.addView(capacityInput)
        layout.addView(completionTimeInput)
        layout.addView(taskTypeLabel)
        layout.addView(taskTypeSpinner)
        layout.addView(linkLabel)
        layout.addView(linkInput)
        layout.addView(linkInfoText)
        layout.addView(targetLabel)
        layout.addView(targetViewsInput)
        layout.addView(targetLikesInput)
        layout.addView(targetSharesInput)
        layout.addView(targetClicksInput)
        layout.addView(balanceInfoText)

        scrollView.addView(layout)

        AlertDialog.Builder(this)
            .setTitle("Create New Task")
            .setView(scrollView)
            .setPositiveButton("Save as Draft") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val reward = rewardInput.text.toString().toIntOrNull() ?: 0
                val capacity = capacityInput.text.toString().toIntOrNull() ?: 0
                val completionTime = completionTimeInput.text.toString().toIntOrNull() ?: 5
                val linkUrl = linkInput.text.toString().trim()
                val selectedType = taskTypeSpinner.selectedItem.toString()

                if (title.isNotEmpty() && description.isNotEmpty() && reward > 0 && capacity > 0 && completionTime > 0 && linkUrl.isNotEmpty()) {
                    val totalCost = reward * capacity
                    createTask(title, description, reward, capacity, TaskType.valueOf(selectedType),
                        completionTime,
                        targetViewsInput.text.toString().toIntOrNull() ?: 0,
                        targetLikesInput.text.toString().toIntOrNull() ?: 0,
                        targetSharesInput.text.toString().toIntOrNull() ?: 0,
                        targetClicksInput.text.toString().toIntOrNull() ?: 0,
                        linkUrl, isDraft = true)
                } else {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Please fill all required fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Create & Pay") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val reward = rewardInput.text.toString().toIntOrNull() ?: 0
                val capacity = capacityInput.text.toString().toIntOrNull() ?: 0
                val completionTime = completionTimeInput.text.toString().toIntOrNull() ?: 5
                val linkUrl = linkInput.text.toString().trim()
                val selectedType = taskTypeSpinner.selectedItem.toString()

                if (title.isNotEmpty() && description.isNotEmpty() && reward > 0 && capacity > 0 && completionTime > 0 && linkUrl.isNotEmpty()) {
                    val totalCost = reward * capacity
                    if (totalCost > totalBudget) {
                        Toast.makeText(this@AdvertiserDashboardActivity,
                            "Insufficient balance! Total cost: 🪙$totalCost, Your balance: 🪙$totalBudget\nPlease save as draft.",
                            Toast.LENGTH_LONG).show()
                    } else {
                        createTask(title, description, reward, capacity, TaskType.valueOf(selectedType),
                            completionTime,
                            targetViewsInput.text.toString().toIntOrNull() ?: 0,
                            targetLikesInput.text.toString().toIntOrNull() ?: 0,
                            targetSharesInput.text.toString().toIntOrNull() ?: 0,
                            targetClicksInput.text.toString().toIntOrNull() ?: 0,
                            linkUrl, isDraft = false)
                    }
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
        targetClicks: Int = 0,
        linkUrl: String = "",
        isDraft: Boolean = false
    ) {
        lifecycleScope.launch {
            val totalCost = reward * capacity
            if (!isDraft && totalCost > totalBudget) {
                Toast.makeText(this@AdvertiserDashboardActivity,
                    "Insufficient balance!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val videoUrl = if (type == TaskType.WATCH_VIDEO) linkUrl else null
            val targetUrl = if (type == TaskType.VISIT_SITE || type == TaskType.SURVEY) linkUrl else null
            val socialMediaLink = if (type in listOf(TaskType.LIKE_CONTENT, TaskType.SHARE_POST,
                    TaskType.FOLLOW_ACCOUNT, TaskType.COMMENT)) linkUrl else null

            val taskStatus = if (isDraft) TaskStatus.PENDING else TaskStatus.ACTIVE

            val task = Task(
                advertiserId = currentAdvertiserId,
                title = title,
                description = description,
                taskType = type,
                rewardCoins = reward,
                totalCapacity = capacity,
                completedCount = 0,
                status = taskStatus,
                expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
                completionTimeSeconds = completionTimeSeconds,
                targetViews = targetViews,
                targetLikes = targetLikes,
                targetShares = targetShares,
                targetClicks = targetClicks,
                currentViews = 0,
                currentLikes = 0,
                currentShares = 0,
                currentClicks = 0,
                videoUrl = videoUrl,
                targetUrl = targetUrl,
                socialMediaLink = socialMediaLink
            )

            database.taskDao().insertTask(task)

            if (!isDraft) {
                database.userDao().deductCoins(currentAdvertiserId, totalCost)
            }

            database.activityLogDao().insertLog(
                ActivityLog(
                    userId = currentAdvertiserId,
                    action = if (isDraft) "TASK_SAVED_DRAFT" else "TASK_CREATED",
                    details = "${if (isDraft) "Saved draft" else "Created"} task: $title, reward: $reward, type: $type, link: ${linkUrl.ifEmpty { "none" }}"
                )
            )

            Toast.makeText(this@AdvertiserDashboardActivity,
                if (isDraft) "Task saved as draft!" else "Task created successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAnalyticsDialog() {
        lifecycleScope.launch {
            database.campaignDao().getCampaignsByAdvertiser(currentAdvertiserId).collectLatest { campaignList ->
                database.taskDao().getTasksByAdvertiser(currentAdvertiserId).collectLatest { taskList ->
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
                        .show()
                }
            }
        }
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

    private fun showAdvertiserSettingsDialog() {
        val settings = arrayOf("👤 View Profile", "💳 Payment Details", "🔐 Change Password", "📊 Campaign Statistics", "ℹ️ About CoinTask")

        AlertDialog.Builder(this)
            .setTitle("Advertiser Settings")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> showAdvertiserProfile()
                    1 -> showPaymentDetails()
                    2 -> showChangePasswordDialog()
                    3 -> showCampaignStatistics()
                    4 -> showAboutDialog()
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
                    💵 Total Budget: 🪙$totalBudget
                    💸 Spent: 🪙$spentAmount
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

                    Status: ${if (advertiser.paymentVerified) "✅ Verified" else "⏳ Pending Admin Approval"}
                """.trimIndent()

                AlertDialog.Builder(this@AdvertiserDashboardActivity)
                    .setTitle("Payment Details")
                    .setMessage(paymentDetails)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
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
                    Toast.makeText(this@AdvertiserDashboardActivity, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this@AdvertiserDashboardActivity, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 8 || !newPassword.matches(Regex(".*[A-Z].*")) || !newPassword.matches(Regex(".*[0-9].*"))) {
                    Toast.makeText(this@AdvertiserDashboardActivity,
                        "Password must be 8+ chars with uppercase and number", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val user = database.userDao().getUserByIdSuspend(currentAdvertiserId)
                    if (user != null && PasswordUtils.checkPassword(currentPassword, user.password)) {
                        val hashedPassword = PasswordUtils.hashPassword(newPassword)
                        database.userDao().updateUser(user.copy(password = hashedPassword))
                        Toast.makeText(this@AdvertiserDashboardActivity, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AdvertiserDashboardActivity, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCampaignStatistics() {
        lifecycleScope.launch {
            database.campaignDao().getCampaignsByAdvertiser(currentAdvertiserId).collectLatest { campaigns ->
                val stats = """
                    📊 Campaign Statistics

                    Total Campaigns: ${campaigns.size}
                    Active: ${campaigns.count { it.status == CampaignStatus.ACTIVE }}
                    Paused: ${campaigns.count { it.status == CampaignStatus.PAUSED }}
                    Completed: ${campaigns.count { it.status == CampaignStatus.COMPLETED }}

                    Total Tasks: ${campaigns.sumOf { it.totalTasks }}
                    Completed Tasks: ${campaigns.sumOf { it.completedTasks }}
                """.trimIndent()

                AlertDialog.Builder(this@AdvertiserDashboardActivity)
                    .setTitle("Campaign Statistics")
                    .setMessage(stats)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showAboutDialog() {
        val aboutText = """
            💰 CoinTask - Task Rewards Platform
            Version: 2.0.0

            Create campaigns and tasks to engage users!
            © 2026-2031 CoinTask. All rights reserved.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About CoinTask")
            .setMessage(aboutText)
            .setPositiveButton("OK", null)
            .show()
    }
}
