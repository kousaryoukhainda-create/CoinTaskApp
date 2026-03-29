package com.cointask.admin

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cointask.auth.LoginActivity
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.UserRole
import com.cointask.data.models.WithdrawalRequest
import com.cointask.data.models.WithdrawalStatus
import com.cointask.databinding.ActivityAdminPanelBinding
import com.cointask.utils.PreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)

        setupClickListeners()
        loadPlatformStatistics()
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnManageUsers.setOnClickListener {
            showManageUsersDialog()
        }

        binding.btnManageTasks.setOnClickListener {
            showManageTasksDialog()
        }

        binding.btnViewFraudDetection.setOnClickListener {
            showFraudDetectionDialog()
        }

        binding.btnViewActivityLogs.setOnClickListener {
            showActivityLogsDialog()
        }

        binding.btnPlatformSettings.setOnClickListener {
            showPlatformSettingsDialog()
        }

        binding.btnApproveWithdrawals.setOnClickListener {
            showWithdrawalRequestsDialog()
        }

        binding.btnAdvertiserPayments.setOnClickListener {
            showAdvertiserPaymentsDialog()
        }

        binding.btnAdminSettings.setOnClickListener {
            showAdminSettingsDialog()
        }
    }

    private fun showManageUsersDialog() {
        lifecycleScope.launch {
            val users = database.userDao().getAllUsersList()
            
            if (users.isEmpty()) {
                Toast.makeText(this@AdminPanelActivity, "No users found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val userNames = users.map { "${it.fullName} (${it.email}) - ${it.role}" }.toTypedArray()
            val actions = arrayOf("View Details", "Suspend/Unsuspend", "Block/Unblock", "Add Coins", "Deduct Coins")

            AlertDialog.Builder(this@AdminPanelActivity)
                .setTitle("Manage Users")
                .setItems(userNames) { _, which ->
                    showUserActionsDialog(users[which], actions)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showUserActionsDialog(user: com.cointask.data.models.User, actions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("User: ${user.fullName}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showUserDetails(user)
                    1 -> toggleUserSuspension(user)
                    2 -> toggleUserBlock(user)
                    3 -> showAddCoinsDialog(user)
                    4 -> showDeductCoinsDialog(user)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showUserDetails(user: com.cointask.data.models.User) {
        val details = """
            Name: ${user.fullName}
            Email: ${user.email}
            Role: ${user.role}
            Coins: ${user.coins}
            Verified: ${if (user.isVerified) "Yes" else "No"}
            Active: ${if (user.isActive) "Yes" else "No"}
            Suspended: ${if (user.isSuspended) "Yes" else "No"}
            Suspension Count: ${user.suspensionCount}
            Last Login: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(user.lastLogin))}
            Created: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(user.createdAt))}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("User Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleUserSuspension(user: com.cointask.data.models.User) {
        lifecycleScope.launch {
            if (user.isSuspended) {
                database.userDao().updateUser(user.copy(isSuspended = false))
                Toast.makeText(this@AdminPanelActivity, "User unsuspended", Toast.LENGTH_SHORT).show()
            } else {
                database.userDao().autoSuspendUser(user.id)
                database.userDao().incrementSuspensionCount(user.id)
                Toast.makeText(this@AdminPanelActivity, "User suspended", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleUserBlock(user: com.cointask.data.models.User) {
        lifecycleScope.launch {
            if (user.isActive) {
                database.userDao().blockUser(user.id)
                Toast.makeText(this@AdminPanelActivity, "User blocked", Toast.LENGTH_SHORT).show()
            } else {
                database.userDao().updateUser(user.copy(isActive = true))
                Toast.makeText(this@AdminPanelActivity, "User unblocked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddCoinsDialog(user: com.cointask.data.models.User) {
        val editText = EditText(this).apply {
            hint = "Enter coins to add"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("Add Coins to ${user.fullName}")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val amount = editText.text.toString().toIntOrNull() ?: return@setPositiveButton
                if (amount > 0) {
                    lifecycleScope.launch {
                        database.userDao().addCoins(user.id, amount)
                        Toast.makeText(this@AdminPanelActivity, "Added $amount coins", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeductCoinsDialog(user: com.cointask.data.models.User) {
        val editText = EditText(this).apply {
            hint = "Enter coins to deduct"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("Deduct Coins from ${user.fullName}")
            .setView(editText)
            .setPositiveButton("Deduct") { _, _ ->
                val amount = editText.text.toString().toIntOrNull() ?: return@setPositiveButton
                if (amount > 0 && amount <= user.coins) {
                    lifecycleScope.launch {
                        database.userDao().deductCoins(user.id, amount)
                        Toast.makeText(this@AdminPanelActivity, "Deducted $amount coins", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AdminPanelActivity, "Invalid amount or insufficient coins", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManageTasksDialog() {
        lifecycleScope.launch {
            val tasks = database.taskDao().getAllTasksList()
            
            if (tasks.isEmpty()) {
                Toast.makeText(this@AdminPanelActivity, "No tasks found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val taskNames = tasks.map { "${it.title} - ${it.rewardCoins} 🪙 (${it.status})" }.toTypedArray()
            val actions = arrayOf("View Details", "Pause/Resume", "Delete")

            AlertDialog.Builder(this@AdminPanelActivity)
                .setTitle("Manage Tasks")
                .setItems(taskNames) { _, which ->
                    showTaskActionsDialog(tasks[which], actions)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showTaskActionsDialog(task: com.cointask.data.models.Task, actions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("Task: ${task.title}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showTaskDetails(task)
                    1 -> toggleTaskStatus(task)
                    2 -> deleteTask(task)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showTaskDetails(task: com.cointask.data.models.Task) {
        val details = """
            Title: ${task.title}
            Description: ${task.description}
            Type: ${task.taskType}
            Reward: ${task.rewardCoins} 🪙
            Capacity: ${task.completedCount}/${task.totalCapacity}
            Status: ${task.status}
            Expires: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(task.expiresAt))}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Task Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleTaskStatus(task: com.cointask.data.models.Task) {
        lifecycleScope.launch {
            val newStatus = if (task.status == com.cointask.data.models.TaskStatus.ACTIVE) {
                com.cointask.data.models.TaskStatus.PAUSED
            } else {
                com.cointask.data.models.TaskStatus.ACTIVE
            }
            database.taskDao().updateTask(task.copy(status = newStatus))
            Toast.makeText(this@AdminPanelActivity, "Task ${if (newStatus == com.cointask.data.models.TaskStatus.ACTIVE) "resumed" else "paused"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteTask(task: com.cointask.data.models.Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.taskDao().updateTask(task.copy(status = com.cointask.data.models.TaskStatus.COMPLETED))
                    Toast.makeText(this@AdminPanelActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFraudDetectionDialog() {
        lifecycleScope.launch {
            val suspiciousLogs = database.activityLogDao().getAllLogsList().filter { it.isSuspicious }

            val message = if (suspiciousLogs.isEmpty()) {
                "No suspicious activities detected."
            } else {
                suspiciousLogs.joinToString("\n\n") { log ->
                    "User ID: ${log.userId}\nAction: ${log.action}\nDetails: ${log.details}\nFraud Score: ${log.fraudScore}\nTime: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))}"
                }
            }

            AlertDialog.Builder(this@AdminPanelActivity)
                .setTitle("Fraud Detection (${suspiciousLogs.size} alerts)")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showActivityLogsDialog() {
        lifecycleScope.launch {
            val logs = database.activityLogDao().getAllLogsList().take(20)

            val message = if (logs.isEmpty()) {
                "No activity logs found."
            } else {
                logs.joinToString("\n") { log ->
                    "[${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))}] User ${log.userId}: ${log.action} - ${log.details}"
                }
            }

            AlertDialog.Builder(this@AdminPanelActivity)
                .setTitle("Recent Activity Logs (Last 20)")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showPlatformSettingsDialog() {
        val settings = arrayOf(
            "Clear All Data (Reset App)",
            "View App Info"
        )

        AlertDialog.Builder(this)
            .setTitle("Platform Settings")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> showResetConfirmationDialog()
                    1 -> showAppInfoDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset App Data")
            .setMessage("Are you sure you want to delete ALL data? This will:\n\n• Delete all users\n• Delete all tasks\n• Delete all transactions\n• Delete all campaigns\n• Delete all activity logs\n• Delete all withdrawal requests\n\nThis action CANNOT be undone!")
            .setPositiveButton("Reset") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Delete all data
                        val users = database.userDao().getAllUsersList()
                        users.forEach { database.userDao().deleteUser(it) }
                        
                        val tasks = database.taskDao().getAllTasksList()
                        tasks.forEach { database.taskDao().deleteTask(it) }
                        
                        val campaigns = database.campaignDao().getAllCampaignsList()
                        campaigns.forEach { database.campaignDao().deleteCampaign(it) }
                        
                        val transactions = database.transactionDao().getAllTransactionsList()
                        transactions.forEach { database.transactionDao().deleteTransaction(it) }
                        
                        val logs = database.activityLogDao().getAllLogsList()
                        logs.forEach { database.activityLogDao().deleteLog(it) }
                        
                        val withdrawals = database.withdrawalRequestDao().getAllWithdrawals()
                        withdrawals.forEach { database.withdrawalRequestDao().deleteWithdrawal(it) }
                        
                        Toast.makeText(this@AdminPanelActivity, "App data reset successfully", Toast.LENGTH_LONG).show()
                        
                        // Redirect to login
                        preferencesManager.clearSession()
                        startActivity(Intent(this@AdminPanelActivity, LoginActivity::class.java))
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminPanelActivity, "Error resetting data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAppInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("CoinTask - App Info")
            .setMessage("Version: 1.0\n\nA task rewards platform built with Kotlin and Material Design 3.\n\nFeatures:\n• User task completion\n• Advertiser campaign management\n• Admin oversight\n• Coin-based reward system\n• Withdrawal processing")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadPlatformStatistics() {
        lifecycleScope.launch {
            try {
                val users = database.userDao().getAllUsersList()
                val tasks = database.taskDao().getAllTasksList()
                val transactions = database.transactionDao().getAllTransactionsList()
                val suspiciousActivities = database.activityLogDao().getAllLogsList().count { it.isSuspicious }
                val pendingWithdrawals = database.withdrawalRequestDao().getPendingWithdrawalsList()

                val totalUsers = users.count { it.role == UserRole.USER }
                val totalAdvertisers = users.count { it.role == UserRole.ADVERTISER }
                val totalTasks = tasks.size
                val totalRevenue = transactions.filter { it.type == com.cointask.data.models.TransactionType.CAMPAIGN_PAYMENT }
                    .sumOf { it.amount }
                val platformCommission = (totalRevenue * 0.1).toInt()
                val pendingWithdrawalAmount = pendingWithdrawals.sumOf { it.amount }

                binding.tvTotalUsers.text = totalUsers.toString()
                binding.tvTotalAdvertisers.text = totalAdvertisers.toString()
                binding.tvTotalTasks.text = totalTasks.toString()
                binding.tvTotalRevenue.text = "🪙$totalRevenue"
                binding.tvPlatformCommission.text = "🪙$platformCommission"
                binding.tvSuspiciousActivities.text = "$suspiciousActivities (${pendingWithdrawals.size} pending)"

            } catch (e: Exception) {
                Toast.makeText(this@AdminPanelActivity, "Error loading statistics", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showWithdrawalRequestsDialog() {
        lifecycleScope.launch {
            val withdrawals = database.withdrawalRequestDao().getAllWithdrawals()
                .sortedByDescending { it.requestDate }

            if (withdrawals.isEmpty()) {
                Toast.makeText(this@AdminPanelActivity, "No withdrawal requests found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val withdrawalItems = withdrawals.map { wd ->
                val user = database.userDao().getUserByIdSuspend(wd.userId)
                val date = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date(wd.requestDate))
                "${wd.status} - 🪙${wd.amount} (${user?.fullName ?: "Unknown"}) - $date"
            }.toTypedArray()

            AlertDialog.Builder(this@AdminPanelActivity)
                .setTitle("Withdrawal Requests")
                .setItems(withdrawalItems) { _, which ->
                    showWithdrawalDetails(withdrawals[which])
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showWithdrawalDetails(withdrawal: WithdrawalRequest) {
        lifecycleScope.launch {
            val user = database.userDao().getUserByIdSuspend(withdrawal.userId)
            val admin = withdrawal.processedBy?.let { database.userDao().getUserByIdSuspend(it) }

            val requestDate = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date(withdrawal.requestDate))
            val processedDate = withdrawal.processedDate?.let {
                SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date(it))
            } ?: "Not processed"

            val details = """
                Withdrawal Request Details

                User: ${user?.fullName ?: "Unknown"}
                Email: ${user?.email ?: "N/A"}

                Amount: ${withdrawal.amount} 🪙
                Status: ${withdrawal.status}

                Bank Information:
                Bank Name: ${withdrawal.bankName}
                Account Number: ${withdrawal.accountNumber}
                Account Holder: ${withdrawal.accountName}

                Requested: $requestDate
                Processed By: ${admin?.fullName ?: "Not processed"}
                Processed Date: $processedDate

                ${withdrawal.rejectionReason?.let { "Rejection Reason: $it" } ?: ""}
                ${withdrawal.transactionReference?.let { "Transaction Reference: $it" } ?: ""}
                ${withdrawal.notes?.let { "Notes: $it" } ?: ""}
            """.trimIndent()

            AlertDialog.Builder(this@AdminPanelActivity)
                .setTitle("Withdrawal Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("Process") { _, _ ->
                    if (withdrawal.status == WithdrawalStatus.PENDING) {
                        showProcessWithdrawalDialog(withdrawal)
                    } else {
                        Toast.makeText(this@AdminPanelActivity,
                            "This withdrawal is already ${withdrawal.status}", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun showProcessWithdrawalDialog(withdrawal: WithdrawalRequest) {
        val actions = arrayOf("Approve & Mark as Processing", "Mark as Completed", "Reject Withdrawal")

        AlertDialog.Builder(this)
            .setTitle("Process Withdrawal")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> approveWithdrawal(withdrawal)
                    1 -> completeWithdrawal(withdrawal)
                    2 -> rejectWithdrawalDialog(withdrawal)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun approveWithdrawal(withdrawal: WithdrawalRequest) {
        lifecycleScope.launch {
            database.withdrawalRequestDao().updateWithdrawalStatus(
                withdrawal.id,
                WithdrawalStatus.PROCESSING,
                System.currentTimeMillis(),
                preferencesManager.getUserId()
            )

            val transactions = database.transactionDao().getTransactionsByUserSuspend(withdrawal.userId)
            transactions.find { it.amount == withdrawal.amount && it.type == com.cointask.data.models.TransactionType.WITHDRAWAL }?.let { tx ->
                database.transactionDao().updateTransaction(tx.copy(status = com.cointask.data.models.TransactionStatus.PROCESSING))
            }

            database.activityLogDao().insertLog(
                com.cointask.data.models.ActivityLog(
                    userId = preferencesManager.getUserId(),
                    action = "WITHDRAWAL_APPROVED",
                    details = "Admin approved withdrawal of ${withdrawal.amount} coins for user ${withdrawal.userId}"
                )
            )

            Toast.makeText(this@AdminPanelActivity, "Withdrawal approved and marked as processing", Toast.LENGTH_SHORT).show()
            loadPlatformStatistics()
        }
    }

    private fun completeWithdrawal(withdrawal: WithdrawalRequest) {
        val refInput = EditText(this).apply {
            hint = "Enter transaction reference"
        }

        AlertDialog.Builder(this)
            .setTitle("Mark as Completed")
            .setView(refInput)
            .setMessage("Enter the transaction reference or confirmation number:")
            .setPositiveButton("Complete") { _, _ ->
                lifecycleScope.launch {
                    val ref = refInput.text.toString().trim().ifEmpty { "MANUAL-${System.currentTimeMillis()}" }

                    database.withdrawalRequestDao().updateWithdrawalStatus(
                        withdrawal.id,
                        WithdrawalStatus.COMPLETED,
                        System.currentTimeMillis(),
                        preferencesManager.getUserId(),
                        ref
                    )

                    val transactions = database.transactionDao().getTransactionsByUserSuspend(withdrawal.userId)
                    transactions.find { it.amount == withdrawal.amount && it.type == com.cointask.data.models.TransactionType.WITHDRAWAL }?.let { tx ->
                        database.transactionDao().updateTransaction(tx.copy(status = com.cointask.data.models.TransactionStatus.COMPLETED, referenceId = ref))
                    }

                    database.activityLogDao().insertLog(
                        com.cointask.data.models.ActivityLog(
                            userId = preferencesManager.getUserId(),
                            action = "WITHDRAWAL_COMPLETED",
                            details = "Admin completed withdrawal of ${withdrawal.amount} coins. Ref: $ref"
                        )
                    )

                    Toast.makeText(this@AdminPanelActivity, "Withdrawal completed!", Toast.LENGTH_SHORT).show()
                    loadPlatformStatistics()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectWithdrawalDialog(withdrawal: WithdrawalRequest) {
        val reasonInput = EditText(this).apply {
            hint = "Enter rejection reason"
            setLines(3)
        }

        AlertDialog.Builder(this)
            .setTitle("Reject Withdrawal")
            .setView(reasonInput)
            .setMessage("Enter reason for rejection (coins will be refunded to user):")
            .setPositiveButton("Reject") { _, _ ->
                lifecycleScope.launch {
                    val reason = reasonInput.text.toString().trim().ifEmpty { "No reason provided" }

                    database.withdrawalRequestDao().updateWithdrawalStatus(
                        withdrawal.id,
                        WithdrawalStatus.REJECTED,
                        System.currentTimeMillis(),
                        preferencesManager.getUserId(),
                        reason
                    )

                    database.userDao().addCoins(withdrawal.userId, withdrawal.amount)

                    val transactions = database.transactionDao().getTransactionsByUserSuspend(withdrawal.userId)
                    transactions.find { it.amount == withdrawal.amount && it.type == com.cointask.data.models.TransactionType.WITHDRAWAL }?.let { tx ->
                        database.transactionDao().updateTransaction(tx.copy(status = com.cointask.data.models.TransactionStatus.CANCELLED))
                    }

                    database.activityLogDao().insertLog(
                        com.cointask.data.models.ActivityLog(
                            userId = preferencesManager.getUserId(),
                            action = "WITHDRAWAL_REJECTED",
                            details = "Admin rejected withdrawal of ${withdrawal.amount} coins. Reason: $reason"
                        )
                    )

                    Toast.makeText(this@AdminPanelActivity, "Withdrawal rejected. Coins refunded to user.", Toast.LENGTH_SHORT).show()
                    loadPlatformStatistics()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show advertiser payment verification dialog
     * Admin can verify payment details and add coins to advertiser accounts
     */
    private fun showAdvertiserPaymentsDialog() {
        lifecycleScope.launch {
            val advertisers = database.userDao().getAllUsersList()
                .filter { it.role == UserRole.ADVERTISER }

            if (advertisers.isEmpty()) {
                Toast.makeText(this@AdminPanelActivity, "No advertisers found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val advertiserItems = advertisers.map { adv ->
                val verified = if (adv.paymentVerified) "✅" else "⏳"
                "$verified ${adv.fullName} - ${adv.coins} 🪙 (${if (adv.paymentVerified) "Verified" else "Pending"})"
            }.toTypedArray()

            AlertDialog.Builder(this@AdminPanelActivity)
                .setTitle("Advertiser Payment Verification")
                .setItems(advertiserItems) { _, which ->
                    showAdvertiserPaymentDetails(advertisers[which])
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showAdvertiserPaymentDetails(advertiser: com.cointask.data.models.User) {
        val details = """
            Advertiser Payment Details

            Name: ${advertiser.fullName}
            Email: ${advertiser.email}
            Current Balance: ${advertiser.coins} 🪙

            Payment Information:
            Transaction ID: ${advertiser.transactionId ?: "N/A"}
            CNIC: ${advertiser.cnic ?: "N/A"}
            Account Title: ${advertiser.accountTitle ?: "N/A"}
            Account Number: ${advertiser.accountNumber ?: "N/A"}

            Status: ${if (advertiser.paymentVerified) "✅ Verified" else "⏳ Pending Verification"}
        """.trimIndent()

        val actions = if (!advertiser.paymentVerified) {
            arrayOf("View Details", "Verify Payment & Add Coins", "Reject Payment")
        } else {
            arrayOf("View Details", "Add Bonus Coins")
        }

        AlertDialog.Builder(this)
            .setTitle("Advertiser: ${advertiser.fullName}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showFullAdvertiserDetails(advertiser)
                    1 -> if (!advertiser.paymentVerified) verifyAdvertiserPayment(advertiser)
                         else addBonusCoins(advertiser)
                    2 -> rejectAdvertiserPayment(advertiser)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showFullAdvertiserDetails(advertiser: com.cointask.data.models.User) {
        val details = """
            Full Advertiser Details

            Name: ${advertiser.fullName}
            Email: ${advertiser.email}
            Role: ${advertiser.role}

            Account Status:
            - Active: ${if (advertiser.isActive) "Yes" else "No"}
            - Verified: ${if (advertiser.isVerified) "Yes" else "No"}
            - Suspended: ${if (advertiser.isSuspended) "Yes" else "No"}

            Current Balance: ${advertiser.coins} 🪙

            Payment Information:
            Transaction ID: ${advertiser.transactionId ?: "N/A"}
            CNIC: ${advertiser.cnic ?: "N/A"}
            Account Title: ${advertiser.accountTitle ?: "N/A"}
            Account Number: ${advertiser.accountNumber ?: "N/A"}

            Payment Status: ${if (advertiser.paymentVerified) "✅ Verified" else "⏳ Pending"}

            Last Login: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(advertiser.lastLogin))}
            Created: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(advertiser.createdAt))}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Advertiser Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun verifyAdvertiserPayment(advertiser: com.cointask.data.models.User) {
        val coinsInput = EditText(this).apply {
            hint = "Enter bonus coins to add (e.g., 10000)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("10000")
        }

        AlertDialog.Builder(this)
            .setTitle("Verify Payment & Add Coins")
            .setView(coinsInput)
            .setMessage("Verify payment and add bonus coins to advertiser's account?")
            .setPositiveButton("Verify & Add Coins") { _, _ ->
                val coinsToAdd = coinsInput.text.toString().toIntOrNull() ?: 10000

                lifecycleScope.launch {
                    val updatedAdvertiser = advertiser.copy(
                        coins = advertiser.coins + coinsToAdd,
                        paymentVerified = true,
                        isVerified = true,
                        isActive = true
                    )
                    database.userDao().updateUser(updatedAdvertiser)

                    database.activityLogDao().insertLog(
                        com.cointask.data.models.ActivityLog(
                            userId = advertiser.id,
                            action = "ADVERTISER_PAYMENT_VERIFIED",
                            details = "Admin verified payment and added $coinsToAdd 🪙. Transaction: ${advertiser.transactionId}"
                        )
                    )

                    Toast.makeText(this@AdminPanelActivity,
                        "Payment verified! $coinsToAdd 🪙 added to ${advertiser.fullName}",
                        Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addBonusCoins(advertiser: com.cointask.data.models.User) {
        val coinsInput = EditText(this).apply {
            hint = "Enter bonus coins"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("Add Bonus Coins")
            .setView(coinsInput)
            .setMessage("Enter number of bonus coins to add:")
            .setPositiveButton("Add") { _, _ ->
                val coinsToAdd = coinsInput.text.toString().toIntOrNull() ?: return@setPositiveButton

                lifecycleScope.launch {
                    database.userDao().addCoins(advertiser.id, coinsToAdd)

                    database.activityLogDao().insertLog(
                        com.cointask.data.models.ActivityLog(
                            userId = advertiser.id,
                            action = "BONUS_COINS_ADDED",
                            details = "Admin added $coinsToAdd bonus coins to ${advertiser.email}"
                        )
                    )

                    Toast.makeText(this@AdminPanelActivity,
                        "Added $coinsToAdd 🪙 to ${advertiser.fullName}",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectAdvertiserPayment(advertiser: com.cointask.data.models.User) {
        val reasonInput = EditText(this).apply {
            hint = "Enter rejection reason"
            setLines(3)
        }

        AlertDialog.Builder(this)
            .setTitle("Reject Payment")
            .setView(reasonInput)
            .setMessage("Enter reason for rejecting this payment:")
            .setPositiveButton("Reject") { _, _ ->
                lifecycleScope.launch {
                    val reason = reasonInput.text.toString().trim().ifEmpty { "Invalid payment details" }

                    val updatedAdvertiser = advertiser.copy(
                        paymentVerified = false,
                        transactionId = null,
                        cnic = null,
                        accountTitle = null,
                        accountNumber = null
                    )
                    database.userDao().updateUser(updatedAdvertiser)

                    database.activityLogDao().insertLog(
                        com.cointask.data.models.ActivityLog(
                            userId = advertiser.id,
                            action = "ADVERTISER_PAYMENT_REJECTED",
                            details = "Admin rejected payment for ${advertiser.email}. Reason: $reason"
                        )
                    )

                    Toast.makeText(this@AdminPanelActivity,
                        "Payment rejected. Advertiser needs to re-submit.",
                        Toast.LENGTH_SHORT).show()
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
                startActivity(Intent(this@AdminPanelActivity, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAdminSettingsDialog() {
        val settings = arrayOf(
            "👤 View Admin Profile",
            "🔐 Change Password",
            "ℹ️ About CoinTask"
        )

        AlertDialog.Builder(this)
            .setTitle("Admin Settings")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> showAdminProfile()
                    1 -> showChangePasswordDialog()
                    2 -> showAboutDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAdminProfile() {
        lifecycleScope.launch {
            val admin = database.userDao().getUserByIdSuspend(preferencesManager.getUserId())
            admin?.let {
                val profileDetails = """
                    👨‍💼 Admin Profile

                    Name: ${it.fullName}
                    Email: ${it.email}
                    Role: ${it.role}

                    ✅ Verified: ${if (it.isVerified) "Yes" else "No"}
                    🔓 Active: ${if (it.isActive) "Yes" else "No"}

                    📅 Member Since: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it.createdAt))}
                    🕐 Last Login: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(it.lastLogin))}
                """.trimIndent()

                AlertDialog.Builder(this@AdminPanelActivity)
                    .setTitle("My Profile")
                    .setMessage(profileDetails)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        // Reuse the forgot password flow from LoginActivity
        startActivity(Intent(this@AdminPanelActivity, LoginActivity::class.java))
        Toast.makeText(this, "Please use the password reset feature on login screen", Toast.LENGTH_LONG).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About CoinTask")
            .setMessage("CoinTask - Task Rewards Platform\n\nVersion: 1.0\n\nAdmin Features:\n• Manage users and tasks\n• Approve withdrawals\n• Verify advertiser payments\n• View activity logs\n• Monitor fraud detection")
            .setPositiveButton("OK", null)
            .show()
    }
}
