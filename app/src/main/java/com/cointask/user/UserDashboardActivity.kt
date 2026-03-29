package com.cointask.user

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.Task
import com.cointask.data.models.TaskFilter
import com.cointask.data.models.TaskStatus
import com.cointask.data.models.Transaction
import com.cointask.data.models.TransactionStatus
import com.cointask.data.models.TransactionType
import com.cointask.databinding.ActivityUserDashboardBinding
import com.cointask.user.adapters.TaskAdapter
import com.cointask.user.fragments.TaskFilterBottomSheet
import com.cointask.utils.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserDashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var taskAdapter: TaskAdapter
    private var currentUserId = -1
    private var currentCoins = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        currentUserId = preferencesManager.getUserId()
        
        setupUI()
        loadUserData()
        setupTaskRecyclerView()
        setupClickListeners()
        insertSampleTasks()
    }
    
    private fun setupUI() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserData()
            loadTasks()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    
    private fun animateCoinCounter(start: Int, end: Int) {
        val animator = ValueAnimator.ofInt(start, end)
        animator.duration = 1000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            binding.tvCoins.text = animation.animatedValue.toString()
        }
        animator.start()
    }
    
    private fun loadUserData() {
        lifecycleScope.launch {
            database.userDao().getUserById(currentUserId).collectLatest { user ->
                user?.let {
                    currentCoins = it.coins
                    animateCoinCounter(currentCoins, it.coins)
                    binding.tvUsername.text = it.fullName
                    binding.progressLevel.progress = (it.coins / 1000).coerceAtMost(100)
                }
            }
        }
    }
    
    private fun setupTaskRecyclerView() {
        taskAdapter = TaskAdapter { task ->
            showTaskCompletionDialog(task)
        }
        
        binding.recyclerTasks.apply {
            layoutManager = LinearLayoutManager(this@UserDashboardActivity)
            adapter = taskAdapter
        }
        
        loadTasks()
    }
    
    private fun loadTasks(filter: TaskFilter? = null) {
        lifecycleScope.launch {
            val tasks = if (filter != null) {
                database.taskDao().getFilteredTasks(
                    type = filter.type,
                    minReward = filter.minReward,
                    status = TaskStatus.ACTIVE
                )
            } else {
                database.taskDao().getActiveTasks()
            }
            
            tasks.collectLatest { taskList ->
                taskAdapter.submitList(taskList)
                binding.tvTaskCount.text = "${taskList.size} available tasks"
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnFilter.setOnClickListener {
            TaskFilterBottomSheet { filter ->
                loadTasks(filter)
            }.show(supportFragmentManager, "task_filter")
        }
        
        binding.btnWithdraw.setOnClickListener {
            showWithdrawalDialog()
        }
        
        binding.btnHistory.setOnClickListener {
            showTransactionHistory()
        }
    }
    
    private fun showTaskCompletionDialog(task: Task) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(task.title)
            .setMessage("${task.description}\n\nReward: ${task.rewardCoins} coins\n\nComplete this task to earn coins!")
            .setPositiveButton("Complete Task") { _, _ ->
                completeTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun completeTask(task: Task) {
        lifecycleScope.launch {
            try {
                // Check if task is still available
                if (task.completedCount >= task.totalCapacity) {
                    Toast.makeText(this@UserDashboardActivity, 
                        "Task capacity reached!", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Verify task completion
                val isCompleted = verifyTaskCompletion(task)
                
                if (isCompleted) {
                    // Update task completion count
                    val updatedTask = task.copy(completedCount = task.completedCount + 1)
                    database.taskDao().updateTask(updatedTask)
                    
                    // Add coins to user
                    database.userDao().addCoins(currentUserId, task.rewardCoins)
                    
                    // Create transaction record
                    val transaction = Transaction(
                        userId = currentUserId,
                        type = TransactionType.EARNED_FROM_TASK,
                        amount = task.rewardCoins,
                        taskId = task.id,
                        description = "Completed task: ${task.title}",
                        status = TransactionStatus.COMPLETED
                    )
                    database.transactionDao().insertTransaction(transaction)
                    
                    // Log activity
                    logActivity("completed_task", "Completed task ${task.id} and earned ${task.rewardCoins} coins")
                    
                    Toast.makeText(this@UserDashboardActivity, 
                        "🎉 Task completed! +${task.rewardCoins} coins", Toast.LENGTH_LONG).show()
                    
                    // Refresh data
                    loadUserData()
                    loadTasks()
                } else {
                    Toast.makeText(this@UserDashboardActivity, 
                        "Task verification failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, 
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun verifyTaskCompletion(task: Task): Boolean {
        // In a real app, this would involve actual verification
        // For demo, we'll simulate verification
        return when (task.taskType) {
            com.cointask.data.models.TaskType.WATCH_VIDEO -> {
                // Simulate video watch
                Toast.makeText(this, "Video watched!", Toast.LENGTH_SHORT).show()
                true
            }
            com.cointask.data.models.TaskType.VISIT_SITE -> {
                // Simulate site visit
                Toast.makeText(this, "Website visited!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> true
        }
    }
    
    private fun showWithdrawalDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Withdraw Coins")
            .setMessage("Current balance: $currentCoins coins\n\nMinimum withdrawal: 1000 coins")
            .setPositiveButton("Request Withdrawal") { _, _ ->
                if (currentCoins >= 1000) {
                    lifecycleScope.launch {
                        val transaction = Transaction(
                            userId = currentUserId,
                            type = TransactionType.WITHDRAWAL,
                            amount = currentCoins,
                            description = "Withdrawal request",
                            status = TransactionStatus.PENDING
                        )
                        database.transactionDao().insertTransaction(transaction)
                        Toast.makeText(this@UserDashboardActivity, 
                            "Withdrawal request submitted!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showTransactionHistory() {
        lifecycleScope.launch {
            val transactions = database.transactionDao().getTransactionsByUser(currentUserId)
            val history = StringBuilder()
            transactions.collectLatest { list ->
                list.take(10).forEach { tx ->
                    history.append("${java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(tx.timestamp))}: ${tx.description} - ${tx.amount} coins\n")
                }
            }
            
            androidx.appcompat.app.AlertDialog.Builder(this@UserDashboardActivity)
                .setTitle("Transaction History")
                .setMessage(history.toString().ifEmpty { "No transactions yet" })
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    private suspend fun logActivity(action: String, details: String) {
        val log = com.cointask.data.models.ActivityLog(
            userId = currentUserId,
            action = action,
            details = details
        )
        database.activityLogDao().insertLog(log)
    }
    
    private fun insertSampleTasks() {
        lifecycleScope.launch {
            val existingTasks = database.taskDao().getActiveTasks()
            var hasTasks = false
            existingTasks.collectLatest { tasks ->
                if (tasks.isNotEmpty()) hasTasks = true
            }
            
            if (!hasTasks) {
                val sampleTasks = listOf(
                    Task(
                        advertiserId = 1,
                        title = "Watch Introduction Video",
                        description = "Watch our 2-minute introduction video about the platform",
                        taskType = com.cointask.data.models.TaskType.WATCH_VIDEO,
                        rewardCoins = 50,
                        totalCapacity = 100,
                        expiresAt = System.currentTimeMillis() + 7 * 24 * 3600000,
                        videoUrl = "https://example.com/video"
                    ),
                    Task(
                        advertiserId = 1,
                        title = "Visit Partner Website",
                        description = "Visit our partner's website and stay for 30 seconds",
                        taskType = com.cointask.data.models.TaskType.VISIT_SITE,
                        rewardCoins = 30,
                        totalCapacity = 200,
                        expiresAt = System.currentTimeMillis() + 7 * 24 * 3600000,
                        targetUrl = "https://example.com"
                    ),
                    Task(
                        advertiserId = 1,
                        title = "Share on Social Media",
                        description = "Share our post on your social media",
                        taskType = com.cointask.data.models.TaskType.SHARE_POST,
                        rewardCoins = 75,
                        totalCapacity = 50,
                        expiresAt = System.currentTimeMillis() + 5 * 24 * 3600000,
                        socialMediaLink = "https://example.com/share"
                    ),
                    Task(
                        advertiserId = 1,
                        title = "Like Our Page",
                        description = "Like our Facebook page",
                        taskType = com.cointask.data.models.TaskType.LIKE_CONTENT,
                        rewardCoins = 25,
                        totalCapacity = 300,
                        expiresAt = System.currentTimeMillis() + 10 * 24 * 3600000
                    ),
                    Task(
                        advertiserId = 1,
                        title = "Premium Survey",
                        description = "Complete a 5-minute survey about your preferences",
                        taskType = com.cointask.data.models.TaskType.SURVEY,
                        rewardCoins = 150,
                        totalCapacity = 30,
                        expiresAt = System.currentTimeMillis() + 3 * 24 * 3600000
                    )
                )
                
                sampleTasks.forEach { task ->
                    database.taskDao().insertTask(task.copy(status = com.cointask.data.models.TaskStatus.ACTIVE))
                }
            }
        }
    }
}
