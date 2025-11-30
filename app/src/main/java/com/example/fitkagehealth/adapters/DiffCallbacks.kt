package com.example.fitkagehealth.adapters

import androidx.recyclerview.widget.DiffUtil
import com.example.fitkagehealth.model.Exercise
import com.example.fitkagehealth.model.ExercisePlan

// Shared diff callback for Exercise
object ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
    override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
        return oldItem == newItem
    }
}

// Shared diff callback for ExercisePlan
object ExercisePlanDiffCallback : DiffUtil.ItemCallback<ExercisePlan>() {
    override fun areItemsTheSame(oldItem: ExercisePlan, newItem: ExercisePlan): Boolean {
        return oldItem.exercise.id == newItem.exercise.id
    }

    override fun areContentsTheSame(oldItem: ExercisePlan, newItem: ExercisePlan): Boolean {
        return oldItem == newItem
    }
}
