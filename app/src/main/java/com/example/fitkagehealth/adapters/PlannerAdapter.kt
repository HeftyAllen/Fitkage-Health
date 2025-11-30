package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitkagehealth.databinding.ItemPlannerExerciseBinding
import com.example.fitkagehealth.model.ExercisePlan

class PlannerAdapter(
    private var exercisePlans: List<ExercisePlan>,
    private val onExerciseUpdated: (ExercisePlan) -> Unit = {}
) : RecyclerView.Adapter<PlannerAdapter.PlannerViewHolder>() {

    fun updateExercises(newExercisePlans: List<ExercisePlan>) {
        this.exercisePlans = newExercisePlans
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlannerViewHolder {
        val binding = ItemPlannerExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlannerViewHolder, position: Int) {
        holder.bind(exercisePlans[position])
    }

    override fun getItemCount(): Int = exercisePlans.size

    inner class PlannerViewHolder(
        private val binding: ItemPlannerExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercisePlan: ExercisePlan) {
            // Set basic exercise info
            binding.exerciseName.text = exercisePlan.exercise.name
            binding.bodyPartText.text = exercisePlan.exercise.bodyPart ?: "Full Body"
            binding.setsText.text = exercisePlan.sets.toString()
            binding.repsText.text = exercisePlan.reps.toString()
            binding.restTimeText.text = "Rest: ${exercisePlan.restTime}s between sets"

            // Load GIF with Glide
            Glide.with(binding.root.context)
                .load(exercisePlan.exercise.gifUrl)
                .into(binding.exerciseGif)

            // Set checkbox state
            binding.checkbox.isChecked = exercisePlan.sets > 0

            // Checkbox listener
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                exercisePlan.sets = if (isChecked) 3 else 0
                binding.setsText.text = exercisePlan.sets.toString()
                updateRestTimeText(exercisePlan)
                onExerciseUpdated(exercisePlan)
            }

            // Sets controls
            binding.addSetBtn.setOnClickListener {
                exercisePlan.sets++
                binding.setsText.text = exercisePlan.sets.toString()
                binding.checkbox.isChecked = true
                updateRestTimeText(exercisePlan)
                onExerciseUpdated(exercisePlan)
            }

            binding.removeSetBtn.setOnClickListener {
                if (exercisePlan.sets > 0) {
                    exercisePlan.sets--
                    binding.setsText.text = exercisePlan.sets.toString()
                    binding.checkbox.isChecked = exercisePlan.sets > 0
                    updateRestTimeText(exercisePlan)
                    onExerciseUpdated(exercisePlan)
                }
            }

            // Reps controls
            binding.addRepBtn.setOnClickListener {
                exercisePlan.reps++
                binding.repsText.text = exercisePlan.reps.toString()
                onExerciseUpdated(exercisePlan)
            }

            binding.removeRepBtn.setOnClickListener {
                if (exercisePlan.reps > 1) {
                    exercisePlan.reps--
                    binding.repsText.text = exercisePlan.reps.toString()
                    onExerciseUpdated(exercisePlan)
                }
            }
        }

        private fun updateRestTimeText(exercisePlan: ExercisePlan) {
            binding.restTimeText.text = "Rest: ${exercisePlan.restTime}s between sets"
        }
    }
}