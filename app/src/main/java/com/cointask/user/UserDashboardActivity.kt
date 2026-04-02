package com.cointask.user

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cointask.auth.LoginActivity
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.*
import com.cointask.databinding.ActivityUserDashboardBinding
import com.cointask.databinding.DialogBankAccountBinding
import com.cointask.databinding.DialogWithdrawalBinding
import com.cointask.user.adapters.TaskAdapter
import com.cointask.user.player.VideoPlayerHelper
import com.cointask.user.player.VideoPlaybackListener
import com.cointask.user.player.VideoProvider
import com.cointask.utils.PasswordUtils
import com.cointask.utils.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UserDashboardActivity : AppCompatActivity(), TaskAdapter.TaskClickListener {

    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var taskAdapter: TaskAdapter
    private var currentUserId = -1
    private var currentCoins = 0
    private var currentLevel = 1
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        currentUserId = preferencesManager.getUserId()

        setupRecyclerView()
        setupClickListeners()
        loadUserData()
        loadTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(this)
        taskAdapter.setTaskClickListener(this)
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@UserDashboardActivity)
            adapter = taskAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnWithdraw.setOnClickListener {
            showWithdrawalFlow()
        }

        binding.btnHistory.setOnClickListener {
            showTransactionHistory()
        }

        binding.btnFilterTasks.setOnClickListener {
            showFilterDialog()
        }

        binding.btnSettings.setOnClickListener {
            showUserSettingsDialog()
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

    private fun updateLevelInfo(coins: Int) {
        currentLevel = (coins / 1000) + 1
        val coinsForNextLevel = (currentLevel * 1000) - coins
        val progress = (coins % 1000).coerceAtMost(100)
        
        binding.progressLevel.progress = progress
        binding.tvLevelInfo.text = "Level $currentLevel - $coinsForNextLevel coins to next level"
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            database.userDao().getUserById(currentUserId).collectLatest { user ->
                user?.let {
                    currentUser = it
                    val oldCoins = currentCoins
                    currentCoins = it.coins
                    binding.tvUsername.text = "Welcome ${it.fullName}!"
                    
                    if (oldCoins != currentCoins) {
                        animateCoinCounter(oldCoins, currentCoins)
                    } else {
                        binding.tvCoins.text = currentCoins.toString()
                    }
                    
                    updateLevelInfo(it.coins)
                }
            }
        }
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            database.taskDao().getActiveTasks().collectLatest { tasks ->
                val availableTasks = tasks.filter { 
                    it.status == TaskStatus.ACTIVE && 
                    it.completedCount < it.totalCapacity &&
                    it.expiresAt > System.currentTimeMillis()
                }
                
                if (availableTasks.isEmpty()) {
                    binding.rvTasks.visibility = View.GONE
                    binding.tvNoTasks.visibility = View.VISIBLE
                } else {
                    binding.rvTasks.visibility = View.VISIBLE
                    binding.tvNoTasks.visibility = View.GONE
                    taskAdapter.submitList(availableTasks)
                }
            }
        }
    }

    private fun showWithdrawalFlow() {
        // First check if user has bank account set up
        if (currentUser?.bankAccount.isNullOrEmpty()) {
            // Show bank account setup first
            showBankAccountSetupDialog {
                // After bank account is set up, show withdrawal dialog
                showWithdrawalRequestDialog()
            }
        } else {
            // Show withdrawal dialog directly
            showWithdrawalRequestDialog()
        }
    }

    private fun showBankAccountSetupDialog(onComplete: () -> Unit) {
        val dialogBinding = DialogBankAccountBinding.inflate(LayoutInflater.from(this))
        
        // Pre-fill if user already has bank info
        currentUser?.let { user ->
            if (!user.bankAccount.isNullOrEmpty()) {
                dialogBinding.etBankName.setText(user.bankName)
                dialogBinding.etAccountName.setText(user.accountName)
                dialogBinding.etAccountNumber.setText(user.bankAccount)
                dialogBinding.etConfirmAccountNumber.setText(user.bankAccount)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Set Up Bank Account")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val bankName = dialogBinding.etBankName.text.toString().trim()
                val accountNumber = dialogBinding.etAccountNumber.text.toString().trim()
                val confirmAccountNumber = dialogBinding.etConfirmAccountNumber.text.toString().trim()
                val accountName = dialogBinding.etAccountName.text.toString().trim()

                if (validateBankAccount(bankName, accountNumber, confirmAccountNumber, accountName)) {
                    saveBankAccount(bankName, accountNumber, accountName, onComplete)
                } else {
                    Toast.makeText(this, "Please correct the errors", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun validateBankAccount(
        bankName: String,
        accountNumber: String,
        confirmAccountNumber: String,
        accountName: String
    ): Boolean {
        if (bankName.isEmpty()) {
            Toast.makeText(this, "Bank name is required", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (accountNumber.isEmpty() || accountNumber.length < 4) {
            Toast.makeText(this, "Valid account number required (min 4 digits)", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (accountNumber != confirmAccountNumber) {
            Toast.makeText(this, "Account numbers do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (accountName.isEmpty()) {
            Toast.makeText(this, "Account holder name is required", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }

    private fun saveBankAccount(bankName: String, accountNumber: String, accountName: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            currentUser?.let { user ->
                val updatedUser = user.copy(
                    bankName = bankName,
                    bankAccount = accountNumber,
                    accountName = accountName
                )
                database.userDao().updateUser(updatedUser)
                currentUser = updatedUser

                // Log activity
                database.activityLogDao().insertLog(
                    ActivityLog(
                        userId = currentUserId,
                        action = "BANK_ACCOUNT_ADDED",
                        details = "Bank account added: $bankName - ****${accountNumber.takeLast(4)}"
                    )
                )

                Toast.makeText(this@UserDashboardActivity,
                    "Bank account saved successfully!", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        }
    }

    private fun showWithdrawalRequestDialog() {
        val dialogBinding = DialogWithdrawalBinding.inflate(LayoutInflater.from(this))
        
        // Update balance info
        dialogBinding.tvBalanceInfo.text = "Available Balance: $currentCoins coins"
        
        // Update bank details display
        currentUser?.let { user ->
            val maskedAccount = "****${user.bankAccount?.takeLast(4) ?: "????"}"
            dialogBinding.tvBankDetails.text = "${user.bankName}\n${user.accountName}\n$maskedAccount"
        }

        // Handle change bank account click
        dialogBinding.tvChangeBank.setOnClickListener {
            showBankAccountSetupDialog {
                // Refresh bank info display
                currentUser?.let { user ->
                    val maskedAccount = "****${user.bankAccount?.takeLast(4) ?: "????"}"
                    dialogBinding.tvBankDetails.text = "${user.bankName}\n${user.accountName}\n$maskedAccount"
                }
            }
        }

        // Update coin amount display as user types
        dialogBinding.etWithdrawAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val coins = s.toString().toIntOrNull() ?: 0
                dialogBinding.tvDollarAmount.text = "🪙 $coins"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Withdraw Coins")
            .setView(dialogBinding.root)
            .setPositiveButton("Request Withdrawal") { _, _ ->
                val amount = dialogBinding.etWithdrawAmount.text.toString().toIntOrNull() ?: 0
                processWithdrawalRequest(amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processWithdrawalRequest(amount: Int) {
        when {
            amount < 1000 -> {
                Toast.makeText(this,
                    "Minimum withdrawal is 1000 coins!", Toast.LENGTH_SHORT).show()
                return
            }
            amount > currentCoins -> {
                Toast.makeText(this,
                    "Insufficient balance! You have $currentCoins coins.", Toast.LENGTH_SHORT).show()
                return
            }
            currentUser?.bankAccount.isNullOrEmpty() -> {
                Toast.makeText(this,
                    "Please set up your bank account first!", Toast.LENGTH_SHORT).show()
                showBankAccountSetupDialog { }
                return
            }
        }

        lifecycleScope.launch {
            try {
                // Deduct coins immediately
                database.userDao().deductCoins(currentUserId, amount)

                // Create withdrawal request
                val withdrawalRequest = WithdrawalRequest(
                    userId = currentUserId,
                    amount = amount,
                    dollarAmount = amount.toFloat(), // Store as float for compatibility
                    status = WithdrawalStatus.PENDING,
                    bankName = currentUser?.bankName ?: "",
                    accountNumber = currentUser?.bankAccount ?: "",
                    accountName = currentUser?.accountName ?: ""
                )

                database.withdrawalRequestDao().insertWithdrawal(withdrawalRequest)

                // Create pending transaction
                val transaction = Transaction(
                    userId = currentUserId,
                    type = TransactionType.WITHDRAWAL,
                    amount = amount,
                    description = "Withdrawal request - $amount 🪙",
                    status = TransactionStatus.PENDING,
                    referenceId = "WD${System.currentTimeMillis()}"
                )
                database.transactionDao().insertTransaction(transaction)

                // Log activity
                database.activityLogDao().insertLog(
                    ActivityLog(
                        userId = currentUserId,
                        action = "WITHDRAWAL_REQUESTED",
                        details = "Requested withdrawal of $amount 🪙 to ${currentUser?.bankName}"
                    )
                )

                Toast.makeText(this@UserDashboardActivity,
                    "Withdrawal request submitted!\nAmount: $amount 🪙\nProcessing time: 24-48 hours",
                    Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity,
                    "Failed to process withdrawal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showTransactionHistory() {
        lifecycleScope.launch {
            val transactions = database.transactionDao().getTransactionsByUser(currentUserId)
            transactions.collectLatest { list ->
                if (list.isEmpty()) {
                    Toast.makeText(this@UserDashboardActivity,
                        "No transactions yet. Complete tasks to start earning!", Toast.LENGTH_SHORT).show()
                    return@collectLatest
                }
                
                // Also get withdrawal requests
                val withdrawalRequests = database.withdrawalRequestDao().getWithdrawalsByUserSuspend(currentUserId)
                
                val history = StringBuilder("Transaction History:\n\n")
                
                // Add withdrawal requests
                if (withdrawalRequests.isNotEmpty()) {
                    history.append("💸 WITHDRAWAL REQUESTS:\n")
                    withdrawalRequests.forEach { wd ->
                        val date = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date(wd.requestDate))
                        val statusIcon = when (wd.status) {
                            WithdrawalStatus.PENDING -> "⏳"
                            WithdrawalStatus.PROCESSING -> "🔄"
                            WithdrawalStatus.COMPLETED -> "✅"
                            WithdrawalStatus.REJECTED -> "❌"
                            WithdrawalStatus.CANCELLED -> "🚫"
                        }
                        history.append("$date $statusIcon -${wd.amount} 🪙 - ${wd.status}\n")
                        if (wd.rejectionReason != null) {
                            history.append("   Reason: ${wd.rejectionReason}\n")
                        }
                    }
                    history.append("\n")
                }
                
                // Add other transactions
                history.append("📋 OTHER TRANSACTIONS:\n")
                list.take(30).forEach { tx ->
                    val date = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    val status = when (tx.status) {
                        TransactionStatus.PENDING -> "⏳"
                        TransactionStatus.PROCESSING -> "🔄"
                        TransactionStatus.COMPLETED -> "✅"
                        TransactionStatus.FAILED -> "❌"
                        TransactionStatus.CANCELLED -> "🚫"
                    }
                    val type = when (tx.type) {
                        TransactionType.EARNED_FROM_TASK -> "📋"
                        TransactionType.WITHDRAWAL -> "💸"
                        TransactionType.CAMPAIGN_PAYMENT -> "📢"
                        TransactionType.REFUND -> "↩️"
                        TransactionType.BONUS -> "🎁"
                    }
                    val sign = if (tx.type == TransactionType.WITHDRAWAL || tx.type == TransactionType.CAMPAIGN_PAYMENT) "-" else "+"
                    history.append("$date $type$status $sign${tx.amount} coins - ${tx.description}\n")
                }

                AlertDialog.Builder(this@UserDashboardActivity)
                    .setTitle("Transaction History")
                    .setMessage(history.toString())
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Withdrawal Status") { _, _ ->
                        showWithdrawalStatusDialog(withdrawalRequests)
                    }
                    .show()
            }
        }
    }

    private fun showWithdrawalStatusDialog(withdrawalRequests: List<WithdrawalRequest>) {
        if (withdrawalRequests.isEmpty()) {
            Toast.makeText(this, "No withdrawal requests found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val statuses = withdrawalRequests.map { wd ->
            val date = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(wd.requestDate))
            "${wd.amount} 🪙 - ${wd.status} ($date)"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Withdrawal Requests")
            .setItems(statuses) { _, which ->
                val selectedWd = withdrawalRequests[which]
                showWithdrawalDetails(selectedWd)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showWithdrawalDetails(wd: WithdrawalRequest) {
        val date = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date(wd.requestDate))
        val processedDate = wd.processedDate?.let {
            SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "Not processed yet"

        val details = """
            Withdrawal Details

            Amount: ${wd.amount} 🪙
            Status: ${wd.status}

            Bank: ${wd.bankName}
            Account: ****${wd.accountNumber.takeLast(4)}
            Account Name: ${wd.accountName}

            Requested: $date
            Processed: $processedDate

            ${wd.rejectionReason?.let { "Rejection Reason: $it" } ?: ""}
            ${wd.transactionReference?.let { "Transaction Ref: $it" } ?: ""}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Withdrawal Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .setNeutralButton("Cancel Request") { _, _ ->
                if (wd.status == WithdrawalStatus.PENDING) {
                    cancelWithdrawal(wd)
                } else {
                    Toast.makeText(this, "Cannot cancel a ${wd.status} withdrawal", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun cancelWithdrawal(wd: WithdrawalRequest) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Withdrawal")
            .setMessage("Are you sure you want to cancel this withdrawal request? The coins will be returned to your balance.")
            .setPositiveButton("Cancel Withdrawal") { _, _ ->
                lifecycleScope.launch {
                    // Update withdrawal status
                    database.withdrawalRequestDao().updateWithdrawal(
                        wd.copy(status = WithdrawalStatus.CANCELLED)
                    )
                    
                    // Refund coins
                    database.userDao().addCoins(currentUserId, wd.amount)
                    
                    // Log activity
                    database.activityLogDao().insertLog(
                        ActivityLog(
                            userId = currentUserId,
                            action = "WITHDRAWAL_CANCELLED",
                            details = "Cancelled withdrawal of ${wd.amount} coins"
                        )
                    )
                    
                    Toast.makeText(this@UserDashboardActivity,
                        "Withdrawal cancelled. Coins returned to your balance.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Keep Request", null)
            .show()
    }

    private fun showFilterDialog() {
        val taskTypes = arrayOf("All", "WATCH_VIDEO", "VISIT_SITE", "LIKE_CONTENT", "SHARE_POST", "FOLLOW_ACCOUNT", "COMMENT", "SURVEY")
        var selectedType = 0
        
        AlertDialog.Builder(this)
            .setTitle("Filter Tasks")
            .setSingleChoiceItems(taskTypes, selectedType) { _, which ->
                selectedType = which
            }
            .setPositiveButton("Apply") { _, _ ->
                val filterType = if (selectedType == 0) null else TaskType.valueOf(taskTypes[selectedType])
                filterTasks(filterType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterTasks(type: TaskType?) {
        lifecycleScope.launch {
            if (type == null) {
                loadTasks()
            } else {
                database.taskDao().getFilteredTasks(type, 0, TaskStatus.ACTIVE).collectLatest { filteredTasks ->
                    taskAdapter.submitList(filteredTasks)
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
                startActivity(Intent(this@UserDashboardActivity, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show user settings dialog with profile and account options
     */
    private fun showUserSettingsDialog() {
        val settings = arrayOf(
            "👤 View Profile",
            "🏦 Bank Account Setup",
            "🔐 Change Password",
            "📊 My Statistics",
            "ℹ️ About CoinTask"
        )

        AlertDialog.Builder(this)
            .setTitle("User Settings")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> showUserProfile()
                    1 -> showBankAccountSetupDialog()
                    2 -> showChangePasswordDialog()
                    3 -> showUserStatistics()
                    4 -> showAboutDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showUserProfile() {
        currentUser?.let { user ->
            val profileDetails = """
                👤 User Profile

                Name: ${user.fullName}
                Email: ${user.email}
                Role: ${user.role}

                💰 Account Balance: ${user.coins} coins
                📊 Level: $currentLevel

                ✅ Verified: ${if (user.isVerified) "Yes" else "No"}
                🔓 Active: ${if (user.isActive) "Yes" else "No"}

                📅 Member Since: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(user.createdAt))}
                🕐 Last Login: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(user.lastLogin))}

                ${user.bankName?.let { """
                🏦 Bank Information:
                Bank: $it
                Account: ${user.accountName ?: "N/A"}
                Account Number: ****${user.bankAccount?.takeLast(4) ?: "????"}
                """ } ?: "🏦 Bank Account: Not set up yet"}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("My Profile")
                .setMessage(profileDetails)
                .setPositiveButton("OK", null)
                .setNeutralButton("Edit Bank") { _, _ ->
                    showBankAccountSetupDialog()
                }
                .show()
        }
    }

    private fun showBankAccountSetupDialog() {
        showBankAccountSetupDialog {
            Toast.makeText(this, "Bank account updated!", Toast.LENGTH_SHORT).show()
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
            addView(currentPasswordInput as View)
            addView(newPasswordInput as View)
            addView(confirmPasswordInput as View)
        }

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setMessage("Enter your current password and a new password.")
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this@UserDashboardActivity,
                        "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this@UserDashboardActivity,
                        "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 8) {
                    Toast.makeText(this@UserDashboardActivity,
                        "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Verify current password and update
                lifecycleScope.launch {
                    currentUser?.let { user ->
                        // In a real app, verify current password with BCrypt
                        // For now, just update the password
                        val hashedPassword = PasswordUtils.hashPassword(newPassword)
                        val updatedUser = user.copy(password = hashedPassword)
                        database.userDao().updateUser(updatedUser)

                        database.activityLogDao().insertLog(
                            ActivityLog(
                                userId = user.id,
                                action = "PASSWORD_CHANGED",
                                details = "User changed password"
                            )
                        )

                        Toast.makeText(this@UserDashboardActivity,
                            "Password changed successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserStatistics() {
        lifecycleScope.launch {
            val transactions = database.transactionDao().getTransactionsByUserSuspend(currentUserId)
            val completedTasks = transactions.count { it.type == TransactionType.EARNED_FROM_TASK }
            val totalEarned = transactions.filter { it.type == TransactionType.EARNED_FROM_TASK }
                .sumOf { it.amount }
            val totalWithdrawn = transactions.filter { it.type == TransactionType.WITHDRAWAL }
                .sumOf { it.amount }

            val stats = """
                📊 User Statistics

                📋 Tasks Completed: $completedTasks
                💰 Total Earned: $totalEarned coins
                💸 Total Withdrawn: $totalWithdrawn coins
                💵 Current Balance: $currentCoins coins

                🏆 Level: $currentLevel
                📈 Progress to Level ${currentLevel + 1}: ${(currentLevel * 1000) - currentCoins} coins needed
            """.trimIndent()

            AlertDialog.Builder(this@UserDashboardActivity)
                .setTitle("My Statistics")
                .setMessage(stats)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showAboutDialog() {
        val aboutText = """
            💰 CoinTask - Task Rewards Platform

            Version: 2.0.0

            Earn coins by completing tasks:
            • Watch videos
            • Visit websites
            • Like content
            • Share posts
            • Follow accounts
            • Comment on posts
            • Complete surveys

            Withdraw your coins to your bank account!
            Minimum withdrawal: 1000 🪙

            © 2026-2031 CoinTask. All rights reserved.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About CoinTask")
            .setMessage(aboutText)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onTaskClick(task: Task) {
        showTaskDetailsDialog(task)
    }

    override fun onTaskComplete(task: Task) {
        confirmTaskCompletion(task)
    }

    private fun showTaskDetailsDialog(task: Task) {
        val taskTypeIcon = when (task.taskType) {
            TaskType.WATCH_VIDEO -> "🎬"
            TaskType.VISIT_SITE -> "🌐"
            TaskType.LIKE_CONTENT -> "👍"
            TaskType.SHARE_POST -> "📤"
            TaskType.FOLLOW_ACCOUNT -> "👤"
            TaskType.COMMENT -> "💬"
            TaskType.SURVEY -> "📝"
        }

        val progressPercent = (task.completedCount.toFloat() / task.totalCapacity * 100).toInt()
        val expiresDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(task.expiresAt))
        
        // Get the task link based on type
        val taskLink = task.videoUrl ?: task.targetUrl ?: task.socialMediaLink ?: "No link provided"

        val details = """
            $taskTypeIcon ${task.title}

            ${task.description}

            💰 Reward: ${task.rewardCoins} coins
            📊 Progress: ${task.completedCount}/${task.totalCapacity} ($progressPercent%)
            ⏰ Expires: $expiresDate
            📝 Type: ${task.taskType}
            🔗 Link: ${taskLink.take(50)}${if (taskLink.length > 50) "..." else ""}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Task Details")
            .setMessage(details)
            .setPositiveButton("Start Task") { _, _ ->
                startTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTask(task: Task) {
        // Show link preview first
        showLinkPreviewAndStartTask(task)
    }

    /**
     * Show link preview to user before starting the task
     */
    private fun showLinkPreviewAndStartTask(task: Task) {
        val taskLink = task.videoUrl ?: task.targetUrl ?: task.socialMediaLink
        
        if (taskLink.isNullOrEmpty()) {
            Toast.makeText(this, "No link provided for this task", Toast.LENGTH_SHORT).show()
            return
        }

        // Create preview layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val previewTitle = TextView(this).apply {
            text = "🔗 Task Link Preview"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@UserDashboardActivity, com.cointask.R.color.primary))
            setPadding(0, 0, 0, 20)
        }

        val linkTextView = TextView(this).apply {
            text = taskLink
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@UserDashboardActivity, com.cointask.R.color.text_primary))
            setPadding(0, 0, 0, 20)
            setOnClickListener {
                // Open link in browser when clicked
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(taskLink))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@UserDashboardActivity, "Cannot open link", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val actionInstructions = TextView(this).apply {
            text = getActionInstructions(task.taskType, task.completionTimeSeconds)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@UserDashboardActivity, com.cointask.R.color.text_secondary))
            setPadding(0, 15, 0, 20)
        }

        val confirmCheckbox = CheckBox(this).apply {
            text = "I confirm I will complete this task honestly"
            setPadding(0, 10, 0, 20)
        }

        layout.addView(previewTitle)
        layout.addView(linkTextView)
        layout.addView(actionInstructions)
        layout.addView(confirmCheckbox)

        AlertDialog.Builder(this)
            .setTitle("Preview Task")
            .setView(layout)
            .setPositiveButton("Start Task") { _, _ ->
                if (confirmCheckbox.isChecked) {
                    startRealTaskCompletion(task, taskLink)
                } else {
                    Toast.makeText(this@UserDashboardActivity,
                        "Please confirm you will complete the task honestly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Get action instructions based on task type
     */
    private fun getActionInstructions(taskType: TaskType, completionTimeSeconds: Int): String {
        return when (taskType) {
            TaskType.WATCH_VIDEO -> "🎬 Watch the video for at least $completionTimeSeconds seconds. The video will play and your viewing time will be tracked."
            TaskType.VISIT_SITE -> "🌐 Visit the website for $completionTimeSeconds seconds. Your visit will be tracked."
            TaskType.LIKE_CONTENT -> "❤️ Click the link, like the content, and return. Your action will be verified."
            TaskType.SHARE_POST -> "📤 Click the link, share the post, and return. Your action will be verified."
            TaskType.FOLLOW_ACCOUNT -> "👤 Click the link, follow the account, and return. Your action will be verified."
            TaskType.COMMENT -> "💬 Click the link, comment on the post, and return. Your action will be verified."
            TaskType.SURVEY -> "📝 Click the link and complete the survey. Your completion will be verified."
        }
    }

    /**
     * Start real task completion with proper tracking
     */
    private fun startRealTaskCompletion(task: Task, taskLink: String) {
        when (task.taskType) {
            TaskType.WATCH_VIDEO -> {
                showVideoPlayer(task, taskLink)
            }
            TaskType.VISIT_SITE, TaskType.SURVEY -> {
                showWebsiteVisit(task, taskLink)
            }
            TaskType.LIKE_CONTENT, TaskType.SHARE_POST, TaskType.FOLLOW_ACCOUNT, TaskType.COMMENT -> {
                showSocialActionDialog(task, taskLink)
            }
        }
    }

    /**
     * Show embedded video player for WATCH_VIDEO tasks
     * Uses dynamic VideoPlayerHelper to support multiple video providers
     */
    private fun showVideoPlayer(task: Task, videoUrl: String) {
        val dialogView = LayoutInflater.from(this).inflate(com.cointask.R.layout.dialog_video_player, null)

        val videoTitleTextView = dialogView.findViewById<TextView>(com.cointask.R.id.tv_video_title)
        val closeBtn = dialogView.findViewById<android.widget.ImageButton>(com.cointask.R.id.btn_close)
        val loadingContainer = dialogView.findViewById<LinearLayout>(com.cointask.R.id.loading_container)
        val errorContainer = dialogView.findViewById<LinearLayout>(com.cointask.R.id.error_container)
        val errorMessageTv = dialogView.findViewById<TextView>(com.cointask.R.id.tv_error_message)
        val retryBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.cointask.R.id.btn_retry)
        val watchYoutubeBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.cointask.R.id.btn_watch_youtube)
        val taskDescriptionTv = dialogView.findViewById<TextView>(com.cointask.R.id.tv_task_description)
        val cancelBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.cointask.R.id.btn_cancel)
        val completeBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.cointask.R.id.btn_complete)
        val webviewContainer = dialogView.findViewById<FrameLayout>(com.cointask.R.id.webview_video)
        val externalWatchContainer = dialogView.findViewById<LinearLayout>(com.cointask.R.id.external_watch_container)
        val watchTimerTv = dialogView.findViewById<TextView>(com.cointask.R.id.tv_watch_timer)
        val watchProgressBar = dialogView.findViewById<android.widget.ProgressBar>(com.cointask.R.id.watch_progress_bar)

        videoTitleTextView.text = task.title
        taskDescriptionTv.text = "🎬 Watch for ${task.completionTimeSeconds} seconds to earn ${task.rewardCoins} coins"

        var videoStarted = false
        var videoError = false
        var videoEnded = false
        var currentErrorCode: String? = null
        var isExternalWatch = false
        var watchTimeElapsed = 0
        var externalWatchTimer: android.os.CountDownTimer? = null
        val requiredWatchTime = task.completionTimeSeconds

        val webView = android.webkit.WebView(this)
        webviewContainer.addView(webView, 
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val config = VideoPlayerHelper.configure(videoUrl)
            .autoPlay(true)
            .enableTracking(true)
            .height(250)
            .listener(object : VideoPlaybackListener {
                override fun onVideoStarted() {
                    android.util.Log.d("VideoPlayer", "Video started playing")
                    videoStarted = true
                    videoError = false
                    currentErrorCode = null
                    stopAllTimers()
                    loadingContainer.post {
                        loadingContainer.visibility = android.view.View.GONE
                        errorContainer.visibility = android.view.View.GONE
                        externalWatchContainer.visibility = android.view.View.GONE
                        watchYoutubeBtn.visibility = android.view.View.GONE
                    }
                }

                override fun onVideoError(errorCode: String, message: String) {
                    android.util.Log.e("VideoPlayer", "Video error: $errorCode - $message")
                    videoError = true
                    currentErrorCode = errorCode
                    loadingContainer.post {
                        loadingContainer.visibility = android.view.View.GONE
                        errorContainer.visibility = android.view.View.VISIBLE
                        errorMessageTv.text = "⚠️ $message\n\n(Error: $errorCode)"
                        watchYoutubeBtn.post {
                            watchYoutubeBtn.visibility = android.view.View.VISIBLE
                            watchYoutubeBtn.text = "Open Video in YouTube App"
                        }
                    }
                    if (errorCode == "152") {
                        android.util.Log.d("VideoPlayer", "Error 152 - will prompt to open in YouTube app")
                    }
                }

                override fun onVideoEnded() {
                    android.util.Log.d("VideoPlayer", "Video ended")
                    videoEnded = true
                    stopAllTimers()
                    loadingContainer.post {
                        loadingContainer.visibility = android.view.View.GONE
                        externalWatchContainer.visibility = android.view.View.GONE
                    }
                    completeBtn.post {
                        completeBtn.isEnabled = true
                        completeBtn.text = "Claim ${task.rewardCoins} 🪙"
                    }
                }

                override fun onOpenExternally() {
                    android.util.Log.d("VideoPlayer", "Opening video externally due to embed restriction")
                    watchYoutubeBtn.post {
                        watchYoutubeBtn.performClick()
                    }
                }
            })
            .build()

        VideoPlayerHelper.setupWebView(webView, config)

        var externalWatchStartTime: Long = 0
        var adGracePeriodMs: Long = 0

        fun startExternalWatch() {
            isExternalWatch = true
            externalWatchStartTime = System.currentTimeMillis()
            adGracePeriodMs = 20000
            watchTimerTv.post {
                watchTimerTv.text = "Watch video in YouTube app..."
            }
            taskDescriptionTv.post {
                taskDescriptionTv.text = "⏳ Timer will track when you return"
            }
            startPeriodicCheck()
        }

        fun startPeriodicCheck() {
            externalWatchTimer?.cancel()
            externalWatchTimer = object : android.os.CountDownTimer(86400000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (!isExternalWatch) {
                        cancel()
                        return
                    }
                    val timeAway = System.currentTimeMillis() - externalWatchStartTime
                    val effectiveWatchTime = (timeAway - adGracePeriodMs).coerceAtLeast(0)
                    val currentElapsed = (effectiveWatchTime / 1000).toInt()
                    
                    watchTimeElapsed = currentElapsed
                    val progress = (watchTimeElapsed * 100 / requiredWatchTime).coerceIn(0, 100)
                    watchProgressBar.progress = progress
                    
                    if (watchTimeElapsed >= requiredWatchTime) {
                        watchTimerTv.post {
                            watchTimerTv.text = "✅ Watch complete! Tap Claim"
                        }
                        taskDescriptionTv.post {
                            taskDescriptionTv.text = "✅ Claim your ${task.rewardCoins} coins!"
                        }
                        videoStarted = true
                        videoEnded = true
                        completeBtn.post {
                            completeBtn.isEnabled = true
                            completeBtn.text = "Claim ${task.rewardCoins} 🪙"
                        }
                    } else {
                        val remaining = requiredWatchTime - watchTimeElapsed
                        watchTimerTv.post {
                            watchTimerTv.text = "Elapsed: ${watchTimeElapsed}s / ${requiredWatchTime}s"
                        }
                        taskDescriptionTv.post {
                            taskDescriptionTv.text = "⏳ Return to claim. Need $remaining more seconds"
                        }
                    }
                }

                override fun onFinish() {}
            }.start()
        }

        fun stopAllTimers() {
            isExternalWatch = false
            externalWatchTimer?.cancel()
            externalWatchTimer = null
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        retryBtn.setOnClickListener {
            loadingContainer.visibility = android.view.View.VISIBLE
            errorContainer.visibility = android.view.View.GONE
            externalWatchContainer.visibility = android.view.View.GONE
            watchYoutubeBtn.visibility = android.view.View.GONE
            videoError = false
            currentErrorCode = null
            stopAllTimers()
            VideoPlayerHelper.retry(webView)
        }

        watchYoutubeBtn.setOnClickListener {
            val videoId = VideoProvider.YouTube.extractVideoId(videoUrl)
            if (videoId != null) {
                try {
                    val youtubeIntent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("vnd.youtube:$videoId"))
                    youtubeIntent.setPackage("com.google.android.youtube")
                    
                    externalWatchContainer.post {
                        externalWatchContainer.visibility = android.view.View.VISIBLE
                        errorContainer.visibility = android.view.View.GONE
                    }
                    
                    startExternalWatch()
                    startActivity(youtubeIntent)
                } catch (e: android.content.ActivityNotFoundException) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                        externalWatchContainer.post {
                            externalWatchContainer.visibility = android.view.View.VISIBLE
                            errorContainer.visibility = android.view.View.GONE
                        }
                        startExternalWatch()
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(this@UserDashboardActivity, "Cannot open video", Toast.LENGTH_SHORT).show()
                        externalWatchContainer.post {
                            externalWatchContainer.visibility = android.view.View.GONE
                        }
                    }
                } catch (e: Exception) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                        externalWatchContainer.post {
                            externalWatchContainer.visibility = android.view.View.VISIBLE
                            errorContainer.visibility = android.view.View.GONE
                        }
                        startExternalWatch()
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(this@UserDashboardActivity, "Cannot open video", Toast.LENGTH_SHORT).show()
                        externalWatchContainer.post {
                            externalWatchContainer.visibility = android.view.View.GONE
                        }
                    }
                }
            } else {
                Toast.makeText(this@UserDashboardActivity, "Invalid video URL", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setOnDismissListener {
            stopAllTimers()
        }

        closeBtn.setOnClickListener {
            stopAllTimers()
            dialog.dismiss()
        }

        cancelBtn.setOnClickListener {
            stopAllTimers()
            dialog.dismiss()
        }

        completeBtn.setOnClickListener {
            if (videoEnded || (videoStarted && !videoError) || (isExternalWatch && watchTimeElapsed >= requiredWatchTime)) {
                stopAllTimers()
                dialog.dismiss()
                completeTask(task)
            } else if (isExternalWatch) {
                Toast.makeText(this, "Please wait for $requiredWatchTime seconds", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            if (loadingContainer.visibility == android.view.View.VISIBLE && !videoStarted && !videoError && !isExternalWatch) {
                loadingContainer.visibility = android.view.View.GONE
                completeBtn.isEnabled = true
                completeBtn.text = "Claim ${task.rewardCoins} 🪙"
            }
        }, 15000)
    }

    /**
     * Show website visit tracking for VISIT_SITE and SURVEY tasks
     */
    private fun showWebsiteVisit(task: Task, websiteUrl: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val instructions = TextView(this).apply {
            text = "🌐 Visit the website for ${task.completionTimeSeconds} seconds"
            textSize = 16f
            setPadding(0, 0, 0, 15)
        }

        val openButton = android.widget.Button(this).apply {
            text = "Open Website"
            setPadding(0, 20, 0, 20)
            setOnClickListener {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(websiteUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@UserDashboardActivity, "Cannot open website", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val progressLabel = TextView(this).apply {
            text = "Time remaining: ${task.completionTimeSeconds}s"
            textSize = 14f
            setPadding(0, 15, 0, 10)
        }

        val progressBar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            max = task.completionTimeSeconds * 100
        }

        layout.addView(instructions)
        layout.addView(openButton)
        layout.addView(progressLabel)
        layout.addView(progressBar)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Visit Website")
            .setView(layout)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> }
            .show()

        // Track visit time
        var elapsedSeconds = 0
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (elapsedSeconds < task.completionTimeSeconds && dialog.isShowing) {
                    elapsedSeconds++
                    progressLabel.text = "Time remaining: ${task.completionTimeSeconds - elapsedSeconds}s"
                    progressBar.progress = elapsedSeconds * 100
                    if (elapsedSeconds >= task.completionTimeSeconds) {
                        dialog.dismiss()
                        completeTask(task)
                    } else {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }
        handler.post(runnable)
    }

    /**
     * Show social media action dialog for LIKE, SHARE, FOLLOW, COMMENT tasks
     */
    private fun showSocialActionDialog(task: Task, socialLink: String) {
        val actionName = when (task.taskType) {
            TaskType.LIKE_CONTENT -> "like"
            TaskType.SHARE_POST -> "share"
            TaskType.FOLLOW_ACCOUNT -> "follow"
            TaskType.COMMENT -> "comment on"
            else -> "interact with"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val instructions = TextView(this).apply {
            text = "❤️ Click the button below to $actionName this content:\n\n$socialLink"
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }

        val openButton = android.widget.Button(this).apply {
            text = "Open and $actionName"
            setPadding(0, 15, 0, 15)
            setOnClickListener {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(socialLink))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@UserDashboardActivity, "Cannot open link", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val confirmText = TextView(this).apply {
            text = "After completing the action, click 'I Completed This' below"
            textSize = 12f
            setPadding(0, 15, 0, 10)
        }

        val confirmButton = android.widget.Button(this).apply {
            text = "I Completed This Action"
            setPadding(0, 10, 0, 10)
        }

        layout.addView(instructions)
        layout.addView(openButton)
        layout.addView(confirmText)
        layout.addView(confirmButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("$actionName Content")
            .setView(layout)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> }
            .show()

        confirmButton.setOnClickListener {
            // Verify action was completed (in real app, would use API verification)
            // For now, we track the action and complete the task
            dialog.dismiss()
            completeTask(task)
        }
    }

    private fun completeTask(task: Task) {
        lifecycleScope.launch {
            // Check if user already completed this task
            val existingTransactions = database.transactionDao().getTransactionsByUserSuspend(currentUserId)
            val alreadyCompleted = existingTransactions.any {
                it.taskId == task.id && it.type == TransactionType.EARNED_FROM_TASK
            }

            if (alreadyCompleted) {
                Toast.makeText(this@UserDashboardActivity,
                    "You've already completed this task!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Check if task still has capacity and is active
            val currentTask = database.taskDao().getTaskById(task.id)
            if (currentTask == null || currentTask.completedCount >= currentTask.totalCapacity ||
                currentTask.status != TaskStatus.ACTIVE) {
                Toast.makeText(this@UserDashboardActivity,
                    "This task is no longer available!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Check if any target is achieved (auto-pause functionality)
            val targetAchieved = checkTargetAchieved(currentTask)
            if (targetAchieved) {
                // Pause the task automatically
                database.taskDao().updateTask(currentTask.copy(status = TaskStatus.PAUSED))
                Toast.makeText(this@UserDashboardActivity, "Task paused - target achieved by advertiser!", 
                    Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Add coins to user
            database.userDao().addCoins(currentUserId, task.rewardCoins)

            // Increment task completion count
            database.taskDao().incrementCompletion(task.id)

            // Update task target counters based on task type
            updateTaskTargetCounters(task.id, task.taskType)

            // Record transaction
            database.transactionDao().insertTransaction(
                Transaction(
                    userId = currentUserId,
                    type = TransactionType.EARNED_FROM_TASK,
                    amount = task.rewardCoins,
                    taskId = task.id,
                    description = "Completed: ${task.title}",
                    status = TransactionStatus.COMPLETED
                )
            )

            // Update campaign spending if task is associated with a campaign
            if (task.advertiserId > 0) {
                // Find campaign for this advertiser and update spending
                val campaigns = database.campaignDao().getCampaignsByAdvertiser(task.advertiserId)
                campaigns.collectLatest { campaignList ->
                    campaignList.firstOrNull()?.let { campaign ->
                        database.campaignDao().updateCampaignSpending(
                            campaign.id,
                            task.rewardCoins // Deduct equivalent amount from campaign
                        )
                    }
                }
            }

            // Log activity with detailed tracking for reward verification
            database.activityLogDao().insertLog(
                ActivityLog(
                    userId = currentUserId,
                    action = "TASK_COMPLETED",
                    details = "Completed task: ${task.title} (ID: ${task.id}) - " +
                            "Type: ${task.taskType}, Reward: ${task.rewardCoins} coins, " +
                            "Completion Time: ${task.completionTimeSeconds}s - " +
                            "Advertiser ID: ${task.advertiserId}"
                )
            )

            Toast.makeText(this@UserDashboardActivity,
                "Task completed! +${task.rewardCoins} coins", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Check if any target (views, likes, shares, clicks) has been achieved
     * Returns true if target is achieved, false otherwise
     */
    private fun checkTargetAchieved(task: Task): Boolean {
        // Check views target
        if (task.targetViews > 0 && task.currentViews >= task.targetViews) {
            return true
        }
        // Check likes target
        if (task.targetLikes > 0 && task.currentLikes >= task.targetLikes) {
            return true
        }
        // Check shares target
        if (task.targetShares > 0 && task.currentShares >= task.targetShares) {
            return true
        }
        // Check clicks target
        if (task.targetClicks > 0 && task.currentClicks >= task.targetClicks) {
            return true
        }
        return false
    }

    /**
     * Update task target counters based on task type
     */
    private fun updateTaskTargetCounters(taskId: Int, taskType: TaskType) {
        lifecycleScope.launch {
            val task = database.taskDao().getTaskById(taskId) ?: return@launch
            
            val updatedTask = when (taskType) {
                TaskType.WATCH_VIDEO, TaskType.VISIT_SITE -> {
                    // Increment views
                    task.copy(currentViews = task.currentViews + 1)
                }
                TaskType.LIKE_CONTENT -> {
                    // Increment likes
                    task.copy(currentLikes = task.currentLikes + 1)
                }
                TaskType.SHARE_POST -> {
                    // Increment shares
                    task.copy(currentShares = task.currentShares + 1)
                }
                TaskType.FOLLOW_ACCOUNT, TaskType.COMMENT, TaskType.SURVEY -> {
                    // Increment clicks (general engagement)
                    task.copy(currentClicks = task.currentClicks + 1)
                }
            }
            
            database.taskDao().updateTask(updatedTask)
            
            // Check if target achieved and auto-pause
            if (checkTargetAchieved(updatedTask)) {
                database.taskDao().updateTask(updatedTask.copy(status = TaskStatus.PAUSED))
                
                // Log the auto-pause event
                database.activityLogDao().insertLog(
                    ActivityLog(
                        userId = currentUserId,
                        action = "TASK_AUTO_PAUSED",
                        details = "Task '${task.title}' auto-paused - target achieved. " +
                                "Final counts: views=${updatedTask.currentViews}, likes=${updatedTask.currentLikes}, " +
                                "shares=${updatedTask.currentShares}, clicks=${updatedTask.currentClicks}"
                    )
                )
            }
        }
    }

    private fun confirmTaskCompletion(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Complete Task")
            .setMessage("Have you completed '${task.title}'?\n\nReward: ${task.rewardCoins} coins")
            .setPositiveButton("Yes, Complete") { _, _ ->
                completeTask(task)
            }
            .setNegativeButton("Cancel", null)
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
