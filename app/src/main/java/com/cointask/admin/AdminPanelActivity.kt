package com.cointask.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cointask.databinding.ActivityAdminPanelBinding

class AdminPanelActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminPanelBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.tvTotalUsers.text = "1,234"
        binding.tvTotalAdvertisers.text = "89"
        binding.tvTotalTasks.text = "567"
        binding.tvTotalRevenue.text = "$12,345"
        binding.tvPlatformCommission.text = "$1,234"
        binding.tvSuspiciousActivities.text = "12"
        
        binding.btnPlatformSettings.setOnClickListener {
            Toast.makeText(this, "Platform Settings coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
