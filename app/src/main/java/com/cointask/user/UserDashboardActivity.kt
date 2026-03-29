package com.cointask.user

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.Task
import com.cointask.data.models.Transaction
import com.cointask.data.models.TransactionStatus
import com.cointask.data.models.TransactionType
import com.cointask.databinding.ActivityUserDashboardBinding
import com.cointask.utils.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private var currentUserId = -1
    private var currentCoins = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        currentUserId = preferencesManager.getUserId()

        loadUserData()
        setupClickListeners()
        insertSampleTasks()
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
                    animateCoinCounter(0, it.coins)
                    binding.tvUsername.text = "Welcome ${it.fullName}!"
                    binding.progressLevel.progress = (it.coins / 1000).coerceAtMost(100)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnWithdraw.setOnClickListener {
            showWithdrawalDialog()
        }

        binding.btnHistory.setOnClickListener {
            showTransactionHistory()
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
                    history.append("${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                        .format(Date(tx.timestamp))}: ${tx.description} - ${tx.amount} coins\n")
                }
            }

            androidx.appcompat.app.AlertDialog.Builder(this@UserDashboardActivity)
                .setTitle("Transaction History")
                .setMessage(history.toString().ifEmpty { "No transactions yet" })
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun insertSampleTasks() {
        lifecycleScope.launch {
            val existingTasks = database.taskDao().getAllTasks()
            if (existingTasks.isEmpty()) {
                val sampleTasks = listOf(
                    Task(
                        advertiserId = 1,
                        title = "Watch Introduction Video",
                        description = "Watch our 2-minute introduction video",
                        taskType = Task.TaskType.WATCH_VIDEO,
                        rewardCoins = 50,
                        totalCapacity = 100,
                        expiresAt = System.currentTimeMillis() + 7 * 24 * 3600000,
                        status = Task.TaskStatus.ACTIVE
                    ),
                    Task(
                        advertiserId = 1,
                        title = "Visit Partner Website",
                        description = "Visit our partner's website",
                        taskType = Task.TaskType.VISIT_SITE,
                        rewardCoins = 30,
                        totalCapacity = 200,
                        expiresAt = System.currentTimeMillis() + 7 * 24 * 3600000,
                        status = Task.TaskStatus.ACTIVE
                    )
                )

                sampleTasks.forEach { task ->
                    database.taskDao().insertTask(task)
                }
            }
        }
    }
}
