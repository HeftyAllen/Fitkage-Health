package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitkagehealth.databinding.ItemCustomWorkoutBinding
import com.example.fitkagehealth.model.WorkoutPlan

class CustomWorkoutsAdapter(
    private val onWorkoutClick: (WorkoutPlan) -> Unit,
    private val onWorkoutDelete: (WorkoutPlan) -> Unit
) : ListAdapter<WorkoutPlan, CustomWorkoutsAdapter.ViewHolder>(WorkoutPlanDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomWorkoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCustomWorkoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workoutPlan: WorkoutPlan) {
            binding.workoutName.text = workoutPlan.name
            binding.exerciseCount.text = "${workoutPlan.exercises.size} exercises"
            binding.difficultyText.text = workoutPlan.difficulty

            binding.root.setOnClickListener {
                onWorkoutClick(workoutPlan)
            }

            binding.deleteButton.setOnClickListener {
                onWorkoutDelete(workoutPlan)
            }
        }
    }

    companion object {
        private val WorkoutPlanDiffCallback = object : DiffUtil.ItemCallback<WorkoutPlan>() {
            override fun areItemsTheSame(oldItem: WorkoutPlan, newItem: WorkoutPlan): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WorkoutPlan, newItem: WorkoutPlan): Boolean =
                oldItem == newItem
        }
    }
}