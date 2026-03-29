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
                // Verify password using BCrypt
                val passwordValid = PasswordUtils.verifyPassword(password, user.password)
                
                if (!passwordValid) {
                    Toast.makeText(this@LoginActivity,
                        "Invalid credentials", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                if (user.isSuspended) {
                    Toast.makeText(this@LoginActivity,
                        "Account suspended. Contact support.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                database.userDao().updateLastLogin(user.id)
                preferencesManager.saveUserSession(user.id, user.email, user.role)

                Toast.makeText(this@LoginActivity,
                    "Welcome ${user.fullName}!", Toast.LENGTH_SHORT).show()
                navigateBasedOnRole(user.role)
            } else {
                Toast.makeText(this@LoginActivity,
                    "Invalid credentials", Toast.LENGTH_SHORT).show()
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
}
