package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitkagehealth.R
import com.example.fitkagehealth.databinding.ItemExerciseSummaryBinding
import com.example.fitkagehealth.model.ExercisePlan

class WorkoutSummaryAdapter(
    private val exercises: List<ExercisePlan>
) : RecyclerView.Adapter<WorkoutSummaryAdapter.WorkoutSummaryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutSummaryViewHolder {
        val binding = ItemExerciseSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkoutSummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutSummaryViewHolder, position: Int) {
        holder.bind(exercises[position])
    }

    override fun getItemCount(): Int = exercises.size

    inner class WorkoutSummaryViewHolder(
        private val binding: ItemExerciseSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercisePlan: ExercisePlan) {
            binding.exerciseName.text = exercisePlan.exercise.name
            binding.setsRepsText.text = "${exercisePlan.sets} x ${exercisePlan.reps}"

            if (exercisePlan.completed) {
                binding.statusText.text = "Completed"
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle)
            } else {
                binding.statusText.text = "Skipped"
                binding.statusIcon.setImageResource(R.drawable.ic_remove_circle)
            }

            Glide.with(binding.root.context)
                .load(exercisePlan.exercise.gifUrl)
                .into(binding.exerciseGif)
        }
    }
}

// Alternative version using ListAdapter for better performance
class WorkoutSummaryListAdapter :
    ListAdapter<ExercisePlan, WorkoutSummaryListAdapter.WorkoutSummaryViewHolder>(ExercisePlanDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutSummaryViewHolder {
        val binding = ItemExerciseSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkoutSummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutSummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkoutSummaryViewHolder(
        private val binding: ItemExerciseSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercisePlan: ExercisePlan) {
            binding.exerciseName.text = exercisePlan.exercise.name
            binding.setsRepsText.text = "${exercisePlan.sets} x ${exercisePlan.reps}"

            if (exercisePlan.completed) {
                binding.statusText.text = "Completed"
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle)
            } else {
                binding.statusText.text = "Skipped"
                binding.statusIcon.setImageResource(R.drawable.ic_remove_circle)
            }

            Glide.with(binding.root.context)
                .load(exercisePlan.exercise.gifUrl)
                .into(binding.exerciseGif)
        }
    }
}
