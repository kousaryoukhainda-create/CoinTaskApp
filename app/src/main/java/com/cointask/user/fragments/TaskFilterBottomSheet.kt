package com.cointask.user.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.cointask.data.models.TaskFilter
import com.cointask.data.models.TaskType
import com.cointask.databinding.FragmentTaskFilterBinding

class TaskFilterBottomSheet(
    private val onFilterApplied: (TaskFilter) -> Unit
) : BottomSheetDialogFragment() {
    
    private var _binding: FragmentTaskFilterBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskFilterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupButtons()
    }
    
    private fun setupSpinners() {
        val taskTypes = listOf("All") + TaskType.values().map { it.name }
        val taskTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, taskTypes)
        binding.spinnerTaskType.adapter = taskTypeAdapter
        
        val rewardOptions = listOf("Any", "10+", "50+", "100+", "500+")
        val rewardAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, rewardOptions)
        binding.spinnerMinReward.adapter = rewardAdapter
    }
    
    private fun setupButtons() {
        binding.btnApplyFilter.setOnClickListener {
            val selectedType = when (binding.spinnerTaskType.selectedItem?.toString()) {
                "Watch Video" -> TaskType.WATCH_VIDEO
                "Visit Site" -> TaskType.VISIT_SITE
                "Like Content" -> TaskType.LIKE_CONTENT
                "Share Post" -> TaskType.SHARE_POST
                else -> null
            }
            
            val minReward = when (binding.spinnerMinReward.selectedItem?.toString()) {
                "10+" -> 10
                "50+" -> 50
                "100+" -> 100
                "500+" -> 500
                else -> 0
            }
            
            onFilterApplied(TaskFilter(type = selectedType, minReward = minReward))
            dismiss()
        }
        
        binding.btnClearFilter.setOnClickListener {
            onFilterApplied(TaskFilter())
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
