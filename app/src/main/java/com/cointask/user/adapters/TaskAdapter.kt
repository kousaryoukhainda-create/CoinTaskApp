package com.cointask.user.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cointask.data.models.Task
import com.cointask.data.models.TaskType
import com.cointask.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val context: android.content.Context
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var taskClickListener: TaskClickListener? = null

    interface TaskClickListener {
        fun onTaskClick(task: Task)
        fun onTaskComplete(task: Task)
    }

    fun setTaskClickListener(listener: TaskClickListener) {
        taskClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), taskClickListener)
    }

    class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task, listener: TaskClickListener?) {
            binding.apply {
                tvTitle.text = task.title
                tvDescription.text = task.description
                tvReward.text = "+${task.rewardCoins} coins"

                val progress = (task.completedCount.toFloat() / task.totalCapacity * 100).toInt()
                progressTask.progress = progress
                tvProgress.text = "$progress% completed (${task.completedCount}/${task.totalCapacity})"

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvExpires.text = "Expires: ${dateFormat.format(Date(task.expiresAt))}"

                setTaskIcon(task.taskType)

                root.setOnClickListener { listener?.onTaskClick(task) }

                // Animation
                root.alpha = 0f
                root.translationY = 50f
                root.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setStartDelay(adapterPosition * 50L)
                    .start()
            }
        }

        private fun setTaskIcon(type: TaskType) {
            val icon = when (type) {
                TaskType.WATCH_VIDEO -> "▶️"
                TaskType.VISIT_SITE -> "🌐"
                TaskType.LIKE_CONTENT -> "❤️"
                TaskType.SHARE_POST -> "📤"
                TaskType.FOLLOW_ACCOUNT -> "➕"
                TaskType.COMMENT -> "💬"
                TaskType.SURVEY -> "📊"
            }
            binding.ivTaskIcon.text = icon
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
