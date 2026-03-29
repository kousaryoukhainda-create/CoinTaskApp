package com.cointask.data.models

data class TaskFilter(
    val type: TaskType? = null,
    val minReward: Int = 0,
    val maxReward: Int = Int.MAX_VALUE,
    val status: TaskStatus = TaskStatus.ACTIVE
)
