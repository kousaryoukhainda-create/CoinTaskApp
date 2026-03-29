package com.cointask.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cointask.data.database.AppDatabase
import com.cointask.data.models.User
import com.cointask.data.models.UserRole
import com.cointask.databinding.ActivityRegisterBinding
import com.cointask.utils.PasswordUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val fullName = binding.etFullName.text.toString().trim()

            if (validateInputs(email, password, confirmPassword, fullName)) {
                performRegistration(email, password, fullName)
            }
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String
    ): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password required"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name required"
            return false
        }
        return true
    }

    private fun performRegistration(email: String, password: String, fullName: String) {
        lifecycleScope.launch {
            val existingUser = database.userDao().getUserByEmail(email)
            
            if (existingUser != null) {
                Toast.makeText(this@RegisterActivity,
                    "Email already registered", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Hash the password before storing
            val hashedPassword = PasswordUtils.hashPassword(password)

            val newUser = User(
                email = email,
                password = hashedPassword,
                fullName = fullName,
                role = UserRole.USER,
                coins = 0,
                isVerified = false
            )

            try {
                database.userDao().insertUser(newUser)
                Toast.makeText(this@RegisterActivity,
                    "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity,
                    "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
