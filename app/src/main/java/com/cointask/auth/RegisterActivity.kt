package com.cointask.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.ActivityLog
import com.cointask.data.models.User
import com.cointask.data.models.UserRole
import com.cointask.databinding.ActivityRegisterBinding
import com.cointask.utils.PasswordUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var database: AppDatabase
    private var selectedRole: UserRole = UserRole.ADMIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupRoleTabs()
        setupClickListeners()
        checkAdminExists()
    }

    private fun setupRoleTabs() {
        binding.tabLayoutRoles.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val selectedPosition = tab?.position ?: 0
                val adminTabExists = binding.tabLayoutRoles.getTabAt(0)?.text?.toString()?.contains("Admin") == true
                
                when {
                    // Admin tab exists and is selected
                    adminTabExists && selectedPosition == 0 -> selectRole(UserRole.ADMIN)
                    // Admin tab exists, User tab selected
                    adminTabExists && selectedPosition == 1 -> selectRole(UserRole.USER)
                    // Admin tab exists, Advertiser tab selected
                    adminTabExists && selectedPosition == 2 -> selectRole(UserRole.ADVERTISER)
                    // Admin tab removed, first tab is User
                    !adminTabExists && selectedPosition == 0 -> selectRole(UserRole.USER)
                    // Admin tab removed, second tab is Advertiser
                    !adminTabExists && selectedPosition == 1 -> selectRole(UserRole.ADVERTISER)
                    else -> selectRole(UserRole.USER)
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Initialize with first available role selected
        val adminTabExists = binding.tabLayoutRoles.getTabAt(0)?.text?.toString()?.contains("Admin") == true
        if (adminTabExists) {
            selectRole(UserRole.ADMIN)
        } else {
            selectRole(UserRole.USER)
        }
    }

    private fun selectRole(role: UserRole) {
        selectedRole = role

        when (role) {
            UserRole.ADMIN -> {
                binding.layoutAdvertiserPayment.visibility = View.GONE
            }
            UserRole.USER -> {
                binding.layoutAdvertiserPayment.visibility = View.GONE
            }
            UserRole.ADVERTISER -> {
                binding.layoutAdvertiserPayment.visibility = View.VISIBLE
                setFieldRequired(binding.etTransactionId, true)
                setFieldRequired(binding.etCNIC, true)
                setFieldRequired(binding.etAccountTitle, true)
                setFieldRequired(binding.etAccountNumber, true)
            }
        }
    }

    private fun setFieldRequired(editText: android.widget.EditText, required: Boolean) {
        if (required) {
            editText.hint = editText.hint.toString().replace(" (Optional)", "") + " *"
        } else {
            editText.hint = editText.hint.toString().replace(" *", "")
        }
    }

    private fun checkAdminExists() {
        lifecycleScope.launch {
            val adminCount = database.userDao().getActiveAdminCount()
            if (adminCount > 0) {
                // Admin already exists, hide admin registration option
                binding.tabLayoutRoles.removeTabAt(0)
                selectRole(UserRole.USER)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            if (!binding.checkboxTerms.isChecked) {
                Toast.makeText(this,
                    "Please agree to the Terms of Service and Privacy Policy",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val fullName = binding.etFullName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            val transactionId = binding.etTransactionId.text.toString().trim()
            val cnic = binding.etCNIC.text.toString().trim()
            val accountTitle = binding.etAccountTitle.text.toString().trim()
            val accountNumber = binding.etAccountNumber.text.toString().trim()

            if (validateInputs(email, password, confirmPassword, fullName,
                    transactionId, cnic, accountTitle, accountNumber)) {
                performRegistration(email, password, fullName, phone,
                    transactionId, cnic, accountTitle, accountNumber)
            }
        }

        binding.btnLogin.setOnClickListener {
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        transactionId: String = "",
        cnic: String = "",
        accountTitle: String = "",
        accountNumber: String = ""
    ): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.etEmail.error = "Email required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            isValid = false
        } else {
            binding.etEmail.error = null
        }

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name required"
            isValid = false
        } else if (fullName.length < 2) {
            binding.etFullName.error = "Name too short"
            isValid = false
        } else {
            binding.etFullName.error = null
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password required"
            isValid = false
        } else if (password.length < 8) {
            binding.etPassword.error = "Password must be at least 8 characters"
            isValid = false
        } else if (!password.matches(Regex(".*[A-Z].*"))) {
            binding.etPassword.error = "Password must contain at least one uppercase letter"
            isValid = false
        } else if (!password.matches(Regex(".*[0-9].*"))) {
            binding.etPassword.error = "Password must contain at least one number"
            isValid = false
        } else {
            binding.etPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.etConfirmPassword.error = null
        }

        if (selectedRole == UserRole.ADVERTISER) {
            if (transactionId.isEmpty()) {
                binding.etTransactionId.error = "Transaction ID is required"
                isValid = false
            } else {
                binding.etTransactionId.error = null
            }

            if (cnic.isEmpty() || !isValidCNIC(cnic)) {
                binding.etCNIC.error = "Valid CNIC is required (e.g., 12345-1234567-1)"
                isValid = false
            } else {
                binding.etCNIC.error = null
            }

            if (accountTitle.isEmpty()) {
                binding.etAccountTitle.error = "Account title is required"
                isValid = false
            } else {
                binding.etAccountTitle.error = null
            }

            if (accountNumber.isEmpty()) {
                binding.etAccountNumber.error = "Account number is required"
                isValid = false
            } else {
                binding.etAccountNumber.error = null
            }
        }

        return isValid
    }

    private fun isValidCNIC(cnic: String): Boolean {
        val cnicPattern = Regex("^\\d{5}-\\d{7}-\\d{1}\$")
        return cnicPattern.matches(cnic)
    }

    private fun performRegistration(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        transactionId: String = "",
        cnic: String = "",
        accountTitle: String = "",
        accountNumber: String = ""
    ) {
        lifecycleScope.launch {
            val existingUser = database.userDao().getUserByEmail(email)

            if (existingUser != null) {
                Toast.makeText(this@RegisterActivity,
                    "This email is already registered. Please login or use a different email.",
                    Toast.LENGTH_LONG).show()
                return@launch
            }

            // Check if trying to register admin when one already exists
            if (selectedRole == UserRole.ADMIN) {
                val adminCount = database.userDao().getActiveAdminCount()
                if (adminCount > 0) {
                    Toast.makeText(this@RegisterActivity,
                        "An admin account already exists. Only one admin is allowed.",
                        Toast.LENGTH_LONG).show()
                    return@launch
                }
            }

            val hashedPassword = PasswordUtils.hashPassword(password)

            val newUser = User(
                email = email,
                password = hashedPassword,
                fullName = fullName,
                role = selectedRole,
                coins = 0, // No bonus coins on registration
                isVerified = true, // Auto-verify all users
                isActive = true, // Auto-activate all users
                isSuspended = false,
                suspensionCount = 0,
                bankAccount = null,
                bankName = null,
                accountName = null,
                ipAddress = null,
                deviceId = null,
                transactionId = if (selectedRole == UserRole.ADVERTISER) transactionId else null,
                cnic = if (selectedRole == UserRole.ADVERTISER) cnic else null,
                accountTitle = if (selectedRole == UserRole.ADVERTISER) accountTitle else null,
                accountNumber = if (selectedRole == UserRole.ADVERTISER) accountNumber else null,
                paymentVerified = false // Admin needs to verify advertiser payment
            )

            try {
                val userId = database.userDao().insertUser(newUser).toInt()

                database.activityLogDao().insertLog(
                    ActivityLog(
                        userId = userId,
                        action = "USER_REGISTERED",
                        details = "New user registered: $fullName ($email) as ${selectedRole.name}",
                        isSuspicious = false,
                        fraudScore = 0f
                    )
                )

                val welcomeMessage = when (selectedRole) {
                    UserRole.ADMIN -> "Welcome! You are the platform administrator."
                    UserRole.USER -> "Welcome! You can now start completing tasks to earn coins."
                    UserRole.ADVERTISER -> "Welcome! Your account is created. Admin will verify your payment and add bonus coins."
                }

                Toast.makeText(this@RegisterActivity,
                    "Registration successful!\n$welcomeMessage",
                    Toast.LENGTH_LONG).show()

                startActivity(android.content.Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity,
                    "Registration failed: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.etEmail.text.toString().isNotEmpty() ||
            binding.etFullName.text.toString().isNotEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exit Registration")
                .setMessage("Are you sure you want to exit? Your progress will be lost.")
                .setPositiveButton("Exit") { _, _ -> finish() }
                .setNegativeButton("Continue", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
