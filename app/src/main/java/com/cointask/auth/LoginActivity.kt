package com.cointask.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cointask.admin.AdminPanelActivity
import com.cointask.advertiser.AdvertiserDashboardActivity
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.UserRole
import com.cointask.databinding.ActivityLoginBinding
import com.cointask.user.UserDashboardActivity
import com.cointask.utils.PreferencesManager
import com.cointask.utils.PasswordUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        
        if (preferencesManager.isLoggedIn()) {
            navigateBasedOnRole(preferencesManager.getUserRole())
            return
        }
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                performLogin(email, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }
    
    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email required"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password required"
            return false
        }
        return true
    }
    
    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            val user = database.userDao().getUserByEmail(email)

            if (user != null && user.isActive) {
                if (user.isSuspended) {
                    Toast.makeText(this@LoginActivity,
                        "Account suspended. Contact support.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Verify password using BCrypt
                val passwordValid = PasswordUtils.verifyPassword(password, user.password)

                if (!passwordValid) {
                    Toast.makeText(this@LoginActivity,
                        "Invalid credentials", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                database.userDao().updateLastLogin(user.id)
                preferencesManager.saveUserSession(user.id, user.email, user.role)

                Toast.makeText(this@LoginActivity,
                    "Welcome ${user.fullName}!", Toast.LENGTH_SHORT).show()
                navigateBasedOnRole(user.role)
            } else {
                if (user != null && !user.isActive) {
                    Toast.makeText(this@LoginActivity,
                        "Account is not active. Contact admin.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@LoginActivity,
                        "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun navigateBasedOnRole(role: UserRole) {
        val intent = when (role) {
            UserRole.USER -> Intent(this, UserDashboardActivity::class.java)
            UserRole.ADVERTISER -> Intent(this, AdvertiserDashboardActivity::class.java)
            UserRole.ADMIN -> Intent(this, AdminPanelActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun showForgotPasswordDialog() {
        val emailInput = android.widget.EditText(this).apply {
            hint = "Enter your email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email address and we'll send you instructions to reset your password.\n\nNote: In this local version, you'll need to contact an admin to reset your password.")
            .setView(emailInput)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this@LoginActivity,
                        "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check if email exists
                lifecycleScope.launch {
                    val user = database.userDao().getUserByEmail(email)
                    if (user != null) {
                        // In a production app, this would send an email
                        // For now, show a dialog with options
                        showPasswordResetOptions(user)
                    } else {
                        Toast.makeText(this@LoginActivity,
                            "No account found with this email", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPasswordResetOptions(user: com.cointask.data.models.User) {
        val options = arrayOf(
            "Generate Temporary Password",
            "Contact Admin for Reset",
            "Security Question Reset"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Password Reset Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> generateTempPassword(user)
                    1 -> contactAdminReset(user)
                    2 -> securityQuestionReset(user)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateTempPassword(user: com.cointask.data.models.User) {
        // Generate a temporary password
        val tempPassword = "Temp${(1000..9999).random()}"
        val hashedPassword = PasswordUtils.hashPassword(tempPassword)

        lifecycleScope.launch {
            val updatedUser = user.copy(password = hashedPassword)
            database.userDao().updateUser(updatedUser)

            // Log the password reset
            database.activityLogDao().insertLog(
                com.cointask.data.models.ActivityLog(
                    userId = user.id,
                    action = "PASSWORD_RESET",
                    details = "Password reset via temporary password generation"
                )
            )

            androidx.appcompat.app.AlertDialog.Builder(this@LoginActivity)
                .setTitle("Temporary Password Generated")
                .setMessage("Your temporary password is:\n\n$tempPassword\n\nPlease login with this password and change it immediately from your profile settings.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun contactAdminReset(user: com.cointask.data.models.User) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Contact Admin")
            .setMessage("To reset your password, please contact the system administrator.\n\nAdmin Email: admin@cointask.com\nSupport: support@cointask.com")
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy Email") { _, _ ->
                // In a real app, copy to clipboard
                Toast.makeText(this@LoginActivity, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun securityQuestionReset(user: com.cointask.data.models.User) {
        // In a production app, this would use security questions
        // For now, show a message
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Security Questions")
            .setMessage("Security questions feature coming soon.\n\nFor now, please use the temporary password option or contact admin.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit CoinTask?")
            .setPositiveButton("Exit") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Stay", null)
            .show()
    }
}
