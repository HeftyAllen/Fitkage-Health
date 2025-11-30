package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitkagehealth.databinding.ItemSelectedExerciseBinding
import com.example.fitkagehealth.model.ExercisePlan

class SelectedExercisesAdapter(
    private val onRemoveClick: (ExercisePlan) -> Unit,
    private val onSettingsClick: (ExercisePlan) -> Unit
) : ListAdapter<ExercisePlan, SelectedExercisesAdapter.SelectedExerciseViewHolder>(
    ExercisePlanDiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedExerciseViewHolder {
        val binding = ItemSelectedExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SelectedExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectedExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SelectedExerciseViewHolder(
        private val binding: ItemSelectedExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercisePlan: ExercisePlan) {
            binding.exerciseName.text = exercisePlan.exercise.name
            binding.setsText.text = "${exercisePlan.sets} sets"
            binding.repsText.text = "${exercisePlan.reps} reps"
            binding.restText.text = "${exercisePlan.restSeconds}s rest"

            Glide.with(binding.root.context)
                .load(exercisePlan.exercise.gifUrl)
                .into(binding.exerciseGif)

            binding.settingsButton.setOnClickListener {
                onSettingsClick(exercisePlan)
            }

            binding.removeButton.setOnClickListener {
                onRemoveClick(exercisePlan)
            }
        }
    }
}
