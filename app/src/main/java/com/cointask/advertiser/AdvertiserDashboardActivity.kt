package com.cointask.advertiser

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cointask.databinding.ActivityAdvertiserDashboardBinding

class AdvertiserDashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdvertiserDashboardBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvertiserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.tvAdvertiserName.text = "Welcome Advertiser!"
        binding.tvTotalSpent.text = "$1,250"
        binding.tvTotalTasks.text = "45"
        binding.tvCompletedTasks.text = "32"
        binding.tvRemainingBudget.text = "$2,750"
        binding.tvRoi.text = "142%"
        
        binding.fabCreateCampaign.setOnClickListener {
            Toast.makeText(this, "Create Campaign feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
